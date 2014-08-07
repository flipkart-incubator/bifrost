package com.flipkart.bifrost.framework;

public class RemoteListenableTask<T> {
    private RemoteCallable<T> callable;
    private RemoteCallableCompletionListener<T> completionListener;

    public RemoteListenableTask(RemoteCallable<T> callable, RemoteCallableCompletionListener<T> completionListener) {
        this.callable = callable;
        this.completionListener = completionListener;
    }

    public RemoteCallable<T> getCallable() {
        return callable;
    }

    public RemoteCallableCompletionListener<T> getCompletionListener() {
        return completionListener;
    }
}
