package ru.ifmo.rain.brilyantov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import ru.ifmo.rain.brilyantov.concurrent.ParallelMapperImpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class HelloUDPServer implements HelloServer {

    private static int BUF_SIZE = 1024;
    private DatagramSocket socket = null;
    private List<Thread> threads = new ArrayList<>();

    private void process(DatagramPacket packet) {
        String reply = "Hello, " + MessageHelloUdp.packetToString(packet);
        try {
            Multiplexor.sendMessage(
                    socket,
                    packet.getAddress(),
                    packet.getPort(),
                    reply
            );
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
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println("failed to create server's socket");
            return;
        }
        byte[] buffer = new byte[BUF_SIZE];
        Runnable task = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    process(Multiplexor.readPacket(socket, buffer));
                } catch (IOException e) {
                    System.out.println("failed to read message");
                    break;
                }
            }
        };
        ParallelMapperImpl.startThreads(threadCount, threads, i -> task);
    }

    @Override
    public void close() {
        try {
            threads.stream().forEach(Thread::interrupt);
            ParallelMapperImpl.endThreads(threads);
        } catch (InterruptedException e) {
            System.out.println("failed to finish working threads : " + e.getMessage());
        }
    }
}
