package com.flipkart.bifrost.protocol;

import com.flipkart.bifrost.framework.BifrostException;

public class ProtocolResponse<T> {
    private boolean successful = false;
    private T response;
    private BifrostException.ErrorCode errorCode;
    private String errorMessage;

    public ProtocolResponse(BifrostException.ErrorCode errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.successful = false;
    }

    public ProtocolResponse(T response) {
        this.response = response;
        this.successful = true;
    }

    public ProtocolResponse() {
    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

    public BifrostException.ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(BifrostException.ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
