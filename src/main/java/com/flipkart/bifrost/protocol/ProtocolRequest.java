package com.flipkart.bifrost.protocol;

import com.flipkart.bifrost.framework.RemoteCallable;

public class ProtocolRequest<T> {
    private boolean responseReturned = true;
    private RemoteCallable<T> callable;

    public ProtocolRequest(RemoteCallable<T> callable, boolean responseReturned) {
        this.callable = callable;
        this.responseReturned = responseReturned;
    }

    public ProtocolRequest() {
    }

    public boolean isResponseReturned() {
        return responseReturned;
    }

    public void setResponseReturned(boolean responseReturned) {
        this.responseReturned = responseReturned;
    }

    public RemoteCallable<T> getCallable() {
        return callable;
    }

    public void setCallable(RemoteCallable<T> callable) {
        this.callable = callable;
    }
}
