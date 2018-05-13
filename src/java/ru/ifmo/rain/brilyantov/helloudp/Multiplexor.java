package ru.ifmo.rain.brilyantov.helloudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.fromString;
import static ru.ifmo.rain.brilyantov.helloudp.MessageHelloUdp.packetToString;

public class Multiplexor {
    private final InetAddress serverAddress;
    private final int port;
    private final DatagramSocket socket;
    private final ExecutorService receiverThread;
    private static int BUF_SIZE = 1024;
    private static int LOAD_FACTOR = 100_000;
    private byte[] receiveBuffer = new byte[BUF_SIZE];
    private final Map<MessageHelloUdp.RequestId, MessageHelloUdp> responces = new HashMap<>();
    private final Map<MessageHelloUdp.RequestId, CompletableFuture<MessageHelloUdp>> requests = new HashMap<>();

    public static DatagramPacket readPacket(
            DatagramSocket socket,
            byte[] receiveBuffer
    ) throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveBuffer, BUF_SIZE);
        socket.setSoTimeout(100);
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
                MessageHelloUdp response;
                try {
                    response = readResponce(socket, receiveBuffer);
                    System.out.println("response : " + response);
                } catch (MessageHelloUdp.MessageHelloUdpParseException e) {
                    continue;
                } catch (IOException e) {
                    return;
                }
                synchronized (requests) {
                    if (requests.containsKey(response.requestId)) {
                        requests.remove(response.requestId).complete(response);
                    } else {
                        if (responces.size() > LOAD_FACTOR) {
                            responces.clear();
                        }
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
            Object requestMsg
    ) throws IOException {
        byte[] sendBuffer = requestMsg.toString().getBytes();
        DatagramPacket message = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        socket.send(message);
    }

    private static MessageHelloUdp readResponce(
            DatagramSocket socket,
            byte[] receiveBuffer
    ) throws IOException, MessageHelloUdp.MessageHelloUdpParseException {
        String responce = packetToString(readPacket(socket, receiveBuffer));
        System.out.println("received \"" + responce + "\"");
        return fromString(responce);
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


    void shutdown() {
        socket.close();
        receiverThread.shutdown();
    }

}