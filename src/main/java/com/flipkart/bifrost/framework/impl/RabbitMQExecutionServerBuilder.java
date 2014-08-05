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
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 */
public class RabbitMQExecutionServerBuilder<T> {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQExecutionServerBuilder.class);

    private ObjectMapper objectMapper;
    private Connection connection;
    private int concurrency;
    private String requestQueue;
    private Class<? extends RemoteCallable> callerSubType;

    protected RabbitMQExecutionServerBuilder(Class<? extends RemoteCallable> callerSubType) {
        this.callerSubType = callerSubType;
    }

    /**
     * Set the connection to the cluster. Build will fail with an exception if this is not provided.
     * @param connection The started connection to the cluster.
     * @return This builder.
     */
    public RabbitMQExecutionServerBuilder<T> connection(Connection connection) {
        this.connection = connection;
        return this;
    }

    /**
     * The name of the request queue. Build method will fail if this is not provided.
     * @param requestQueue A non-null queue name.
     * @return
     */
    public RabbitMQExecutionServerBuilder<T> requestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    /**
     * Jackson ObjectMapper to be used for serialization and de-serialization of the {@link com.flipkart.bifrost.framework.RemoteCallable} object.
     * If this is not provided, it will be set to a default initialized ObjectMapper.
     * @param objectMapper A valid initialized ObjectMapper.
     * @return This builder.
     */
    public RabbitMQExecutionServerBuilder<T> objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * The number of threads used to execute the {@link com.flipkart.bifrost.framework.RemoteCallable} requests from clients.
     * This is set to 10 by default.
     * @param concurrency A non-zero positive number.
     * @return
     */
    public RabbitMQExecutionServerBuilder<T> concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public RabbitMQBifrostRemoteCallExecutionServer<T> build() throws Exception {
        if(null == connection) {
            throw new IllegalArgumentException("Call connection() to set a connection.");
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
        if(Strings.isNullOrEmpty(requestQueue)) {
            throw new IllegalArgumentException("Set request queue name");
        }

        return new RabbitMQBifrostRemoteCallExecutionServer<T>(connection, objectMapper, concurrency, requestQueue);
    }
}
