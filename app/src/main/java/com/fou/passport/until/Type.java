package com.fou.passport.until;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.service.autofill.SaveInfo;
import android.text.InputType;
import android.view.View;

import com.fou.passport.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ***** keyWords ******
 * <p>
 * Username (用户名)
 * Email (邮箱)
 * Password (密码)
 * Category (分类)
 * Website (网站) - for passwords, bank cards, certificates or licenses
 * Application (应用)
 * **Two-factor authentication (两步验证)
 * **Attachment (附件)
 * **Account association (账户关联)
 * Creation time (创建时间)
 * *Modification history (修改历史记录)
 * Card(卡号)
 * Issuing bank (开户行)
 * cvv (验证号（仅信用卡）)
 * Phone(手机号码)
 * *Name (名称（(证书或许可证)）)
 * *Code (编码(证书或许可证))
 */

public class Type {

    //自动填充框架获取
    public static final String PASSWORD = "password";
    public static final String USERNAME = "username";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    public static final String CARD = "card";
    public static final String CVV = "cvv";
    public static final String[] strings = new String[]{USERNAME, PHONE, EMAIL};

    //自己实现或用户填写
    public static final String OTHER_ISSUING_BANK = "issuing_bank";
    public static final String OTHER_CATEGORY = "category"; //展示时遍历生成
    public static final String OTHER_WEBSITE = "website";
    public static final String OTHER_APPLICATION = "application";
    public static final String OTHER_CREATE_TIME = "creation_time";
    public static final String OTHER_MODIFICATION_HISTORY = "modification_history";
    //此顺序影响着detailActivity里的字段排列顺序
    public static final String[] allStrings =
            new String[]{USERNAME, PHONE, EMAIL, PASSWORD, CARD ,OTHER_ISSUING_BANK, CVV ,OTHER_WEBSITE ,OTHER_APPLICATION ,OTHER_CREATE_TIME};

    //分区
    public static final String  CATEGORY_PASSWORD= "passwords";
    public static final String  CATEGORY_BANKCARD= "bank_card";
    public static final String  CATEGORY_Certificates= "certificates";



    /**
     * account，如果同时包含email，phone等关键词则认为是UserName
     *
     * @param hint
     * @param autofillHints
     * @return
     */
    private static boolean isUserName(String hint, List<String> autofillHintList) {
        String[] account = new String[]{"email" ,"phone", "mobile" ,"手机"  ,"电话" , "邮箱"};
        //hint中是否含多个关键词
        List<String> containAccountStrByHint = Arrays.stream(account).filter(hint::contains).collect(Collectors.toList());
        String[] account2 = new String[]{View.AUTOFILL_HINT_EMAIL_ADDRESS, View.AUTOFILL_HINT_PHONE};
        //AutofillHint中是否含多个关键词
        List<String> containAccountStrByAutofillHints = Arrays.stream(account2).filter(s -> autofillHintList.contains(s)).collect(Collectors.toList());
        if (autofillHintList.contains(View.AUTOFILL_HINT_USERNAME)
                ||hint.contains("account")
                ||hint.contains("username")
                ||hint.contains("账号")
                ||hint.contains("账户")
                ||hint.contains("用户名")
                || containAccountStrByAutofillHints.size() > 1
                || containAccountStrByHint.size() > 1) {
            return true;
        }
        return false;
    }

    /**
     * 获取每个输入框的填充类型
     *
     * @param node
     * @return 输入框的填充类型, 如果不是输入框（EditText）,则返回空；
     * 返回类型有：
     */
    //TODO whether the name be belonged to account.  多语言应该直接将所有语言写在这里面
    public static String getKey(AssistStructure.ViewNode node) {
        if (node.getAutofillType() != View.AUTOFILL_TYPE_TEXT) {
            return null;//如果不是输入框，直接返回（用到了hint,inputType等edittext专有属性）
        }
        // 获取输入框的提示信息、输入类型和Autofill Hints
        String hint = node.getHint() != null ? node.getHint().toLowerCase().replace(" ", "") : "";
        int inputType = node.getInputType();
        String[] autofillHints = node.getAutofillHints() != null ? node.getAutofillHints() : new String[0];
        List<String> autofillHintList = Arrays.asList(autofillHints);
        // 判断输入框类型
        if (autofillHintList.contains(View.AUTOFILL_HINT_PASSWORD)
                || (inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) !=0 // 判断是否为密码输入框
                || (inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) !=0 // 判断是否为密码输入框
                || (inputType & InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) !=0 // 判断是否为密码输入框
                || (inputType &  InputType.TYPE_NUMBER_VARIATION_PASSWORD)!=0) { // 判断是否为密码输入框
            // 处理密码输入框
            return PASSWORD;
        } else if (isUserName(hint, autofillHintList)) {
            //处理账户输入框
            return USERNAME;
        } else if (autofillHintList.contains(View.AUTOFILL_HINT_PHONE)
                || (inputType & InputType.TYPE_CLASS_PHONE) !=0
                || hint.contains("phone") // 判断是否为电话号码输入框
                || hint.contains("mobile")
                || hint.contains("手机")
                || hint.contains("电话")) {
            // 处理电话号码输入框
            return PHONE;
        } else if (autofillHintList.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS)
                || (inputType & InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) !=0
                || (inputType & InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)!=0
                || hint.contains("email")
                || hint.contains("邮箱")) {
            // 处理邮箱输入框
            return EMAIL;
        } else if (autofillHintList.contains(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
                || hint.contains("bankcard")
                || hint.contains("debitcard")
                || hint.contains("creditcard")
                || hint.contains("信用卡")
                || hint.contains("银行卡")) {
            // 处理信用卡输入框
            return CARD;
        } else if (autofillHintList.contains(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
                || hint.contains("cvv")
                || hint.contains("cvc")
                || hint.contains("csc")
                || hint.contains("securitycode")
                || hint.contains("安全码")
                || hint.contains("验证号")){
            // 处理信用卡输入框
            return CVV;
        }
        return null;
    }


    /**
     * get saveInfo Flag
     * @param keyWord
     * @return
     */
    public static int getTypeByKeyWord(String keyWord) {
        if (keyWord.equals(PASSWORD)) {
            return SaveInfo.SAVE_DATA_TYPE_PASSWORD;
        } else if (keyWord.equals(USERNAME)) {
            return SaveInfo.SAVE_DATA_TYPE_USERNAME;
        } else if (keyWord.equals(PHONE)) {
            return SaveInfo.SAVE_DATA_TYPE_USERNAME;
        } else if (keyWord.equals(EMAIL)) {
            return SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS;
        } else if (keyWord.equals(CARD)) {
            return SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD;
        } else if (keyWord.equals(CVV)) {
            return SaveInfo.SAVE_DATA_TYPE_GENERIC;
        }  else {
            return SaveInfo.SAVE_DATA_TYPE_GENERIC;
        }
    }

}