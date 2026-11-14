package com.fou.passport.until;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 基于 Android Keystore 的本地加密存储：密钥保存在硬件安全区，密文存放在普通 prefs 中。
 * 即使 prefs 文件被 root/备份导出，没有设备上的 Keystore 密钥也无法解密。
 */
public final class SecureStore {
    private static final String PREFS_NAME = "secure-settings";
    private static final String KEY_ALIAS = "passport_secure_store_v1";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int GCM_TAG_BITS = 128;
    private static volatile SecureStore sInstance;

    private final SharedPreferences mPrefs;

    private SecureStore(Context context) {
        mPrefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SecureStore get(Context context) {
        if (sInstance == null) {
            synchronized (SecureStore.class) {
                if (sInstance == null) {
                    sInstance = new SecureStore(context);
                }
            }
        }
        return sInstance;
    }

    public synchronized void putString(String key, String value) {
        try {
            byte[] plain = value.getBytes("UTF-8");
            SecretKey secretKey = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(plain);
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            mPrefs.edit()
                    .putString(key, Base64.encodeToString(payload, Base64.NO_WRAP))
                    .apply();
        } catch (GeneralSecurityException | IOException e) {
            android.util.Log.e("SecureStore", "putString failed", e);
        }
    }

    public synchronized String getString(String key) {
        String encoded = mPrefs.getString(key, null);
        if (encoded == null) {
            return null;
        }
        try {
            byte[] payload = Base64.decode(encoded, Base64.NO_WRAP);
            if (payload.length == 0) {
                return null;
            }
            SecretKey secretKey = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(GCM_TAG_BITS, payload, 0, 12));
            byte[] plain = cipher.doFinal(payload, 12, payload.length - 12);
            return new String(plain, "UTF-8");
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            //密钥被清除或数据损坏时视为不存在
            return null;
        }
    }

    public synchronized void remove(String key) {
        mPrefs.edit().remove(key).apply();
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return keyGenerator.generateKey();
    }

    /**
     * 生成随机字节串（用于数据库口令等）。
     */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
