package com.fou.passport.ui.newitem;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.until.Log;
import com.fou.passport.until.SecurityHelper;
import com.fou.passport.until.Type;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class AdapterBottomSheet extends RecyclerView.Adapter<AdapterBottomSheet.AppViewHolder> {

    private List<ApplicationInfo> appList;
    private OnBottomSheetItemClickListener onBottomSheetItemClickListener;

    public AdapterBottomSheet(List<ApplicationInfo> appList) {
        this.appList = appList;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bottomsheet_layout, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {

        ApplicationInfo appInfo = appList.get(position);
        PackageManager packageManager = MyApplication.applicationContext.getPackageManager();
        // 设置应用程序名称
        holder.appName.setText(appInfo.loadLabel(packageManager));
        // 设置应用程序图标
        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager));
        //click
        holder.linearLayout.setOnClickListener(v -> {
           //向NewItem传递点击事件
            onBottomSheetItemClickListener.getApplicationAboutClickedItem(appInfo);
        });
    }


    @Override
    public int getItemCount() {
        return appList.size();
    }

    // ViewHolder类
    public class AppViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        MaterialTextView appName;
        ShapeableImageView appIcon;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.textView_item);
            appIcon = itemView.findViewById(R.id.imageView_item);
            linearLayout = itemView.findViewById(R.id.linearLayout);
        }
    }

    /**
     * item点击接口实现（向newItem传递参数）
     * @param onBottomSheetItemClickListener
     */
    public void getPackageEntityAboutClickedItem(OnBottomSheetItemClickListener onBottomSheetItemClickListener){
        this.onBottomSheetItemClickListener=onBottomSheetItemClickListener;
    }

    /**
     * BottomSheetItem点击监听器
     */
    interface OnBottomSheetItemClickListener{
        /**
         * @param applicationInfo 点击item所对应的applicationInfo
         */
        void getApplicationAboutClickedItem(ApplicationInfo applicationInfo);
    }

}


