package com.fou.passport;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.fou.passport.room.MyDB;
import com.fou.passport.until.AppExecutors;
import com.google.android.material.color.DynamicColors;

public class MyApplication extends Application {
    public static AppExecutors appExecutors;
    public static Context applicationContext;
    public static SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        appExecutors=new AppExecutors();
        applicationContext=this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.applicationContext);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }
}
