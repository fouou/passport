package com.fou.passport.ui.listfragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.DetailActivity;
import com.fou.passport.until.SecurityHelper;
import com.fou.passport.until.Type;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ListAdapter_searchview extends RecyclerView.Adapter<ListAdapter_searchview.ListHolder> {
    private Activity activity;
    List<PackageEntity> list;

    public ListAdapter_searchview(Activity activity, List<PackageEntity> packageEntityList) {
        this.activity=activity;
        this.list = packageEntityList;
    }

    @NonNull
    @Override
    public ListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fragment_list, parent, false);
        //view.startAnimation(AnimationUtils.loadAnimation(MyApplication.applicationContext,R.anim.scale_right));//anim
        return new ListHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListHolder holder, int position) {
        PackageEntity packageEntity = list.get(position);
        try {//签名一致
            if (packageEntity.application_id != null && packageEntity.certificates_hash != null && packageEntity.certificates_hash.equals(SecurityHelper.getCertificatesHash(packageEntity.application_id))) {
                ApplicationInfo applicationInfo = MyApplication.applicationContext.getPackageManager().getApplicationInfo(list.get(position).application_id, 0);
                if (applicationInfo!=null) {
                    holder.imageView.setImageDrawable(applicationInfo.loadIcon(MyApplication.applicationContext.getPackageManager()));
                    holder.textView_appName.setText(applicationInfo.loadLabel(MyApplication.applicationContext.getPackageManager()).toString());
                    showText(holder.textView_account, packageEntity);//展示邮箱手机号码之类的
                    holder.linearLayout.setOnClickListener(v -> {
                        Intent intent = new Intent(activity, DetailActivity.class);
                        intent.putExtra(ListAdapter.KEY_INTENT, list.get(holder.getAdapterPosition()).id);
                        activity.startActivity(intent);
                    });
                    return;
                }
            }
            //签名不一致，或用户自定义的
            holder.imageView.setImageDrawable(ContextCompat.getDrawable(MyApplication.applicationContext, R.drawable.app_ic));
            holder.textView_appName.setText(packageEntity.app_name);
            showText(holder.textView_account ,packageEntity);
            holder.linearLayout.setOnClickListener(v -> {
                Intent intent = new Intent(activity, DetailActivity.class);
                intent.putExtra(ListAdapter.KEY_INTENT, list.get(holder.getAdapterPosition()).id);
                activity.startActivity(intent);
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ListHolder extends RecyclerView.ViewHolder {

        public final ShapeableImageView imageView;
        public final MaterialTextView textView_appName;
        public final MaterialTextView textView_account;
        private final LinearLayout linearLayout;

        public ListHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView_appName = itemView.findViewById(R.id.textView_appName);
            textView_account = itemView.findViewById(R.id.textView_account);
            linearLayout = itemView.findViewById(R.id.linearLayout);
        }
    }

    /**
     * 展示app名称下方的小字
     * @param materialTextView
     * @param packageEntity
     */
    public void showText(MaterialTextView materialTextView , PackageEntity packageEntity){
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<MetadataEntity> metadataEntityList= MyDB.getDb().getMetadataDao().getMetadataEntityListByPackageId(packageEntity.id);
            if (!metadataEntityList.isEmpty()) {
                List<MetadataEntity> metadataEntityList_showText=metadataEntityList.stream().filter(metadataEntity -> metadataEntity.auto_fill_type== View.AUTOFILL_TYPE_TEXT
                        && Arrays.asList(Type.strings).contains(metadataEntity.key_word)).collect(Collectors.toList());
                if (!metadataEntityList_showText.isEmpty()) {
                    MyApplication.appExecutors.mainThread().execute(() -> {
                        materialTextView.setText(metadataEntityList_showText.get(0).auto_fill_value_text);
                    });
                }
            }
        });
    }
}
