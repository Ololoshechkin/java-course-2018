package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.packetToString;

public class HelloUDPServer implements HelloServer {

    //    private List<Thread> threads = new ArrayList<>();
    private ExecutorService threadPool;
    private HelloUDPStreams streams;

    private void process(HelloUDPStreams streams, DatagramPacket packet) {
        String reply = "Hello, " + packetToString(packet);
        try {
            streams.sendString(reply);
        } catch (IOException e) {
            System.out.println("failed to send reply");
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int threadsCount = Integer.parseInt(args[1]);
        new HelloUDPServer().start(port, threadsCount);
    }

    @Override
    public void start(int port, int threadCount) {
        try {
            streams = new HelloUDPStreams(
                    InetAddress.getLoopbackAddress(),
                    port,
                    new DatagramSocket(port)
            );
            Runnable task = () -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket packet = streams.readPacket();
                        process(streams, packet);
                    } catch (IOException e) {
                        if (!streams.socketIsClosed()) {
                            System.out.println("failed to read message");
                        } else {
                            break;
                        }
                    }
                }
            };
            threadPool = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                threadPool.submit(task);
            }
        } catch (SocketException e) {
            System.out.println("failed to create server's socket");
        }
    }

    @Override
    public void close() {
        threadPool.shutdown();
        streams.close();
    }
}
