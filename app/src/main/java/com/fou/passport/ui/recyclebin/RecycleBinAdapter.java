package com.fou.passport.ui.recyclebin;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.DatasetEntity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class RecycleBinAdapter extends RecyclerView.Adapter<RecycleBinAdapter.RecycleHolder> {

    public interface OnRecycleActionListener {
        void onRestore(int position);

        void onDelete(int position);
    }

    public static class RecycleItem {
        public final DatasetEntity datasetEntity;
        public final String appName;
        public final String applicationId;
        public final String accountText;
        public final String deletedAtText;

        public RecycleItem(DatasetEntity datasetEntity, String appName, String applicationId,
                           String accountText, String deletedAtText) {
            this.datasetEntity = datasetEntity;
            this.appName = appName;
            this.applicationId = applicationId;
            this.accountText = accountText;
            this.deletedAtText = deletedAtText;
        }
    }

    private final List<RecycleItem> list;
    private final OnRecycleActionListener listener;

    public RecycleBinAdapter(List<RecycleItem> list, OnRecycleActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecycleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle_bin, parent, false);
        return new RecycleHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecycleHolder holder, int position) {
        RecycleItem item = list.get(position);
        holder.textView_appName.setText(item.appName);
        holder.textView_account.setText(item.accountText);
        holder.textView_deletedAt.setText(item.deletedAtText);
        holder.imageView.setImageDrawable(appIcon(item.applicationId));
        holder.button_restore.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onRestore(adapterPosition);
            }
        });
        holder.button_delete.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onDelete(adapterPosition);
            }
        });
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

    static class RecycleHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imageView;
        final MaterialTextView textView_appName;
        final MaterialTextView textView_account;
        final MaterialTextView textView_deletedAt;
        final com.google.android.material.button.MaterialButton button_restore;
        final com.google.android.material.button.MaterialButton button_delete;

        RecycleHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView_appName = itemView.findViewById(R.id.textView_appName);
            textView_account = itemView.findViewById(R.id.textView_account);
            textView_deletedAt = itemView.findViewById(R.id.textView_deletedAt);
            button_restore = itemView.findViewById(R.id.button_restore);
            button_delete = itemView.findViewById(R.id.button_delete);
        }
    }
}
