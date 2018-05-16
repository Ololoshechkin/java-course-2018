package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.*;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.fromString;

public class HelloUDPClientStreams extends HelloUDPStreams {

    private DatagramPacket receivePacket;

    private final InetSocketAddress serverAddress;

    public HelloUDPClientStreams(InetAddress address, int port, DatagramSocket socket) throws SocketException {
        super(socket);
        this.serverAddress = new InetSocketAddress(address, port);
        this.receivePacket = createReceivePacket();
    }

    public void sendMessage(MessageHelloUdp requestMsg) throws IOException {
        sendString(requestMsg.toString(), serverAddress);
    }
    @Override
    protected DatagramPacket getReceivePacket() {
        return receivePacket;
    }
}