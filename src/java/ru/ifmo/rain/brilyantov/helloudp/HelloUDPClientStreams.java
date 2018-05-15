package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.fromString;

public class HelloUDPClientStreams extends HelloUDPStreams {

    private final InetSocketAddress serverAddress;

    public HelloUDPClientStreams(InetAddress address, int port, DatagramSocket socket) throws SocketException {
        super(socket);
        this.serverAddress = new InetSocketAddress(address, port);
    }

    public void sendMessage(MessageHelloUdp requestMsg) throws IOException {
        sendString(requestMsg.toString(), serverAddress);
    }

    public MessageHelloUdp readMessage() throws IOException, MessageHelloUdp.MessageHelloUdpParseException {
        String responce = readString();
        return fromString(responce);
    }

}