package com.fou.passport.ui.recyclebin;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityRecycleBinBinding;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.BaseActivity;
import com.fou.passport.until.Type;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 回收站：展示被软删除的账户，支持恢复或彻底删除。
 */
public class RecycleBinActivity extends BaseActivity implements RecycleBinAdapter.OnRecycleActionListener {

    private ActivityRecycleBinBinding binding;
    private final List<RecycleBinAdapter.RecycleItem> items = new ArrayList<>();
    private RecycleBinAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecycleBinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.materialToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.recycle_bin);
        adapter = new RecycleBinAdapter(items, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        loadData();
    }

    private void loadData() {
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<DatasetAndMetadata> deletedList = MyDB.getDb().getDatasetDao().getDeletedDatasetAndMetadataList();
            Map<Integer, PackageEntity> packageMap = new HashMap<>();
            for (PackageEntity packageEntity : MyDB.getDb().getPackageDao().getAllPackageEntityList()) {
                packageMap.put(packageEntity.id, packageEntity);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            List<RecycleBinAdapter.RecycleItem> loaded = new ArrayList<>();
            for (DatasetAndMetadata datasetAndMetadata : deletedList) {
                PackageEntity packageEntity = packageMap.get(datasetAndMetadata.datasetEntity.package_id);
                String applicationId = packageEntity == null ? null : packageEntity.application_id;
                String appName = resolveAppName(applicationId, packageEntity == null ? null : packageEntity.app_name);
                String accountText = resolveAccountText(datasetAndMetadata.metadataEntityList);
                String deletedAt = getString(R.string.deleted_at,
                        dateFormat.format(new Date(datasetAndMetadata.datasetEntity.deleted_at)));
                loaded.add(new RecycleBinAdapter.RecycleItem(datasetAndMetadata.datasetEntity,
                        appName, applicationId, accountText, deletedAt));
            }
            MyApplication.appExecutors.mainThread().execute(() -> {
                items.clear();
                items.addAll(loaded);
                adapter.notifyDataSetChanged();
                updateEmptyView();
            });
        });
    }

    private String resolveAppName(String applicationId, String fallbackName) {
        if (applicationId != null && !applicationId.isEmpty()) {
            try {
                ApplicationInfo applicationInfo = MyApplication.applicationContext.getPackageManager()
                        .getApplicationInfo(applicationId, 0);
                return applicationInfo.loadLabel(MyApplication.applicationContext.getPackageManager()).toString();
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return fallbackName == null ? "" : fallbackName;
    }

    private String resolveAccountText(List<MetadataEntity> metadataEntityList) {
        if (metadataEntityList == null) {
            return "";
        }
        for (MetadataEntity metadataEntity : metadataEntityList) {
            if (metadataEntity.auto_fill_value_text != null && !metadataEntity.auto_fill_value_text.isEmpty()
                    && Arrays.asList(Type.strings).contains(metadataEntity.key_word)) {
                return metadataEntity.auto_fill_value_text;
            }
        }
        return "";
    }

    @Override
    public void onRestore(int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        RecycleBinAdapter.RecycleItem item = items.get(position);
        MyApplication.appExecutors.diskIO().execute(() -> {
            MyDB.getDb().getDatasetDao().restore(item.datasetEntity.id);
            MyApplication.appExecutors.mainThread().execute(() -> {
                items.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyView();
                Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show();
                sendBroadcast(new Intent(ACTION_DATABASE_CHANGED));
            });
        });
    }

    @Override
    public void onDelete(int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        RecycleBinAdapter.RecycleItem item = items.get(position);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_permanently)
                .setMessage(R.string.delete_permanently_sure)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) ->
                        MyApplication.appExecutors.diskIO().execute(() -> {
                            MyDB.getDb().getDatasetDao().delete(item.datasetEntity);
                            MyApplication.appExecutors.mainThread().execute(() -> {
                                items.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyView();
                                Toast.makeText(this, R.string.delete_permanently_success, Toast.LENGTH_SHORT).show();
                                sendBroadcast(new Intent(ACTION_DATABASE_CHANGED));
                            });
                        }))
                .show();
    }

    private void updateEmptyView() {
        binding.textViewEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
