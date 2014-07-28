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
        ProtocolRequest<T> request = mapper.readValue(body, new TypeReference<ProtocolRequest<T>>() {});
        ProtocolResponse protocolResponse = null;
        try {
            T response = request.getCallable().call();
            protocolResponse = new ProtocolResponse<T>(response);
        } catch (Exception e) {
            logger.error("Error: " + e);
            protocolResponse = new ProtocolResponse(BifrostException.ErrorCode.APPLICATION_ERROR,
                                        "Application level error: " + e.getMessage());
        }
        if(request.isResponseReturned()) {
            AMQP.BasicProperties replyProperties = new AMQP.BasicProperties.Builder()
                                                            .correlationId(properties.getCorrelationId())
                                                            .build();
            getChannel().basicPublish("", properties.getReplyTo(), replyProperties,
                                        mapper.writeValueAsBytes(protocolResponse));
        }
        getChannel().basicAck(envelope.getDeliveryTag(), false);

    }
}
