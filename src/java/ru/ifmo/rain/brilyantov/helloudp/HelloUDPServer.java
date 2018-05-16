package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            streams = new HelloUDPServerStreams(new DatagramSocket(port));
            threadPool = Executors.newFixedThreadPool(threads + 1);
            Runnable readerTask = () -> {
                while (!streams.isClosed()) {
                    try {
                        DatagramPacket curPacket = streams.readPacket();
                        threadPool.submit(() -> {
                            String response = process(packetToString(curPacket));
                            try {
                                streams.sendString(response, curPacket.getSocketAddress());
                            } catch (IOException e) {
                                System.err.println("Failed to send message");
                            }
                        });
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Failed to receive message");
                        } else {
                            break;
                        }
                    }
                }
            };
            threadPool.submit(readerTask);
        } catch (SocketException e) {
            System.err.println("Failed to bind to address");
        }
    }

    @Override
    public void close() {
        streams.close();
        isRunning = false;
        ShutdownHelper.shutdownAndAwaitTermination(threadPool);
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
