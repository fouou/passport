package com.fou.passport.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.fou.passport.MyApplication;
import com.google.android.material.color.DynamicColors;

public class BaseActivity extends AppCompatActivity {
    public static final String ACTION_SHOOT_CUT = "ACTION_SHOOT_CUT";
    public static final String INTENT_KEY_SHOOT_CUT_IS_PERMIT = "INTENT_KEY_SHOOT_CUT_IS_PERMIT";
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //禁用自动填充
        getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        //动态启用禁用截屏(广播)
        beginShoot_cut();

    }


    /**
     * 截屏
     */
    private void beginShoot_cut() {
//初始化截屏
        boolean isPermit_shoot_cut = MyApplication.sharedPreferences.getBoolean("switch_preference_shoot_cut", false);
        setPermit_shoot_cut(isPermit_shoot_cut);
//接受广播
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    boolean isPermit_shoot_cut = intent.getBooleanExtra(INTENT_KEY_SHOOT_CUT_IS_PERMIT, false);
                    setPermit_shoot_cut(isPermit_shoot_cut);
                }
            }
        };
//注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SHOOT_CUT);
        registerReceiver(broadcastReceiver, intentFilter);
    }



    private void setPermit_shoot_cut(boolean isPermit_shoot_cut) {
        if (isPermit_shoot_cut) {
            //允许截屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            return;
        }
        //禁用截屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver!=null) {
            unregisterReceiver(broadcastReceiver);
        }
    }
}
