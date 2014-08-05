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

package com.flipkart.bifrost.framework.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.flipkart.bifrost.framework.RemoteCallable;
import com.flipkart.bifrost.framework.Connection;
import com.flipkart.bifrost.framework.BifrostExecutor;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@inheritDoc}
 */
public class RabbitMQBifrostExecutorBuilder<T> {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQBifrostExecutorBuilder.class.getSimpleName());

    private ExecutorService executorService;
    private ObjectMapper objectMapper;
    private int concurrency = 0;
    private String requestQueue;
    private String responseQueue;
    private Connection connection;
    private long responseWaitTimeout;
    private Class<? extends RemoteCallable> callerSubType;

    protected RabbitMQBifrostExecutorBuilder(Class<? extends RemoteCallable> callerSubType)  {
        this.callerSubType = callerSubType;
    }

    /**
     * Set the connection to the cluster. Build will fail with an exception if this is not provided.
     * @param connection The started connection to the cluster.
     * @return This builder.
     */
    public RabbitMQBifrostExecutorBuilder<T> connection(Connection connection) {
        this.connection = connection;
        return this;
    }

    /**
     * The name of the request queue. Build method will fail if this is not provided.
     * @param requestQueue A non-null queue name.
     * @return
     */
    public RabbitMQBifrostExecutorBuilder<T> requestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    /**
     * The name of the response queue. Build method will fail if this is not provided.
     * @param responseQueue A non-null queue name.
     * @return
     */
    public RabbitMQBifrostExecutorBuilder<T> responseQueue(String responseQueue) {
        this.responseQueue = responseQueue;
        return this;
    }

    /**
     * Set the executor service to be used to start waiter threads for the response.
     * Set to a unbounded cached thread pool in case this is not provided.
     * <b>Warning: </b> Cached thread pool will not work very well in high-load scenarios,
     * as it will fail to create new threads.
     * @param executorService A valid executor service.
     * @return This builder.
     */
    public RabbitMQBifrostExecutorBuilder<T> executorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Jackson ObjectMapper to be used for serialization and de-serialization of the {@link com.flipkart.bifrost.framework.RemoteCallable} object.
     * If this is not provided, it will be set to a default initialized ObjectMapper.
     * @param objectMapper A valid initialized ObjectMapper.
     * @return This builder.
     */
    public RabbitMQBifrostExecutorBuilder<T> objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * The allowed number of in-flight requests if using {@link com.flipkart.bifrost.framework.RemoteCallable}
     * objects to submit the job. This does not affect jobs submitted using {@link com.flipkart.bifrost.framework.RemoteTask}.
     * This is set to 10 by default.
     * @param concurrency Number of in-flight requests.
     * @return
     */
    public RabbitMQBifrostExecutorBuilder<T> concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    /**
     * The max time in milliseconds that the system will wait for a response, before failing with an exception.
     * This is set to 5000 by default.
     * @param responseWaitTimeout
     * @return
     */
    public RabbitMQBifrostExecutorBuilder<T> responseWaitTimeout(long responseWaitTimeout) {
        this.responseWaitTimeout = responseWaitTimeout;
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
        objectMapper.getSubtypeResolver().registerSubtypes(new NamedType(callerSubType));
        if(0 == concurrency) {
            concurrency = (0 >= concurrency)? 10 : concurrency;
            logger.warn("No concurrency specified. Setting to 10.");
        }
        if(0 == responseWaitTimeout) {
            responseWaitTimeout = 5000;
            logger.warn("No timeout specified, set to 5000ms");
        }
        if(Strings.isNullOrEmpty(requestQueue)) {
            throw new IllegalArgumentException("Set request queue name");
        }
        if(Strings.isNullOrEmpty(responseQueue)) {
            throw new IllegalArgumentException("Set response queue name");
        }
        return new RabbitMQBifrostExecutor<T>(connection, executorService, concurrency,
                                        objectMapper, requestQueue, responseQueue, responseWaitTimeout);
    }
}
