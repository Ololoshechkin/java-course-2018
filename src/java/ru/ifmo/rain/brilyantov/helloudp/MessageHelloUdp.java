package ru.ifmo.rain.brilyantov.helloudp;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

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

    public static class MessageHelloUdpParseException extends ParseException {

        public MessageHelloUdpParseException(String description) {
            super("failed to parse MessageHelloUdp from string -- " + description, 0);
        }

    }

    public static MessageHelloUdp fromString(String requestString) throws MessageHelloUdpParseException {
        int digitPos = 0;
        int underlinePos = 0;
        while (digitPos < requestString.length() && !Character.isDigit(requestString.charAt(digitPos)))
            digitPos++;
        while (underlinePos < requestString.length() && requestString.charAt(underlinePos) != '_')
            underlinePos++;
        if (underlinePos == requestString.length()) {
            throw new MessageHelloUdpParseException("no \"_\" found in the given string");
        }
        if (digitPos == requestString.length()) {
            throw new MessageHelloUdpParseException("no numbers found in the given string");
        }
        String queryPrefix = requestString.substring(0, digitPos);
        try {
            return new MessageHelloUdp(
                    queryPrefix,
                    new RequestId(
                            Integer.parseInt(requestString.substring(
                                    queryPrefix.length(),
                                    underlinePos
                            )),
                            Integer.parseInt(requestString.substring(
                                    underlinePos + 1,
                                    requestString.length()
                            ))
                    )
            );
        } catch (NumberFormatException e) {
            throw new MessageHelloUdpParseException(e.getMessage());
        }
    }

    public static String packetToString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public boolean check(MessageHelloUdp query) {
        return query.requestId.equals(requestId) && queryPrefix.endsWith(query.queryPrefix);
    }

}
