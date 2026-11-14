package com.fou.passport;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import com.fou.passport.room.MyDB;
import com.fou.passport.until.AppExecutors;
import com.fou.passport.until.AppLock;
import com.fou.passport.until.MyPreferences;

public class MyApplication extends Application {
    public static AppExecutors appExecutors;
    public static Context applicationContext;
    public static SharedPreferences sharedPreferences;
    //处于启动状态的 Activity 数量，归零说明应用已退到后台
    private int startedActivityCount;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable lockIfBackground = () -> {
        if (startedActivityCount == 0) {
            AppLock.lock();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //SQLCipher 新版需要在使用数据库前手动加载 native 库
        System.loadLibrary("sqlcipher");
        appExecutors=new AppExecutors();
        applicationContext=this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.applicationContext);

        MyPreferences preferences = MyPreferences.getInstance(this);

        //主题模式：跟随系统/强制亮色/强制暗色，需在 Activity 创建前应用；
        //动态取色由 BaseActivity 在视图创建前逐 Activity 应用，关闭后可立即恢复默认配色
        AppCompatDelegate.setDefaultNightMode(resolveNightMode(preferences.getThemeMode()));

        //应用语言：在 Activity 创建前应用，system 表示跟随系统
        applyAppLanguage(preferences.getAppLanguage());

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedActivityCount++;
                //已有新页面启动，取消配置变更时的延迟锁定
                mainHandler.removeCallbacks(lockIfBackground);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (startedActivityCount > 0) {
                    startedActivityCount--;
                }
                if (activity.isChangingConfigurations()) {
                    //配置变更会重建 Activity，延迟判断避免旋转等场景误锁
                    mainHandler.removeCallbacks(lockIfBackground);
                    mainHandler.postDelayed(lockIfBackground, 500);
                } else if (startedActivityCount == 0) {
                    //全部页面离开前台，下次进入需要重新解锁
                    AppLock.lock();
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }

    private int resolveNightMode(String themeMode) {
        if ("light".equals(themeMode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if ("dark".equals(themeMode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private void applyAppLanguage(String languageTag) {
        if (languageTag == null || languageTag.isEmpty() || "system".equals(languageTag)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        }
    }
}
