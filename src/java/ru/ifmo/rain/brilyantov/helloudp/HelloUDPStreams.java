package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelper.packetToString;

public abstract class HelloUDPStreams implements AutoCloseable {
    protected final DatagramSocket socket;
    private static final int SOCKET_TIMEOUT = 500;

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
        socket.send(packet);
    }

    protected DatagramPacket createReceivePacket() throws SocketException {
        byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
        return new DatagramPacket(receiveBuffer, receiveBuffer.length);
    }

    protected abstract DatagramPacket getReceivePacket() throws SocketException;

    public DatagramPacket readPacket() throws IOException {
        DatagramPacket packet = getReceivePacket();
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

    public boolean isClosed() {
        return socket.isClosed();
    }
}
