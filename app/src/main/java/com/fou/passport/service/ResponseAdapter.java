package com.fou.passport.service;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import androidx.annotation.NonNull;

import com.fou.passport.digital_asset_links.ParseWebDomain;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.until.Log;
import com.fou.passport.until.Type;

import java.util.ArrayList;
import java.util.List;

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
        FillResponse.Builder builder = new FillResponse.Builder();
        //addDataset
        DatasetAdapter datasetAdapter = new DatasetAdapter(clientParser, context);
            for (DatasetAndMetadata datasetAndMetadata : datasetAndMetadataList) {
                Dataset dataset = datasetAdapter.getDataset(datasetAndMetadata);
                if (dataset != null) {
                    builder.addDataset(dataset);
                }
            }

        //setSaveInfo
        SaveInfo.Builder saveInfoBuilder = getSaveInfoBuilder(context);
        if (saveInfoBuilder != null) {
            builder.setSaveInfo(saveInfoBuilder.build());
            return builder.build();
        }
        return null;
    }

    /**
     * @param context
     * @return
     */
    private SaveInfo.Builder getSaveInfoBuilder(Context context) {
        List<AutofillId> autofillIdList = new ArrayList<>();
        saveType = 0;
        //对于多页面只保存最后一个表单（有的软件多页注册得用parse,有的软件是一页是注册，
        // 一页是登录，得用lastStructureParse,只取当前这一页，不然无法弹出保存弹窗）
        clientParser.parseLastStructure(node -> {
            if (node.getAutofillType() == View.AUTOFILL_TYPE_TEXT) {
                //This input field is the input type we need.
                String keyWord = Type.getKey(node);
                if (keyWord != null) {
                    autofillIdList.add(node.getAutofillId());
                    saveType |= Type.getTypeByKeyWord(keyWord);
                }
            } else if (node.getId() != -1) {
                //we can add directly when (node.getAutofillType() != View.AUTOFILL_TYPE_TEXT);
                autofillIdList.add(node.getAutofillId());
            }
        });
        if (autofillIdList.isEmpty()) {
            return null;
        }
        Log.ResponseAdapter("autofillIdList.size()" + autofillIdList.size());
        return new SaveInfo.Builder(saveType, autofillIdList.toArray(new AutofillId[0]))
                .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE); // 设置SaveInfo#FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE标志;
    }
}
