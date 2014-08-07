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
import com.flipkart.bifrost.framework.*;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ListenTest {
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
            scheduler.submit(new RemoteListenableTask<>(new TestAction(), new CallListener(counter)));
            return null;
        }
    }

    private static class CallListener implements RemoteCallableCompletionListener<Void> {
        private AtomicInteger counter;

        private CallListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void onComplete(RemoteCallable<Void> callable, Void data) {
            counter.getAndIncrement();
        }

        @Override
        public void onError(RemoteCallable<Void> callable, BifrostException e) {
            counter.getAndIncrement();
        }
    }

    @Test
    public void testSendReceive() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Connection connection = new Connection(Lists.newArrayList("localhost"), "guest", "guest");
        connection.start();

        BifrostExecutor<Void> executor = BifrostExecutor.<Void>builder(TestAction.class)
                                                    .connection(connection)
                                                    .objectMapper(mapper)
                                                    .requestQueue("bifrost-send")
                                                    .responseQueue("bifrost-recv")
                                                    .concurrency(20)
                                                    .executorService(Executors.newFixedThreadPool(20))
                                                    .build();

        BifrostRemoteCallExecutionServer<Void> executionServer = BifrostRemoteCallExecutionServer.<Void>builder(TestAction.class)
                                                                                .objectMapper(mapper)
                                                                                .connection(connection)
                                                                                .concurrency(20)
                                                                                .requestQueue("bifrost-send")
                                                                                .build();
        executionServer.start();


        long startTime = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        int requestCount = 1000000;
        CompletionService<Void> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(50));
        List<Future<Void>> futures = Lists.newArrayListWithCapacity(requestCount);
        for(int i = 0; i < requestCount; i++) {
            futures.add(ecs.submit(new ServiceCaller(executor, counter)));
        }
        for(int i = 0; i < requestCount; i++) {
            try {
                ecs.take().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        while(counter.get() != requestCount );
        System.out.println(String.format("Completed: %d in %d ms", counter.get(), (System.currentTimeMillis() - startTime)));
        executor.shutdown();
        executionServer.stop();
        connection.stop();

        Assert.assertEquals(requestCount, counter.get());
    }
}
