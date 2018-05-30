package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.IntStream;


public class HelloUDPClient implements HelloClient {

    private static final int MAX_PORT = 65536;

    private String readCheckedMessage(HelloUDPClientStreams streams, String query) throws IOException {
        while (true) {
            streams.sendString(query);
            try {
                String response = streams.readString();
                if (MessageHelper.check(response, query)) {
                    return response;
                }
            } catch (IOException e) {
                System.out.println("received broken UDP packet");
            }
        }
    }

    @Override
    public void run(String host, int port, String queryPrefix, int threadCount, int queriesPerThread) {
        System.out.println(queryPrefix);
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        IntFunction<Callable<Void>> taskGen = threadId -> () -> {
            try (HelloUDPClientStreams streams = new HelloUDPClientStreams(
                    InetAddress.getByName(host),
                    port,
                    new DatagramSocket()
            )) {
                for (int i = 0; i < queriesPerThread; i++) {
                    try {
                        String query = MessageHelper.createMessage(queryPrefix, threadId, i);
                        String response = readCheckedMessage(streams, query);
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
        IntStream.range(0, threadCount)
                .mapToObj(taskGen)
                .forEach(threadPool::submit);
        ShutdownHelper.shutdownAndAwaitTermination(threadPool);
    }

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

}
