package com.fou.passport.ui.settingfragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import com.fou.passport.until.AutofillCompat;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.BaseActivity;
import com.fou.passport.ui.CreateAccount;
import com.fou.passport.ui.DetailActivity;
import com.fou.passport.ui.Login;
import com.fou.passport.ui.listfragment.ListAdapter;
import com.fou.passport.ui.recyclebin.RecycleBinActivity;
import com.fou.passport.until.BackupManager;
import com.fou.passport.until.DemoDataGenerator;
import com.fou.passport.until.MyPreferences;
import com.fou.passport.until.Type;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
    private static final String KEY_BACKUP_EXPORT = "preference_backup_export";
    private static final String KEY_BACKUP_IMPORT = "preference_backup_import";
    SwitchPreference mSwitchPreference_autofill, switch_preference_shoot_cut;
    SwitchPreference switch_preference_dynamic_color;
    Preference preference_theme_color;
    ListPreference theme_preference, language_preference;
    Preference preference_backup_export, preference_backup_import;
    Preference preference_quick_access, preference_security_check, preference_recycle_bin;
    Preference preference_change_credential;
    Preference preference_demo_data_category, preference_generate_demo_data, preference_clear_demo_data;
    private static final int REQUEST_CODE_SET_DEFAULT = 1;

    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<Intent> verifyBeforeChangeLauncher;
    private String pendingExportPassword;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.pref_settingsfragment);
        //必须在 Fragment 到达 STARTED 之前注册
        registerBackupLaunchers();
        bindingViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mSwitchPreference_autofill.setChecked(
                    MyPreferences.getInstance(getContext()).isOurAutofillServiceEnabled());
        } else {
            //Android 8.0 以下没有系统自动填充框架，只作为密码本使用
            mSwitchPreference_autofill.setVisible(false);
        }
    }

    private void registerBackupLaunchers() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                uri -> {
                    String password = pendingExportPassword;
                    pendingExportPassword = null;
                    if (uri != null && password != null) {
                        exportBackup(uri, password);
                    }
                });
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        promptBackupPassword(R.string.backup_import_title,
                                password -> importBackup(uri, password));
                    }
                });
        verifyBeforeChangeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        startActivity(new Intent(getContext(), CreateAccount.class)
                                .putExtra(CreateAccount.EXTRA_EDIT_CREDENTIAL, true));
                    }
                });
    }

    private void bindingViews() {
        mSwitchPreference_autofill = findPreference("switch_preference_autofill");
        switch_preference_shoot_cut = findPreference("switch_preference_shoot_cut");
        theme_preference = findPreference("theme_preference");
        language_preference = findPreference("language_preference");
        switch_preference_dynamic_color = findPreference("switch_preference_dynamic_color");
        preference_theme_color = findPreference("preference_theme_color");
        preference_backup_export = findPreference(KEY_BACKUP_EXPORT);
        preference_backup_import = findPreference(KEY_BACKUP_IMPORT);
        preference_quick_access = findPreference("preference_quick_access");
        preference_change_credential = findPreference("preference_change_credential");
        preference_security_check = findPreference("preference_security_check");
        preference_recycle_bin = findPreference("preference_recycle_bin");
        mSwitchPreference_autofill.setOnPreferenceChangeListener(this);
        theme_preference.setOnPreferenceChangeListener(this);
        //持久化交给 MyPreferences，避免与默认 SharedPreferences 出现两份状态
        theme_preference.setValue(MyPreferences.getInstance(getContext()).getThemeMode());
        updateThemeSummary(theme_preference.getValue());
        switch_preference_shoot_cut.setOnPreferenceChangeListener(this);
        language_preference.setOnPreferenceChangeListener(this);
        language_preference.setValue(MyPreferences.getInstance(getContext()).getAppLanguage());
        updateLanguageSummary(language_preference.getValue());
        initThemeColorPreferences();
        preference_backup_export.setOnPreferenceClickListener(preference -> {
            promptBackupPassword(R.string.backup_export_title, password -> {
                pendingExportPassword = password;
                exportLauncher.launch(backupFileName());
            });
            return true;
        });
        //备份文件是自定义扩展名，放开类型过滤才能选到
        preference_backup_import.setOnPreferenceClickListener(preference -> {
            importLauncher.launch(new String[]{"*/*"});
            return true;
        });
        preference_quick_access.setOnPreferenceClickListener(preference -> {
            showQuickAccess();
            return true;
        });
        preference_change_credential.setOnPreferenceClickListener(preference -> {
            //修改解锁方式前先验证身份，防止他人拿到已解锁的手机后直接改掉密码
            verifyBeforeChangeLauncher.launch(new Intent(getContext(), Login.class)
                    .putExtra(Login.EXTRA_VERIFY_ONLY, true));
            return true;
        });
        updateCredentialSummary();
        preference_security_check.setOnPreferenceClickListener(preference -> {
            runSecurityCheck();
            return true;
        });
        preference_recycle_bin.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getContext(), RecycleBinActivity.class));
            return true;
        });
        initDemoDataPreferences();
    }

    /**
     * 演示数据入口只在调试包显示，正式包自动隐藏。
     */
    private void initDemoDataPreferences() {
        preference_demo_data_category = findPreference("preference_demo_data_category");
        preference_generate_demo_data = findPreference("preference_generate_demo_data");
        preference_clear_demo_data = findPreference("preference_clear_demo_data");
        boolean debuggable = (requireContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        preference_demo_data_category.setVisible(debuggable);
        preference_generate_demo_data.setVisible(debuggable);
        preference_clear_demo_data.setVisible(debuggable);
        if (!debuggable) {
            return;
        }
        preference_generate_demo_data.setOnPreferenceClickListener(preference -> {
            generateDemoData();
            return true;
        });
        preference_clear_demo_data.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.clear_demo_data)
                    .setMessage(R.string.clear_demo_data_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, (dialog, which) -> clearDemoData())
                    .show();
            return true;
        });
    }

    private void generateDemoData() {
        Context context = requireContext();
        Context appContext = context.getApplicationContext();
        MyApplication.appExecutors.diskIO().execute(() -> {
            int count = DemoDataGenerator.generate(appContext);
            MyApplication.appExecutors.mainThread().execute(() -> {
                if (getContext() == null) {
                    return;
                }
                Toast.makeText(getContext(), getString(R.string.demo_data_generated, count), Toast.LENGTH_SHORT).show();
                getContext().sendBroadcast(new Intent(BaseActivity.ACTION_DATABASE_CHANGED));
            });
        });
    }

    private void clearDemoData() {
        Context appContext = requireContext().getApplicationContext();
        MyApplication.appExecutors.diskIO().execute(() -> {
            DemoDataGenerator.clear(appContext);
            MyApplication.appExecutors.mainThread().execute(() -> {
                if (getContext() == null) {
                    return;
                }
                Toast.makeText(getContext(), R.string.demo_data_cleared, Toast.LENGTH_SHORT).show();
                getContext().sendBroadcast(new Intent(BaseActivity.ACTION_DATABASE_CHANGED));
            });
        });
    }

    /**
     * 快速访问：展示 DetailActivity 里收藏过的账户。
     */
    private void showQuickAccess() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<DatasetAndMetadata> favorites = MyDB.getDb().getDatasetDao().getFavoriteDatasetAndMetadataList();
            Map<Integer, PackageEntity> packageMap = loadPackageMap();
            List<String> entries = new ArrayList<>();
            for (DatasetAndMetadata datasetAndMetadata : favorites) {
                entries.add(accountLabel(datasetAndMetadata, packageMap));
            }
            MyApplication.appExecutors.mainThread().execute(() -> {
                if (favorites.isEmpty()) {
                    new MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.quick_access)
                            .setMessage(R.string.quick_access_empty)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    return;
                }
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.quick_access)
                        .setItems(entries.toArray(new String[0]), (dialog, which) ->
                                context.startActivity(new Intent(context, DetailActivity.class)
                                        .putExtra(ListAdapter.KEY_INTENT,
                                                favorites.get(which).datasetEntity.package_id)))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    /**
     * 安全检查：扫描重复密码与弱密码。
     */
    private void runSecurityCheck() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<DatasetAndMetadata> datasetAndMetadataList =
                    MyDB.getDb().getDatasetDao().getDatasetAndMetadataList();
            Map<Integer, PackageEntity> packageMap = loadPackageMap();
            Map<String, List<String>> passwordAccounts = new HashMap<>();
            List<String> weakEntries = new ArrayList<>();
            for (DatasetAndMetadata datasetAndMetadata : datasetAndMetadataList) {
                String password = findPassword(datasetAndMetadata.metadataEntityList);
                if (password == null || password.isEmpty()) {
                    continue;
                }
                String label = accountLabel(datasetAndMetadata, packageMap);
                List<String> accounts = passwordAccounts.get(password);
                if (accounts == null) {
                    accounts = new ArrayList<>();
                    passwordAccounts.put(password, accounts);
                }
                accounts.add(label);
                if (isWeakPassword(password)) {
                    weakEntries.add(getString(R.string.security_check_weak_entry, label));
                }
            }
            List<String> issues = new ArrayList<>(weakEntries);
            for (List<String> accounts : passwordAccounts.values()) {
                if (accounts.size() > 1) {
                    issues.add(getString(R.string.security_check_duplicate_entry, accounts.size()));
                }
            }
            MyApplication.appExecutors.mainThread().execute(() -> {
                String message = issues.isEmpty()
                        ? getString(R.string.security_check_ok)
                        : android.text.TextUtils.join("\n", issues);
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.security_check)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            });
        });
    }

    private Map<Integer, PackageEntity> loadPackageMap() {
        Map<Integer, PackageEntity> packageMap = new HashMap<>();
        for (PackageEntity packageEntity : MyDB.getDb().getPackageDao().getPackageEntityList()) {
            packageMap.put(packageEntity.id, packageEntity);
        }
        return packageMap;
    }

    private String accountLabel(DatasetAndMetadata datasetAndMetadata, Map<Integer, PackageEntity> packageMap) {
        PackageEntity packageEntity = packageMap.get(datasetAndMetadata.datasetEntity.package_id);
        String appName = packageEntity == null || packageEntity.app_name == null
                ? "" : packageEntity.app_name;
        String account = "";
        if (datasetAndMetadata.metadataEntityList != null) {
            for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                if (metadataEntity.auto_fill_value_text != null
                        && !metadataEntity.auto_fill_value_text.isEmpty()
                        && Arrays.asList(Type.strings).contains(metadataEntity.key_word)) {
                    account = metadataEntity.auto_fill_value_text;
                    break;
                }
            }
        }
        return account.isEmpty() ? appName : appName + " / " + account;
    }

    private String findPassword(List<MetadataEntity> metadataEntityList) {
        if (metadataEntityList == null) {
            return null;
        }
        for (MetadataEntity metadataEntity : metadataEntityList) {
            if (Type.PASSWORD.equals(metadataEntity.key_word)) {
                return metadataEntity.auto_fill_value_text;
            }
        }
        return null;
    }

    /**
     * 长度过短、纯数字、全部字符相同都视为弱密码。
     */
    private boolean isWeakPassword(String password) {
        if (password.length() < 8) {
            return true;
        }
        boolean allDigits = true;
        boolean allSame = true;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c < '0' || c > '9') {
                allDigits = false;
            }
            if (c != password.charAt(0)) {
                allSame = false;
            }
        }
        return allDigits || allSame;
    }

    @Override
    public void onResume() {
        super.onResume();
        //修改解锁方式返回后刷新摘要
        updateCredentialSummary();
    }

    /**
     * 摘要显示当前已启用的解锁方式。
     */
    private void updateCredentialSummary() {
        if (preference_change_credential == null) {
            return;
        }
        MyPreferences preferences = MyPreferences.getInstance(getContext());
        int summaryRes;
        if (preferences.hasPasswordCredential() && preferences.hasPatternCredential()) {
            summaryRes = R.string.credential_both;
        } else if (preferences.hasPatternCredential()) {
            summaryRes = R.string.credential_pattern;
        } else {
            summaryRes = R.string.credential_password;
        }
        preference_change_credential.setSummary(summaryRes);
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        //ListPreference 默认用 AppCompat 的方形对话框，这里换成 M3 圆角对话框
        if (preference instanceof ListPreference) {
            showListPreferenceDialog((ListPreference) preference);
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }

    private void showListPreferenceDialog(ListPreference listPreference) {
        CharSequence[] entries = listPreference.getEntries();
        CharSequence[] entryValues = listPreference.getEntryValues();
        if (entries == null || entryValues == null) {
            return;
        }
        int checkedIndex = listPreference.findIndexOfValue(listPreference.getValue());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(listPreference.getTitle())
                .setSingleChoiceItems(entries, checkedIndex, (dialog, which) -> {
                    String value = entryValues[which].toString();
                    if (listPreference.callChangeListener(value)) {
                        listPreference.setValue(value);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mSwitchPreference_autofill.equals(preference)) {
            setService((Boolean)newValue);//自动填充服务
        }else if(theme_preference.equals(preference)){
            setTheme((String)newValue);//主题
        }else if(switch_preference_shoot_cut.equals(preference)){
            setShootCut((Boolean) newValue);//主题
        }else if(switch_preference_dynamic_color.equals(preference)){
            setDynamicColor((Boolean) newValue);//动态取色
        }else if(language_preference.equals(preference)){
            setLanguage((String) newValue);//语言
        }
        return true;
    }

    /**
     * 切换应用语言：system 跟随系统，其余为 BCP-47 语言标签。
     */
    private void setLanguage(String languageTag) {
        MyPreferences.getInstance(getContext()).setAppLanguage(languageTag);
        updateLanguageSummary(languageTag);
        if ("system".equals(languageTag)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        }
    }

    private void updateLanguageSummary(String languageTag) {
        if (language_preference == null) {
            return;
        }
        int index = language_preference.findIndexOfValue(languageTag);
        CharSequence[] entries = language_preference.getEntries();
        if (index >= 0 && index < entries.length) {
            language_preference.setSummary(entries[index]);
        }
    }

    private void setShootCut(Boolean bool) {
        Intent intent=new Intent();
        intent.setAction(BaseActivity.ACTION_SHOOT_CUT);
        intent.putExtra(BaseActivity.INTENT_KEY_SHOOT_CUT_IS_PERMIT,bool);
        getContext().sendBroadcast(intent);
    }

    private void initThemeColorPreferences() {
        MyPreferences preferences = MyPreferences.getInstance(getContext());
        //旧版本允许两者同时为真，统一收敛为「自定义主题色优先」
        if (preferences.getThemeSeedColor() != 0 && preferences.isDynamicColorEnabled()) {
            preferences.setDynamicColorEnabled(false);
        }
        switch_preference_dynamic_color.setChecked(preferences.isDynamicColorEnabled());
        switch_preference_dynamic_color.setOnPreferenceChangeListener(this);
        preference_theme_color.setOnPreferenceClickListener(preference -> {
            showThemeColorPicker();
            return true;
        });
        boolean supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        switch_preference_dynamic_color.setEnabled(supported);
        preference_theme_color.setEnabled(supported);
        updateThemeColorSummary(supported);
    }

    private void updateThemeColorSummary(boolean supported) {
        MyPreferences preferences = MyPreferences.getInstance(getContext());
        if (!supported) {
            switch_preference_dynamic_color.setSummary(R.string.theme_color_unavailable);
            preference_theme_color.setSummary(R.string.theme_color_unavailable);
            return;
        }
        int seedColor = preferences.getThemeSeedColor();
        if (seedColor != 0) {
            //自定义主题色优先，壁纸取色不再生效
            switch_preference_dynamic_color.setChecked(false);
            switch_preference_dynamic_color.setSummary(R.string.dynamic_color_override_by_custom);
            preference_theme_color.setSummary(getString(R.string.theme_color_custom,
                    String.format(Locale.US, "#%06X", 0xFFFFFF & seedColor)));
            return;
        }
        boolean dynamicColor = preferences.isDynamicColorEnabled();
        switch_preference_dynamic_color.setChecked(dynamicColor);
        switch_preference_dynamic_color.setSummary(R.string.dynamic_color_summary);
        preference_theme_color.setSummary(dynamicColor
                ? R.string.theme_color_follow_wallpaper : R.string.theme_color_off);
    }

    private void setDynamicColor(boolean enabled) {
        MyPreferences preferences = MyPreferences.getInstance(getContext());
        preferences.setDynamicColorEnabled(enabled);
        if (enabled) {
            //开启壁纸取色时清除自定义种子色，避免两套颜色互相覆盖
            preferences.setThemeSeedColor(0);
        }
        recreateActivity();
    }

    /**
     * 自定义色盘：选出的颜色作为独立主题色生成整套 Material You 色板。
     */
    private void showThemeColorPicker() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        ColorWheelView colorWheel = content.findViewById(R.id.colorWheel);
        TextView colorValue = content.findViewById(R.id.textView_color_value);
        MyPreferences preferences = MyPreferences.getInstance(context);
        int seedColor = preferences.getThemeSeedColor();
        int initialColor = seedColor != 0 ? seedColor : themePrimaryColor();
        colorWheel.setColor(initialColor);
        updateColorPreview(colorValue, initialColor);
        colorWheel.setOnColorChangedListener(color -> updateColorPreview(colorValue, color));
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.color_picker_title)
                .setView(content)
                .setNeutralButton(R.string.color_picker_reset, null)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            applyThemeSeedColor(colorWheel.getColor());
            dialog.dismiss();
        });
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
            applyThemeSeedColor(0);
            dialog.dismiss();
        });
    }

    private int themePrimaryColor() {
        View root = getView();
        if (root != null) {
            return MaterialColors.getColor(root, com.google.android.material.R.attr.colorPrimary);
        }
        return 0xFF6750A4;
    }

    private void updateColorPreview(TextView view, int color) {
        view.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & color));
        view.setBackgroundTintList(ColorStateList.valueOf(color));
        view.setTextColor(ColorUtils.calculateLuminance(color) > 0.5 ? Color.BLACK : Color.WHITE);
    }

    private void applyThemeSeedColor(int color) {
        MyPreferences preferences = MyPreferences.getInstance(getContext());
        preferences.setThemeSeedColor(color);
        //主题色与壁纸取色互斥，选色后关闭壁纸取色
        preferences.setDynamicColorEnabled(false);
        recreateActivity();
    }

    private void recreateActivity() {
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private void setTheme(String theme) {
        MyPreferences.getInstance(getContext()).setThemeMode(theme);
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
        updateThemeSummary(theme);
    }

    private void updateThemeSummary(String value) {
        int index = theme_preference.findIndexOfValue(value);
        if (index >= 0) {
            theme_preference.setSummary(theme_preference.getEntries()[index]);
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        if (!MyPreferences.getInstance(getContext()).isOurAutofillServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            startActivity(intent);
        }
    }

    private void disableService() {
        if (MyPreferences.getInstance(getContext()).isOurAutofillServiceEnabled()) {
            AutofillCompat.disableAutofillServices(getContext());
        }
    }

    private interface OnPasswordEntered {
        void onPasswordEntered(String password);
    }

    /**
     * 让用户输入备份口令；口令为空时不关闭弹窗。
     */
    private void promptBackupPassword(@StringRes int titleRes, OnPasswordEntered callback) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_backup_password, null);
        TextInputLayout inputLayout = content.findViewById(R.id.textInputLayout_backup_password);
        TextInputEditText editText = content.findViewById(R.id.editText_backup_password);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = editText.getText() == null ? "" : editText.getText().toString();
            if (password.isEmpty()) {
                inputLayout.setError(getString(R.string.backup_password_empty));
                return;
            }
            inputLayout.setError(null);
            dialog.dismiss();
            callback.onPasswordEntered(password);
        });
    }

    private String backupFileName() {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        return "passport-backup-" + date + ".pass";
    }

    private void exportBackup(Uri uri, String password) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        MyApplication.appExecutors.diskIO().execute(() -> {
            try (OutputStream out = appContext.getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    throw new IOException("cannot open output stream");
                }
                BackupManager.export(out, password);
                showToast(R.string.backup_export_success);
            } catch (BackupManager.EmptyBackupException e) {
                showToast(R.string.backup_empty);
            } catch (Exception e) {
                showToast(R.string.backup_export_failed);
            }
        });
    }

    private void importBackup(Uri uri, String password) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        MyApplication.appExecutors.diskIO().execute(() -> {
            try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    throw new IOException("cannot open input stream");
                }
                BackupManager.BackupData data = BackupManager.read(in, password);
                if (data.packages.isEmpty()) {
                    showToast(R.string.backup_empty);
                    return;
                }
                MyApplication.appExecutors.mainThread().execute(() -> askRestoreMode(data));
            } catch (Exception e) {
                showToast(R.string.backup_import_failed);
            }
        });
    }

    private void askRestoreMode(BackupManager.BackupData data) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.backup_restore_title)
                .setMessage(R.string.backup_restore_message)
                .setNeutralButton(R.string.cancel, null)
                .setNegativeButton(R.string.backup_restore_overwrite,
                        (dialog, which) -> restore(data, true))
                .setPositiveButton(R.string.backup_restore_append,
                        (dialog, which) -> restore(data, false))
                .show();
    }

    private void restore(BackupManager.BackupData data, boolean overwrite) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        MyApplication.appExecutors.diskIO().execute(() -> {
            try {
                BackupManager.restore(data, overwrite);
                //通知列表页重新读取数据库
                appContext.sendBroadcast(new Intent(BaseActivity.ACTION_DATABASE_CHANGED));
                showToast(R.string.backup_import_success);
            } catch (Exception e) {
                showToast(R.string.backup_import_failed);
            }
        });
    }

    private void showToast(@StringRes int resId) {
        MyApplication.appExecutors.mainThread().execute(() ->
                Toast.makeText(MyApplication.applicationContext, resId, Toast.LENGTH_SHORT).show());
    }
}
