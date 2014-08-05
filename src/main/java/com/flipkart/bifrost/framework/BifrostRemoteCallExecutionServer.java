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

import com.flipkart.bifrost.framework.impl.RabbitMQExecutionServerBuilder;

/**
 * An execution server for the incoming requests. Call {@link BifrostRemoteCallExecutionServer#start()} to start up the server.
 * @param <T> The type of the response object to be sent back.
 */
public abstract class BifrostRemoteCallExecutionServer<T> {
    public static final class Builder<T> extends RabbitMQExecutionServerBuilder<T> {
        private Builder(Class<? extends RemoteCallable> callerSubType) {
            super(callerSubType);
        }
    }

    /**
     * Get a builder for the execution server.
     * @param callerSubType
     * @param <T>
     * @return
     */
    public static<T>  Builder<T> builder(Class<? extends RemoteCallable> callerSubType) {
        return new Builder<>(callerSubType);
    }

    /**
     * Start the execution server.
     * @throws BifrostException in case of any error.
     */
    abstract public void start() throws BifrostException;

    /**
     * Stop the execution server.
     * @throws BifrostException in case of any error.
     */
    abstract public void stop() throws BifrostException;
}
