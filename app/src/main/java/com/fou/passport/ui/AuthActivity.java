package com.fou.passport.ui;

import static android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE;
import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;
import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET;
import static com.fou.passport.service.DatasetAdapter.indexOf;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityAuthBinding;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.service.ClientParser;
import com.fou.passport.ui.widget.PatternLockView;
import com.fou.passport.until.Log;
import com.fou.passport.until.MyPreferences;
import com.fou.passport.until.SystemAuthHelper;
import com.fou.passport.until.Type;

import java.util.ArrayList;
import java.util.List;

public class AuthActivity extends BaseActivity {
    // Unique id for dataset intents.
    private static int sDatasetPendingIntentId = 0;
    public static final String EXTRA_DATASET_ID = "EXTRA_DATASET_ID";
    //跨应用通用填充模式：0 普通回填，1 只回填账号类字段，2 只回填银行卡类字段（密码永不参与）
    public static final String EXTRA_GENERIC_MODE = "EXTRA_GENERIC_MODE";
    //通用填充候选中被选中的具体字段与值，保证按用户看到的那一条回填
    public static final String EXTRA_GENERIC_KEY = "EXTRA_GENERIC_KEY";
    public static final String EXTRA_GENERIC_VALUE = "EXTRA_GENERIC_VALUE";
    public static final int GENERIC_MODE_NONE = 0;
    public static final int GENERIC_MODE_IDENTITY = 1;
    public static final int GENERIC_MODE_CARD = 2;
    //主密码连续失败限制
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MILLIS = 30_000L;
    private Intent mReplyIntent;
    private String package_name;
    private ActivityAuthBinding binding;
    private boolean hasPasswordCredential;
    private boolean hasPatternCredential;
    private boolean showingPattern;
    private BiometricPrompt biometricPrompt;
    private boolean authenticating;
    private ActivityResultLauncher<Intent> deviceCredentialLauncher;

