package io.github.defective4.tv.dvbservices.util;

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

    public static String hash(String data) {
        byte[] md = MD.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : md) {
            String part = Integer.toHexString(b & 0xff);
            if (part.length() == 1) hash.append("0");
            hash.append(part);
        }
        return hash.toString();
    }
}
