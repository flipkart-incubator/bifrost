package com.flipkart.bifrost.framework;

public class BifrostException extends Exception {
    public BifrostException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BifrostException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static enum ErrorCode {
        SERIALIZATION_ERROR,
        UNSUPPORTED_REQUEST_TYPE,
        IO_ERROR,
        SUCCESS_STATUS_MISMATCH,
        INVALID_PROTOCOL_RESPONSE,
        APPLICATION_ERROR
    }

    private final ErrorCode errorCode;
}
