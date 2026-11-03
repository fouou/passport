package com.fou.passport.ui.settingfragment;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.ui.BaseActivity;
import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
    SwitchPreference mSwitchPreference_compatible_mode, mSwitchPreference_autofill, switch_preference_shoot_cut;
    ListPreference theme_preference;
    private static final int REQUEST_CODE_SET_DEFAULT = 1;
    AutofillManager mAutofillManager;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.pref_settingsfragment);

        bindingViews();
        mAutofillManager = getContext().getSystemService(AutofillManager.class);
        if (mAutofillManager != null && !mAutofillManager.hasEnabledAutofillServices()) {
            mSwitchPreference_autofill.setChecked(false);
        }else {
            mSwitchPreference_autofill.setChecked(true);
        }
    }

    private void bindingViews() {
        mSwitchPreference_autofill = findPreference("switch_preference_autofill");
        mSwitchPreference_compatible_mode = findPreference("switch_preference_compatible_mode");
        switch_preference_shoot_cut = findPreference("switch_preference_shoot_cut");
        theme_preference = findPreference("theme_preference");
        mSwitchPreference_autofill.setOnPreferenceChangeListener(this);
        mSwitchPreference_compatible_mode.setOnPreferenceChangeListener(this);
        theme_preference.setOnPreferenceChangeListener(this);
        switch_preference_shoot_cut.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mSwitchPreference_autofill.equals(preference)) {
            setService((Boolean)newValue);//自动填充服务
        }else if (mSwitchPreference_compatible_mode.equals(preference)) {
            setCompatible((Boolean) newValue);//兼容模式
        }else if(theme_preference.equals(preference)){
            setTheme((String)newValue);//主题
        }else if(switch_preference_shoot_cut.equals(preference)){
            setShootCut((Boolean) newValue);//主题
        }
        return true;
    }

    private void setShootCut(Boolean bool) {
        Intent intent=new Intent();
        intent.setAction(BaseActivity.ACTION_SHOOT_CUT);
        intent.putExtra(BaseActivity.INTENT_KEY_SHOOT_CUT_IS_PERMIT,bool);
        getContext().sendBroadcast(intent);
    }

    private void setTheme(String theme) {
        switch (theme) {
            case "system":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                break;
            case "light":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
            case "dark":
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
        }
    }

    private void setCompatible(Boolean newValue) {
        boolean isOn = newValue;
        if (isOn) {

            // 执行开关打开时的操作
        } else {
            // 执行开关关闭时的操作
        }
    }

    private void setService(boolean isChecked) {
        if (isChecked) {
            startEnableService();
        }else {
            disableService();
        }
    }

    private void startEnableService() {
        if (mAutofillManager != null && !mAutofillManager.hasEnabledAutofillServices()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
            intent.setData(Uri.parse("package:com.example.android.autofill.service"));
            startActivity(intent);
        }
    }

    private void disableService() {
        if (mAutofillManager != null && mAutofillManager.hasEnabledAutofillServices()) {
            mAutofillManager.disableAutofillServices();
        } else {
        }
    }
}
