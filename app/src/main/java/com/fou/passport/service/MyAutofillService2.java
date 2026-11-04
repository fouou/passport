package com.fou.passport.service;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static java.util.stream.Collectors.toList;

import android.app.KeyguardManager;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.webkit.WebView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.digital_asset_links.BeanDigitalAssetLinks;
import com.fou.passport.digital_asset_links.ParseWebDomain;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageDao;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.until.AppExecutors;
import com.fou.passport.until.SecurityHelper;
import com.fou.passport.until.Log;
import com.fou.passport.until.Type;
import com.google.common.net.InternetDomainName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class MyAutofillService2 extends AutofillService {

    private ResponseAdapter responseAdapter;
    private PackageDao packageDao;
    private AppExecutors appExecutors;
    private MyDB myDB;
    private List<DatasetAndMetadata> usefulDatasetAndMetadataList;
    private String application_id;
    private ClientParser parser;
    private SaveDataToDatabase saveDataToDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        myDB = MyDB.getDb();
        packageDao = myDB.getPackageDao();
        appExecutors = new AppExecutors();
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal, @NonNull FillCallback callback) {

        Log.MyAutofillService("onFillRequest");
        List<FillContext> fillContexts = request.getFillContexts();
        List<AssistStructure> structures = fillContexts.stream().map(FillContext::getStructure).collect(toList());
        AssistStructure latestStructure = fillContexts.get(fillContexts.size() - 1).getStructure();
        parser = new ClientParser(structures, latestStructure);

        parser.parseLastStructure(new ClientParser.NodeProcessor() {
            int i=1;
            @Override
            public void processNode(AssistStructure.ViewNode node) {

                String key=Type.getKey(node);
                if (key==null) {
                    key="null";}
                    Log.MyAutofillService("第"+i++);
                    Log.MyAutofillService(key);
                if (node.getAutofillType()== View.AUTOFILL_TYPE_TEXT) {
                    Log.MyAutofillService(""+node.getInputType());
                }

            }
        });

        responseAdapter = new ResponseAdapter(parser);
        application_id = latestStructure.getActivityComponent().getPackageName();//get application_id
        boolean manual = (request.getFlags() & FillRequest.FLAG_MANUAL_REQUEST) != 0;//手动触发的填充请求
        //工作线程填充
        appExecutors.diskIO().execute(() -> {
            ParseWebDomain parseWebDomain = new ParseWebDomain(parser);
            //是Web走此自动填充
            if (parseWebDomain.isWeb()) {
                autoFillForWebDomain(callback, parseWebDomain);
            } else {
                //不是web走此自动填充
                autoFillForNormalApp(callback, manual);
            }
        });

    }

    /**
     * 普通填充表单走这里
     *
     * @param callback
     * @param manual
     */
    private void autoFillForNormalApp(@NonNull FillCallback callback, boolean manual) {
        beginGenerateResponse(manual, new Begin() {
            @Override
            public void hasDataset(List<DatasetAndMetadata> datasetAndMetadataList) {
                FillResponse fillResponse = responseAdapter.buildFillResponse(datasetAndMetadataList, MyAutofillService2.this);
                //即使没有可用数据集也要回调，否则系统会一直等待填充服务响应
                callback.onSuccess(fillResponse);
            }

            @Override
            public void noDataset() {
                FillResponse fillResponse = responseAdapter.buildFillResponseNoDataset(MyAutofillService2.this);
                callback.onSuccess(fillResponse);
            }
        });
    }

    /**
     * WEB的自动填充
     *
     * @param callback
     * @param parseWebDomain
     */
    private void autoFillForWebDomain(@NonNull FillCallback callback, ParseWebDomain parseWebDomain) {
        List<BeanDigitalAssetLinks> beanDigitalAssetLinks = parseWebDomain.getBeanDigitalAssetLinksList();
        if (beanDigitalAssetLinks != null) {
            //通过资产链接查询数据库中对应的app的PackageEntity
            List<PackageEntity> packageEntityList = getPackageEntityListByBeanDigitalAssetLinks(beanDigitalAssetLinks);
            //数据库中存在此网站的app端数据
            if (!packageEntityList.isEmpty()) {
                List<DatasetAndMetadata> datasetAndMetadataList = getUsefulDatasetAndMetadataForWeb(packageEntityList);
//是WEB，从app中取数据
                if (!datasetAndMetadataList.isEmpty()) {
                    FillResponse fillResponse = responseAdapter.buildFillResponse(datasetAndMetadataList, MyAutofillService2.this);
                    if (fillResponse != null) {
                        callback.onSuccess(fillResponse);
                        return;
                    }
                }
            }
        }
//是web，从浏览器中取数据
        String canonicalDomain = ParseWebDomain.getCanonicalDomain(parseWebDomain.webDomain);
        if (!canonicalDomain.equals("")) {
            PackageEntity packageEntity = myDB.getPackageDao().getPackageEntity(application_id, SecurityHelper.getCertificatesHash(application_id));
            if (packageEntity != null) {
                List<DatasetAndMetadata> datasetAndMetadataList = getUsefulDatasetAndMetadataForWeb(packageEntity);
                //过滤浏览器内与该网址匹配的dataset
                List<DatasetAndMetadata> datasetAndMetadataList_filter_website = datasetAndMetadataList.stream().filter(datasetAndMetadata -> !datasetAndMetadata.metadataEntityList.stream().filter(metadataEntity -> Type.OTHER_WEBSITE.equals(metadataEntity.key_word) && canonicalDomain.equals(metadataEntity.auto_fill_value_text)).collect(toList()).isEmpty()).collect(toList());
                if (!datasetAndMetadataList_filter_website.isEmpty()) {
                    FillResponse fillResponse = responseAdapter.buildFillResponse(datasetAndMetadataList_filter_website, MyAutofillService2.this);
                    if (fillResponse != null) {
                        callback.onSuccess(fillResponse);
                        return;
                    }
                }
            }
        }
//是web，没有数据
        FillResponse fillResponse = responseAdapter.buildFillResponseNoDataset(MyAutofillService2.this);
        callback.onSuccess(fillResponse);
    }


    /**
     * WEB 自动填充 收集浏览器内有用的数据
     *
     * @param packageEntity
     * @return
     */
    @NonNull
    private List<DatasetAndMetadata> getUsefulDatasetAndMetadataForWeb(PackageEntity packageEntity) {
        List<DatasetEntity> datasetEntityList = myDB.getPackageDao().getDatasetEntityList(packageEntity.id);
        List<DatasetAndMetadata> datasetAndMetadataList = new ArrayList<>();
        //stream
        List<DatasetEntity> datasetEntityList_filter = datasetEntityList.stream().filter(datasetEntity -> {
            //return true when the dataset contains metadata
            DatasetAndMetadata datasetAndMetadata = myDB.getDatasetDao().getDatasetAndMetadataInstance(datasetEntity.id);
            if (!datasetAndMetadata.metadataEntityList.isEmpty()) {
                //add
                datasetAndMetadataList.add(datasetAndMetadata);
                return true;
            }
            //Delete datasetEntity that is null
            myDB.getDatasetDao().delete(datasetEntity);
            return false;
        }).collect(toList());
        if (datasetEntityList_filter.isEmpty()) {
            myDB.getPackageDao().delete(packageEntity);
        }
        return datasetAndMetadataList;
    }


    /**
     * WEB
     * 过滤掉无用的DatasetAndMetadata，并将其从ROOM中删除 手机网址对应app的数据
     *
     * @param packageEntityList
     * @return
     */
    @NonNull
    private List<DatasetAndMetadata> getUsefulDatasetAndMetadataForWeb(List<PackageEntity> packageEntityList) {
        List<DatasetAndMetadata> datasetAndMetadataList = new ArrayList<>();
        for (PackageEntity packageEntity : packageEntityList) {
            List<DatasetEntity> datasetEntityList1 = myDB.getPackageDao().getDatasetEntityList(packageEntity.id);
            List<DatasetEntity> datasetEntityList_filter = datasetEntityList1.stream().filter(datasetEntity -> {
                //return true when the dataset contains metadata
                DatasetAndMetadata datasetAndMetadata = myDB.getDatasetDao().getDatasetAndMetadataInstance(datasetEntity.id);
                if (!datasetAndMetadata.metadataEntityList.isEmpty()) {
                    //add
                    datasetAndMetadataList.add(datasetAndMetadata);
                    return true;
                }
                //Delete datasetEntity that is null
                myDB.getDatasetDao().delete(datasetEntity);
                return false;
            }).collect(toList());
            //过滤后为empty则说明此packageEntity本身没有数据或都是无用的数据，删掉此packageEntity
            if (datasetEntityList_filter.isEmpty()) {
                myDB.getPackageDao().delete(packageEntity);
            }
        }
        return datasetAndMetadataList;
    }

    /**
     * WEB填充
     * <p>
     * 通过资产链接查询数据库中对应的app的PackageEntity
     *
     * @param beanDigitalAssetLinks
     * @return
     */
    @NonNull
    private List<PackageEntity> getPackageEntityListByBeanDigitalAssetLinks(List<BeanDigitalAssetLinks> beanDigitalAssetLinks) {
        List<PackageEntity> packageEntityList = new ArrayList<>();//容纳该网址对应数据库中app的数据的集合
        //过滤web的资产链接
        for (BeanDigitalAssetLinks beanDigitalAssetLink1 : beanDigitalAssetLinks) {
            String nameSpace = beanDigitalAssetLink1.getTarget().getNamespace();
            String packageName = beanDigitalAssetLink1.getTarget().getPackage_name();
            List<String> fingerprints = beanDigitalAssetLink1.getTarget().getSha256_cert_fingerprints();
            //小心nameSpace=web的资产链接没有指纹，fingerprints可能为null
            if (nameSpace.equals("android_app") && fingerprints != null && !fingerprints.isEmpty()) {
                String unformatFingerprint = unformatFingerprint(fingerprints.get(0));
                //Queries the corresponding packageEntity in database;
                PackageEntity packageEntity = myDB.getPackageDao().getPackageEntity(packageName, unformatFingerprint);
                //数据库中存在的或者已经安装的app与web资产链接匹配成功后进行相应的存储，其余的以浏览器的包名和签名存储
                if (packageEntity != null) {
                    packageEntityList.add(packageEntity);
                }
            }
        }
        return packageEntityList;
    }


    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        Log.MyAutofillService("onSaveRequest");
        List<FillContext> fillContexts = request.getFillContexts();
        List<AssistStructure> structures = fillContexts.stream().map(FillContext::getStructure).collect(toList());
        AssistStructure latestStructure = fillContexts.get(fillContexts.size() - 1).getStructure();
        parser = new ClientParser(structures, latestStructure);
        saveDataToDatabase = new SaveDataToDatabase(parser, this);
        //网络请求，存储请求均在此线程，以便实现同步
       appExecutors.diskIO().execute(() -> {
            ParseWebDomain parseWebDomain = new ParseWebDomain(parser);
            if (parseWebDomain.isWeb()) {
//是浏览器发起的请求
//数据库中存在的或者已经安装的app与web资产链接匹配成功后进行相应的存储，其余的以浏览器的包名和签名存储（nameSpace=web的，或者数据库和手机上都没有此app的），
// 即走下面的普通app存储路线
                List<BeanDigitalAssetLinks> beanDigitalAssetLinks = parseWebDomain.getBeanDigitalAssetLinksList();
                if (beanDigitalAssetLinks != null && !beanDigitalAssetLinks.isEmpty()) {
                    //过滤掉web的资产链接
                    List<BeanDigitalAssetLinks> beanDigitalAssetLinksList_androidAPP = beanDigitalAssetLinks.stream().filter(beanDigitalAssetLinks1 -> {
                        String nameSpace = beanDigitalAssetLinks1.getTarget().getNamespace();
                        String packageName = beanDigitalAssetLinks1.getTarget().getPackage_name();
                        List<String> fingerprints = beanDigitalAssetLinks1.getTarget().getSha256_cert_fingerprints();
                        //小心nameSpace=web的资产链接没有指纹，fingerprints可能为null
                        //直接写一行不需要判断，因为第一步是nameSpace.equals("android_app")，nameSpace=android_app的一定会有fingerprints
                        if (fingerprints != null && !fingerprints.isEmpty()) {
                            String unformatFingerprint = unformatFingerprint(fingerprints.get(0));
                            //Queries the corresponding packageEntity in database;
                            PackageEntity packageEntity = myDB.getPackageDao().getPackageEntity(packageName, unformatFingerprint);
                            //数据库中存在的或者已经安装的app与web资产链接匹配成功后进行相应的存储，其余的以浏览器的包名和签名存储
                            return nameSpace.equals("android_app") && (packageEntity != null || isAppInstall(packageName));
                        }
                        return false;
                    }).collect(toList());

                    if (!beanDigitalAssetLinksList_androidAPP.isEmpty()) {
                        for (BeanDigitalAssetLinks digitalAssetLinks : beanDigitalAssetLinksList_androidAPP) {
                            String packageName = digitalAssetLinks.getTarget().getPackage_name();
                            String fingerprints = digitalAssetLinks.getTarget().getSha256_cert_fingerprints().get(0);
                            String unformatFingerprint = unformatFingerprint(fingerprints);
                            //Queries the corresponding packageEntity in database;
                            PackageEntity packageEntity = myDB.getPackageDao().getPackageEntity(packageName, unformatFingerprint);
                            if (packageEntity != null) {
                                newDatasetOrUpDate(packageEntity);//have package in database
                            } else {
                                saveDataToDatabase.newPackage(packageName, unformatFingerprint);//This package is not in database
                            }
                        }
                        return;
                    }
                }
            }
//普通app的存储
            application_id = latestStructure.getActivityComponent().getPackageName();
            //Queries the corresponding packageEntity in database;
            PackageEntity packageEntity = myDB.getPackageDao().getPackageEntity(application_id, SecurityHelper.getCertificatesHash(application_id));
            if (packageEntity != null) {
                newDatasetOrUpDate(packageEntity);//have package in database
            } else {
                saveDataToDatabase.newPackage(application_id, "");//This package is not in database
            }
        });
    }


    /**
     * new Dataset or update
     *
     * @param packageEntity
     */
    private void newDatasetOrUpDate(PackageEntity packageEntity) {
        //检查每一个dataset是否需要新建并将其保存到list中，如果都为true则说明确实没有用户当前输入的账户，则新建账户
        List<DatasetBoolean> datasetBooleanList = getDatasetBooleanList(packageEntity);
        //检查isNeedNewDatasetList，执行相应的操作（更新或新建）
        List<DatasetBoolean> datasetBooleanList1 = datasetBooleanList.stream().filter(datasetBoolean -> datasetBoolean.isKey_wordMatching).collect(toList());
        List<DatasetBoolean> datasetBooleanList2 = datasetBooleanList1.stream().filter(datasetBoolean -> !datasetBoolean.isNeedNewDataset).collect(toList());
        if (datasetBooleanList1.isEmpty() || datasetBooleanList2.isEmpty()) {//与所有账户不匹配 || 匹配到了账户但是与该账户内的账号不相等 ——>则新建dataset
            //新建dataset
            Log.Special_2("兴建");
            saveDataToDatabase.newDataset(packageEntity);
        } else {
            //则更新dataset
            Log.Special_2("更新");
            DatasetEntity datasetEntity = datasetBooleanList2.get(0).datasetEntity;
            myDB.getDatasetDao().delete(datasetEntity);//删
            saveDataToDatabase.newDataset(packageEntity);
        }
    }


    /**
     * @param packageEntity
     * @return 数据库中该包下每个每个dataset与用户输入的数据比较的结果列表
     */
    private List<DatasetBoolean> getDatasetBooleanList(PackageEntity packageEntity) {
        //这几种输入框中的内容不同则新建账户
        List<String> stringList = Arrays.asList(Type.strings);
        //数据库中该包下每个每个dataset与用户输入的数据比较的结果列表
        List<DatasetBoolean> datasetBooleanList = new ArrayList<>();
        List<DatasetAndMetadata> datasetAndMetadataList = myDB.getDatasetDao().getDatasetAndMetadataListById(packageEntity.id);
        for (DatasetAndMetadata datasetAndMetadata : datasetAndMetadataList) {
            DatasetBoolean datasetBoolean = new DatasetBoolean(datasetAndMetadata.datasetEntity);
            for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                List<AssistStructure.ViewNode> nodes = parser.noRepeatParse();
                for (AssistStructure.ViewNode node : nodes) {
                    String key_word = Type.getKey(node);
                    if (stringList.contains(key_word) //说明是一个有stringList中的某一个
                            && key_word.equals(metadataEntity.key_word)) { //key_word匹配成功
                        datasetBoolean.isKey_wordMatching = true;//dataset匹配成功，说明该账户可用于与用户提交的账户相比较
                        String text = node.getAutofillValue().getTextValue().toString();
                        if (!text.equals(metadataEntity.auto_fill_value_text)) {
                            datasetBoolean.isNeedNewDataset = true;//如果所有的都为true，则说明与每个账户的账号都不匹配，也新建
                        }
                    }
                }
            }
            datasetBooleanList.add(datasetBoolean);
        }
        return datasetBooleanList;
    }

    /**
     * @param manual
     * @param begin
     */
    private void beginGenerateResponse(boolean manual, Begin begin) {
        //Queries the corresponding packageEntity only in database;
        PackageEntity packageEntity = packageDao.getPackageEntity(application_id, SecurityHelper.getCertificatesHash(application_id));
        if (packageEntity != null) {
            List<DatasetEntity> datasetEntityList = myDB.getPackageDao().getDatasetEntityList(packageEntity.id);
            if (!datasetEntityList.isEmpty() && !getUsefulDatasetList(datasetEntityList).isEmpty()) {
                //Has usefulDatasets in the corresponding packageEntity;
                begin.hasDataset(usefulDatasetAndMetadataList);
                return;
            }
            //Delete useless packageEntity;
            packageDao.delete(packageEntity);
        }
        //no dataset
        begin.noDataset();
    }

    /**
     * generate UsefulDatasetList and add usefulDatasetAndMetadata
     * for usefulDatasetAndMetadataList
     *
     * @param
     * @return Return a usefulDatasetList
     */
    private List<DatasetEntity> getUsefulDatasetList(List<DatasetEntity> datasetEntityList) {
        usefulDatasetAndMetadataList = new ArrayList<>();
        //stream
        return datasetEntityList.stream().filter(datasetEntity -> {
            //return true when the dataset contains metadata
            DatasetAndMetadata datasetAndMetadata = myDB.getDatasetDao().getDatasetAndMetadataInstance(datasetEntity.id);
            if (!datasetAndMetadata.metadataEntityList.isEmpty()) {
                //add
                usefulDatasetAndMetadataList.add(datasetAndMetadata);
                return true;
            }
            //Delete datasetEntity that is null
            myDB.getDatasetDao().delete(datasetEntity);
            return false;
        }).collect(toList());
    }


    /**
     * 资产链接返回指纹列表，我们取第一个（主签名）比较即可
     * 将 E3:F9:E1:E0:CF:99:D0:E5:6A:05:5B:A6:5E:24:1B:33:99:F7:CE:A5:24:32:6B:0C:DD:6E:C1:32:7E:D0:FD:C1
     * -->（e3f9e1e0cf99d0e56a055ba65e241b3399f7cea524326b0cdd6ec1327ed0fdc1）
     * 去掉冒号，并全部改小写。就成了SecurityHelper.getCertificatesHash()中得到的指纹
     *
     * @param fingerprint
     * @return
     */
    public String unformatFingerprint(String fingerprint) {
        if (fingerprint == null) {
            return null;
        }
        return fingerprint.replaceAll(":", "").toLowerCase();
    }

    /**
     * 通过包名查询手机上是否安装了此app
     *
     * @param packageName
     * @return
     */
    public boolean isAppInstall(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * BeginClass
     */
    interface Begin {
        /**
         * The datasetAndMetadataList available in the current package
         * you can generate a fillResponse that with some datasets
         *
         * @param datasetAndMetadataList
         */
        void hasDataset(List<DatasetAndMetadata> datasetAndMetadataList);

        /**
         * There is no related package or the dataset does not meet the requirements
         * you can generate a fillResponse that without dataset
         */
        void noDataset();
    }


}
