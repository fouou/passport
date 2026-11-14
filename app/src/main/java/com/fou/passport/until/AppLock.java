package com.fou.passport.until;

/**
 * 应用解锁状态，只保存在内存中：进程被杀或应用退到后台后重新锁定。
 */
public final class AppLock {
    private static volatile boolean unlocked;

    private AppLock() {
    }

    public static void unlock() {
        unlocked = true;
    }

    public static void lock() {
        unlocked = false;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }
}
