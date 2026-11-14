package com.fou.passport.until;

import android.content.Context;
import android.util.Base64;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 数据库口令管理：
 * - 新安装：随机生成口令，用 Keystore 加密后保存，SQLCipher 使用随机口令。
 * - 老版本升级：库里原本用硬编码口令加密，首次启动时用旧口令打开并 rekey 到新口令，
 *   迁移失败则沿用旧口令，保证用户数据不丢。
 */
public final class DbKeyStore {
    private static final String DB_NAME = "myDatabase";
    private static final String KEY_DB_PASSPHRASE = "db_passphrase";
    private static final String LEGACY_PASSPHRASE = "userEnteredPassphrase";

    private DbKeyStore() {
    }

    public static synchronized byte[] getPassphrase(Context context) {
        SecureStore store = SecureStore.get(context);
        String passphrase = store.getString(KEY_DB_PASSPHRASE);
        if (passphrase != null) {
            return passphraseBytes(passphrase);
        }

        File dbFile = context.getDatabasePath(DB_NAME);
        if (dbFile.exists()) {
            String newPassphrase = generatePassphrase();
            if (rekey(dbFile.getAbsolutePath(), newPassphrase)) {
                store.putString(KEY_DB_PASSPHRASE, newPassphrase);
                return passphraseBytes(newPassphrase);
            }
            //迁移失败（旧库本身损坏或口令不符）：沿用旧口令
            store.putString(KEY_DB_PASSPHRASE, LEGACY_PASSPHRASE);
            return passphraseBytes(LEGACY_PASSPHRASE);
        }

        String newPassphrase = generatePassphrase();
        store.putString(KEY_DB_PASSPHRASE, newPassphrase);
        return passphraseBytes(newPassphrase);
    }

    /**
     * SQLCipher 新版使用 byte[] 口令，旧版 getBytes(char[]) 已移除；现有口令均为 ASCII，UTF-8 编码等价。
     */
    private static byte[] passphraseBytes(String passphrase) {
        return passphrase.getBytes(StandardCharsets.UTF_8);
    }

    private static String generatePassphrase() {
        return Base64.encodeToString(SecureStore.randomBytes(32), Base64.NO_WRAP);
    }

    private static boolean rekey(String dbPath, String newPassphrase) {
        SQLiteDatabase database = null;
        try {
            database = SQLiteDatabase.openDatabase(dbPath, LEGACY_PASSPHRASE, null,
                    SQLiteDatabase.OPEN_READWRITE, null);
            database.rawExecSQL("PRAGMA rekey = '" + newPassphrase.replace("'", "''") + "'");
            return true;
        } catch (Throwable e) {
            android.util.Log.e("DbKeyStore", "legacy rekey failed", e);
            return false;
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }
}
