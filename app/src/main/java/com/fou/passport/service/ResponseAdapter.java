package com.fou.passport.service;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.os.Build;
import android.service.autofill.CustomDescription;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.fou.passport.R;
import com.fou.passport.digital_asset_links.ParseWebDomain;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.until.Log;
import com.fou.passport.until.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResponseAdapter {
    private ClientParser clientParser;
    private int saveType;
    public ResponseAdapter(ClientParser clientParser) {
        this.clientParser = clientParser;
    }
    /**
     * buildFillResponseNoDataset
     *
     * @param context
     * @return
     */
    public FillResponse buildFillResponseNoDataset(Context context) {
        FillResponse.Builder builder = new FillResponse.Builder();
        //setSaveInfo
        SaveInfo.Builder saveInfoBuilder = getSaveInfoBuilder(context);
        if (saveInfoBuilder != null) {
            builder.setSaveInfo(saveInfoBuilder.build());
            return builder.build();
        }
        return null;
    }

    /**
     * build a fillResponse that been added dataset
     *
     * @param datasetAndMetadataList
     * @param context
     * @return
     */
    public FillResponse buildFillResponse(List<DatasetAndMetadata> datasetAndMetadataList, Context context) {
        return buildFillResponse(datasetAndMetadataList, context, false);
    }

    /**
     * @param identityOnly 是否为跨应用通用填充（只回填账号类或银行卡类字段）
     */
    public FillResponse buildFillResponse(List<DatasetAndMetadata> datasetAndMetadataList, Context context,
                                          boolean identityOnly) {
        //当前页面必须真的存在凭据输入框才展示数据集；
        //聊天框、搜索框、设置页等没有凭据字段的界面返回空，避免历史数据到处弹建议
        if (!hasCredentialField()) {
            return null;
        }
        FillResponse.Builder builder = new FillResponse.Builder();
        //addDataset
        DatasetAdapter datasetAdapter = new DatasetAdapter(clientParser, context);
        int datasetCount = 0;
            for (DatasetAndMetadata datasetAndMetadata : datasetAndMetadataList) {
                Dataset dataset = datasetAdapter.getDataset(datasetAndMetadata, identityOnly);
                if (dataset != null) {
                    builder.addDataset(dataset);
                    datasetCount++;
                }
            }

        Log.ResponseAdapter("datasetCount=" + datasetCount);
        //setSaveInfo
        SaveInfo.Builder saveInfoBuilder = getSaveInfoBuilder(context);
        if (saveInfoBuilder != null) {
            builder.setSaveInfo(saveInfoBuilder.build());
        } else if (datasetCount == 0) {
            //既没有数据集也没有保存信息，交给系统当作“无响应内容”处理
            return null;
        }
        return builder.build();
    }

    /**
     * 判断当前页面是否存在可识别的凭据输入框（账号/手机/邮箱/密码/银行卡等）
     */
    private boolean hasCredentialField() {
        boolean[] found = {false};
        clientParser.parseLastStructure(node -> {
            if (!found[0] && node.getAutofillType() == View.AUTOFILL_TYPE_TEXT
                    && Type.getKey(node) != null) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /**
     * @param context
     * @return
     */
    private SaveInfo.Builder getSaveInfoBuilder(Context context) {
        List<AutofillId> autofillIdList = new ArrayList<>();
        Set<AutofillId> addedIds = new HashSet<>();
        boolean[] hasPassword = {false};
        boolean[] hasCard = {false};
        boolean[] isWeb = {false};
        saveType = 0;
        //对于多页面只保存最后一个表单（有的软件多页注册得用parse,有的软件是一页是注册，
        // 一页是登录，得用lastStructureParse,只取当前这一页，不然无法弹出保存弹窗）
        clientParser.parseLastStructure(node -> {
            if (node.getWebDomain() != null) {
                isWeb[0] = true;
            }
            if (node.getAutofillType() != View.AUTOFILL_TYPE_TEXT) {
                //只监听凭据输入框，dtoggle/list 等控件不参与保存，避免页面切换被误判为提交
                return;
            }
            //This input field is the input type we need.
            String keyWord = Type.getKey(node);
            if (keyWord == null) {
                //搜索框、备注等非凭据输入框不参与保存
                return;
            }
            AutofillId autofillId = node.getAutofillId();
            if (autofillId == null || !addedIds.add(autofillId)) {
                return;
            }
            autofillIdList.add(autofillId);
            saveType |= Type.getTypeByKeyWord(keyWord);
            if (Type.PASSWORD.equals(keyWord)) {
                hasPassword[0] = true;
            }
            if (Type.CARD.equals(keyWord) || Type.CVV.equals(keyWord)) {
                hasCard[0] = true;
            }
        });
        //只有密码表单或银行卡表单才请求保存，避免手机号/邮箱/搜索等非登录表单乱弹
        if (autofillIdList.isEmpty() || (!hasPassword[0] && !hasCard[0])) {
            return null;
        }
        SaveInfo.Builder builder = new SaveInfo.Builder(saveType, autofillIdList.toArray(new AutofillId[0]));
        if (isWeb[0]) {
            //Web 表单提交后页面跳转、Activity 不结束，需要“视图不可见即保存”；
            //原生页面不设该标志，交由系统在页面结束时触发，避免未提交就弹
            builder.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE);
        }
        //自定义保存弹窗描述区，替换系统默认的朴素提示（API 27+ 才有 CustomDescription）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            RemoteViews customDescription = new RemoteViews(context.getPackageName(), R.layout.save_custom_description);
            builder.setCustomDescription(new CustomDescription.Builder(customDescription).build());
        }
        Log.ResponseAdapter("autofillIdList.size()" + autofillIdList.size());
        return builder;
    }
}
