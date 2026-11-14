package com.fou.passport.ui.categoryfragment;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.ui.DetailActivity;
import com.fou.passport.ui.listfragment.ListAdapter;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryHolder> {

    /**
     * 模板页的一行：账户 + 展示用的应用名/账号/分类。
     */
    public static class CategoryItem {
        public final DatasetEntity datasetEntity;
        public final String appName;
        public final String applicationId;
        public final String accountText;
        /**
         * 该账户包含的字段分类，可能同时属于多个分类。
         */
        public final List<Integer> categories;

        public CategoryItem(DatasetEntity datasetEntity, String appName, String applicationId,
                            String accountText, List<Integer> categories) {
            this.datasetEntity = datasetEntity;
            this.appName = appName;
            this.applicationId = applicationId;
            this.accountText = accountText;
            this.categories = categories;
        }
    }

    private static final int[] CATEGORY_LABEL_ORDER = {
            CategoryFragment.CATEGORY_PASSWORD,
            CategoryFragment.CATEGORY_BANKCARD,
            CategoryFragment.CATEGORY_PHONE,
            CategoryFragment.CATEGORY_EMAIL,
            CategoryFragment.CATEGORY_USERNAME,
            CategoryFragment.CATEGORY_OTHER
    };

    private final AppCompatActivity activity;
    private final List<CategoryItem> list;

    public CategoryAdapter(AppCompatActivity activity, List<CategoryItem> list) {
        this.activity = activity;
        this.list = list;
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_account, parent, false);
        return new CategoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        CategoryItem item = list.get(position);
        holder.textView_appName.setText(item.appName);
        holder.textView_account.setText(item.accountText);
        holder.textView_category.setText(categoryLabel(item));
        holder.imageView.setImageDrawable(appIcon(item.applicationId));
        holder.itemView.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, DetailActivity.class)
                        .putExtra(ListAdapter.KEY_INTENT, item.datasetEntity.package_id)));
    }

    /**
     * 展示账户包含的所有字段分类，例如“银行卡 / 密码”“邮箱 / 用户名”。
     */
    private CharSequence categoryLabel(CategoryItem item) {
        List<String> labels = new ArrayList<>();
        for (int category : CATEGORY_LABEL_ORDER) {
            if (item.categories.contains(category)) {
                labels.add(activity.getString(categoryLabelRes(category)));
            }
        }
        return android.text.TextUtils.join(" / ", labels);
    }

    private int categoryLabelRes(int category) {
        switch (category) {
            case CategoryFragment.CATEGORY_PASSWORD:
                return R.string.passwords;
            case CategoryFragment.CATEGORY_BANKCARD:
                return R.string.bank_cards;
            case CategoryFragment.CATEGORY_PHONE:
                return R.string.category_phone;
            case CategoryFragment.CATEGORY_EMAIL:
                return R.string.category_email;
            case CategoryFragment.CATEGORY_USERNAME:
                return R.string.category_username;
            default:
                return R.string.category_other;
        }
    }

    private android.graphics.drawable.Drawable appIcon(String applicationId) {
        if (applicationId != null && !applicationId.isEmpty()) {
            try {
                ApplicationInfo applicationInfo = MyApplication.applicationContext.getPackageManager()
                        .getApplicationInfo(applicationId, 0);
                return applicationInfo.loadIcon(MyApplication.applicationContext.getPackageManager());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return ContextCompat.getDrawable(MyApplication.applicationContext, R.drawable.app_ic);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class CategoryHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imageView;
        final MaterialTextView textView_appName;
        final MaterialTextView textView_account;
        final MaterialTextView textView_category;

        CategoryHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView_appName = itemView.findViewById(R.id.textView_appName);
            textView_account = itemView.findViewById(R.id.textView_account);
            textView_category = itemView.findViewById(R.id.textView_category);
        }
    }
}
