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
//                        byte[] receiveBuffer = new byte[streams.getSocket().getReceiveBufferSize()];
//                        DatagramPacket receivePacket = streams.getReceivePacket(receiveBuffer);
//                        streams.getSocket().receive(receivePacket);

                        DatagramPacket receivePacket = streams.readPacket();


                        String requestMsg = packetToString(receivePacket);
//                        DatagramPacket request = streams.readPacket();
//                        String requestMsg = packetToString(request);
//                        System.out.println("request : " + requestMsg);
                        String response = process(requestMsg);
                        streams.sendString(response, receivePacket.getSocketAddress());
//                        String response = "Hello, " + requestMsg;
//                        byte[] responseBuffer = response.getBytes(StandardCharsets.UTF_8);
//
//                        DatagramPacket packetToSend = new DatagramPacket(
//                                responseBuffer,
//                                responseBuffer.length
//                        );
//                        packetToSend.setSocketAddress(receivePacket.getSocketAddress());
//
//                        streams.sendPacket(packetToSend);
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Error working with datagram: " + e.getMessage());
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

    private static final String ERROR_MSG = "Running:\n" +
            "HelloUDPServer <port> <number of threads>";

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println(ERROR_MSG);
            return;
        }

        int port;
        int threads;

        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error parsing number " + e.getMessage());
            return;
        }

        new HelloUDPServer().start(port, threads);
    }
}