package ru.ifmo.rain.brilyantov.helloudp;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class MessageHelper {

    public static String createMessage(String queryPrefix, int threadId, int requestNumber) {
        return queryPrefix + threadId + "_" + requestNumber;
    }

    public static String packetToString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static boolean check(String response, String request) {
        return response.matches(".*" + Pattern.quote(request) + "(|\\p{Space}.*)");
    }

}
