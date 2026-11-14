package com.fou.passport.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Toast;

import com.fou.passport.R;
import com.fou.passport.databinding.ActivityCreatAccountBinding;
import com.fou.passport.ui.widget.PatternLockView;
import com.fou.passport.until.AppLock;
import com.fou.passport.until.MyPreferences;

public class CreateAccount extends BaseActivity {
    //设置页以编辑模式打开本页，用于修改已有的解锁方式
    public static final String EXTRA_EDIT_CREDENTIAL = "EXTRA_EDIT_CREDENTIAL";
    private ActivityCreatAccountBinding binding;
    private String pendingPattern;
    private boolean editMode;

    @Override
    protected boolean isUnlockExempt() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        editMode = getIntent().getBooleanExtra(EXTRA_EDIT_CREDENTIAL, false);
        setupPasswordToggle();
        binding.usePassword.setOnCheckedChangeListener((button, checked) -> updateCredentialVisibility());
        binding.usePattern.setOnCheckedChangeListener((button, checked) -> updateCredentialVisibility());
        binding.patternLock.setOnPatternListener((dots, patternString) -> {
            if (dots.size() < PatternLockView.MIN_DOTS) {
                pendingPattern = null;
                Toast.makeText(this, R.string.pattern_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            pendingPattern = patternString;
            Toast.makeText(this, R.string.pattern_saved, Toast.LENGTH_SHORT).show();
        });
        MyPreferences preferences = MyPreferences.getInstance(this);
        if (editMode) {
            //编辑模式回显当前已启用的方式，两种可以同时勾选
            binding.usePassword.setChecked(preferences.hasPasswordCredential());
            binding.usePattern.setChecked(preferences.hasPatternCredential());
            applyEditModeUi();
        } else {
            binding.usePassword.setChecked(true);
            binding.usePattern.setChecked(false);
        }
        updateCredentialVisibility();
        binding.buttonSignIn.setOnClickListener(v -> createAccount());
    }

    /**
     * 勾选状态决定显示哪个输入区，密码与图案可同时显示。
     */
    private void updateCredentialVisibility() {
        boolean usePassword = binding.usePassword.isChecked();
        boolean usePattern = binding.usePattern.isChecked();
        binding.passwordLayout.setVisibility(usePassword ? View.VISIBLE : View.GONE);
        binding.patternHint.setVisibility(usePattern ? View.VISIBLE : View.GONE);
        binding.patternLock.setVisibility(usePattern ? View.VISIBLE : View.GONE);
        if (!usePattern) {
            pendingPattern = null;
            binding.patternLock.clearPattern();
        }
        MyPreferences preferences = MyPreferences.getInstance(this);
        //编辑模式：留空/不重画表示保留原凭证
        binding.passwordLayout.setHint(preferences.hasPasswordCredential()
                ? R.string.password_keep_hint : R.string.password);
        binding.patternHint.setText(preferences.hasPatternCredential()
                ? R.string.pattern_keep_hint : R.string.draw_pattern_hint);
    }

    private void applyEditModeUi() {
        binding.title.setText(R.string.change_unlock_credential);
        binding.subtitle.setText(R.string.change_unlock_credential_desc);
        binding.buttonSignIn.setText(R.string.save_changes);
        binding.checkBox.setVisibility(View.GONE);
    }

    /**
     * 密码可见性图标：隐藏（掩码）时显示斜杠眼，显示明文时显示睁眼。
     */
    private void setupPasswordToggle() {
        updatePasswordToggleIcon();
        binding.passwordLayout.setEndIconOnClickListener(v -> {
            boolean masked = binding.editTextPassword.getTransformationMethod()
                    instanceof PasswordTransformationMethod;
            binding.editTextPassword.setTransformationMethod(
                    masked ? null : new PasswordTransformationMethod());
            updatePasswordToggleIcon();
            if (binding.editTextPassword.getText() != null) {
                binding.editTextPassword.setSelection(
                        binding.editTextPassword.getText().length());
            }
        });
    }

    private void updatePasswordToggleIcon() {
        boolean masked = binding.editTextPassword.getTransformationMethod()
                instanceof PasswordTransformationMethod;
        binding.passwordLayout.setEndIconDrawable(masked
                ? R.drawable.visibility_off_fill0_wght700_grad0_opsz48
                : R.drawable.visibility_fill0_wght700_grad0_opsz48);
        binding.passwordLayout.setEndIconContentDescription(getString(masked
                ? R.string.show_password : R.string.hide_password));
    }

    private void createAccount() {
        MyPreferences preferences = MyPreferences.getInstance(this);
        if (!editMode && preferences.hasAnyCredential()) {
            Toast.makeText(this, R.string.error_master_exists, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        if (!editMode && !binding.checkBox.isChecked()) {
            Toast.makeText(this, R.string.error_agree_terms, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean usePassword = binding.usePassword.isChecked();
        boolean usePattern = binding.usePattern.isChecked();
        if (!usePassword && !usePattern) {
            Toast.makeText(this, R.string.error_enable_one_credential, Toast.LENGTH_SHORT).show();
            return;
        }
        String password = binding.editTextPassword.getText() == null
                ? "" : binding.editTextPassword.getText().toString().trim();
        if (usePassword && password.isEmpty() && !preferences.hasPasswordCredential()) {
            Toast.makeText(this, R.string.error_password_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (usePattern && pendingPattern == null && !preferences.hasPatternCredential()) {
            Toast.makeText(this, R.string.draw_pattern_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        //先完成全部校验再落盘，避免中途失败留下半套凭证
        if (usePassword) {
            if (!password.isEmpty()) {
                //仅保存加盐哈希，不落明文
                preferences.setMasterPassword(password);
            }
        } else if (preferences.hasPasswordCredential()) {
            preferences.removePasswordCredential();
        }
        if (usePattern) {
            if (pendingPattern != null) {
                preferences.setMasterPattern(pendingPattern);
            }
        } else if (preferences.hasPatternCredential()) {
            preferences.removePatternCredential();
        }
        if (editMode) {
            Toast.makeText(this, R.string.credential_updated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Toast.makeText(this, usePattern && !usePassword
                ? R.string.pattern_saved : R.string.master_set_success, Toast.LENGTH_SHORT).show();
        //首次设置完成后直接进入应用，不再让用户重复输入同一凭证
        AppLock.unlock();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
