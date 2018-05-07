package ru.ifmo.rain.brilyantov.helloudp;

import ru.ifmo.rain.brilyantov.concurrent.ParallelMapperImpl;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.RequestId;

public class HelloUDPClient {

    private Multiplexor multiplexor;

    HelloUDPClient(InetAddress serverAddress, int port) throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        this.multiplexor = new Multiplexor(serverAddress, port, socket);
    }

    void start(
            String queryPrefix,
            int threadCount,
            int queriesPerThread
    ) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        ParallelMapperImpl.startThreads(threadCount, threads, threadId -> () -> {
            for (int j = 0; j < queriesPerThread; j++) {
                RequestId requestId = new RequestId(threadId, j);
                try {
                    multiplexor.sendRequest(new MessageHelloUdp(queryPrefix, requestId));
                    CompletableFuture<MessageHelloUdp> responceFuture = multiplexor.expectResponce(requestId);
                    String response;
                    while (true) {
                        try {
                            response = responceFuture.get(30, TimeUnit.MILLISECONDS).toString();
                            break;
                        } catch (TimeoutException | InterruptedException | ExecutionException ignored) {
                        }
                    }
                    System.out.println("response : " + response + " we are : " + threadId);
                } catch (IOException e) {
                    System.out.println("failed to send request in thread " + threadId);
                }
            }
        });
        ParallelMapperImpl.endThreads(threads);
        multiplexor.shutdown();
    }

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String queryPrefix = args[2];
        int threadsCount = Integer.parseInt(args[3]);
        int queriesPerThread = Integer.parseInt(args[4]);
        System.out.println("running client... host = " + host + " , port = " + port + " , prefix = " + queryPrefix);
        try {
            HelloUDPClient client = new HelloUDPClient(
                    InetAddress.getByName(host),
                    port
            );
            client.start(queryPrefix, threadsCount, queriesPerThread);
        } catch (Exception e) {
            System.out.println("failed to connect " + e.getMessage());
        }
    }

}
