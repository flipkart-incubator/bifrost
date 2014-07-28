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
