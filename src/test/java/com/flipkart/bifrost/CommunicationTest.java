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

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CommunicationTest {
    public static final class TestAction extends RemoteCallable<Void> {

        public TestAction() {
            super("test");
        }

        @Override
        public Void call() throws Exception {
            return null;
        }
    }

    private static final class ServiceCaller implements Callable<Void> {
        private BifrostExecutor<Void> scheduler;
        private AtomicInteger counter;

        private ServiceCaller(BifrostExecutor<Void> scheduler, AtomicInteger counter) {
            this.scheduler = scheduler;
            this.counter = counter;
        }

        @Override
        public Void call() throws Exception {
            Future<Void> result = scheduler.submit(new TestAction());
            result.get();
            counter.getAndIncrement();
            return null;
        }
    }

    @Test
    public void testSendReceive() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        mapper.getSubtypeResolver().registerSubtypes(new NamedType(TestAction.class, "test"));

        Connection connection = new Connection(Lists.newArrayList("localhost"));
        connection.start();

        BifrostExecutor<Void> executor = BifrostExecutor.<Void>builder()
                                                    .connection(connection)
                                                    .objectMapper(mapper)
                                                    .requestQueue("bifrost-send")
                                                    .responseQueue("bifrost-recv")
                                                    .concurrency(10)
                                                    .executorService(Executors.newFixedThreadPool(10))
                                                    .build();

        RemoteCallExecutionServer<Void> executionServer = RemoteCallExecutionServer.<Void>builder()
                                                                                .objectMapper(mapper)
                                                                                .connection(connection)
                                                                                .concurrency(10)
                                                                                .requestQueue("bifrost-send")
                                                                                .build();
        executionServer.start();



        long startTime = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        int requestCount = 10;
        CompletionService<Void> ecs
                = new ExecutorCompletionService<Void>(Executors.newFixedThreadPool(50));
        List<Future<Void>> futures = Lists.newArrayListWithCapacity(requestCount);
        for(int i = 0; i < requestCount; i++) {
            futures.add(ecs.submit(new ServiceCaller(executor, counter)));
        }
        for(int i = 0; i < requestCount; i++) {
            ecs.take().get();
        }
        System.out.println(String.format("Completed: %d in %d ms", counter.get(), (System.currentTimeMillis() - startTime)));

        executor.shutdown();
        executionServer.stop();
        connection.stop();

        Assert.assertEquals(requestCount, counter.get());
    }
}
