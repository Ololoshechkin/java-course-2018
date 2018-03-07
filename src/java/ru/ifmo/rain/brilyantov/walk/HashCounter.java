package ru.ifmo.rain.brilyantov.walk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class HashCounter {
    private static final int FNV_PRIME = 0x01000193;
    private static final int FNV_START = 0x811c9dc5;
    private static final int FNV_AND = (1 << 8) - 1;
    private static byte[] buffer = new byte[1024];

    public static int getFNV1Hash(Path file) {
        int hval = FNV_START;
        try (InputStream inputStream = new FileInputStream(file.toString())) {
            int sz;
            while ((sz = inputStream.read(buffer, 0, 1024)) != -1) {
                for (int i = 0; i < sz; i++) {
                    hval = (hval * FNV_PRIME)^ (buffer[i] & FNV_AND);
                }
            }
        } catch (IOException e) {
            hval = 0;
        }
        return hval;
    }

}
