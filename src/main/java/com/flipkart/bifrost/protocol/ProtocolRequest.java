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
