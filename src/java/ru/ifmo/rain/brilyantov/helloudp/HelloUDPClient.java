package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import ru.ifmo.rain.brilyantov.concurrent.ParallelMapperImpl;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.RequestId;

public class HelloUDPClient implements HelloClient {

    private Multiplexor multiplexor = null;

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String queryPrefix = args[2];
        int threadsCount = Integer.parseInt(args[3]);
        int queriesPerThread = Integer.parseInt(args[4]);
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

    @Override
    public void run(String host, int port, String queryPrefix, int threadCount, int queriesPerThread) {
        System.out.println(queryPrefix);
        final DatagramSocket socket;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("failed to start connection" + e.getMessage());
            return;
        }
        try {
            this.multiplexor = new Multiplexor(InetAddress.getByName(host), port, socket);
        } catch (UnknownHostException e) {
            System.out.println("invalid host name");
            return;
        }
        List<Thread> threads = new ArrayList<>();
        ParallelMapperImpl.startThreads(threadCount, threads, threadId -> () -> {
            for (int j = 0; j < queriesPerThread; j++) {
                RequestId requestId = new RequestId(threadId, j);
                try {
                    MessageHelloUdp query = new MessageHelloUdp(queryPrefix, requestId);
                    String expectedResponce = query.transformed().toString();
                    String response = null;
                    while (true) {
                        multiplexor.sendRequest(query);
                        CompletableFuture<MessageHelloUdp> responseFuture = multiplexor.expectResponce(requestId);
                        try {
                            response = responseFuture.get(500, TimeUnit.MILLISECONDS).toString();
                            System.out.println("response : " + response + " we are : " + threadId);
                            if (response.equals(expectedResponce)) {
                                break;
                            }
                        } catch (TimeoutException ignored) {
                        } catch (InterruptedException | ExecutionException e) {
                            break;
                        }
                    }
                    System.out.println("response : " + response + " we are : " + threadId);
                } catch (IOException e) {
                    System.out.println("failed to send request in thread " + threadId);
                }
            }
        });
        try {
            try {
                ParallelMapperImpl.endThreads(threads);
            } finally {
                multiplexor.shutdown();
            }
        } catch (InterruptedException e) {
            System.out.println("failed to finish client's job : " + e.getMessage());
        }
    }
}
