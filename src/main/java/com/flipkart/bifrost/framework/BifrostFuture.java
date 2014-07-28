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
