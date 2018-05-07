package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer {

    private final DatagramSocket socket;

    private void process(DatagramPacket packet) {
        try {
            MessageHelloUdp request = MessageHelloUdp.fromPacket(packet);
            MessageHelloUdp reply = request.transformed();
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
        } catch (MessageHelloUdp.MessageHelloUdpParseException e) {
            System.out.println("invalid message format " + e.getMessage());
        }
    }

    HelloUDPServer(int port, int threadCount) throws SocketException {
        socket = new DatagramSocket(port);
        byte[] buffer = new byte[1024];
        Runnable task = () -> {
            while (true) {
                try {
                    process(Multiplexor.readPacket(socket, buffer));
                } catch (IOException e) {
                    System.out.println("failed to read message");
                }
            }
        };
        for (int i = 0; i < threadCount; i++) {
            new Thread(task).run();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int threadsCount = Integer.parseInt(args[1]);
        try {
            new HelloUDPServer(port, threadsCount);
        } catch (SocketException e) {
            System.out.println("failed to start server " + e.getLocalizedMessage());
        }
    }

}
