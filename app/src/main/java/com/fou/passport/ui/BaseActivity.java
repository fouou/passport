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
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.fou.passport.MyApplication;
import com.fou.passport.until.AppLock;
import com.fou.passport.until.MyPreferences;
import com.fou.passport.until.SystemAuthHelper;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class BaseActivity extends AppCompatActivity {
    public static final String ACTION_SHOOT_CUT = "ACTION_SHOOT_CUT";
    public static final String INTENT_KEY_SHOOT_CUT_IS_PERMIT = "INTENT_KEY_SHOOT_CUT_IS_PERMIT";
    //备份导入等场景修改数据库后通知界面刷新
    public static final String ACTION_DATABASE_CHANGED = "ACTION_DATABASE_CHANGED";
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //动态取色必须在视图创建前应用，关闭后重建 Activity 即可回到默认配色
        applyDynamicColors();
        super.onCreate(savedInstanceState);
        //禁用自动填充
        getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        //动态启用禁用截屏(广播)
        beginShoot_cut();

    }


    /**
     * 壁纸取色与自定义色盘取色共用同一套 Material You 动态取色接口。
     */
    private void applyDynamicColors() {
        MyPreferences preferences = MyPreferences.getInstance(this);
        int seedColor = preferences.getThemeSeedColor();
        //自定义主题色独立生效；只有两者都没设置时才保持默认配色
        if (seedColor == 0 && !preferences.isDynamicColorEnabled()) {
            return;
        }
        DynamicColorsOptions.Builder builder = new DynamicColorsOptions.Builder();
        if (seedColor != 0) {
            builder.setContentBasedSource(seedColor);
        }
        DynamicColors.applyToActivityIfAvailable(this, builder.build());
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
//注册广播（应用内部广播，targetSdk 33+ 必须显式声明未导出，否则 SecurityException）
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SHOOT_CUT);
        ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
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
    protected void onStart() {
        super.onStart();
        //从后台回到前台时重新锁定：应用有凭证或系统已有锁屏时，除登录/注册/自动填充验证外的页面都要求先解锁
        if (!isFinishing() && !isUnlockExempt()
                && SystemAuthHelper.requiresUnlock(this)
                && !AppLock.isUnlocked()) {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }

    /**
     * 登录、首次设置与自动填充验证页不受应用锁限制。
     */
    protected boolean isUnlockExempt() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver!=null) {
            unregisterReceiver(broadcastReceiver);
        }
    }
}
