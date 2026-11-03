package com.fou.passport.ui;

import static android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE;
import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;
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
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityAuthBinding;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.service.ClientParser;
import com.fou.passport.service.DatasetAdapter;
import com.fou.passport.service.RemoteViewsHelper;
import com.fou.passport.until.Log;
import com.fou.passport.until.MyPreferences;
import com.fou.passport.until.Type;

import java.util.List;

public class AuthActivity extends BaseActivity {
    // Unique id for dataset intents.
    private static int sDatasetPendingIntentId = 0;
    public static final String EXTRA_DATASET_ID = "EXTRA_DATASET_ID";
    private Intent mReplyIntent;
    private String package_name;
    private ActivityAuthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        package_name = getPackageName();
        //生物识别
        canAuthenticate();
    }

    private void bindingViews() {
        binding.ok.setOnClickListener(v -> {
            Editable password = binding.password.getText();
            String correctPassword = MyPreferences.getInstance(AuthActivity.this).getMasterPassword();
            if (password.toString().equals(correctPassword)) {//密码正确
                MyApplication.appExecutors.diskIO().execute(() -> {
                    beginAutofill();
                });
            } else {
                Toast.makeText(MyApplication.applicationContext, R.string.password_incorrect, Toast.LENGTH_SHORT).show();
            }
        });
        binding.cancel.setOnClickListener(v -> AuthActivity.this.finish());
    }

    private void canAuthenticate() {
        BiometricManager biometricManager = BiometricManager.from(this.getApplicationContext());
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.AuthActivityy("应用可以进行生物识别技术进行身份验证。");
                showBiometricPrompt();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                bindingViews();
                Log.AuthActivityy("该设备上没有搭载可用的生物特征功能。");

                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                bindingViews();

                Log.AuthActivityy("生物识别功能当前不可用。");

                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                bindingViews();
                Log.AuthActivityy("用户没有录入生物识别数据。");
                break;
            default:
                bindingViews();
        }
    }

    public static IntentSender getAuthIntentSenderForDataset(Context originContext, int dataset_id) {
        Intent intent = new Intent(originContext, AuthActivity.class);
        intent.putExtra(EXTRA_DATASET_ID, dataset_id);
        return PendingIntent.getActivity(originContext, ++sDatasetPendingIntentId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender();
    }

    public static IntentSender getAuthIntentSenderForDataset2(Context originContext, int dataset_id) {
        KeyguardManager keyguardManager=originContext.getSystemService(KeyguardManager.class);
        if (keyguardManager.isKeyguardSecure()) {
            Intent intent=keyguardManager.createConfirmDeviceCredentialIntent("n","m");
            if (intent!=null){
                intent.putExtra(EXTRA_DATASET_ID, dataset_id);
                return PendingIntent.getActivity(originContext, ++sDatasetPendingIntentId, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender();
            }
        }
        return  null;
    }


    //生物认证的setting
    private void showBiometricPrompt() {
        //需要提供的参数callback
        BiometricPrompt biometricPrompt = new BiometricPrompt(AuthActivity.this,
                MyApplication.appExecutors.mainThread(), new BiometricPrompt.AuthenticationCallback() {
            //各种异常的回调
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                                getString(R.string.authentication_error) + errString, Toast.LENGTH_LONG).show();
            }

            //认证成功的回调
            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                MyApplication.appExecutors.diskIO().execute(() -> {
                    beginAutofill();
                });


                /*BiometricPrompt.CryptoObject authenticatedCryptoObject =
                        result.getCryptoObject();*/
                // User has verified the signature, cipher, or message
                // authentication code (MAC) associated with the crypto object,
                // so you can use it in your app's crypto-driven workflows.
            }

            //认证失败的回调
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.biometric))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        // 显示认证对话框
        biometricPrompt.authenticate(promptInfo);
    }

    private void beginAutofill() {
        Intent intent = getIntent();
        //只包含自动填充的view
        AssistStructure structure = intent.getParcelableExtra(EXTRA_ASSIST_STRUCTURE);
        ClientParser clientParser = new ClientParser(structure);
        int dataset_id = intent.getIntExtra(EXTRA_DATASET_ID, -1);
        if (dataset_id != -1) {
            DatasetAndMetadata datasetAndMetadata = MyDB.getDb().getDatasetDao().getDatasetAndMetadataInstance(dataset_id);
            mReplyIntent = new Intent();
            Dataset.Builder builder = new Dataset.Builder(RemoteViewsHelper.viewsWithNoAuth(package_name, DatasetAdapter.getText(datasetAndMetadata)));
            clientParser.parse(viewNode -> {
                for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                    if (View.AUTOFILL_TYPE_TEXT == viewNode.getAutofillType()) {
                        String key=Type.getKey(viewNode);
                        if (key!=null && key.equals(metadataEntity.key_word)) {
                            setValueToDatasetBuilder(viewNode, metadataEntity, builder);
                        }
                    } else if (viewNode.getId() != -1 && viewNode.getId() == metadataEntity.xml_id) {
                        setValueToDatasetBuilder(viewNode, metadataEntity, builder);
                    }
                }
            });
            mReplyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, builder.build());
        }
        finish();
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