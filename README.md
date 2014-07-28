#Bifrost
A remote code execution framework over RabbitMQ.

Provides a callable interface like Java executor service. Code is executed remotely using RabbitMQ as communication channel.

##Quickstart Guide
The basic workflow goes like this:

* On server side, open connection to RMQ cluster, create ExecutionServer, start() and wait for commands.
* On client side, open connection to RMQ Cluster, create Executor and keep submitting Callable<T> objects.

###Connection to cluster
Create a <i>Connection</i> object.

    Connection connection = new Connection(Lists.newArrayList("localhost"));

    //Start the connection
    connection.start();

    //Do whatever

    //Stop the connection
    connection.stop();

###RemoteCallable

The basic entity used is the <i>RemoteCallable<T></i> template. This object is json serialized and forwarded to the remote executor. You can derive this class to create your own executor.

    class TestAction extends RemoteCallable<Void> {

        public TestAction() {
            super("test");   //Set some name in the constructor
        }

        @Override
        public Void call() throws Exception {
            //Implement your method
            System.out.println("Hello World");
            return null;
        }
    }

The <i>HttpCallCommand<T></i> is a callable that can be used to make remote HTTP service calls.

###Server
The server component takes care of actually running the callable that you have sent from the client. To create a server use the following builder:

    RemoteCallExecutionServer<Void> executionServer = RemoteCallExecutionServer.<Void>builder(TestAction.class)
                                                                            .objectMapper(mapper)
                                                                            .connection(connection)
                                                                            .concurrency(10)
                                                                            .requestQueue("bifrost-send")
                                                                            .build();

    executionServer.start(); //Start the server
    ...
    executionServer.stop(); //To stop the server

###Client
The client interface is like a standard Java ExecutorService. To use it, create builder like the following:

    BifrostExecutor<Map<String,Object>> executor = BifrostExecutor.<Map<String,Object>>builder(TestAction.class)
                    .connection(connection)
                    .objectMapper(mapper)
                    .requestQueue("bifrost-send")
                    .responseQueue("bifrost-recv")
                    .executorService(Executors.newFixedThreadPool(10))
                    .build();

    //To submit job and wait for response
    Future<Void> result = scheduler.submit(new TestAction());
    result.get();

    //To submit job and not wait for response use a RemoteTask<T> wrapper
    scheduler.submit(new RemoteTask<Void>(new TestAction());

    //To stop the executor
    executor.shutdown();


##Version

0.1

##Tech

Uses RabbitMQ. Written in Java.

##Contribution, Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/flipkart-incubator/bifrost/issues).

Please follow the [contribution guidelines](https://github.com/flipkart-incubator/bifrost/blob/master/CONTRIBUTING.md) when submitting pull requests.


LICENSE
-------

Copyright 2014 Flipkart Internet Pvt. Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

