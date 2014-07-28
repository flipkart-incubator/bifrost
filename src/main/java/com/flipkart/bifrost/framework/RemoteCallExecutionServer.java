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

import com.flipkart.bifrost.framework.impl.server.RabbitMQExecutionServerBuilder;

public abstract class RemoteCallExecutionServer<T> {
    public static final class Builder<T> extends RabbitMQExecutionServerBuilder<T> {
        private Builder(Class<? extends RemoteCallable> callerSubType) {
            super(callerSubType);
        }
    }

    public static<T>  Builder<T> builder(Class<? extends RemoteCallable> callerSubType) {
        return new Builder<T>(callerSubType);
    }
    abstract public void start() throws BifrostException;
    abstract public void stop() throws BifrostException;
}
