package com.fou.passport.until;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.text.InputType;
import android.view.View;

import org.junit.Test;

public class TypeTest {

    private static final String[] NO_HINTS = new String[0];

    @Test
    public void passwordVariationIsRecognized() {
        int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        assertEquals(Type.PASSWORD, Type.getKeyByHint(null, inputType, NO_HINTS));
    }

    @Test
    public void numberPasswordVariationIsRecognized() {
        int inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        assertEquals(Type.PASSWORD, Type.getKeyByHint(null, inputType, NO_HINTS));
    }

    @Test
    public void multilineTextIsNotMistakenForPassword() {
        //多行文本的标志位曾与密码变体按位与命中，导致普通输入框被判成密码
        int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        assertNull(Type.getKeyByHint(null, inputType, NO_HINTS));
    }

    @Test
    public void verificationCodeIsIgnored() {
        assertNull(Type.getKeyByHint("手机验证码", InputType.TYPE_CLASS_PHONE, NO_HINTS));
        assertNull(Type.getKeyByHint("SMS code", InputType.TYPE_CLASS_TEXT, NO_HINTS));
    }

    @Test
    public void phoneHintIsPhone() {
        assertEquals(Type.PHONE, Type.getKeyByHint("手机号", InputType.TYPE_CLASS_PHONE, NO_HINTS));
    }

    @Test
    public void emailVariationIsEmail() {
        int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        assertEquals(Type.EMAIL, Type.getKeyByHint(null, inputType, NO_HINTS));
    }

    @Test
    public void emailHintIsEmail() {
        assertEquals(Type.EMAIL,
                Type.getKeyByHint("邮箱地址", InputType.TYPE_CLASS_TEXT, NO_HINTS));
    }

    @Test
    public void autofillUsernameHintIsUsername() {
        assertEquals(Type.USERNAME, Type.getKeyByHint(null, InputType.TYPE_CLASS_TEXT,
                new String[]{View.AUTOFILL_HINT_USERNAME}));
    }

    @Test
    public void accountHintIsUsername() {
        assertEquals(Type.USERNAME,
                Type.getKeyByHint("登录账号", InputType.TYPE_CLASS_TEXT, NO_HINTS));
    }

    @Test
    public void bankCardHintIsCard() {
        assertEquals(Type.CARD,
                Type.getKeyByHint("银行卡号", InputType.TYPE_CLASS_NUMBER, NO_HINTS));
    }

    @Test
    public void securityCodeHintIsCvv() {
        assertEquals(Type.CVV, Type.getKeyByHint("CVV", InputType.TYPE_CLASS_NUMBER, NO_HINTS));
    }

    @Test
    public void plainTextIsIgnored() {
        assertNull(Type.getKeyByHint("备注", InputType.TYPE_CLASS_TEXT, NO_HINTS));
    }
}
