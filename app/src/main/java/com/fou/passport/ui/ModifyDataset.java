package com.fou.passport.ui;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityModifyDatasetBinding;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.listfragment.ListAdapter;
import com.fou.passport.until.Type;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModifyDataset extends BaseActivity {
    ActivityModifyDatasetBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModifyDatasetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//getIntent
        int dataset_id = getIntent().getIntExtra(DetailActivity.INTENT_KEY_DATASET_ID, -1);
        int package_id = getIntent().getIntExtra(DetailActivity.INTENT_KEY_PACKAGE_ID, -1);
        int position = getIntent().getIntExtra(DetailActivity.INTENT_KEY_POSITION, -1);
        if (dataset_id == -1 || package_id == -1 || position == -1) {
            Toast.makeText(this, getString(R.string.wrogn), Toast.LENGTH_SHORT).show();
            finish();
        }
        binding.top.setText(getString(R.string.modify));

//showImage
        MyApplication.appExecutors.diskIO().execute(() -> {
            PackageEntity packageEntity = MyDB.getDb().getPackageDao().getPackageEntityById(package_id);
            if (packageEntity != null) {
                MyApplication.appExecutors.mainThread().execute(() -> {
                    if (packageEntity.application_id != null) {
                        try {
                            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageEntity.application_id, 0);
                            binding.shapeableImageView.setImageDrawable(applicationInfo.loadIcon(getPackageManager()));
                            binding.materialTextViewAppNameUp.setText(getString(R.string.account_count, position + 1));
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        binding.materialTextViewAppNameUp.setText(packageEntity.app_name);
                    }
                });
            }
        });

//showMetadata
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<MetadataEntity> metadataEntityList = MyDB.getDb().getMetadataDao().getMetadataEntityListByDatasetId(dataset_id);
            if (!metadataEntityList.isEmpty()) {
                for (MetadataEntity metadataEntity : metadataEntityList) {
                    if (metadataEntity.auto_fill_type == View.AUTOFILL_TYPE_TEXT) {
                        for (int i = 2; i < binding.linearLayoutContainer.getChildCount(); i++) {
                            TextInputLayout textInputLayout = (TextInputLayout) binding.linearLayoutContainer.getChildAt(i);
                            if (textInputLayout.getTag().equals(metadataEntity.key_word)) {
                                MyApplication.appExecutors.mainThread().execute(() -> Objects.requireNonNull(textInputLayout.getEditText()).setText(metadataEntity.auto_fill_value_text));
                            }
                        }
                    }
                }
            }
        });
//cancel
        binding.cancel.setOnClickListener(v -> finish());
//save
        binding.save.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.ok)
                    .setMessage(R.string.modify_sure)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                    .setPositiveButton(R.string.ok, (dialog, which) -> saveAndTurnToDetailActivity(dataset_id, package_id))
                    .show();
        });
    }

    private void saveAndTurnToDetailActivity(int dataset_id, int package_id) {
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<MetadataEntity> metadataEntityList = new ArrayList<>();
            for (int i = 2; i < binding.linearLayoutContainer.getChildCount(); i++) {
                TextInputLayout textInputLayout = (TextInputLayout) binding.linearLayoutContainer.getChildAt(i);
                String text = Objects.requireNonNull(textInputLayout.getEditText()).getText().toString().trim();
                if (!text.equals("")) {
                    metadataEntityList.add(new MetadataEntity(textInputLayout.getTag().toString(), text, dataset_id, package_id));
                }
            }
            if (!metadataEntityList.isEmpty()) {
                //删除此dataset内的所有数据
                MyDB.getDb().getMetadataDao().deleteByDatasetId(dataset_id);
                //添加时间和app名
                metadataEntityList.add(new MetadataEntity(Type.OTHER_CREATE_TIME,String.valueOf(System.currentTimeMillis()),dataset_id, package_id));
                metadataEntityList.add(new MetadataEntity(Type.OTHER_APPLICATION,MyDB.getDb().getPackageDao().getPackageEntityById(package_id).app_name,dataset_id, package_id));

                long[] longs = MyDB.getDb().getMetadataDao().insert(metadataEntityList.toArray(new MetadataEntity[0]));
                if (Arrays.stream(longs).filter(value -> (int) value == -1).boxed().collect(Collectors.toList()).isEmpty()) {

                    MyApplication.appExecutors.mainThread().execute(() -> {
                        Toast.makeText(MyApplication.applicationContext, R.string.modify_success, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, DetailActivity.class);
                        intent.putExtra(ListAdapter.KEY_INTENT, package_id);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
            }
        });
    }
}