package com.flipkart.bifrost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.flipkart.bifrost.framework.BifrostExecutor;
import com.flipkart.bifrost.framework.RemoteCallExecutionServer;
import com.flipkart.bifrost.framework.RemoteCallable;
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

        mapper.getSubtypeResolver().registerSubtypes(new NamedType(HttpCallCommand.class, "http"));

        Connection connection = new Connection(Lists.newArrayList("localhost"));
        connection.start();

        //Create executor
        BifrostExecutor<Map<String,Object>> executor = BifrostExecutor.<Map<String,Object>>builder()
                .connection(connection)
                .objectMapper(mapper)
                .requestQueue("bifrost-send")
                .responseQueue("bifrost-recv")
                .executorService(Executors.newFixedThreadPool(10))
                .build();

        //Create execution server
        RemoteCallExecutionServer<Map<String,Object>> executionServer = RemoteCallExecutionServer.<Map<String,Object>>builder()
                                                                                .objectMapper(mapper)
                                                                                .connection(connection)
                                                                                .concurrency(10)
                                                                                .requestQueue("bifrost-send")
                                                                                .build();
        executionServer.start();

        //Start making calls
        RemoteCallable<Map<String,Object>> callable = HttpCallCommand.createGet(
                "http://jsonip.com/");
        Future<Map<String,Object>> result = executor.submit(callable);
        Map<String,Object> r = result.get();

        //Shutdown exverything when done
        executor.shutdown();
        executionServer.stop();
        connection.stop();

        Assert.assertTrue(r.containsKey("ip"));

    }
}
