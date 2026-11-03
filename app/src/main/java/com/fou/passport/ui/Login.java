package com.fou.passport.ui;

import android.app.assist.AssistStructure;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fou.passport.R;
import com.fou.passport.databinding.ActivityLoginBinding;

public class Login extends BaseActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //启用自动填充 只启用Login
        getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_AUTO);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //关于spannableString的页面绘制
        baseUiSet();
        binding.buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, MainActivity.class));
                finish();
            }
        });
    }

    private void baseUiSet() {
        //设置已有账户，登录中的高亮字体
        SpannableString spannableString = new SpannableString("Don't have an account? Create account");
        ForegroundColorSpan foregroundColorSpan3 = new ForegroundColorSpan(getResources().getColor(R.color.white));//高亮
        spannableString.setSpan(foregroundColorSpan3, 23, 37, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        ClickableSpan clickableSpan=new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(Login.this,CreateAccount.class));
            }
        };//跳转
        spannableString.setSpan(clickableSpan,23, 37, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.textViewCreateAccount.setMovementMethod(LinkMovementMethod.getInstance());//不设置 没有点击事件
        binding.textViewCreateAccount.setText(spannableString);
    }
}