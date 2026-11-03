package com.fou.passport.service;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;

import com.fou.passport.digital_asset_links.BeanDigitalAssetLinks;
import com.fou.passport.digital_asset_links.ParseWebDomain;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.until.SecurityHelper;
import com.fou.passport.until.Log;
import com.fou.passport.until.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SaveDataToDatabase {
    private ClientParser clientParser;
    private Context context;

    public SaveDataToDatabase(ClientParser clientParser, Context context) {
        this.clientParser = clientParser;
        this.context = context;
    }

    /**
     * 新建一个包
     * @param application_id package_name
     * @param webAppFingerPrint 浏览器中保存app时需要传递资产链接中的指纹以进行签名校验，普通app只需传入“”即可
     */
    public void newPackage(String application_id ,String webAppFingerPrint) {
        android.util.Log.d("HAHA", "newpackage");
        ApplicationInfo applicationInfo = null;
        try {
            //找到了App
            applicationInfo = context.getPackageManager().getApplicationInfo(application_id, 0);
            assert applicationInfo != null;
            String app_name = applicationInfo.loadLabel(context.getPackageManager()).toString();
            String  installAppFingerPrint=SecurityHelper.getCertificatesHash(application_id);
            //遇到是web但是Application_id对应上了签名没对应上的直接不保存
            if (!webAppFingerPrint.equals("") && !webAppFingerPrint.equals(installAppFingerPrint)) {
                Log.SaveDataToDatabase("Web签名校验出现问题");
                return;
            }
            MyDB.getDb().getPackageDao().insert(new PackageEntity(application_id, installAppFingerPrint, app_name));
            //取到刚刚插入的packageEntity
            PackageEntity packageEntity = MyDB.getDb().getPackageDao().getPackageEntity(application_id, SecurityHelper.getCertificatesHash(application_id));
            newDataset(packageEntity);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新建一个账户
     */
    public void newDataset(PackageEntity packageEntity) {
        MyDB myDB = MyDB.getDb();
        //插入dataset
        long[] l = myDB.getDatasetDao().insert(new DatasetEntity(packageEntity.id));
        DatasetEntity datasetEntity = myDB.getDatasetDao().getDatasetEntityById((int) l[0]);
        List<MetadataEntity> metadataEntityList = new ArrayList<>();
        List<AssistStructure.ViewNode> viewNodeList = clientParser.noRepeatParse();
        for (AssistStructure.ViewNode node : viewNodeList) {
            if (node.getAutofillValue()!=null) {
            if (node.getAutofillType() == View.AUTOFILL_TYPE_TEXT) {//editText
                Log.SaveDataToDatabase("AUTOFILL_TYPE_TEXT");
                parseNodeAboutEditText(packageEntity, datasetEntity, metadataEntityList, node);
            } else if (node.getId() != -1) {//除edittext以外有id的控件
                //得到有自动填充以及有id的node
                switch (node.getAutofillType()) {
                    case View.AUTOFILL_TYPE_TOGGLE:
                        Log.SaveDataToDatabase("AUTOFILL_TYPE_TOGGLE");
                        Log.SaveDataToDatabase("toggle:"+node.getAutofillValue().getToggleValue());

                        metadataEntityList.add(new MetadataEntity(null, node.getId(), node.getAutofillType(), null, node.getAutofillValue().getToggleValue(), null, 0, datasetEntity.id, packageEntity.id));
                        break;
                    case View.AUTOFILL_TYPE_LIST:
                        Log.SaveDataToDatabase("AUTOFILL_TYPE_LIST");
                        Log.SaveDataToDatabase("ListValue: "+node.getAutofillValue().getListValue());
                        CharSequence[] options=node.getAutofillOptions();
                        int list_options=node.getAutofillValue().getListValue();
                        if (options!=null && list_options>0 && options.length>list_options) {
                            metadataEntityList.add(new MetadataEntity(null, node.getId(), node.getAutofillType(), null, false, options[list_options].toString(), 0, datasetEntity.id, packageEntity.id));
                        }
                        break;
                    case View.AUTOFILL_TYPE_DATE:
                        Log.SaveDataToDatabase("AUTOFILL_TYPE_DATE");
                        Log.SaveDataToDatabase("DATE:"+node.getAutofillValue().getDateValue());
                        metadataEntityList.add(new MetadataEntity(null, node.getId(), node.getAutofillType(), null, false, null, node.getAutofillValue().getDateValue(), datasetEntity.id, packageEntity.id));
                        break;
                }
            }
        }
        }
        //过滤
        if (metadataEntityList.isEmpty() //空的metadataEntityList不保存
                //不含有edittext的也不保存
                || metadataEntityList.stream().filter(metadataEntity -> metadataEntity.auto_fill_type == View.AUTOFILL_TYPE_TEXT).collect(Collectors.toList()).isEmpty()
                //只含有一个密码的也不保存
                || (metadataEntityList.size() == 1 && Type.PASSWORD.equals(metadataEntityList.get(0).key_word))) {
            //删掉刚刚新建的dataset
            myDB.getDatasetDao().delete(datasetEntity);
            //删掉刚刚新建的PackageEntity
            if (myDB.getPackageDao().getDatasetEntityList(datasetEntity.package_id).isEmpty()) {
                myDB.getPackageDao().deletePackageEntityById(datasetEntity.package_id);
            }
            return;
        }
        //addOther
        addOtherMetadataEntity(metadataEntityList,datasetEntity,packageEntity,viewNodeList);
        MyDB.getDb().getMetadataDao().insert(metadataEntityList.toArray(new MetadataEntity[0]));
    }

    /**
     * 添加一些metadataEntity
     * @param metadataEntityList
     * @param datasetEntity
     * @param packageEntity
     * @param viewNodeList
     */
    private void addOtherMetadataEntity(List<MetadataEntity> metadataEntityList , DatasetEntity datasetEntity , PackageEntity packageEntity , List<AssistStructure.ViewNode> viewNodeList) {
        String canonicalDomain=isWeb(viewNodeList);
        if (canonicalDomain!=null) {
            metadataEntityList.add(new MetadataEntity(Type.OTHER_WEBSITE, -1, View.AUTOFILL_TYPE_TEXT,canonicalDomain, false, null, 0, datasetEntity.id, packageEntity.id));
        }
        //metadataEntityList.add(new MetadataEntity(Type.OTHER_CATEGORY, -1, View.AUTOFILL_TYPE_TEXT, Type.CATEGORY_PASSWORD, false, 0, 0, datasetEntity.id, packageEntity.id));
        metadataEntityList.add(new MetadataEntity(Type.OTHER_CREATE_TIME, -1, View.AUTOFILL_TYPE_TEXT, String.valueOf(System.currentTimeMillis()), false, null, 0, datasetEntity.id, packageEntity.id));
        metadataEntityList.add(new MetadataEntity(Type.OTHER_APPLICATION, -1, View.AUTOFILL_TYPE_TEXT, packageEntity.app_name, false, null, 0, datasetEntity.id, packageEntity.id));
    }

    /**
     * 保存的是否为浏览器提交的表单,
     * @param viewNodeList
     * @return  不是返回null，是 返回相应的顶级域名
     */
    private String isWeb(List<AssistStructure.ViewNode> viewNodeList) {
        String canonicalDomain = null;
        for (AssistStructure.ViewNode viewNode : viewNodeList) {
            if (viewNode.getWebDomain()!=null) {
                canonicalDomain= ParseWebDomain.getCanonicalDomain(viewNode.getWebDomain());
            }
        }
        return canonicalDomain;
    }

    /**
     * @param packageEntity
     * @param datasetEntity
     * @param metadataEntityList
     * @param node
     */
    private void parseNodeAboutEditText(PackageEntity packageEntity, DatasetEntity datasetEntity, List<MetadataEntity> metadataEntityList, AssistStructure.ViewNode node) {
        if (!"".equals(node.getAutofillValue().getTextValue().toString().trim())) {//不保存无用的
            String key = Type.getKey(node);
            if (key != null) {
                metadataEntityList.add(new MetadataEntity(key, node.getId(), node.getAutofillType(), node.getAutofillValue().getTextValue().toString(), false, null, 0, datasetEntity.id, packageEntity.id));
            }
        }
    }
}
