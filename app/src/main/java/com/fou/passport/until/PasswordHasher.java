package com.fou.passport.until;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 主密码加盐哈希（PBKDF2-HMAC-SHA256），用于替代明文存储主密码。
 * 存储格式：pbkdf2:迭代次数:盐(Base64):哈希(Base64)
 */
public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] digest = derive(password, salt, ITERATIONS);
        return PREFIX + ":" + ITERATIONS + ":"
                + Base64.encodeToString(salt, Base64.NO_WRAP) + ":"
                + Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    /**
     * 恒定时间比较，避免时序侧信道。参数为 null 时返回 false。
     */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
            return secretKeyFactory().generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 不可用", e);
        }
    }

    /**
     * Android 8.0 以下没有 PBKDF2WithHmacSHA256，回退到 SHA1 版本。
     * 凭证只保存在本机，同一设备上的生成与校验会使用相同算法。
     */
    private static SecretKeyFactory secretKeyFactory() throws NoSuchAlgorithmException {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        }
    }

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 恒定时间字符串比较（用于老版本明文口令的兼容校验）。
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(utf8(a), utf8(b));
    }
}
