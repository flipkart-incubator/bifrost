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

package com.flipkart.bifrost.framework;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * A connection to the RabbitMQ cluster. Supports authenticated connections.
 */
public class Connection {
    private static final Logger logger = Logger.getLogger(Connection.class.getSimpleName());
    private com.rabbitmq.client.Connection connection;
    private List<String> hosts;
    private String username;
    private String password;

    /**
     * An un-authenticated connection to the cluster.
     * The {@link Connection#start()} method needs to be called to make this connection active.
     * @param hosts List of hostnames in the cluster.
     */
    public Connection(List<String> hosts) {
        this.hosts = hosts;
    }

    /**
     * An authenticated connection to the cluster.
     * The {@link Connection#start()} method needs to be called to make this connection active.
     * @param hosts List of hostnames in the cluster.
     * @param username Username to be used to connect.
     * @param password Password for the user.
     */
    public Connection(List<String> hosts, String username, String password) {
        this.hosts = hosts;
        this.username = username;
        this.password = password;
    }

    /**
     * Start the connection. This will try to connect to the cluster hosts provided in the contructor.
     * Will throw an error in case of failure.
     * @throws BifrostException in case of connection failure.
     */
    public void start() throws BifrostException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        Address brokers[] = new Address[hosts.size()];
        int i=0;
        for(String member : hosts) {
            brokers[i++] = new Address(member);
        }
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
        try {
            connection = connectionFactory.newConnection(brokers);
        } catch (IOException e) {
            throw new BifrostException(BifrostException.ErrorCode.IO_ERROR, "Could not connect", e);
        }
    }

    /**
     * Stop the Connection.
     * @throws Exception in case of failure.
     */
    public void stop() throws Exception {
        connection.close();
    }

    /**
     * Get a handle to the raw connection. Mostly this is for internal usage.
     */
    public com.rabbitmq.client.Connection getConnection() {
        return connection;
    }

}
