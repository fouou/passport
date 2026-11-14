package com.fou.passport.until;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PasswordHasherTest {
    @Test
    public void hashAndVerifyCorrectPassword() {
        String stored = PasswordHasher.hash("Test1234");
        assertTrue(PasswordHasher.verify("Test1234", stored));
    }

    @Test
    public void verifyRejectsWrongPassword() {
        String stored = PasswordHasher.hash("Test1234");
        assertFalse(PasswordHasher.verify("Test1235", stored));
        assertFalse(PasswordHasher.verify(null, stored));
        assertFalse(PasswordHasher.verify("Test1234", null));
        assertFalse(PasswordHasher.verify("Test1234", "garbage"));
    }

    @Test
    public void samePlainTextProducesDifferentHashes() {
        //加盐：同一密码两次哈希结果不同，避免彩虹表
        assertNotEquals(PasswordHasher.hash("same"), PasswordHasher.hash("same"));
    }

    @Test
    public void constantTimeEqualsWorks() {
        assertTrue(PasswordHasher.constantTimeEquals("abc", "abc"));
        assertFalse(PasswordHasher.constantTimeEquals("abc", "abd"));
        assertFalse(PasswordHasher.constantTimeEquals(null, "abc"));
    }
}
