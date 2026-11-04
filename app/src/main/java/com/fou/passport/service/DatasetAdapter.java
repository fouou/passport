package com.fou.passport.service;

import static android.app.Activity.RESULT_OK;
import static com.fou.passport.until.Type.EMAIL;
import static com.fou.passport.until.Type.PHONE;
import static com.fou.passport.until.Type.USERNAME;
import static com.fou.passport.until.Util.logw;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.fou.passport.digital_asset_links.ParseWebDomain;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.ui.AuthActivity;
import com.fou.passport.until.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DatasetAdapter {
    private ClientParser clientParser;
    private Context context;
    boolean isAtLeastSetValue;
    private boolean isAtLeastSetValue1;

    public DatasetAdapter(ClientParser clientParser,Context context) {
        this.clientParser = clientParser;
        this.context=context;
    }

    /**
     * get a dataset that add a value at least
     *
     * @param datasetAndMetadata
     * @return Return a dataset when it has been added a value at least
     * Otherwise, return null.
     */
    public Dataset getDataset(DatasetAndMetadata datasetAndMetadata) {



        RemoteViews remoteViews = RemoteViewsHelper.viewsWithAuth(context.getPackageName(), getText(datasetAndMetadata));
        Dataset.Builder builder = new Dataset.Builder(remoteViews)
                .setAuthentication(AuthActivity.getAuthIntentSenderForDataset(context,datasetAndMetadata.datasetEntity.id));;
        isAtLeastSetValue1 = false;//Initialize isAtLeastSetValue to false
        clientParser.parseLastStructure(node -> {
            if (node.getAutofillType()== View.AUTOFILL_TYPE_TEXT) {
                autofillForEdittext(datasetAndMetadata, builder, node);//edit text
            }else {
                autofillForNotEdittext(datasetAndMetadata, builder, node);//not edittext
            }
        });
        if (isAtLeastSetValue) {//至少builder.setValue() 过一次才能为true
            return builder.build();
        }
        return null;
    }


    /**
     * 不是edittext的自动填充
     * @param datasetAndMetadata
     * @param builder
     * @param node
     */
    private void autofillForNotEdittext(DatasetAndMetadata datasetAndMetadata, Dataset.Builder builder, AssistStructure.ViewNode node) {
        if (node.getId()!=-1) {
            for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                //资源 id 跨页面可能重复，必须同时校验自动填充类型一致，
                //否则任意页面的开关/下拉等控件会因 id 碰撞被无关数据填充，导致到处弹建议
                if (node.getId()==metadataEntity.xml_id && node.getAutofillType() == metadataEntity.auto_fill_type) {
                    if (setValueToDatasetBuilder(node, metadataEntity, builder)) {
                        isAtLeastSetValue = true;//dataset add a value
                    }
                }
            }
        }
    }

    /**
     * edittext字段的自动填充
     * @param datasetAndMetadata
     * @param builder
     * @param node
     */
    private void autofillForEdittext(DatasetAndMetadata datasetAndMetadata, Dataset.Builder builder, AssistStructure.ViewNode node) {
        String key_word=Type.getKey(node);
        if (key_word!=null) {
            //usernameEdittext的填充（次方发更加智能）
            autofillForUsername(datasetAndMetadata, builder, node, key_word);
            //其它Edittext字段的填充
            autofillForOtherKeyword(datasetAndMetadata, builder, node, key_word);
        }
    }

    /**
     * 其它Edittext字段的填充
     * @param datasetAndMetadata
     * @param builder
     * @param node
     * @param key_word
     */
    private void autofillForOtherKeyword(DatasetAndMetadata datasetAndMetadata, Dataset.Builder builder, AssistStructure.ViewNode node, String key_word) {
        for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
            if (!key_word.equals(Type.USERNAME) && key_word.equals(metadataEntity.key_word)) {//TODO 存储的metadata中可能会有两个相同的关键账号
                if (setValueToDatasetBuilder(node, metadataEntity, builder)) {
                    isAtLeastSetValue = true;//dataset add a value
                }
            }
        }
    }

    /**
     * usernameEdittext的填充
     *
     * 因为某些原因存在邮箱或手机号却没有username，而下一次填充出现了新的username字段，则会将手机号或邮箱做为username弹出（存有username则不会进行此操作）
     *
     * [目前暂未按以下文本描述的方法保存，而是将包含多个字段的归为username，原因是没有必要]
     *  对于视图中有username但数据库中没有的情况
     *  是因为保存的时候将"请输入手机号或邮箱"的node归到了"手机号"或者"邮箱"的字段中,
     *  而开始填充的时候无法根据用户输入的内容来区分到底是邮箱还是手机号，
     *  故可以将此dataset中存在的手机号字段或者邮箱字段当作username字段进行自动填充
     * @param datasetAndMetadata
     * @param builder datasetBuilder
     * @param node
     * @param key_word node的key_word
     */
    private void autofillForUsername(DatasetAndMetadata datasetAndMetadata, Dataset.Builder builder, AssistStructure.ViewNode node, String key_word) {
        if (key_word.equals(Type.USERNAME)){
            List<MetadataEntity> metadataEntityList_username= datasetAndMetadata.metadataEntityList.stream().filter(metadataEntity -> Type.USERNAME.equals(metadataEntity.key_word)).collect(Collectors.toList());
            if (!metadataEntityList_username.isEmpty()) {
                if (setValueToDatasetBuilder(node,metadataEntityList_username.get(0) , builder)) {
                    isAtLeastSetValue = true;//dataset add a value
                }
            }else {
                List<MetadataEntity> metadataEntityList_phone_email= datasetAndMetadata.metadataEntityList.stream().filter(metadataEntity -> PHONE.equals(metadataEntity.key_word) || EMAIL.equals(metadataEntity.key_word)).collect(Collectors.toList());
                if (!metadataEntityList_phone_email.isEmpty()) {
                    if (setValueToDatasetBuilder(node,metadataEntityList_phone_email.get(0) , builder)) {
                        isAtLeastSetValue = true;//dataset add a value
                    }
                }
            }
        }
    }


    /**
     *  优先显示邮箱
     * @param datasetAndMetadata
     * @return 返回remoteViews中的text
     */
    public static String getText(DatasetAndMetadata datasetAndMetadata) {
        List<MetadataEntity> metadataEntityList_email = datasetAndMetadata.metadataEntityList.stream().filter(metadataEntity -> USERNAME.equals(metadataEntity.key_word)).collect(Collectors.toList());
        if (!metadataEntityList_email.isEmpty()
                && metadataEntityList_email.get(0).auto_fill_value_text != null) {
            return metadataEntityList_email.get(0).auto_fill_value_text;
        }

        List<MetadataEntity> metadataEntityList_phone = datasetAndMetadata.metadataEntityList.stream().filter(metadataEntity -> PHONE.equals(metadataEntity.key_word)).collect(Collectors.toList());
        if (!metadataEntityList_phone.isEmpty()
                && metadataEntityList_phone.get(0).auto_fill_value_text != null) {
            return metadataEntityList_phone.get(0).auto_fill_value_text;
        }

        List<MetadataEntity> metadataEntityList_account = datasetAndMetadata.metadataEntityList.stream().filter(metadataEntity -> EMAIL.equals(metadataEntity.key_word)).collect(Collectors.toList());
        if (!metadataEntityList_account.isEmpty()
                && metadataEntityList_account.get(0).auto_fill_value_text != null) {
            return metadataEntityList_account.get(0).auto_fill_value_text;
        }
        return "账户";//TODO  细化
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
            logw("Autofill ID null for %s", viewNode.toString());
            return false;
        }
        int autofillType = viewNode.getAutofillType();
        switch (autofillType) {
            case View.AUTOFILL_TYPE_LIST:
                String listValue = metadataEntity.auto_fill_value_list;
                CharSequence[] options= viewNode.getAutofillOptions();
                if (options!=null && options.length>0 && listValue!=null) {
                    int listPosition=indexOf(options,listValue);
                    builder.setValue(autofillId, AutofillValue.forList(listPosition));
                    return true;
                }
                return false;
            case View.AUTOFILL_TYPE_DATE:
                long dateValue = metadataEntity.auto_fill_value_date;
                builder.setValue(autofillId, AutofillValue.forDate(dateValue));
                return true;
            case View.AUTOFILL_TYPE_TEXT:
                String textValue = metadataEntity.auto_fill_value_text;
                if (textValue != null) {
                    builder.setValue(autofillId, AutofillValue.forText(textValue));
                    return true;
                }
                //text 为空直接返回，不能穿透到 toggle 分支（否则会把文本框当开关填）
                return false;
            case View.AUTOFILL_TYPE_TOGGLE:
                boolean toggleValue = metadataEntity.auto_fill_value_toggle;
                builder.setValue(autofillId, AutofillValue.forToggle(toggleValue));
                return true;
            case View.AUTOFILL_TYPE_NONE:
            default:
                logw("Invalid autofill type - %d", autofillType);
                return false;

        }
    }

    /**
     * Helper method for getting the index of a CharSequence object in an array.
     */
    public static int indexOf(@NonNull CharSequence[] array, CharSequence charSequence) {
        int index = -1;
        if (charSequence == null) {
            return index;
        }
        for (int i = 0; i < array.length; i++) {
            if (charSequence.equals(array[i])) {
                index = i;
                break;
            }
        }
        return index;
    }
}
