/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fou.passport.until;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;

import androidx.annotation.NonNull;


public class MyPreferences {
    private static final String RESPONSE_AUTH_KEY = "response_auth";
    private static final String DATASET_AUTH_KEY = "dataset_auth";
    private static final String LEGACY_MASTER_PASSWORD_KEY = "master_password";
    private static final String MASTER_PASSWORD_HASH_KEY = "master_password_hash";
    private static final String LOGGING_LEVEL = "logging_level";
    private static final String DAL_CHECK_REQUIRED = "dal_check_required";
    private static final String NUMBER_DATASETS = "number_datasets";
    private static final String AUTH_FAILED_ATTEMPTS_KEY = "auth_failed_attempts";
    private static final String AUTH_LOCKED_UNTIL_KEY = "auth_locked_until";
    private static final String DYNAMIC_COLOR_ENABLED_KEY = "dynamic_color_enabled";
    private static final String THEME_SEED_COLOR_KEY = "theme_seed_color";
    private static final String THEME_MODE_KEY = "theme_mode";
    private static final String APP_LANGUAGE_KEY = "app_language";
    private static final String MASTER_PATTERN_HASH_KEY = "master_pattern_hash";
    //旧版图案以该前缀混在主密码槽位中，仅用于兼容读取
    private static final String PATTERN_PREFIX = "pattern:";
    private static final String MASTER_USE_PATTERN_KEY = "master_use_pattern";
    private static volatile MyPreferences sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;

    private MyPreferences(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences("my-settings", Context.MODE_PRIVATE);
    }

