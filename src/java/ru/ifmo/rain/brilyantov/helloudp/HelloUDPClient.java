package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.RequestId;

public class HelloUDPClient implements HelloClient {

    private static final int MAX_PORT = 65536;

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("expected exactly 5 args : host, port, queryPrefix, threadsCount, queriesPerThread");
            return;
        }
        String host = args[0];
        String queryPrefix = args[2];
        int port;
        int threadsCount;
        int queriesPerThread;
        try {
            port = Integer.parseInt(args[1]);
            threadsCount = Integer.parseInt(args[3]);
            queriesPerThread = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("port, threadsCount and queriesPerThread should be a integers");
            return;
        }
        if (port < 0 || port > MAX_PORT) {
            System.out.println("port should be in [1..65536]");
            return;
        }
        if (threadsCount < 0) {
            System.out.println("threadsCount should be positive number");
            return;
        }
        System.out.println("running client... host = " + host + " , port = " + port + " , prefix = " + queryPrefix);
        try {
            new HelloUDPClient().run(
                    host,
                    port,
                    queryPrefix,
                    threadsCount,
                    queriesPerThread
            );
        } catch (Exception e) {
            System.out.println("failed to connect " + e.getMessage());
        }
    }

    private MessageHelloUdp readCheckedMessage(HelloUDPClientStreams streams, MessageHelloUdp query) throws IOException {
        MessageHelloUdp response;
        while (true) {
            streams.sendMessage(query);
            try {
                response = streams.readMessage();
                if (response.check(query)) {
                    break;
                }
            } catch (MessageHelloUdp.MessageHelloUdpParseException | IOException e) {
                System.out.println("received broken UDP packet");
            }
        }
        System.out.println("response : " + response + " we are : " + query.requestId);
        return response;
    }

    @Override
    public void run(String host, int port, String queryPrefix, int threadCount, int queriesPerThread) {
        System.out.println(queryPrefix);
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        Function<Integer, Callable<Void>> taskGen = threadId -> () -> {
            try (HelloUDPClientStreams streams = new HelloUDPClientStreams(
                    InetAddress.getByName(host),
                    port,
                    new DatagramSocket()
            )) {
                for (int i = 0; i < queriesPerThread; i++) {
                    RequestId requestId = new RequestId(threadId, i);
                    try {
                        MessageHelloUdp query = new MessageHelloUdp(queryPrefix, requestId);
                        MessageHelloUdp response = readCheckedMessage(streams, query);
                        System.out.println("received response : " + response);
                    } catch (IOException e) {
                        System.out.println("failed to send request in thread " + threadId);
                    }
                }
            } catch (UnknownHostException | SocketException e) {
                System.out.println("failed to connect to server");
            }
            return null;
        };
        try {
            threadPool.invokeAll(
                    IntStream
                            .range(0, threadCount)
                            .boxed()
                            .map(taskGen)
                            .collect(Collectors.toList())
            ).forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("failed to invoke task");
                }
            });
        } catch (Exception e) {
            System.out.println("failed to invoke all given tasks");
        }
        threadPool.shutdown();
    }
}
