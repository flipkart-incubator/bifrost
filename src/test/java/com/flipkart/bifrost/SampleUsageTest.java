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

package com.flipkart.bifrost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.bifrost.framework.BifrostExecutor;
import com.flipkart.bifrost.framework.RemoteCallExecutionServer;
import com.flipkart.bifrost.framework.RemoteCallable;
import com.flipkart.bifrost.http.HttpCallCommand;
import com.flipkart.bifrost.rabbitmq.Connection;
import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SampleUsageTest {

    @Test
    public void testSendReceive() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Connection connection = new Connection(Lists.newArrayList("localhost"), "guest", "guest");
        connection.start();

        //Create executor
        BifrostExecutor<Map<String,Object>> executor = BifrostExecutor.<Map<String,Object>>builder(HttpCallCommand.class)
                .connection(connection)
                .objectMapper(mapper)
                .requestQueue("bifrost-send")
                .responseQueue("bifrost-recv")
                .executorService(Executors.newFixedThreadPool(10))
                .responseWaitTimeout(20000)
                .build();

        //Create execution server
        RemoteCallExecutionServer<Map<String,Object>> executionServer = RemoteCallExecutionServer.<Map<String,Object>>builder(HttpCallCommand.class)
                                                                                .objectMapper(mapper)
                                                                                .connection(connection)
                                                                                .concurrency(10)
                                                                                .requestQueue("bifrost-send")
                                                                                .build();
        executionServer.start();

        //Start making calls
        RemoteCallable<Map<String,Object>> callable = HttpCallCommand.createGet("http://jsonip.com/");
        Future<Map<String,Object>> result = executor.submit(callable);
        Map<String,Object> r = result.get();

        //Shutdown exverything when done
        executor.shutdown();
        executionServer.stop();
        connection.stop();

        Assert.assertTrue(r.containsKey("ip"));

    }
}
