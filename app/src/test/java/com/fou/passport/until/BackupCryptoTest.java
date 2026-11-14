package com.fou.passport.until;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.security.GeneralSecurityException;

public class BackupCryptoTest {

    @Test
    public void encryptThenDecryptRestoresContent() throws Exception {
        String plain = "{\"packages\":[{\"app_name\":\"微信\"}]}";
        String encrypted = BackupCrypto.encrypt(plain, "backup-pass");
        assertEquals(plain, BackupCrypto.decrypt(encrypted, "backup-pass"));
    }

    @Test
    public void sameContentProducesDifferentCipherText() throws Exception {
        String plain = "{\"v\":1}";
        String first = BackupCrypto.encrypt(plain, "backup-pass");
        String second = BackupCrypto.encrypt(plain, "backup-pass");
        //每次使用随机盐和 IV
        assertEquals(false, first.equals(second));
    }

    @Test(expected = GeneralSecurityException.class)
    public void wrongPasswordCannotDecrypt() throws Exception {
        String encrypted = BackupCrypto.encrypt("{\"v\":1}", "backup-pass");
        BackupCrypto.decrypt(encrypted, "wrong-pass");
    }
}
