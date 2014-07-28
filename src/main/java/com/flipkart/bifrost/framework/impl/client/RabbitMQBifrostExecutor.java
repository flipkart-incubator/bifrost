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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
                CallResultWaiter<T> waiter = waiters.remove(correlationId);
                if(null != waiter) {
                    waiter.setResult(mapper.<ProtocolResponse<T>>readValue(
                                            body, new TypeReference<ProtocolResponse<T>>() {}));
                }
            } catch (Exception e) {
                logger.error("Error handling incoming message: ", e);
            } finally {
                getChannel().basicAck(envelope.getDeliveryTag(), false);     //TODO::Policy
            }
        }


    }

    private static final class CallResultWaiter<T> implements Callable<ProtocolResponse<T>> {
        private ProtocolResponse<T> result = null;
        private Lock lock = new ReentrantLock();
        private Condition completed = lock.newCondition();
        private String correlationId;
        private boolean done = false;

        public CallResultWaiter(String correlationId) {
            this.correlationId = correlationId;
        }

        @Override
        public ProtocolResponse<T> call() throws Exception {
            lock.lock();
            try {
                while (!done) {
                    completed.awaitUninterruptibly();
                }
            } finally {
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
    private int concurrency;

    RabbitMQBifrostExecutor(Connection connection, ExecutorService executorService, int concurrency,
                            ObjectMapper objectMapper, String requestQueue, String replyQueueName) throws Exception {
        this.executorService = executorService;
        this.objectMapper = objectMapper;
        this.requestQueue = requestQueue;
        this.replyQueueName = replyQueueName;
        this.publishChannel = connection.getConnection().createChannel();
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
        CallResultWaiter<T> waiter = new CallResultWaiter<T>(correlationId);
        waiters.put(correlationId, waiter);
        Future<ProtocolResponse<T>> dataContainerFuture = executorService.submit(waiter);
        try {
            publishChannel.basicPublish("", requestQueue, properties, objectMapper.writeValueAsBytes(new ProtocolRequest<T>(callable, true)));
        } catch (IOException e) {
            throw new BifrostException(BifrostException.ErrorCode.SERIALIZATION_ERROR, "Could not serialize request");
        }
        return new BifrostFuture<>(dataContainerFuture);
    }

    @Override
    public void submit(RemoteTask<T> task) throws BifrostException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().build();
        try {
            publishChannel.basicPublish("", requestQueue, properties, objectMapper.writeValueAsBytes(task.getCallable()));
        } catch (IOException e) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
