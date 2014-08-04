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

package com.flipkart.bifrost.framework.impl.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.bifrost.framework.BifrostException;
import com.flipkart.bifrost.protocol.ProtocolRequest;
import com.flipkart.bifrost.protocol.ProtocolResponse;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class RabbitMQExecutionServerListener<T> extends DefaultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQExecutionServerListener.class.getSimpleName());
    private ObjectMapper mapper;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public RabbitMQExecutionServerListener(Channel channel, ObjectMapper mapper) {
        super(channel);
        this.mapper = mapper;
    }

    @Override
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body) throws IOException {
        ProtocolResponse protocolResponse = null;
        ProtocolRequest<T> request = null;
        try {
            request = mapper.readValue(body, new TypeReference<ProtocolRequest<T>>() {});
            T response = request.getCallable().call();
            protocolResponse = new ProtocolResponse<T>(response);
        } catch (Exception e) {
            logger.error("Error: " + e);
            protocolResponse = new ProtocolResponse(BifrostException.ErrorCode.APPLICATION_ERROR,
                                        "Application level error: " + e.getMessage());
        }
        try {
            if(null != request && request.isResponseReturned()) {
                AMQP.BasicProperties replyProperties = new AMQP.BasicProperties.Builder()
                        .correlationId(properties.getCorrelationId())
                        .build();
                if(null != properties.getReplyTo()) {
                    getChannel().basicPublish("", properties.getReplyTo(), replyProperties,
                        mapper.writeValueAsBytes(protocolResponse));
                }
            }
            getChannel().basicAck(envelope.getDeliveryTag(), false);

        } catch (Exception e) {
            logger.error("Error occurred returning: ", e);
        }

    }
}
