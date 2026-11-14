package com.fou.passport.until;

import android.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 备份文件加解密：口令 -> PBKDF2(盐, 多轮) -> AES-256-GCM。
 * 文件中只保存盐、IV 和密文，不含口令，换机后输入同一口令即可解密。
 */
public final class BackupCrypto {
    private static final int VERSION = 1;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    private static final int ITERATIONS = 120_000;

    private BackupCrypto() {
    }

    public static String encrypt(String plainText, String password) throws GeneralSecurityException {
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(TAG_BITS, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        JsonObject envelope = new JsonObject();
        envelope.addProperty("version", VERSION);
        envelope.addProperty("iterations", ITERATIONS);
        envelope.addProperty("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        envelope.addProperty("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        envelope.addProperty("data", Base64.encodeToString(cipherText, Base64.NO_WRAP));
        return envelope.toString();
    }

    /**
     * @throws GeneralSecurityException 口令错误或文件损坏（GCM 校验失败）时抛出
     */
    public static String decrypt(String envelopeJson, String password) throws GeneralSecurityException {
        JsonObject envelope = JsonParser.parseString(envelopeJson).getAsJsonObject();
        if (envelope.get("version").getAsInt() != VERSION) {
            throw new GeneralSecurityException("unsupported backup version");
        }
        int iterations = envelope.get("iterations").getAsInt();
        byte[] salt = Base64.decode(envelope.get("salt").getAsString(), Base64.NO_WRAP);
        byte[] iv = Base64.decode(envelope.get("iv").getAsString(), Base64.NO_WRAP);
        byte[] cipherText = Base64.decode(envelope.get("data").getAsString(), Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations),
                new GCMParameterSpec(TAG_BITS, iv));
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws GeneralSecurityException {
        return deriveKey(password, salt, ITERATIONS);
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        byte[] key = secretKeyFactory().generateSecret(spec).getEncoded();
        spec.clearPassword();
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Android 8.0 以下没有 PBKDF2WithHmacSHA256，回退到 SHA1 版本。
     */
    private static SecretKeyFactory secretKeyFactory() throws NoSuchAlgorithmException {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
