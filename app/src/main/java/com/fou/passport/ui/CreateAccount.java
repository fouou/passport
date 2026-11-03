package com.fou.passport.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fou.passport.R;
import com.fou.passport.databinding.ActivityCreatAccountBinding;

public class CreateAccount extends BaseActivity {
private ActivityCreatAccountBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityCreatAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //关于spannableString的页面绘制
        baseUiSetting();

    }

    private void baseUiSetting() {
        //设置单选框中隐私与政策的高亮字体
        SpannableString spannableString = new SpannableString("I agree with our Terms and Conditions");
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.white));
        ForegroundColorSpan foregroundColorSpan2 = new ForegroundColorSpan(getResources().getColor(R.color.white));
        spannableString.setSpan(foregroundColorSpan, 17, 22, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);//高亮
        spannableString.setSpan(foregroundColorSpan2, 27, 37, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);//高亮
        URLSpan urlSpan = new URLSpan("https://www.baidu.com/s?ie=UTF-8&wd=陈奕迅");//跳转term
        spannableString.setSpan(urlSpan, 17, 22, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        URLSpan urlSpan2 = new URLSpan("https://www.baidu.com/s?ie=UTF-8&wd=赵璐思");//跳转conditions
        spannableString.setSpan(urlSpan2, 27, 37, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.checkBox.setMovementMethod(LinkMovementMethod.getInstance());
        binding.checkBox.setText(spannableString);

        //设置已有账户，登录中的高亮字体
        SpannableString spannableString2 = new SpannableString("Already have an account? Sign in");
        ForegroundColorSpan foregroundColorSpan3 = new ForegroundColorSpan(getResources().getColor(R.color.white));//高亮
        spannableString2.setSpan(foregroundColorSpan3, 25, 32, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        ClickableSpan clickableSpan=new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(CreateAccount.this,Login.class));
            }
        };//跳转
        spannableString2.setSpan(clickableSpan, 25, 32, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.textViewSignIn.setMovementMethod(LinkMovementMethod.getInstance());
        binding.textViewSignIn.setText(spannableString2);


    }
}