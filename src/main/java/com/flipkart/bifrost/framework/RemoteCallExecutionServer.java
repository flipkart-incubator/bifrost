package com.flipkart.bifrost.framework;

import com.flipkart.bifrost.framework.impl.server.RabbitMQExecutionServerBuilder;

public abstract class RemoteCallExecutionServer<T> {
    public static final class Builder<T> extends RabbitMQExecutionServerBuilder<T> {
        private Builder() {}
    }

    public static<T>  Builder<T> builder() {
        return new Builder<T>();
    }
    abstract public void start() throws BifrostException;
    abstract public void stop() throws BifrostException;
}
