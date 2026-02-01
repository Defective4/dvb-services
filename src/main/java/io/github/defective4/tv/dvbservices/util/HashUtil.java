package io.github.defective4.tv.dvbservices.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    private static final MessageDigest MD;

    static {
        try {
            MD = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hash(byte[] data) {
        MD.reset();
        return mdString(MD.digest(data));
    }

    public static byte[] hash(File file) throws IOException {
        MD.reset();
        try (InputStream in = new FileInputStream(file)) {
            byte[] data = new byte[1024];
            int read;
            while (true) {
                read = in.read(data);
                if (read < 0) break;
                MD.update(data, 0, read);
            }
        }
        return MD.digest();
    }

    public static String hash(String data) {
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean hashEquals(File file, byte[] hash) throws IOException {
        byte[] compare = hash(file);
        if (compare.length != hash.length) return false;
        for (int i = 0; i < hash.length; i++) {
            if (hash[i] != compare[i]) return false;
        }
        return true;
    }

    private static String mdString(byte[] md) {
        StringBuilder hash = new StringBuilder();
        for (byte b : md) {
            String part = Integer.toHexString(b & 0xff);
            if (part.length() == 1) hash.append("0");
            hash.append(part);
        }
        return hash.toString();
    }
}
