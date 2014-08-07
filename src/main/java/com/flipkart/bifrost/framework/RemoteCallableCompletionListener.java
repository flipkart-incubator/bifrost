package com.flipkart.bifrost.framework;

public interface RemoteCallableCompletionListener<T> {
    public void onComplete(RemoteCallable<T> callable, T data);
    public void onError(RemoteCallable<T> callable, BifrostException e);
}
