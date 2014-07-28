package com.flipkart.bifrost.framework;

public interface BifrostExecutorBuilder<T> {
    public BifrostExecutor<T> build() throws Exception;
}
