package com.fou.passport.ui.newitem;

import static com.fou.passport.until.Type.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityNewItemBinding;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.BaseActivity;
import com.fou.passport.ui.DetailActivity;
import com.fou.passport.ui.listfragment.ListAdapter;
import com.fou.passport.until.Log;
import com.fou.passport.until.SecurityHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NewItem extends BaseActivity implements View.OnClickListener {
    private ActivityNewItemBinding binding;
    private boolean isAdded;
    private PackageEntity packageEntity;
    private List<ApplicationInfo> applicationInfoList_all;
    private ModalBottomSheet modalBottomSheet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState!=null) {
            Log.NewItemm("savedInstanceState != null");
        }else {
            Log.NewItemm("savedInstanceState == null");

        }
        binding = ActivityNewItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //点击detailActivity跳转到这的
        getIntentFromDetailActivity();
        //
        bindViews();
        //set bottomSheet
        setModalBottomSheet();
    }


    /**
     * 处理BottomSheet
     */
    private void setModalBottomSheet() {
        modalBottomSheet = new ModalBottomSheet();
        //BottomSheet的DoneButton的点击事件接口实现
        modalBottomSheet.setDoneButtonClick(bottom_sheet_edittext_text -> {
            packageEntity = null;//将packageEntity设为null,防止点过item又去自定义输入，而使得packageEntity不为空，从而走save1,save2
            binding.materialTextViewAppNameUp.setText(bottom_sheet_edittext_text);
            binding.materialTextViewAppName.setText(bottom_sheet_edittext_text);
            binding.shapeableImageView.setImageDrawable(ContextCompat.getDrawable(MyApplication.applicationContext, R.drawable.app_ic));
        });

        //BottomSheetItem点击回调方法
        modalBottomSheet.OnBottomSheetItemClickListener(applicationInfo -> {
            binding.materialTextViewAppName.setText(applicationInfo.loadLabel(MyApplication.applicationContext.getPackageManager()));
            binding.shapeableImageView.setImageDrawable(applicationInfo.loadIcon(MyApplication.applicationContext.getPackageManager()));
            binding.materialTextViewAppNameUp.setText(applicationInfo.loadLabel(MyApplication.applicationContext.getPackageManager()));
            packageEntity = new PackageEntity(applicationInfo.packageName, SecurityHelper.getCertificatesHash(applicationInfo.packageName), applicationInfo.loadLabel(MyApplication.applicationContext.getPackageManager()).toString());
            modalBottomSheet.dismiss();
        });

        //从modalBottomSheet获取ApplicationInfo
        modalBottomSheet.setApplicationInfoListIsAssignedValueListener(applicationInfoList -> {
            applicationInfoList_all=applicationInfoList;
        });
    }


    private void getIntentFromDetailActivity() {
        Intent intent = getIntent();
        if (intent != null) {
            int package_id = intent.getIntExtra(DetailActivity.INTENT_KEY_PACKAGE_ID, -1);
            if (package_id != -1) {
                MyApplication.appExecutors.diskIO().execute(() -> {
                    packageEntity = MyDB.getDb().getPackageDao().getPackageEntityById(package_id);
                    try {
                        ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageEntity.application_id, 0);
                        binding.shapeableImageView.setImageDrawable(applicationInfo.loadIcon(getPackageManager()));
                        binding.materialTextViewAppNameUp.setText(applicationInfo.loadLabel(getPackageManager()));
                        binding.materialTextViewAppName.setText(applicationInfo.loadLabel(getPackageManager()));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }


    private void bindViews() {
        binding.cancel.setOnClickListener(this);
        binding.save.setOnClickListener(this);
        //binding.textInputLayoutAppName.setOnClickListener(this);
        binding.materialTextViewAppName.setOnClickListener(this);
        binding.materialTextViewAddMore.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
            case R.id.save:
                new MaterialAlertDialogBuilder(NewItem.this).setMessage(R.string.save).setNegativeButton(R.string.cancel, (dialog, which) -> {
                }).setPositiveButton(R.string.save2, (dialog, which) -> {
                    //功作线程保存
                    MyApplication.appExecutors.diskIO().execute(this::saveData);
                }).show();
                break;
            case R.id.materialTextView_appName:
                ModalBottomSheet modalBottomSheet1 = (ModalBottomSheet) getSupportFragmentManager().findFragmentByTag(ModalBottomSheet.TAG);
                if (modalBottomSheet1 == null) {
                    // Fragment 尚未添加到 FragmentManager 中
                    modalBottomSheet.show(getSupportFragmentManager(), ModalBottomSheet.TAG);
                } else {
                    // Fragment 已经添加到 FragmentManager 中
                }
                break;
            case R.id.materialTextView_addMore:
                if (isAdded) return;//只添加一次
                getLayoutInflater().inflate(R.layout.newitem_add_more, binding.linearLayoutContainerNewItem, true);
                binding.linearLayoutContainerNewItem.removeView(binding.materialTextViewAddMore);
                isAdded = true;
                break;
        }
    }

    /**
     * saveData
     */

    private void saveData() {
        List<TextInputLayout> list = new ArrayList<>();
        int viewCount = binding.linearLayoutContainerNewItem.getChildCount();
        for (int i = 3; i < viewCount - 1; i++) {
            list.add((TextInputLayout) binding.linearLayoutContainerNewItem.getChildAt(i));
        }
        if (isAdded) {
            LinearLayout addMore = (LinearLayout) binding.linearLayoutContainerNewItem.getChildAt(viewCount - 1);
            for (int i = 0; i < addMore.getChildCount(); i++) {
                list.add((TextInputLayout) addMore.getChildAt(i));
            }
        }
        parseAndSaveList(list);
    }


    /**
     * parse and save data
     *
     * @param list
     */
    private void parseAndSaveList(List<TextInputLayout> list) {
        List<MetadataEntity> metadataEntityList = new ArrayList<>();
        for (TextInputLayout textInputLayout : list) {
            String key_word = ((String) (textInputLayout.getTag())).trim().toLowerCase();
            String text = textInputLayout.getEditText().getText().toString().trim();
            if (!text.equals("")) {
                switch (key_word) {
                    case USERNAME:
                        metadataEntityList.add(new MetadataEntity(USERNAME, text));
                        break;
                    case PHONE:
                        metadataEntityList.add(new MetadataEntity(PHONE, text));
                        break;
                    case EMAIL:
                        metadataEntityList.add(new MetadataEntity(EMAIL, text));
                        break;
                    case PASSWORD:
                        metadataEntityList.add(new MetadataEntity(PASSWORD, text));
                        break;
                    case CARD:
                        metadataEntityList.add(new MetadataEntity(CARD, text));
                        break;
                    case CVV:
                        metadataEntityList.add(new MetadataEntity(CVV, text));
                        break;
                    case OTHER_WEBSITE:
                        metadataEntityList.add(new MetadataEntity(OTHER_WEBSITE, text));
                        break;
                    case OTHER_ISSUING_BANK:
                        metadataEntityList.add(new MetadataEntity(OTHER_ISSUING_BANK, text));
                        break;
                }
            }
        }
        saveToDatabase(metadataEntityList);
    }

    /**
     * @param metadataEntityList
     */
    private void saveToDatabase(List<MetadataEntity> metadataEntityList) {
        String app_name = binding.materialTextViewAppName.getText().toString().trim();
        if (metadataEntityList.isEmpty() || app_name.equals("")) {
            //展示提示的Toast
            MyApplication.appExecutors.mainThread().execute(() -> {
                Toast.makeText(MyApplication.applicationContext, R.string.tip, Toast.LENGTH_LONG).show();
            });
            return;
        }
        //添加创建时间字段 和适用app字段
        metadataEntityList.add(new MetadataEntity(OTHER_CREATE_TIME, String.valueOf(System.currentTimeMillis())));
        metadataEntityList.add(new MetadataEntity(OTHER_APPLICATION, app_name));

        if (packageEntity != null) {//点击bottomsheet中的item来的
            PackageEntity packageEntityHasInDatabase = MyDB.getDb().getPackageDao().getPackageEntity(packageEntity.application_id, packageEntity.certificates_hash);
            if (packageEntityHasInDatabase != null) {//数据库中存在用户需要保存的package
                save1(metadataEntityList, packageEntityHasInDatabase);
            } else {//数据库中不存在用户需要保存的package
                save2(metadataEntityList);
            }
        } else {//自己输文本来的
            if (applicationInfoList_all != null) {
                List<ApplicationInfo> applicationInfoList = applicationInfoList_all.stream().filter(applicationInfo -> applicationInfo.loadLabel(getPackageManager()).toString().trim().equalsIgnoreCase(app_name)).collect(Collectors.toList());
                if (!applicationInfoList.isEmpty()) {//未点击但存在的app名
                    save3(metadataEntityList, applicationInfoList.get(0)); //TODO 保存的时候要判断数据库中是否有此packageEntity
                } else {//完全自定义的app
                    save4(metadataEntityList, app_name);
                }
            }
        }
    }

    /**
     * @param packageEntity
     */
    private void turnToDetailActivity(int package_id) {
        //展示保存成功的Toast
        MyApplication.appExecutors.mainThread().execute(() -> {
            Toast.makeText(MyApplication.applicationContext, R.string.save_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(ListAdapter.KEY_INTENT, package_id).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void save4(List<MetadataEntity> metadataEntityList, String app_name) {
        PackageEntity packageEntity = new PackageEntity(null, null, app_name);
        //插入packageEntity
        long[] packageId = MyDB.getDb().getPackageDao().insert(packageEntity);
        //插入dataset
        long[] datasetId = MyDB.getDb().getDatasetDao().insert(new DatasetEntity((int) packageId[0]));
        //插入Metadata
        for (MetadataEntity metadataEntity : metadataEntityList) {//完善metadata
            metadataEntity.package_id = (int) packageId[0];
            metadataEntity.dataset_id = (int) datasetId[0];
        }
        MyDB.getDb().getMetadataDao().insert(metadataEntityList.toArray(new MetadataEntity[0]));
        turnToDetailActivity((int) packageId[0]);
    }

    private void save3(List<MetadataEntity> metadataEntityList, ApplicationInfo applicationInfo) {
        PackageEntity packageEntity = new PackageEntity(applicationInfo.packageName, SecurityHelper.getCertificatesHash(applicationInfo.packageName), applicationInfo.loadLabel(getPackageManager()).toString());
        //插入packageEntity
        long[] packageId = MyDB.getDb().getPackageDao().insert(packageEntity);
        //插入dataset
        long[] datasetId = MyDB.getDb().getDatasetDao().insert(new DatasetEntity((int) packageId[0]));
        //插入Metadata
        for (MetadataEntity metadataEntity : metadataEntityList) {//完善metadata
            metadataEntity.package_id = (int) packageId[0];
            metadataEntity.dataset_id = (int) datasetId[0];
        }
        MyDB.getDb().getMetadataDao().insert(metadataEntityList.toArray(new MetadataEntity[0]));
        turnToDetailActivity((int) packageId[0]);
    }

    private void save2(List<MetadataEntity> metadataEntityList) {
        //插入packageEntity
        long[] packageId = MyDB.getDb().getPackageDao().insert(packageEntity);
        //插入dataset
        long[] datasetId = MyDB.getDb().getDatasetDao().insert(new DatasetEntity((int) packageId[0]));
        //插入Metadata
        for (MetadataEntity metadataEntity : metadataEntityList) {//完善metadata
            metadataEntity.package_id = (int) packageId[0];
            metadataEntity.dataset_id = (int) datasetId[0];
        }
        MyDB.getDb().getMetadataDao().insert(metadataEntityList.toArray(new MetadataEntity[0]));
        turnToDetailActivity((int) packageId[0]);
    }

    private void save1(List<MetadataEntity> metadataEntityList, PackageEntity packageEntityHasInDatabase) {
        //插入dataset
        long[] l = MyDB.getDb().getDatasetDao().insert(new DatasetEntity(packageEntityHasInDatabase.id));
        DatasetEntity datasetEntity = MyDB.getDb().getDatasetDao().getDatasetEntityById((int) l[0]);
        //插入Metadata
        for (MetadataEntity metadataEntity : metadataEntityList) {//完善metadata
            metadataEntity.package_id = packageEntityHasInDatabase.id;
            metadataEntity.dataset_id = datasetEntity.id;
        }
        MyDB.getDb().getMetadataDao().insert(metadataEntityList.toArray(new MetadataEntity[0]));
        turnToDetailActivity(packageEntityHasInDatabase.id);
    }
}