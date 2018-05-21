package ru.ifmo.rain.brilyantov.helloudp;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class MessageHelloUdp {

    static class RequestId {
        int threadId;
        int requestNumber;

        public RequestId(int threadId, int requestNumber) {
            this.threadId = threadId;
            this.requestNumber = requestNumber;
        }

        @Override
        public int hashCode() {
            return threadId * 31 + requestNumber;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RequestId
                    && threadId == ((RequestId) obj).threadId
                    && requestNumber == ((RequestId) obj).requestNumber;
        }

        @Override
        public String toString() {
            return "(thread = " + threadId + ", number = " + requestNumber + ")";
        }
    }

    String queryPrefix;
    RequestId requestId;

    public MessageHelloUdp(String queryPrefix, RequestId requestId) {
        this.queryPrefix = queryPrefix;
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return queryPrefix + requestId.threadId + "_" + requestId.requestNumber;
    }

    public static String packetToString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static boolean check(String response, String request) {
        return response.contains(request);
    }

}
