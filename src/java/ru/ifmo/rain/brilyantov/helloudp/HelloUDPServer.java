package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.packetToString;


public class HelloUDPServer implements HelloServer {

    private ExecutorService threadPool;
    private HelloUDPStreams streams;
    private boolean isRunning = true;

    private String process(String request) {
        return "Hello, " + request;
    }

    public void start(int port, int threads) {
        try {
            streams = new HelloUDPStreams(new DatagramSocket(port));
            threadPool = Executors.newFixedThreadPool(threads);
            Runnable task = () -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket receivePacket = streams.readPacket();
                        String response = process(packetToString(receivePacket));
                        streams.sendString(response, receivePacket.getSocketAddress());
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Failed to receive/send message");
                        }
                    }
                }
            };
            IntStream.range(0, threads).forEach(i -> threadPool.submit(task));
        } catch (SocketException e) {
            System.err.println("Failed to bind to address");
        }
    }

    @Override
    public void close() {
        threadPool.shutdownNow();
        streams.close();
        isRunning = false;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("expected exactly 2 arguments in not-null array");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threadsCount = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threadsCount);
        } catch (NumberFormatException e) {
            System.out.println("expected integer arguments");
        }
    }
}