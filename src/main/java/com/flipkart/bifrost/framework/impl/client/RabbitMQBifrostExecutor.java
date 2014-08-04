/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.bifrost.framework.impl.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.bifrost.framework.*;
import com.flipkart.bifrost.protocol.ProtocolRequest;
import com.flipkart.bifrost.protocol.ProtocolResponse;
import com.flipkart.bifrost.rabbitmq.Connection;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RabbitMQBifrostExecutor<T> extends BifrostExecutor<T> {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQBifrostExecutor.class.getSimpleName());

    private static final class ReplyListener<T> extends DefaultConsumer {
        private ConcurrentMap<String, CallResultWaiter<T>> waiters;
        private ObjectMapper mapper;

        /**
         * Constructs a new instance and records its association to the passed-in channel.
         *
         * @param channel the channel to which this consumer is attached
         */
        public ReplyListener(Channel channel, ConcurrentMap<String, CallResultWaiter<T>> waiters, ObjectMapper mapper) {
            super(channel);
            this.waiters = waiters;
            this.mapper = mapper;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
            String correlationId = properties.getCorrelationId();
            try {
                CallResultWaiter<T> waiter = waiters.get(correlationId);
                if(null != waiter) {
                    waiter.setResult(mapper.<ProtocolResponse<T>>readValue(
                                            body, new TypeReference<ProtocolResponse<T>>() {}));
                }
            } catch (Exception e) {
                logger.error("Error handling incoming message: ", e);
            } finally {
                try {
                    getChannel().basicAck(envelope.getDeliveryTag(), false);     //TODO::Policy
                } catch (Exception e) {
                    logger.error("Error acking: ", e);
                }
            }
            waiters.remove(correlationId);
        }


    }

    private static final class CallResultWaiter<T> implements Callable<ProtocolResponse<T>> {
        private ProtocolResponse<T> result = null;
        private Lock lock = new ReentrantLock();
        private Condition completed = lock.newCondition();
        private String correlationId;
        private long requestTimeout = 5000;
        private boolean done = false;
        private ConcurrentMap<String, CallResultWaiter<T>> waiters;

        public CallResultWaiter(String correlationId, long requestTimeout,
                                    ConcurrentMap<String, CallResultWaiter<T>> waiters) {
            this.correlationId = correlationId;
            this.requestTimeout = requestTimeout;
            this.waiters = waiters;
        }

        @Override
        public ProtocolResponse<T> call() throws Exception {
            lock.lock();
            try {
                while (!done) {
                    if(!completed.await(requestTimeout, TimeUnit.MILLISECONDS)) {
                        logger.error(String.format("Request %s dropped after timeout of %d ms expired",
                                                                                correlationId, requestTimeout));
                        done = true;
                        waiters.remove(correlationId);
                    }
                }
            }
            finally {
                lock.unlock();
            }
            return result;
        }

        public void setResult(ProtocolResponse<T> result) {
            lock.lock();
            try {
                this.result = result;
                done = true;
                completed.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    private ConcurrentMap<String, CallResultWaiter<T>> waiters = Maps.newConcurrentMap();
    private List<ReplyListener<T>> listeners = Lists.newArrayList();
    private Channel publishChannel;
    private ExecutorService executorService;
    private ObjectMapper objectMapper;
    private String requestQueue;
    private String replyQueueName;
    private long responseWaitTimeout;

    RabbitMQBifrostExecutor(Connection connection, ExecutorService executorService, int concurrency,
                            ObjectMapper objectMapper, String requestQueue, String replyQueueName, long responseWaitTimeout) throws Exception {
        if(null == connection || null == connection.getConnection()) {
            throw new BifrostException(BifrostException.ErrorCode.INVALID_CONNECTION,
                                       "The provided connection is invalid. Call start() on connection to start it.");
        }
        this.executorService = executorService;
        this.objectMapper = objectMapper;
        this.requestQueue = requestQueue;
        this.replyQueueName = replyQueueName;
        this.publishChannel = connection.getConnection().createChannel();
        this.responseWaitTimeout = responseWaitTimeout;
        AMQP.Queue.DeclareOk requestQueueResponse = publishChannel.queueDeclare(requestQueue, true, false, false,
                Collections.<String, Object>singletonMap("x-ha-policy", "all"));
        AMQP.Queue.DeclareOk replyQueue = publishChannel.queueDeclare(replyQueueName, true, false, false,
                Collections.<String, Object>singletonMap("x-ha-policy", "all"));
        for(int i = 0; i < concurrency; i++) {
            Channel channel = connection.getConnection().createChannel();
            channel.basicQos(1);
            ReplyListener<T> listener = new ReplyListener<T>(channel, waiters, objectMapper);
            channel.basicConsume(replyQueueName, listener);
            listeners.add(listener);
        }
    }

    @Override
    public Future<T> submit(RemoteCallable<T> callable) throws BifrostException {
        final String correlationId = UUID.randomUUID().toString();
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                                                .correlationId(correlationId)
                                                .replyTo(replyQueueName)
                                                .build();
        CallResultWaiter<T> waiter = new CallResultWaiter<>(correlationId, responseWaitTimeout, waiters);
        waiters.put(correlationId, waiter);
        Future<ProtocolResponse<T>> dataContainerFuture = executorService.submit(waiter);
        try {
            publishChannel.basicPublish("", requestQueue, properties,
                                            objectMapper.writeValueAsBytes(new ProtocolRequest<>(callable, true)));
        } catch (Exception e) {
            logger.error("Error occurred while submitting job: ", e);
            waiters.get(correlationId).setResult(new ProtocolResponse<T>(BifrostException.ErrorCode.IO_ERROR, "Message publication error"));
        }
        return new BifrostFuture<>(dataContainerFuture);
    }

    @Override
    public void submit(RemoteTask<T> task) throws BifrostException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().build();
        try {
            publishChannel.basicPublish("", requestQueue, properties, objectMapper.writeValueAsBytes(task.getCallable()));
        } catch (Exception e) {
            logger.error("Error publishing: ", e);
            throw new BifrostException(BifrostException.ErrorCode.SERIALIZATION_ERROR, "Could not serialize request");
        }
    }

    @Override
    public void shutdown() throws BifrostException {
        try {
            publishChannel.close();
            for(ReplyListener<T> listener : listeners) {
                Channel channel = listener.getChannel();
                channel.basicCancel(listener.getConsumerTag());
                channel.close();
            }
        } catch (Exception e) {
            logger.error("Error publishing: ", e);
        }
    }
}
