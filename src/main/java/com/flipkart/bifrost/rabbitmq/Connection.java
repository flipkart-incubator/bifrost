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

package com.flipkart.bifrost.rabbitmq;

import com.flipkart.bifrost.framework.BifrostException;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;

public class Connection {
    private com.rabbitmq.client.Connection connection;
    private List<String> hosts;

    public Connection(List<String> hosts) throws BifrostException {
        this.hosts = hosts;
    }

    public void start() throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        Address brokers[] = new Address[hosts.size()];
        int i=0;
        for(String member : hosts) {
            brokers[i++] = new Address(member);
        }
        try {
            connection = connectionFactory.newConnection(
                    //Executors.newFixedThreadPool(configuration.getNumExecutorThreads()),
                    brokers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() throws Exception {
        connection.close();
    }

    public com.rabbitmq.client.Connection getConnection() {
        return connection;
    }

}
