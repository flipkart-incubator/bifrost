package com.flipkart.bifrost.framework.impl.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.bifrost.rabbitmq.Connection;

public class RabbitMQExecutionServerBuilder<T> {
    private ObjectMapper objectMapper;
    private Connection connection;
    private int concurrency;
    private String requestQueue;

    protected RabbitMQExecutionServerBuilder() {
    }

    public RabbitMQExecutionServerBuilder<T> objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public RabbitMQExecutionServerBuilder<T> connection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public RabbitMQExecutionServerBuilder<T> concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public RabbitMQExecutionServerBuilder<T> requestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    public RabbitMQRemoteCallExecutionServer<T> build() {
        return new RabbitMQRemoteCallExecutionServer<T>(connection, objectMapper, concurrency, requestQueue);
    }
}