    @Override
    protected boolean isUnlockExempt() {
        //自动填充验证由系统直接拉起，不能先跳转应用解锁页
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceCredentialLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    authenticating = false;
                    if (result.getResultCode() == RESULT_OK) {
                        onDeviceCredentialVerified();
                    } else {
                        //取消系统验证后：有应用凭证则恢复输入卡片，否则结束
                        restoreCredentialUiIfNeeded();
                    }
                });

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        package_name = getPackageName();
        //先绑定密码校验界面：生物识别不可用或被取消时仍可输入主密码
        bindingViews();
        //系统验证优先
        canAuthenticate();
    }

    private void bindingViews() {
        setupPasswordToggle();
        binding.ok.setOnClickListener(v -> attemptPasswordUnlock());
        binding.cancel.setOnClickListener(v -> AuthActivity.this.finish());
        binding.switchMode.setOnClickListener(v -> applyUnlockMode(!showingPattern));

        MyPreferences preferences = MyPreferences.getInstance(this);
        hasPasswordCredential = preferences.hasPasswordCredential();
        hasPatternCredential = preferences.hasPatternCredential();
        if (!hasPasswordCredential && !hasPatternCredential) {
            if (!SystemAuthHelper.isSystemUnlockAvailable(this)) {
                //既没有应用凭证也没有系统锁屏，无法验证身份，不允许取用凭据
                Toast.makeText(MyApplication.applicationContext,
                        R.string.error_master_not_set, Toast.LENGTH_SHORT).show();
                finish();
            }
            //只有系统验证可用：隐藏应用凭证卡片，稍后弹出系统验证
            return;
        }
        binding.patternLock.setOnPatternListener((dots, patternString) -> {
            if (dots.size() < PatternLockView.MIN_DOTS) {
                Toast.makeText(MyApplication.applicationContext,
                        R.string.pattern_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!checkLockout(preferences)) {
                binding.patternLock.clearPattern();
                return;
            }
            if (preferences.verifyMasterPattern(patternString)) {
                onUnlocked();
            } else {
                binding.patternLock.showError();
                onUnlockFailed(R.string.pattern_incorrect);
            }
        });
        //两种方式都启用时默认图案，密码通过切换按钮使用
        applyUnlockMode(hasPatternCredential);
    }

    /**
     * 按当前选择的解锁方式显示密码输入框或图案控件。
     */
    private void applyUnlockMode(boolean usePattern) {
        showingPattern = usePattern;
        binding.ok.setVisibility(usePattern || !hasPasswordCredential ? View.GONE : View.VISIBLE);
        binding.patternHint.setVisibility(usePattern ? View.VISIBLE : View.GONE);
        binding.patternLock.setVisibility(usePattern ? View.VISIBLE : View.GONE);
        binding.passwordLayout.setVisibility(hasPasswordCredential && !usePattern
                ? View.VISIBLE : View.GONE);
        binding.switchMode.setText(usePattern
                ? R.string.unlock_with_password : R.string.unlock_with_pattern);
        //只有两种都启用时才需要切换入口
        binding.switchMode.setVisibility(hasPasswordCredential && hasPatternCredential
                ? View.VISIBLE : View.GONE);
        if (usePattern) {
            binding.patternLock.clearPattern();
        }
    }

    /**
     * 密码可见性图标：隐藏（掩码）时显示斜杠眼，显示明文时显示睁眼。
     */
    private void setupPasswordToggle() {
        updatePasswordToggleIcon();
        binding.passwordLayout.setEndIconOnClickListener(v -> {
            boolean masked = binding.password.getTransformationMethod()
                    instanceof PasswordTransformationMethod;
            binding.password.setTransformationMethod(
                    masked ? null : new PasswordTransformationMethod());
            updatePasswordToggleIcon();
            if (binding.password.getText() != null) {
                binding.password.setSelection(binding.password.getText().length());
            }
        });
    }

    private void updatePasswordToggleIcon() {
        boolean masked = binding.password.getTransformationMethod()
                instanceof PasswordTransformationMethod;
        binding.passwordLayout.setEndIconDrawable(masked
                ? R.drawable.visibility_off_fill0_wght700_grad0_opsz48
                : R.drawable.visibility_fill0_wght700_grad0_opsz48);
        binding.passwordLayout.setEndIconContentDescription(getString(masked
                ? R.string.show_password : R.string.hide_password));
    }

    private void attemptPasswordUnlock() {
        MyPreferences preferences = MyPreferences.getInstance(AuthActivity.this);
        if (!checkLockout(preferences)) {
            return;
        }
        if (!preferences.hasPasswordCredential()) {
            //未启用密码解锁时不允许用密码取用凭据
            Toast.makeText(MyApplication.applicationContext,
                    R.string.password_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        Editable password = binding.password.getText();
        if (preferences.verifyMasterPassword(
                password == null ? null : password.toString().trim())) {
            onUnlocked();
        } else {
            onUnlockFailed(R.string.password_incorrect);
        }
    }

    private void onUnlocked() {
        MyPreferences preferences = MyPreferences.getInstance(AuthActivity.this);
        preferences.setAuthFailedAttempts(0);
        preferences.setAuthLockedUntil(0L);
        MyApplication.appExecutors.diskIO().execute(() -> {
            beginAutofill();
        });
    }

    private void onUnlockFailed(int messageRes) {
        MyPreferences preferences = MyPreferences.getInstance(AuthActivity.this);
        int failedAttempts = preferences.getAuthFailedAttempts() + 1;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            preferences.setAuthFailedAttempts(0);
            preferences.setAuthLockedUntil(System.currentTimeMillis() + LOCKOUT_MILLIS);
        } else {
            preferences.setAuthFailedAttempts(failedAttempts);
        }
        Toast.makeText(MyApplication.applicationContext, messageRes, Toast.LENGTH_SHORT).show();
    }

    /**
     * 连续失败过多时锁定一段时间，返回 true 表示可以继续验证。
     */
    private boolean checkLockout(MyPreferences preferences) {
        long remaining = preferences.getAuthLockedUntil() - System.currentTimeMillis();
        if (remaining > 0) {
            Toast.makeText(MyApplication.applicationContext,
                    getString(R.string.auth_too_many_attempts, (remaining / 1000) + 1),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 优先使用系统指纹/设备密码验证；系统不可用时使用应用密码/图案。
     */
    private void canAuthenticate() {
        if (!SystemAuthHelper.isSystemUnlockAvailable(this)) {
            //没有系统锁屏：只能使用应用凭证，界面已就绪
            Log.AuthActivityy("设备没有可用的系统验证，使用应用凭证。");
            return;
        }
        //等界面挂载完成后再弹系统验证，避免窗口未获得焦点时弹窗失败
        binding.getRoot().post(() -> {
            if (!isFinishing() && !isDestroyed()) {
                promptSystemAuth();
            }
        });
    }

    /**
     * 系统验证通过后（生物识别或设备密码）执行填充。
     */
    private void onDeviceCredentialVerified() {
        MyApplication.appExecutors.diskIO().execute(this::beginAutofill);
    }

    /**
     * 系统验证取消/失败后恢复应用凭证卡片；没有应用凭证时直接结束。
     */
    private void restoreCredentialUiIfNeeded() {
        if (hasPasswordCredential || hasPatternCredential) {
            binding.credentialCard.setVisibility(View.VISIBLE);
            return;
        }
        finish();
    }

    private void promptSystemAuth() {
        if (authenticating) {
            return;
        }
        authenticating = true;
        //系统验证弹窗展示期间先隐藏应用密码/图案卡片，避免底下还露出一层输入界面
        binding.credentialCard.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android 11+ 可在同一个系统弹窗里任选指纹/面容/设备密码
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.app_name))
                    .setSubtitle(getString(R.string.biometric))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
            getBiometricPrompt().authenticate(promptInfo);
            return;
        }
        if (SystemAuthHelper.isBiometricAvailable(this)) {
            //Android 10 及以下系统弹窗不支持设备密码，只能先试指纹
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.app_name))
                    .setSubtitle(getString(R.string.biometric))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .setNegativeButtonText(getString(R.string.cancel))
                    .build();
            getBiometricPrompt().authenticate(promptInfo);
            return;
        }
        //没有指纹时改用系统锁屏密码验证
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        Intent intent = keyguardManager == null ? null
                : keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.unlock_passport), getString(R.string.unlock_to_fill));
        if (intent == null) {
            authenticating = false;
            restoreCredentialUiIfNeeded();
            return;
        }
        deviceCredentialLauncher.launch(intent);
    }

    private BiometricPrompt getBiometricPrompt() {
        if (biometricPrompt == null) {
            biometricPrompt = new BiometricPrompt(this,
                    MyApplication.appExecutors.mainThread(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode,
                                                  @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    authenticating = false;
                    //取消后恢复密码/图案卡片，便于继续使用应用凭证
                    restoreCredentialUiIfNeeded();
                    if (hasPasswordCredential || hasPatternCredential) {
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                && errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_CANCELED) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.authentication_error) + errString,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    authenticating = false;
                    onDeviceCredentialVerified();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }
        return biometricPrompt;
    }

    public static IntentSender getAuthIntentSenderForDataset(Context originContext, int dataset_id) {
        return getAuthIntentSenderForDataset(originContext, dataset_id, GENERIC_MODE_NONE);
    }

    public static IntentSender getAuthIntentSenderForDataset(Context originContext, int dataset_id,
                                                             int genericMode) {
        return getAuthIntentSenderForDataset(originContext, dataset_id, genericMode, null, null);
    }

    public static IntentSender getAuthIntentSenderForDataset(Context originContext, int dataset_id,
                                                             int genericMode, String genericKey,
                                                             String genericValue) {
        Intent intent = new Intent(originContext, AuthActivity.class);
        intent.putExtra(EXTRA_DATASET_ID, dataset_id);
        intent.putExtra(EXTRA_GENERIC_MODE, genericMode);
        if (genericKey != null && genericValue != null) {
            intent.putExtra(EXTRA_GENERIC_KEY, genericKey);
            intent.putExtra(EXTRA_GENERIC_VALUE, genericValue);
        }
        //必须是 FLAG_MUTABLE：系统要在认证 Intent 中补充 EXTRA_ASSIST_STRUCTURE 等参数，
        //设为 IMMUTABLE 会导致认证页拿不到界面结构，认证后没有任何字段被回填
        return PendingIntent.getActivity(originContext, ++sDatasetPendingIntentId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE).getIntentSender();
    }

    private void beginAutofill() {
        Intent intent = getIntent();
        //只包含自动填充的view
        AssistStructure structure = intent.getParcelableExtra(EXTRA_ASSIST_STRUCTURE);
        Log.AuthActivityy("beginAutofill structure=" + (structure == null ? "null" : "ok"));
        int dataset_id = intent.getIntExtra(EXTRA_DATASET_ID, -1);
        int genericMode = intent.getIntExtra(EXTRA_GENERIC_MODE, GENERIC_MODE_NONE);
        if (structure == null || dataset_id == -1) {
            finish();
            return;
        }
        DatasetAndMetadata datasetAndMetadata =
                MyDB.getDb().getDatasetDao().getDatasetAndMetadataInstance(dataset_id);
        if (datasetAndMetadata == null || datasetAndMetadata.metadataEntityList == null) {
            finish();
            return;
        }
        String genericKey = intent.getStringExtra(EXTRA_GENERIC_KEY);
        String genericValue = intent.getStringExtra(EXTRA_GENERIC_VALUE);
        boolean useSelectedValue = genericMode == GENERIC_MODE_IDENTITY
                && genericKey != null && genericValue != null;
        //通用填充只使用候选里被选中的那一条账号值，避免同一账户的其它账号字段被填进去
        DatasetAndMetadata fillData = useSelectedValue
                ? selectedDataset(datasetAndMetadata, genericKey, genericValue) : datasetAndMetadata;
        ClientParser clientParser = new ClientParser(structure);
        mReplyIntent = new Intent();
        //认证结果必须是纯值 Dataset：系统会直接用它回填；若返回 FillResponse，系统只会刷新选择列表
        Dataset.Builder builder = new Dataset.Builder();
        int[] valueCount = {0};
        clientParser.parse(viewNode -> {
            if (View.AUTOFILL_TYPE_TEXT == viewNode.getAutofillType()) {
                String key = Type.getKey(viewNode);
                if (key != null && fillTextNode(viewNode, key, fillData, genericMode, builder)) {
                    valueCount[0]++;
                }
                return;
            }
            if (viewNode.getId() == -1) {
                return;
            }
            for (MetadataEntity metadataEntity : fillData.metadataEntityList) {
                if (!isGenericModeAllowed(metadataEntity, genericMode)) {
                    continue;
                }
                if (viewNode.getId() == metadataEntity.xml_id
                        && viewNode.getAutofillType() == metadataEntity.auto_fill_type
                        && setValueToDatasetBuilder(viewNode, metadataEntity, builder)) {
                    valueCount[0]++;
                }
            }
        });
        if (valueCount[0] == 0) {
            //当前页面没有匹配到任何可回填字段；构建空 Dataset 会抛异常导致闪退，直接取消
            Log.AuthActivityy("没有可回填的字段，取消本次填充");
            mReplyIntent = null;
            finish();
            return;
        }
        //EXTRA_AUTHENTICATION_RESULT 放 Dataset：替换原数据集并立即回填
        mReplyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, builder.build());
        //用完即弃，避免替换掉带 RemoteViews 的原始数据集后，下次选择列表无法展示
        mReplyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true);
        finish();
    }

    /**
     * 用候选里被选中的那一条账号值构造填充数据。
     */
    private static DatasetAndMetadata selectedDataset(DatasetAndMetadata source,
                                                      String keyWord, String value) {
        DatasetAndMetadata selected = new DatasetAndMetadata();
        selected.datasetEntity = source.datasetEntity;
        selected.metadataEntityList = new ArrayList<>();
        selected.metadataEntityList.add(new MetadataEntity(keyWord, value));
        return selected;
    }

    /**
     * 文本输入框回填：账号框与候选列表逻辑保持一致（优先用户名，没有则用手机号/邮箱）。
     */
    private boolean fillTextNode(AssistStructure.ViewNode viewNode, String key,
                                 DatasetAndMetadata datasetAndMetadata, int genericMode,
                                 Dataset.Builder builder) {
        if (Type.USERNAME.equals(key)) {
            MetadataEntity username = firstMetadata(datasetAndMetadata, genericMode, Type.USERNAME);
            if (username != null && setValueToDatasetBuilder(viewNode, username, builder)) {
                return true;
            }
            MetadataEntity phoneOrEmail = firstMetadata(datasetAndMetadata, genericMode,
                    Type.PHONE, Type.EMAIL);
            return phoneOrEmail != null
                    && setValueToDatasetBuilder(viewNode, phoneOrEmail, builder);
        }
        boolean filled = false;
        for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
            if (isGenericModeAllowed(metadataEntity, genericMode)
                    && key.equals(metadataEntity.key_word)
                    && setValueToDatasetBuilder(viewNode, metadataEntity, builder)) {
                filled = true;
            }
        }
        return filled;
    }

    /**
     * @return 指定字段中第一个有值的元数据，没有则返回 null。
     */
    private static MetadataEntity firstMetadata(DatasetAndMetadata datasetAndMetadata, int genericMode,
                                                String... keyWords) {
        for (String keyWord : keyWords) {
            for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                if (isGenericModeAllowed(metadataEntity, genericMode)
                        && keyWord.equals(metadataEntity.key_word)
                        && metadataEntity.auto_fill_value_text != null
                        && !metadataEntity.auto_fill_value_text.trim().isEmpty()) {
                    return metadataEntity;
                }
            }
        }
        return null;
    }

    /**
     * 跨应用通用填充只回填对应类别字段，绝不把其它应用的密码填进来。
     */
    private static boolean isGenericModeAllowed(MetadataEntity metadataEntity, int genericMode) {
        if (genericMode == GENERIC_MODE_IDENTITY) {
            return Type.isIdentityKey(metadataEntity.key_word);
        }
        if (genericMode == GENERIC_MODE_CARD) {
            return Type.isCardKey(metadataEntity.key_word);
        }
        return true;
    }

    @Override
    public void finish() {
        if (mReplyIntent != null) {
            setResult(RESULT_OK, mReplyIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.finish();
    }

    /**
     * Setting value to datasetBuilder
     *
     * @param viewNode
     * @param metadataEntity
     * @param builder
     * @return Whether or not values have been added to the DatasetBuilder.
     */
    private boolean setValueToDatasetBuilder(AssistStructure.ViewNode viewNode, MetadataEntity metadataEntity, Dataset.Builder builder) {
        AutofillId autofillId = viewNode.getAutofillId();
        if (autofillId == null) {
            return false;
        }
        int autofillType = viewNode.getAutofillType();
        switch (autofillType) {
            case View.AUTOFILL_TYPE_LIST:
                Log.AuthActivityy("AUTOFILL_TYPE_LIST");

                String listValue = metadataEntity.auto_fill_value_list;
                CharSequence[] options = viewNode.getAutofillOptions();
                if (options != null && options.length > 0 && listValue != null) {
                    int listPosition = indexOf(options, listValue);
                    if (listPosition != -1) {
                        builder.setValue(autofillId, AutofillValue.forList(listPosition));
                        return true;
                    }
                }
                return false;
            case View.AUTOFILL_TYPE_DATE:
                Log.AuthActivityy("AUTOFILL_TYPE_DATE");

                long dateValue = metadataEntity.auto_fill_value_date;
                builder.setValue(autofillId, AutofillValue.forDate(dateValue));
                return true;
            case View.AUTOFILL_TYPE_TEXT:
                Log.AuthActivityy("AUTOFILL_TYPE_TEXT");

                String textValue = metadataEntity.auto_fill_value_text;
                if (textValue != null) {
                    builder.setValue(autofillId, AutofillValue.forText(textValue));
                    return true;
                }
                return false;
            case View.AUTOFILL_TYPE_TOGGLE:
                Log.AuthActivityy("AUTOFILL_TYPE_TOGGLE");

                boolean toggleValue = metadataEntity.auto_fill_value_toggle;
                builder.setValue(autofillId, AutofillValue.forToggle(toggleValue));
                return true;
            case View.AUTOFILL_TYPE_NONE:
            default:
                Log.AuthActivityy("default");

                return false;
        }
    }

}
