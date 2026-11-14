package com.fou.passport.ui;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.fou.passport.R;
import com.fou.passport.databinding.ActivityLoginBinding;
import com.fou.passport.ui.widget.PatternLockView;
import com.fou.passport.until.AppLock;
import com.fou.passport.until.MyPreferences;
import com.fou.passport.until.SystemAuthHelper;

/**
 * 应用解锁页：优先使用系统指纹/面容/设备密码，应用自身密码或图案作为备用。
 */
public class Login extends BaseActivity {
    /**
     * 仅验证身份：用于修改解锁方式前的二次确认，验证通过后返回 RESULT_OK。
     */
    public static final String EXTRA_VERIFY_ONLY = "EXTRA_VERIFY_ONLY";

    private ActivityLoginBinding binding;
    private boolean verifyOnly;
    private boolean authenticating;
    private boolean passwordEnabled;
    private boolean patternEnabled;
    private BiometricPrompt biometricPrompt;
    private ActivityResultLauncher<Intent> deviceCredentialLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //启用自动填充 只启用Login
        getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_AUTO);

        verifyOnly = getIntent().getBooleanExtra(EXTRA_VERIFY_ONLY, false);
        deviceCredentialLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        onAuthenticated();
                    } else {
                        //取消系统密码验证后恢复应用密码/图案入口
                        authenticating = false;
                        setSystemPromptShowing(false);
                    }
                });

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (verifyOnly) {
            //修改解锁方式前先验证身份，标题不再显示“解锁”
            binding.textView.setText(R.string.verify_identity);
        }
        setupCredentialUi();
        setupSystemUnlockUi();
        binding.buttonSignIn.setOnClickListener(v -> signInWithPassword());
    }

    @Override
    protected boolean isUnlockExempt() {
        return true;
    }

    /**
     * 已启用密码时显示密码表单，已启用图案时显示图案控件，两者可同时显示。
     */
    private void setupCredentialUi() {
        MyPreferences preferences = MyPreferences.getInstance(this);
        passwordEnabled = preferences.hasPasswordCredential();
        patternEnabled = preferences.hasPatternCredential();

        setCredentialUiVisible(true);

        if (patternEnabled) {
            binding.patternLockLogin.setOnPatternListener((dots, patternString) -> {
                if (dots.size() < PatternLockView.MIN_DOTS) {
                    binding.patternLockLogin.showError();
                    Toast.makeText(this, R.string.pattern_too_short, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (preferences.verifyMasterPattern(patternString)) {
                    onAuthenticated();
                } else {
                    binding.patternLockLogin.showError();
                    Toast.makeText(this, R.string.pattern_incorrect, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 控制应用密码/图案表单的显隐：系统验证弹窗展示期间隐藏，取消后再显示。
     */
    private void setCredentialUiVisible(boolean visible) {
        int passwordVisibility = visible && passwordEnabled ? View.VISIBLE : View.GONE;
        binding.textInputLayoutPassword.setVisibility(passwordVisibility);
        binding.buttonSignIn.setVisibility(passwordVisibility);
        binding.patternHintLogin.setVisibility(visible && patternEnabled ? View.VISIBLE : View.GONE);
        binding.patternLockLogin.setVisibility(visible && patternEnabled ? View.VISIBLE : View.GONE);
    }

    /**
     * 系统验证弹窗展示期间，连“使用指纹或设备密码解锁”按钮也隐藏，
     * 避免应用密码输入界面露在系统弹窗下面。
     */
    private void setSystemPromptShowing(boolean showing) {
        setCredentialUiVisible(!showing);
        binding.buttonSystemUnlock.setVisibility(showing || !SystemAuthHelper.isSystemUnlockAvailable(this)
                ? View.GONE : View.VISIBLE);
    }

    /**
     * 有系统锁屏时显示并使用系统指纹/设备密码解锁；没有应用凭证时它就是唯一入口。
     */
    private void setupSystemUnlockUi() {
        boolean available = SystemAuthHelper.isSystemUnlockAvailable(this);
        binding.buttonSystemUnlock.setVisibility(available ? View.VISIBLE : View.GONE);
        binding.buttonSystemUnlock.setOnClickListener(v -> promptSystemUnlock());
        if (available) {
            //先把应用密码界面藏起来再弹系统验证，避免系统弹窗下面露出密码输入框
            setSystemPromptShowing(true);
            binding.getRoot().post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    promptSystemUnlock();
                }
            });
        }
    }

    private void promptSystemUnlock() {
        if (authenticating || !SystemAuthHelper.isSystemUnlockAvailable(this)) {
            return;
        }
        authenticating = true;
        setSystemPromptShowing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android 11+ 可在一个系统弹窗里任选指纹/面容/设备密码
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.verify_identity))
                    .setSubtitle(getString(R.string.system_unlock_subtitle))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
            getBiometricPrompt().authenticate(promptInfo);
            return;
        }
        if (SystemAuthHelper.isBiometricAvailable(this)) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.verify_identity))
                    .setSubtitle(getString(R.string.system_unlock_subtitle))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .setNegativeButtonText(getString(R.string.cancel))
                    .build();
            getBiometricPrompt().authenticate(promptInfo);
            return;
        }
        //Android 10 以下没有指纹时，改用系统锁屏密码验证
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        Intent intent = keyguardManager == null ? null
                : keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.verify_identity), getString(R.string.system_unlock_subtitle));
        if (intent == null) {
            authenticating = false;
            setSystemPromptShowing(false);
            return;
        }
        deviceCredentialLauncher.launch(intent);
    }

    private BiometricPrompt getBiometricPrompt() {
        if (biometricPrompt == null) {
            biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                @NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            authenticating = false;
                            onAuthenticated();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode,
                                                          @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            authenticating = false;
                            //取消或失败后恢复应用密码/图案入口，便于继续解锁
                            setSystemPromptShowing(false);
                            if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                    && errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                    && errorCode != BiometricPrompt.ERROR_CANCELED) {
                                Toast.makeText(Login.this, errString, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
        return biometricPrompt;
    }

    private void signInWithPassword() {
        MyPreferences preferences = MyPreferences.getInstance(this);
        if (!preferences.hasAnyCredential()) {
            Toast.makeText(this, R.string.error_master_not_set, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, CreateAccount.class));
            finish();
            return;
        }
        if (!preferences.hasPasswordCredential()) {
            //只启用了图案时不支持密码登录
            Toast.makeText(this, R.string.password_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        String password = binding.editTextPassword.getText() == null
                ? "" : binding.editTextPassword.getText().toString().trim();
        if (password.isEmpty()) {
            Toast.makeText(this, R.string.error_password_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!preferences.verifyMasterPassword(password)) {
            Toast.makeText(this, R.string.password_incorrect, Toast.LENGTH_SHORT).show();
            return;
        }
        onAuthenticated();
    }

    /**
     * 正常进入应用；验证模式下只返回验证结果，不修改应用锁状态。
     */
    private void onAuthenticated() {
        if (verifyOnly) {
            setResult(RESULT_OK);
            finish();
            return;
        }
        AppLock.unlock();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