    public static MyPreferences getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MyPreferences.class) {
                if (sInstance == null) {
                    sInstance = new MyPreferences(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Gets whether {@link FillResponse}s should require authentication.
     */
    public boolean isResponseAuth() {
        return mPrefs.getBoolean(RESPONSE_AUTH_KEY, false);
    }

    /**
     * Enables/disables authentication for the entire autofill {@link FillResponse}.
     */
    public void setResponseAuth(boolean responseAuth) {
        mPrefs.edit().putBoolean(RESPONSE_AUTH_KEY, responseAuth).apply();
    }

    /**
     * Gets whether {@link Dataset}s should require authentication.
     */
    public boolean isDatasetAuth() {
        return mPrefs.getBoolean(DATASET_AUTH_KEY, false);
    }

    /**
     * Enables/disables authentication for individual autofill {@link Dataset}s.
     */
    public void setDatasetAuth(boolean datasetAuth) {
        mPrefs.edit().putBoolean(DATASET_AUTH_KEY, datasetAuth).apply();
    }

    /**
     * 是否已保存过任意一种解锁凭证（密码或图案）。
     */
    public boolean hasAnyCredential() {
        return hasPasswordCredential() || hasPatternCredential();
    }

    /**
     * 是否启用了密码解锁。
     */
    public boolean hasPasswordCredential() {
        return hasStoredMaster() && !isMasterUsePattern();
    }

    /**
     * 是否启用了图案解锁（含旧版把图案存在主密码槽位的情况）。
     */
    public boolean hasPatternCredential() {
        return SecureStore.get(mContext).getString(MASTER_PATTERN_HASH_KEY) != null
                || (isMasterUsePattern() && hasStoredMaster());
    }

    private boolean hasStoredMaster() {
        return SecureStore.get(mContext).getString(MASTER_PASSWORD_HASH_KEY) != null
                || mPrefs.getString(LEGACY_MASTER_PASSWORD_KEY, null) != null;
    }

    /**
     * 设置主密码：只保存加盐哈希（Keystore 加密后存放），不再存明文。
     */
    public void setMasterPassword(@NonNull String masterPassword) {
        promoteLegacyPattern();
        SecureStore.get(mContext).putString(MASTER_PASSWORD_HASH_KEY,
                PasswordHasher.hash(masterPassword));
        mPrefs.edit().remove(LEGACY_MASTER_PASSWORD_KEY).apply();
    }

    /**
     * 设置图案解锁：与密码分开存放，两者可同时存在。
     */
    public void setMasterPattern(@NonNull String pattern) {
        if (isMasterUsePattern()) {
            //旧版图案占用了主密码槽位，设置新图案时直接覆盖
            SecureStore.get(mContext).remove(MASTER_PASSWORD_HASH_KEY);
            mPrefs.edit().remove(LEGACY_MASTER_PASSWORD_KEY).apply();
        }
        setMasterUsePattern(false);
        SecureStore.get(mContext).putString(MASTER_PATTERN_HASH_KEY,
                PasswordHasher.hash(PATTERN_PREFIX + pattern));
    }

    /**
     * 校验图案；旧版图案会继续按主密码槽位校验，保证升级后仍能解锁。
     */
    public boolean verifyMasterPattern(String pattern) {
        if (pattern == null) {
            return false;
        }
        String storedHash = SecureStore.get(mContext).getString(MASTER_PATTERN_HASH_KEY);
        if (storedHash != null) {
            return PasswordHasher.verify(PATTERN_PREFIX + pattern, storedHash);
        }
        return isMasterUsePattern() && verifyMasterPassword(PATTERN_PREFIX + pattern);
    }

    /**
     * 关闭密码解锁，图案不受影响。
     */
    public void removePasswordCredential() {
        SecureStore.get(mContext).remove(MASTER_PASSWORD_HASH_KEY);
        mPrefs.edit().remove(LEGACY_MASTER_PASSWORD_KEY).apply();
    }

    /**
     * 关闭图案解锁，密码不受影响。
     */
    public void removePatternCredential() {
        SecureStore.get(mContext).remove(MASTER_PATTERN_HASH_KEY);
        if (isMasterUsePattern()) {
            SecureStore.get(mContext).remove(MASTER_PASSWORD_HASH_KEY);
        }
        setMasterUsePattern(false);
    }

    /**
     * 把旧版存在主密码槽位的图案哈希迁移到独立的图案槽位。
     */
    private void promoteLegacyPattern() {
        if (!isMasterUsePattern()) {
            return;
        }
        String legacyHash = SecureStore.get(mContext).getString(MASTER_PASSWORD_HASH_KEY);
        if (legacyHash != null) {
            SecureStore.get(mContext).putString(MASTER_PATTERN_HASH_KEY, legacyHash);
            SecureStore.get(mContext).remove(MASTER_PASSWORD_HASH_KEY);
        }
        mPrefs.edit().remove(LEGACY_MASTER_PASSWORD_KEY).apply();
        setMasterUsePattern(false);
    }

    /**
     * 校验主密码；若检测到老版本的明文主密码，校验成功后自动迁移为哈希。
     */
    public boolean verifyMasterPassword(String input) {
        if (input == null) {
            return false;
        }
        String storedHash = SecureStore.get(mContext).getString(MASTER_PASSWORD_HASH_KEY);
        if (storedHash != null) {
            return PasswordHasher.verify(input, storedHash);
        }
        String legacy = mPrefs.getString(LEGACY_MASTER_PASSWORD_KEY, null);
        if (legacy == null) {
            return false;
        }
        boolean matched = PasswordHasher.constantTimeEquals(legacy, input);
        if (matched) {
            //迁移：写入哈希并删除明文
            SecureStore.get(mContext).putString(MASTER_PASSWORD_HASH_KEY,
                    PasswordHasher.hash(legacy));
            mPrefs.edit().remove(LEGACY_MASTER_PASSWORD_KEY).apply();
        }
        return matched;
    }

    public void clearCredentials() {
        SecureStore.get(mContext).remove(MASTER_PASSWORD_HASH_KEY);
        SecureStore.get(mContext).remove(MASTER_PATTERN_HASH_KEY);
        mPrefs.edit()
                .remove(LEGACY_MASTER_PASSWORD_KEY)
                .remove(MASTER_USE_PATTERN_KEY)
                .apply();
    }

    /**
     * 主密码是否以图案形式保存。
     */
    public boolean isMasterUsePattern() {
        return mPrefs.getBoolean(MASTER_USE_PATTERN_KEY, false);
    }

    public void setMasterUsePattern(boolean usePattern) {
        mPrefs.edit().putBoolean(MASTER_USE_PATTERN_KEY, usePattern).apply();
    }

    public Util.LogLevel getLoggingLevel() {
        return Util.LogLevel.values()[mPrefs.getInt(LOGGING_LEVEL, Util.LogLevel.Off.ordinal())];
    }

    public void setLoggingLevel(Util.LogLevel level) {
        mPrefs.edit().putInt(LOGGING_LEVEL, level.ordinal()).apply();
        Util.setLoggingLevel(level);
    }

    public Util.DalCheckRequirement getDalCheckRequirement() {
        return Util.DalCheckRequirement.values()[mPrefs.getInt(DAL_CHECK_REQUIRED,
                Util.DalCheckRequirement.AllUrls.ordinal())];
    }

    public void setDalCheckRequired(Util.DalCheckRequirement level) {
        mPrefs.edit().putInt(DAL_CHECK_REQUIRED, level.ordinal()).apply();
    }

    public int getNumberDatasets(int defaultNumber) {
        return mPrefs.getInt(NUMBER_DATASETS, defaultNumber);
    }

    public void setNumberDatasets(int number) {
        mPrefs.edit().putInt(NUMBER_DATASETS, number).apply();
    }

    /**
     * 主密码连续失败次数（落盘，杀进程后依然生效）。
     */
    public int getAuthFailedAttempts() {
        return mPrefs.getInt(AUTH_FAILED_ATTEMPTS_KEY, 0);
    }

    public void setAuthFailedAttempts(int attempts) {
        mPrefs.edit().putInt(AUTH_FAILED_ATTEMPTS_KEY, attempts).apply();
    }

    /**
     * 锁定截止时间（毫秒时间戳），0 表示未锁定。
     */
    public long getAuthLockedUntil() {
        return mPrefs.getLong(AUTH_LOCKED_UNTIL_KEY, 0L);
    }

    public void setAuthLockedUntil(long lockedUntil) {
        mPrefs.edit().putLong(AUTH_LOCKED_UNTIL_KEY, lockedUntil).apply();
    }

    /**
     * 是否启用 Material You 动态取色（壁纸或自定义种子色）。
     */
    public boolean isDynamicColorEnabled() {
        return mPrefs.getBoolean(DYNAMIC_COLOR_ENABLED_KEY, false);
    }

    public void setDynamicColorEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(DYNAMIC_COLOR_ENABLED_KEY, enabled).apply();
    }

    /**
     * 自定义主题色种子，0 表示跟随系统壁纸。
     */
    public int getThemeSeedColor() {
        return mPrefs.getInt(THEME_SEED_COLOR_KEY, 0);
    }

    public void setThemeSeedColor(int color) {
        mPrefs.edit().putInt(THEME_SEED_COLOR_KEY, color).apply();
    }

    /**
     * 主题模式：system / light / dark。
     */
    public String getThemeMode() {
        return mPrefs.getString(THEME_MODE_KEY, "system");
    }

    public void setThemeMode(String mode) {
        mPrefs.edit().putString(THEME_MODE_KEY, mode).apply();
    }

    /**
     * 应用语言：BCP-47 语言标签，system 表示跟随系统。
     */
    public String getAppLanguage() {
        return mPrefs.getString(APP_LANGUAGE_KEY, "system");
    }

    public void setAppLanguage(String languageTag) {
        mPrefs.edit().putString(APP_LANGUAGE_KEY, languageTag).apply();
    }

    /**
     * 是否已启用 Passport 自身的自动填充服务。
     * 部分 OEM 在服务已选中时 hasEnabledAutofillServices 仍返回 false，需要回退读取系统设置。
     */
    public boolean isOurAutofillServiceEnabled() {
        //Android 8.0 以下没有系统自动填充框架
        if (AutofillCompat.hasEnabledAutofillServices(mContext)) {
            return true;
        }
        String service = Settings.Secure.getString(mContext.getContentResolver(),
                "autofill_service");
        return service != null && service.startsWith(mContext.getPackageName() + "/");
    }
}
