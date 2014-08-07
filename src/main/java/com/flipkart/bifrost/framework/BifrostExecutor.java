/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.bifrost.framework;

import com.flipkart.bifrost.framework.impl.RabbitMQBifrostExecutorBuilder;

import java.util.concurrent.Future;

/**
 * The main client side executor to be used for executing tasks remotely.
 * @param <T>
 */
public abstract class BifrostExecutor<T> {
    public static final class Builder<T> extends RabbitMQBifrostExecutorBuilder<T> {
        private Builder(Class<? extends RemoteCallable> callerSubType) {
            super(callerSubType);
        }
    }

    /**
     * Get a builder to be used to build the executor.
     * @param callerType The class for the {@link com.flipkart.bifrost.framework.RemoteCallable} to be executed using this builder.
     * @param <T> The return type of the {@link com.flipkart.bifrost.framework.RemoteCallable} object.
     * @return A builder for the Executor.
     */
    public static <T> Builder<T> builder(Class<? extends RemoteCallable> callerType) {
        return new Builder<>(callerType);
    }

    /**
     * Execute the given callable remotely and return the result.
     * The returned future can be used to wait for and process the result.
     * Members of the RemoteCallable are json-serialized and forwarded to the
     * {@link com.flipkart.bifrost.framework.BifrostRemoteCallExecutionServer} for execution.
     * In case of cluster failure, the returned future get() will throw an exception.
     * Clients need to handle this and re-enqueue the task if required.
     * @param callable The callable object to executed.
     * @return A future to get the results form the remote computation.
     * @throws BifrostException in case of failure in submitting the callable.
     */
    abstract public Future<T> submit(RemoteCallable<T> callable) throws BifrostException;

    /**
     * Execute the given callable wrapped in {@link com.flipkart.bifrost.framework.RemoteTask} remotely.
     * @param callable A wrapper to the {@link com.flipkart.bifrost.framework.RemoteCallable} object to be executed.
     * @throws BifrostException in case it fails to submit the error.
     */
    abstract public void submit(RemoteTask<T> callable) throws BifrostException;

    /**
     * A completely async remote execution task that invokes the callback when the call completes.
     * @param listenableTask
     * @throws BifrostException
     */
    abstract public void submit(RemoteListenableTask<T> listenableTask) throws BifrostException;

    /**
     * Shutdown the executor.
     * @throws BifrostException
     */
    abstract public void shutdown() throws BifrostException;
}
