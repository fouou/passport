package com.fou.passport.ui.categoryfragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.BaseActivity;
import com.fou.passport.until.Type;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板页：把全部账户按银行卡、密码、手机号、邮箱、用户名等字段归类展示。
 */
public class CategoryFragment extends Fragment {

    public static final int CATEGORY_ALL = 0;
    public static final int CATEGORY_PASSWORD = 1;
    public static final int CATEGORY_BANKCARD = 2;
    public static final int CATEGORY_PHONE = 3;
    public static final int CATEGORY_EMAIL = 4;
    public static final int CATEGORY_USERNAME = 5;
    public static final int CATEGORY_OTHER = 6;

    private final List<CategoryAdapter.CategoryItem> allItems = new ArrayList<>();
    private final List<CategoryAdapter.CategoryItem> shownItems = new ArrayList<>();
    private CategoryAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private int currentCategory = CATEGORY_ALL;
    private BroadcastReceiver databaseChangedReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerDatabaseChangedReceiver();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.empty_view);
        adapter = new CategoryAdapter((AppCompatActivity) requireActivity(), shownItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        setChips(view.findViewById(R.id.chipGroup));
    }

    private void setChips(ChipGroup chipGroup) {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_password) {
                currentCategory = CATEGORY_PASSWORD;
            } else if (checkedId == R.id.chip_bankcard) {
                currentCategory = CATEGORY_BANKCARD;
            } else if (checkedId == R.id.chip_phone) {
                currentCategory = CATEGORY_PHONE;
            } else if (checkedId == R.id.chip_email) {
                currentCategory = CATEGORY_EMAIL;
            } else if (checkedId == R.id.chip_username) {
                currentCategory = CATEGORY_USERNAME;
            } else if (checkedId == R.id.chip_other) {
                currentCategory = CATEGORY_OTHER;
            } else {
                currentCategory = CATEGORY_ALL;
            }
            applyFilter();
        });
    }

    private void loadData() {
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<DatasetAndMetadata> datasetAndMetadataList =
                    MyDB.getDb().getDatasetDao().getDatasetAndMetadataList();
            Map<Integer, PackageEntity> packageMap = new HashMap<>();
            for (PackageEntity packageEntity : MyDB.getDb().getPackageDao().getPackageEntityList()) {
                packageMap.put(packageEntity.id, packageEntity);
            }
            List<CategoryAdapter.CategoryItem> items = buildItems(datasetAndMetadataList, packageMap);
            MyApplication.appExecutors.mainThread().execute(() -> {
                if (getView() == null) {
                    return;
                }
                allItems.clear();
                allItems.addAll(items);
                applyFilter();
            });
        });
    }

    private List<CategoryAdapter.CategoryItem> buildItems(List<DatasetAndMetadata> datasetAndMetadataList,
                                                          Map<Integer, PackageEntity> packageMap) {
        List<CategoryAdapter.CategoryItem> items = new ArrayList<>();
        for (DatasetAndMetadata datasetAndMetadata : datasetAndMetadataList) {
            PackageEntity packageEntity = packageMap.get(datasetAndMetadata.datasetEntity.package_id);
            String applicationId = packageEntity == null ? null : packageEntity.application_id;
            String appName = resolveAppName(applicationId, packageEntity == null ? null : packageEntity.app_name);
            String accountText = resolveAccountText(datasetAndMetadata.metadataEntityList);
            List<Integer> categories = resolveCategories(datasetAndMetadata.metadataEntityList);
            items.add(new CategoryAdapter.CategoryItem(datasetAndMetadata.datasetEntity,
                    appName, applicationId, accountText, categories));
        }
        return items;
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
        //按用户名、手机号、邮箱的优先级展示账号
        String[] priorities = {Type.USERNAME, Type.PHONE, Type.EMAIL};
        for (String keyWord : priorities) {
            for (MetadataEntity metadataEntity : metadataEntityList) {
                if (keyWord.equals(metadataEntity.key_word)
                        && metadataEntity.auto_fill_value_text != null
                        && !metadataEntity.auto_fill_value_text.isEmpty()) {
                    return metadataEntity.auto_fill_value_text;
                }
            }
        }
        return "";
    }

    /**
     * 按账户里保存的字段归类：银行卡、密码、手机号、邮箱、用户名，皆无则归为其他。
     */
    private List<Integer> resolveCategories(List<MetadataEntity> metadataEntityList) {
        List<Integer> categories = new ArrayList<>();
        if (metadataEntityList != null) {
            for (MetadataEntity metadataEntity : metadataEntityList) {
                String keyWord = metadataEntity.key_word;
                if (keyWord == null) {
                    continue;
                }
                if (Type.CARD.equals(keyWord) || Type.CVV.equals(keyWord)) {
                    addCategory(categories, CATEGORY_BANKCARD);
                } else if (Type.PASSWORD.equals(keyWord)) {
                    addCategory(categories, CATEGORY_PASSWORD);
                } else if (Type.PHONE.equals(keyWord)) {
                    addCategory(categories, CATEGORY_PHONE);
                } else if (Type.EMAIL.equals(keyWord)) {
                    addCategory(categories, CATEGORY_EMAIL);
                } else if (Type.USERNAME.equals(keyWord)) {
                    //旧数据可能把手机号/邮箱统一存成用户名，按内容归类
                    String value = metadataEntity.auto_fill_value_text;
                    if (Type.isEmailValue(value)) {
                        addCategory(categories, CATEGORY_EMAIL);
                    } else if (Type.isPhoneValue(value)) {
                        addCategory(categories, CATEGORY_PHONE);
                    } else {
                        addCategory(categories, CATEGORY_USERNAME);
                    }
                }
            }
        }
        if (categories.isEmpty()) {
            categories.add(CATEGORY_OTHER);
        }
        return categories;
    }

    private void addCategory(List<Integer> categories, int category) {
        if (!categories.contains(category)) {
            categories.add(category);
        }
    }

    private void applyFilter() {
        shownItems.clear();
        for (CategoryAdapter.CategoryItem item : allItems) {
            if (currentCategory == CATEGORY_ALL || item.categories.contains(currentCategory)) {
                shownItems.add(item);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (emptyView != null) {
            emptyView.setVisibility(shownItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void registerDatabaseChangedReceiver() {
        databaseChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadData();
            }
        };
        ContextCompat.registerReceiver(requireContext(), databaseChangedReceiver,
                new IntentFilter(BaseActivity.ACTION_DATABASE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseChangedReceiver != null) {
            requireContext().unregisterReceiver(databaseChangedReceiver);
            databaseChangedReceiver = null;
        }
    }
}
