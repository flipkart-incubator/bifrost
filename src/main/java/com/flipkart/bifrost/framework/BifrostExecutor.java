package com.flipkart.bifrost.framework;

import com.flipkart.bifrost.framework.impl.client.RabbitMQBifrostExecutorBuilder;

import java.util.concurrent.Future;

public abstract class BifrostExecutor<T> {
    public static final class Builder<T> extends RabbitMQBifrostExecutorBuilder<T> {
        private Builder() {}
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    abstract public Future<T> submit(RemoteCallable<T> callable) throws BifrostException;
    abstract public void submit(RemoteTask<T> callable) throws BifrostException;
    abstract public void shutdown() throws BifrostException;
}
