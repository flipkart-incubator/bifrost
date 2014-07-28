package com.flipkart.bifrost.framework;

public class RemoteTask<T> {
    private final RemoteCallable<T> callable;

    public RemoteTask(RemoteCallable<T> callable) {
        this.callable = callable;
    }

    public RemoteCallable<T> getCallable() {
        return callable;
    }
}
