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
import com.flipkart.bifrost.framework.BifrostException;
import com.flipkart.bifrost.framework.BifrostRemoteCallExecutionServer;
import com.flipkart.bifrost.framework.Connection;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.List;

class RabbitMQBifrostRemoteCallExecutionServer<T> extends BifrostRemoteCallExecutionServer<T> {
    private List<RabbitMQExecutionServerListener<T>> listeners = Lists.newArrayList();
    private Connection connection;
    private ObjectMapper mapper;
    private int concurrency;
    private String requestQueue;

    RabbitMQBifrostRemoteCallExecutionServer(Connection connection, ObjectMapper mapper,
                                             int concurrency, String requestQueue) {
        this.connection = connection;
        this.mapper = mapper;
        this.concurrency = concurrency;
        this.requestQueue = requestQueue;
    }

    @Override
    public void start() throws BifrostException {
        try {
            if(null == connection || null == connection.getConnection()) {
                throw new BifrostException(BifrostException.ErrorCode.INVALID_CONNECTION,
                        "The provided connection is invalid. Call start() on connection to start it.");
            }
            for(int i = 1; i <= concurrency; i++) {
                Channel channel = connection.getConnection().createChannel();
                RabbitMQExecutionServerListener<T> listener = new RabbitMQExecutionServerListener<T>(
                                                                    channel, mapper);
                channel.basicQos(1);
                channel.basicConsume(requestQueue, listener);
                listeners.add(listener);
            }
        } catch (IOException e) {
            throw new BifrostException(BifrostException.ErrorCode.IO_ERROR, "Error registering listener", e);
        }
    }

    @Override
    public void stop() throws BifrostException {
        try {
            for(RabbitMQExecutionServerListener<T> listener : listeners) {
                if(!Strings.isNullOrEmpty(listener.getConsumerTag())) {
                    Channel channel = listener.getChannel();
                    channel.basicCancel(listener.getConsumerTag());
                    channel.close();
                }
            }
        } catch (IOException e) {
            throw new BifrostException(BifrostException.ErrorCode.IO_ERROR, "Error unregistering listener", e);
        }
    }
}
