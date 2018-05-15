package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.packetToString;

public class HelloUDPStreams implements AutoCloseable {
    private final DatagramSocket socket;
    private static final int SOCKET_TIMEOUT = 500;

    private byte[] createReceiveBuffer() throws SocketException {
        return new byte[socket.getReceiveBufferSize()];
    }

    public HelloUDPStreams(DatagramSocket socket) throws SocketException {
        this.socket = socket;
        this.socket.setSoTimeout(SOCKET_TIMEOUT);
    }

    public void sendString(
            String requestMsg,
            SocketAddress destinationAddress
    ) throws IOException {
        byte[] sendBuffer = requestMsg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(
                sendBuffer,
                0,
                sendBuffer.length,
                destinationAddress
        );
        sendPacket(packet);
    }

    DatagramSocket getSocket() {
        return socket;
    }

    void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    DatagramPacket getReceivePacket(byte[] receiveBuffer) {
        return new DatagramPacket(receiveBuffer, receiveBuffer.length);
    }

    public DatagramPacket readPacket() throws IOException {
        byte[] receiveBuffer = createReceiveBuffer();
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(packet);
        return packet;
    }

    protected String readString() throws IOException {
        return packetToString(readPacket());
    }

    @Override
    public void close() {
        socket.close();
    }
}
