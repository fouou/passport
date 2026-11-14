package com.fou.passport.until;

import android.content.Context;
import android.os.Build;
import android.view.autofill.AutofillManager;

/**
 * Android 8.0 以下没有系统自动填充框架，这里统一收口相关调用，
 * 避免低版本页面加载自动填充类时出错。
 */
public final class AutofillCompat {

    private AutofillCompat() {
    }

    public static boolean isSupported(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        AutofillManager manager = context.getSystemService(AutofillManager.class);
        return manager != null && manager.isAutofillSupported();
    }

    public static boolean hasEnabledAutofillServices(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        AutofillManager manager = context.getSystemService(AutofillManager.class);
        return manager != null && manager.hasEnabledAutofillServices();
    }

    public static void disableAutofillServices(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        AutofillManager manager = context.getSystemService(AutofillManager.class);
        if (manager != null) {
            manager.disableAutofillServices();
        }
    }
}
