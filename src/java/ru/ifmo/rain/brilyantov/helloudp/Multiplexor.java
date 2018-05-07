package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.fromPacket;

public class Multiplexor {
    private final InetAddress serverAddress;
    private final int port;
    private final DatagramSocket socket;
    private final ExecutorService receiverThread;
    private static int BUF_SIZE = 1024;
    private byte[] receiveBuffer = new byte[BUF_SIZE];
    private final Map<MessageHelloUdp.RequestId, MessageHelloUdp> responces = new HashMap<>();
    private final Map<MessageHelloUdp.RequestId, CompletableFuture<MessageHelloUdp>> requests = new HashMap<>();

    public static DatagramPacket readPacket(
            DatagramSocket socket,
            byte[] receiveBuffer
    ) throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveBuffer, BUF_SIZE);
        socket.receive(packet);
        return packet;
    }

    public Multiplexor(InetAddress serverAddress, int port, DatagramSocket socket) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.socket = socket;
        receiverThread = Executors.newSingleThreadExecutor();
        receiverThread.submit(() -> {
            while (true) {
                MessageHelloUdp response = null;
                try {
                    response = readAnyMessage(socket, receiveBuffer);
                } catch (MessageHelloUdp.MessageHelloUdpParseException e) {
                    continue;
                } catch (IOException e) {
                    return;
                }
                synchronized (requests) {
                    if (requests.containsKey(response.requestId)) {
                        requests.remove(response.requestId).complete(response);
                    } else {
                        responces.put(response.requestId, response);
                    }
                }
            }
        });
    }

    void sendRequest(MessageHelloUdp requestMsg) throws IOException {
        sendMessage(socket, serverAddress, port, requestMsg);
    }

    static void sendMessage(
            DatagramSocket socket,
            InetAddress address,
            int port,
            MessageHelloUdp requestMsg
    ) throws IOException {
        byte[] sendBuffer = requestMsg.toString().getBytes();
        DatagramPacket message = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        socket.send(message);
    }

    private static MessageHelloUdp readAnyMessage(
            DatagramSocket socket,
            byte[] receiveBuffer
    ) throws IOException, MessageHelloUdp.MessageHelloUdpParseException {
        DatagramPacket packet = readPacket(socket, receiveBuffer);
        return fromPacket(packet);
    }

    CompletableFuture<MessageHelloUdp> expectResponce(MessageHelloUdp.RequestId requestId) {
        CompletableFuture<MessageHelloUdp> responceFuture = new CompletableFuture<>();
        synchronized (requests) {
            if (responces.containsKey(requestId)) {
                responceFuture.complete(responces.remove(requestId));
            } else {
                requests.put(requestId, responceFuture);
            }
        }
        return responceFuture;
    }


    void shutdown() throws InterruptedException {
        socket.close();
        receiverThread.shutdown();
    }

}