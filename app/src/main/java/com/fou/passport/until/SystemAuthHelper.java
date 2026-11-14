package com.fou.passport.until;

import android.app.KeyguardManager;
import android.content.Context;

import androidx.biometric.BiometricManager;

/**
 * 系统锁屏解锁能力判断：设备已有密码/图案/指纹时，可直接复用系统验证进入应用。
 */
public class SystemAuthHelper {

    /**
     * 设备是否已设置安全锁屏（密码、图案或指纹等）。
     */
    public static boolean isDeviceSecure(Context context) {
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager != null && keyguardManager.isDeviceSecure();
    }

    /**
     * 是否已录入可用的指纹/面容。
     */
    public static boolean isBiometricAvailable(Context context) {
        return BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * 是否可以使用系统解锁：有安全锁屏即可，Android 10 以下没有指纹时走设备密码。
     */
    public static boolean isSystemUnlockAvailable(Context context) {
        return isDeviceSecure(context);
    }

    /**
     * 进入应用是否需要验证：应用自身有凭证，或者系统已有锁屏。
     */
    public static boolean requiresUnlock(Context context) {
        return MyPreferences.getInstance(context).hasAnyCredential() || isDeviceSecure(context);
    }
}
