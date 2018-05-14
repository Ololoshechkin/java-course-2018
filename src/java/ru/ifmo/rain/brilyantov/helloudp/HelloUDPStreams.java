package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.fromString;
import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.packetToString;

public class HelloUDPStreams implements AutoCloseable {
    private final InetAddress serverAddress;
    private final int port;
    private final DatagramSocket socket;
    private static int SOCKET_TIMEOUT = 100;
    private byte[] receiveBuffer;

    public HelloUDPStreams(InetAddress serverAddress, int port, DatagramSocket socket) throws SocketException {
        this.serverAddress = serverAddress;
        this.port = port;
        this.socket = socket;
        receiveBuffer = new byte[socket.getReceiveBufferSize()];
    }

    public void sendString(String requestMsg) throws IOException {
        byte[] sendBuffer = requestMsg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket message = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, port);
        socket.send(message);
    }

    public void sendMessage(MessageHelloUdp requestMsg) throws IOException {
        sendString(requestMsg.toString());
    }

    public DatagramPacket readPacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.receive(packet);
        return packet;
    }

    public String readString() throws IOException {
        return packetToString(readPacket());
    }

    public MessageHelloUdp readMessage() throws IOException, MessageHelloUdp.MessageHelloUdpParseException {
        String responce = readString();
        return fromString(responce);
    }

    public boolean socketIsClosed() {
        return socket.isClosed();
    }

    @Override
    public void close() {
        socket.close();
    }
}