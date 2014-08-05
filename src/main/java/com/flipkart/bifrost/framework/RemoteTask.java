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

/**
 * A wrapper to the enclosed {@link com.flipkart.bifrost.framework.RemoteCallable} object.
 * This needs to be used to call the {@link com.flipkart.bifrost.framework.BifrostExecutor#submit(RemoteTask)}
 * for situations where you want to go full async and don't want to wait for a response.
 * @param <T> should be same as the type parameter for the RemoteCallable object.
 */
public class RemoteTask<T> {
    private final RemoteCallable<T> callable;

    public RemoteTask(RemoteCallable<T> callable) {
        this.callable = callable;
    }

    public RemoteCallable<T> getCallable() {
        return callable;
    }
}
