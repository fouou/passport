package com.fou.passport.ui.listfragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.ui.DetailActivity;
import com.fou.passport.until.Log;
import com.fou.passport.until.SecurityHelper;
import com.fou.passport.until.Type;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListHolder> {
    public static String KEY_INTENT = "ListAdapter";
    private final Activity activity;
    List<PackageEntity> list;
    public ListAdapter(Activity activity, List<PackageEntity> packageEntityList) {
        this.activity=activity;
        this.list = packageEntityList;
    }

    @NonNull
    @Override
    public ListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fragment_list, parent, false);
        //view.startAnimation(AnimationUtils.loadAnimation(activity,R.anim.scale_right));//anim
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
                    //长按删除
                    holder.linearLayout.setOnLongClickListener(v -> {
                        showMenu(v,R.menu.menu_fragmentlist_popup, holder.getAdapterPosition());
                        return true;
                    });
                    holder.linearLayout.setOnClickListener(v -> {
                        Log.ListAdapter("点击了"+position);
                        Intent intent = new Intent(activity, DetailActivity.class);
                        intent.putExtra(KEY_INTENT, list.get(holder.getAdapterPosition()).id);
                        activity.startActivity(intent);
                    });
                    return;
                }
            }
            //签名不一致，或用户自定义的
            holder.imageView.setImageDrawable(ContextCompat.getDrawable(MyApplication.applicationContext, R.drawable.app_ic));
            holder.textView_appName.setText(packageEntity.app_name);
            showText(holder.textView_account ,packageEntity);
            //长按删除
            holder.linearLayout.setOnLongClickListener(v -> {
                showMenu(v,R.menu.menu_fragmentlist_popup,holder.getAdapterPosition());
                return true;
            });
            holder.linearLayout.setOnClickListener(v -> {
                Intent intent = new Intent(activity, DetailActivity.class);
                intent.putExtra(KEY_INTENT, list.get(holder.getAdapterPosition()).id);
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

    private void showMenu(View v, @MenuRes int menuRes,int position) {
        float ICON_MARGIN=5;
        PopupMenu popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(menuRes, popup.getMenu());
        if (popup.getMenu() instanceof MenuBuilder) {
            MenuBuilder menuBuilder = (MenuBuilder) popup.getMenu();
            menuBuilder.setOptionalIconsVisible(true);
            for (MenuItem item : menuBuilder.getVisibleItems()) {
                int iconMarginPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, ICON_MARGIN, MyApplication.applicationContext.getResources().getDisplayMetrics());
                if (item.getIcon() != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        item.setIcon(new InsetDrawable(item.getIcon(), iconMarginPx, 0, iconMarginPx, 0));
                    } else {
                        item.setIcon(new InsetDrawable(item.getIcon(), iconMarginPx, 0, iconMarginPx, 0) {
                            @Override
                            public int getIntrinsicWidth() {
                                return getIntrinsicHeight() + iconMarginPx + iconMarginPx;
                            }
                        });
                    }
                }
            }
        }
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId()==R.id.delete) {
                //存储要删的packageEntity对象，防止工作线程访问时该packageEntity已经被主线程从集合中移除
                PackageEntity packageEntity_delete=list.get(position);
                //工作线程数据库移除
                MyApplication.appExecutors.diskIO().execute(() -> MyDB.getDb().getPackageDao().delete(packageEntity_delete));
                //主线程list移除
                list.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(MyApplication.applicationContext, R.string.delete_success, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

}
