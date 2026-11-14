package com.fou.passport.ui;


import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityDetailBinding;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.ui.detail.DetailAdapter;
import com.fou.passport.ui.listfragment.ListAdapter;
import com.fou.passport.ui.newitem.NewItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

public class DetailActivity extends BaseActivity implements DetailAdapter.OnCardActionListener {
    ActivityDetailBinding binding;
    public static String INTENT_KEY_DATASET_ID = "KEY_DATASET_ID";
    public static String INTENT_KEY_PACKAGE_ID = "KEY_PACKAGE_ID";
    public static String INTENT_KEY_POSITION = "INTENT_KEY_POSITION";
    private DetailAdapter detailAdapter;
    private int packageEntity_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //getId
        Intent intent = getIntent();
        if (intent == null) return;
        packageEntity_id = intent.getIntExtra(ListAdapter.KEY_INTENT, -1);
        if (packageEntity_id == -1) return;
        //set views
        setSupportActionBar(binding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        bindingViews(packageEntity_id);
        //deleteButtonClick
        binding.buttonClear.setOnClickListener(v -> new MaterialAlertDialogBuilder(DetailActivity.this).setTitle(R.string.delete_app).setMessage(R.string.clear_app_info).setNegativeButton(R.string.cancel, (dialog, which) -> {
        }).setPositiveButton(R.string.ok, (dialog, which) -> {
            MyApplication.appExecutors.diskIO().execute(() -> {
                MyDB.getDb().getPackageDao().deletePackageEntityById(packageEntity_id);
                MyApplication.appExecutors.mainThread().execute(() -> {
                    Toast.makeText(DetailActivity.this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }).show());
    }

    private void bindingViews(int id) {
        MyApplication.appExecutors.diskIO().execute(() -> {
            String application_id = MyDB.getDb().getPackageDao().getPackageEntityById(id).application_id;
            List<DatasetAndMetadata> datasetAndMetadataList = MyDB.getDb().getDatasetDao().getDatasetAndMetadataListById(id);
            MyApplication.appExecutors.mainThread().execute(() -> {
                //set app icon
                setAppIcon(application_id);
                //set item
                setItem(datasetAndMetadataList);
            });
        });
    }

    /**
     * List<Dataset>  for
     *
     * @param datasetAndMetadataList
     */
    private void setItem(List<DatasetAndMetadata> datasetAndMetadataList) {
        detailAdapter = new DetailAdapter(datasetAndMetadataList, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //与原实现一致：删除时不播放 RecyclerView 自带的移除动画
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setAdapter(detailAdapter);
    }

    @Override
    public void onCardClick(DatasetEntity datasetEntity, int position) {
        //点击跳转到修改界面
        startActivity(new Intent(this, ModifyDataset.class).putExtra(INTENT_KEY_DATASET_ID, datasetEntity.id).putExtra(INTENT_KEY_PACKAGE_ID, datasetEntity.package_id).putExtra(INTENT_KEY_POSITION, position));
    }

    @Override
    public void onCardLongClick(View cardView, DatasetEntity datasetEntity, int position) {
        showMenu(cardView, R.menu.menu_detailactivity_popup, position, datasetEntity);
    }

    private void setAppIcon(String application_id) {
        if (application_id != null) {
            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = getPackageManager().getApplicationInfo(application_id, 0);
                binding.shapeableImageView.setImageDrawable(applicationInfo.loadIcon(getPackageManager()));
                binding.materialTextView.setText(applicationInfo.loadLabel(getPackageManager()));
                Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.details);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detailactivity_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id==android.R.id.home){
            finish();
        }else if (id==R.id.add) {
            startActivity(new Intent(this, NewItem.class).putExtra(INTENT_KEY_PACKAGE_ID,packageEntity_id));
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMenu(View v, @MenuRes int menuRes, int position, DatasetEntity datasetEntity) {
        float ICON_MARGIN = 5;
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(menuRes, popup.getMenu());
        if (popup.getMenu() instanceof MenuBuilder) {
            MenuBuilder menuBuilder = (MenuBuilder) popup.getMenu();
            menuBuilder.setOptionalIconsVisible(true);
            for (MenuItem item : menuBuilder.getVisibleItems()) {
                int iconMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ICON_MARGIN, getResources().getDisplayMetrics());
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
            int itemId = item.getItemId();
            if (itemId == R.id.editor) {
                startActivity(new Intent(DetailActivity.this, ModifyDataset.class).putExtra(INTENT_KEY_DATASET_ID, datasetEntity.id).putExtra(INTENT_KEY_PACKAGE_ID, datasetEntity.package_id).putExtra(INTENT_KEY_POSITION, position));
            } else if (itemId == R.id.favorite) {
                //收藏/取消收藏，用于设置页的快速访问列表
                boolean favorite = !datasetEntity.favorite;
                datasetEntity.favorite = favorite;
                MyApplication.appExecutors.diskIO().execute(() ->
                        MyDB.getDb().getDatasetDao().setFavorite(datasetEntity.id, favorite));
                Toast.makeText(DetailActivity.this,
                        favorite ? R.string.favorite_added : R.string.favorite_removed,
                        Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.delete) {
                new MaterialAlertDialogBuilder(DetailActivity.this).setTitle(getString(R.string.delete_account_position, position+1)).setMessage(R.string.delete_sure).setNegativeButton(R.string.cancel, (dialog, which) -> {
                }).setPositiveButton(R.string.ok, (dialog, which) -> {
                    MyApplication.appExecutors.diskIO().execute(() -> {
                        //软删除：账户进入回收站，可在设置里恢复
                        MyDB.getDb().getDatasetDao().softDelete(datasetEntity.id, System.currentTimeMillis());
                        MyApplication.appExecutors.mainThread().execute(() -> {
                            v.setAnimation(AnimationUtils.loadAnimation(DetailActivity.this, R.anim.scale_down));
                            detailAdapter.removeItem(position);
                            Toast.makeText(MyApplication.applicationContext, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                            if (detailAdapter.getItemCount() == 0) {
                                //最后一个账户删除后，该应用不再有可用账户，返回列表
                                finish();
                            }
                        });
                    });
                }).show();
            }
            return false;
        });
        MenuItem favoriteItem = popup.getMenu().findItem(R.id.favorite);
        if (favoriteItem != null) {
            favoriteItem.setTitle(datasetEntity.favorite ? R.string.unfavorite : R.string.favorite);
        }
        popup.show();
    }
}
