package com.flipkart.bifrost.framework;

import com.flipkart.bifrost.framework.BifrostException;
import com.flipkart.bifrost.protocol.ProtocolResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BifrostFuture<V> implements Future<V> {
    private Future<ProtocolResponse<V>> mainFuture;

    public BifrostFuture(Future<ProtocolResponse<V>> dataContainerFuture) {
        this.mainFuture = dataContainerFuture;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mainFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mainFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mainFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        ProtocolResponse<V> response = mainFuture.get();
        if(response.isSuccessful()) {
            return response.getResponse();
        }
        throw new ExecutionException("Error running api call",
                        new BifrostException(response.getErrorCode(), response.getErrorMessage()));
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        ProtocolResponse<V> response = mainFuture.get(timeout, unit);
        if(null != response.getResponse()) {
            return response.getResponse();
        }
        throw new ExecutionException("Error running api call",
                new BifrostException(response.getErrorCode(), response.getErrorMessage()));
    }
}
