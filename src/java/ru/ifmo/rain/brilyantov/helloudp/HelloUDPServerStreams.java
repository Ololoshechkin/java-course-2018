package ru.ifmo.rain.brilyantov.helloudp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class HelloUDPServerStreams extends HelloUDPStreams {

    public HelloUDPServerStreams(DatagramSocket socket) throws SocketException {
        super(socket);
    }

    @Override
    protected DatagramPacket getReceivePacket() throws SocketException {
        return createReceivePacket();
    }
}
