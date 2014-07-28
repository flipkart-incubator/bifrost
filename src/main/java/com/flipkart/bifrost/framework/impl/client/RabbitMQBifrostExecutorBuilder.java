package com.flipkart.bifrost.framework.impl.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.bifrost.rabbitmq.Connection;
import com.flipkart.bifrost.framework.BifrostExecutor;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitMQBifrostExecutorBuilder<T> {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQBifrostExecutorBuilder.class.getSimpleName());

    private ExecutorService executorService;
    private ObjectMapper objectMapper;
    private int concurrency = 0;
    private String requestQueue;
    private String responseQueue;
    private Connection connection;

    protected RabbitMQBifrostExecutorBuilder()  {

    }

    public RabbitMQBifrostExecutorBuilder<T> connection(Connection connection) throws Exception {
        this.connection = connection;
        return this;
    }

    public RabbitMQBifrostExecutorBuilder<T> executorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public RabbitMQBifrostExecutorBuilder<T> objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public RabbitMQBifrostExecutorBuilder<T> concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public RabbitMQBifrostExecutorBuilder<T> requestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    public RabbitMQBifrostExecutorBuilder<T> responseQueue(String responseQueue) {
        this.responseQueue = responseQueue;
        return this;
    }

    public BifrostExecutor<T> build() throws Exception {
        if(null == connection) {
            throw new IllegalArgumentException("Call connection() to set a connection.");
        }
        if(null == executorService) {
            executorService = Executors.newCachedThreadPool();
            logger.warn("Thread pool set to cached thread pool.");
        }
        if(null == objectMapper) {
            objectMapper = new ObjectMapper();
            logger.warn("ObjectMapper is set to default");
        }
        if(0 == concurrency) {
            concurrency = (0 == concurrency)? 10 : concurrency;
            logger.warn("No concurrency specified. Setting to 10.");
        }
        if(Strings.isNullOrEmpty(requestQueue)) {
            throw new IllegalArgumentException("Set request queue name");
        }
        if(Strings.isNullOrEmpty(responseQueue)) {
            throw new IllegalArgumentException("Set response queue name");
        }
        return new RabbitMQBifrostExecutor<T>(connection, executorService, concurrency,
                                        objectMapper, requestQueue, responseQueue);
    }
}
