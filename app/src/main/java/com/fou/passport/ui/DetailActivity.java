package com.fou.passport.ui;


import static com.fou.passport.until.Type.*;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.databinding.ActivityDetailBinding;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.ui.listfragment.ListAdapter;
import com.fou.passport.ui.newitem.NewItem;
import com.fou.passport.until.DpToPx;
import com.fou.passport.until.Log;
import com.fou.passport.until.MyUntil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DetailActivity extends BaseActivity {
    ActivityDetailBinding binding;
    public static String INTENT_KEY_DATASET_ID = "KEY_DATASET_ID";
    public static String INTENT_KEY_PACKAGE_ID = "KEY_PACKAGE_ID";
    public static String INTENT_KEY_POSITION = "INTENT_KEY_POSITION";
    private List<MaterialButton> materialButtonList = new ArrayList<>();
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
        if (!datasetAndMetadataList.isEmpty()) {
            for (int i = 0; i < datasetAndMetadataList.size(); i++) {
                Log.DetailActivityy(datasetAndMetadataList.get(i).datasetEntity.id + "");
                extracted(datasetAndMetadataList.get(i), generateContainer(i, datasetAndMetadataList.get(i).datasetEntity));
            }
        }
    }

    /**
     * 生成一个为一个dataset装metadata的linearlayout
     *
     * @param position 第几个账户
     * @return
     */
    private LinearLayout generateContainer(int position, DatasetEntity datasetEntity) {
        //cardView
        MaterialCardView materialCardView = new MaterialCardView(this, null, com.google.android.material.R.attr.materialCardViewElevatedStyle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(DpToPx.dpToPx(10), DpToPx.dpToPx(20), DpToPx.dpToPx(10), DpToPx.dpToPx(20));
        materialCardView.setLayoutParams(params);
        //长按事件
        materialCardView.setOnLongClickListener(v -> {
            showMenu(materialCardView, R.menu.menu_detailactivity_popup, position, datasetEntity);
            return false;
        });
        //点击事件
        setOnclick(materialCardView, datasetEntity, position);
        // 设置LinearLayout
        LinearLayout linearLayout_container_metadata = new LinearLayout(this); // 创建一个LinearLayout
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(DpToPx.dpToPx(20), DpToPx.dpToPx(20), DpToPx.dpToPx(20), DpToPx.dpToPx(20));
        linearLayout_container_metadata.setLayoutParams(layoutParams); // 设置LinearLayout的布局参数与CardView相同
        linearLayout_container_metadata.setOrientation(LinearLayout.VERTICAL); // 设置LinearLayout的方向
        //添加标题（Account 1）
        MaterialTextView materialTextView = new MaterialTextView(this);
        materialTextView.setText(getString(R.string.account_count, position + 1));
        materialTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        materialTextView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params_text = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params_text.setMargins(0, 0, 0, DpToPx.dpToPx(10));
        materialTextView.setLayoutParams(params_text);
        //addView
        linearLayout_container_metadata.addView(materialTextView);
        materialCardView.addView(linearLayout_container_metadata);
        materialCardView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
        binding.linearLayoutContainer.addView(materialCardView);
        return linearLayout_container_metadata;
    }

    /**
     * 点击跳转到修改界面
     *
     * @param materialCardView
     * @param datasetEntity
     * @param position
     */
    private void setOnclick(MaterialCardView materialCardView, DatasetEntity datasetEntity, int position) {
        materialCardView.setOnClickListener(v -> {
            startActivity(new Intent(this, ModifyDataset.class).putExtra(INTENT_KEY_DATASET_ID, datasetEntity.id).putExtra(INTENT_KEY_PACKAGE_ID, datasetEntity.package_id).putExtra(INTENT_KEY_POSITION, position));
        });
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

    /**
     * Add item in the container in order(stringList)
     *
     * @param datasetAndMetadata
     */
    private void extracted(DatasetAndMetadata datasetAndMetadata, LinearLayout datasetContainer) {
        for (String s : allStrings) {//按照allString列表顺序展示
            for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                if (s.equals(metadataEntity.key_word) && metadataEntity.auto_fill_value_text != null) {
                    addItemToContainer(s, metadataEntity.auto_fill_value_text, datasetContainer);
                }
            }
        }
        //添加模板字段
        addCategory(datasetAndMetadata, datasetContainer);
    }


    /**
     * 添加模板字段
     *
     * @param datasetAndMetadata
     * @param datasetContainer
     */
    private void addCategory(DatasetAndMetadata datasetAndMetadata, LinearLayout datasetContainer) {
        Set<String> categorySet = new HashSet<>();//防止重复
        for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
            if (metadataEntity.key_word != null) {
                switch (metadataEntity.key_word) {
                    case CARD:
                        categorySet.add(getString(R.string.bank_cards));
                        break;
                    case PASSWORD:
                        categorySet.add(getString(R.string.passwords));
                        break;
                    // TODO 许可证模板
                }
            }
        }
        if (!categorySet.isEmpty()) {
            String category = MyUntil.turnSetToString(categorySet);
            addItemToContainer(OTHER_CATEGORY, category, datasetContainer);
        }
    }

    /**
     * @param key_word
     * @param text
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void addItemToContainer(String key_word, String text, LinearLayout datasetContainer) {
        View view = getLayoutInflater().inflate(R.layout.item_detail_layout, binding.linearLayoutContainer, false);
        ShapeableImageView imageView = view.findViewById(R.id.item_imageView);
        MaterialTextView textView = view.findViewById(R.id.item_textView);
        MaterialTextView textView_right = view.findViewById(R.id.item_textView_right);
        switch (key_word) {
            case USERNAME:
                textView.setText(R.string.username);
                imageView.setImageDrawable(getDrawable(R.drawable.account_circle_fill0_wght700_grad0_opsz48));
                break;
            case PHONE:
                textView.setText(R.string.phone);
                imageView.setImageDrawable(getDrawable(R.drawable.call_fill0_wght700_grad0_opsz48));
                break;
            case EMAIL:
                textView.setText(R.string.email);
                imageView.setImageDrawable(getDrawable(R.drawable.mail_fill0_wght700_grad0_opsz48));
                break;
            case PASSWORD:
                textView.setText(R.string.passwords);
                imageView.setImageDrawable(getDrawable(R.drawable.key_fill0_wght700_grad0_opsz48));
                addEyeView(view);//小眼睛
                break;
            case CARD:
                textView.setText(R.string.bank_cards);
                imageView.setImageDrawable(getDrawable(R.drawable.credit_card_fill0_wght700_grad0_opsz48));
                break;
            case CVV:
                textView.setText(R.string.cvv);
                imageView.setImageDrawable(getDrawable(R.drawable.security_fill0_wght700_grad0_opsz48__1_));
                break;
            case OTHER_WEBSITE:
                textView.setText(R.string.website);
                imageView.setImageDrawable(getDrawable(R.drawable.language_fill0_wght700_grad0_opsz48));
                break;
            case OTHER_APPLICATION:
                textView.setText(R.string.app);
                imageView.setImageDrawable(getDrawable(R.drawable.crop_square));
                break;
            case OTHER_CREATE_TIME:
                imageView.setImageDrawable(getDrawable(R.drawable.schedule));
                textView.setText(R.string.creation_time);
                textView_right.setText(MyUntil.getSaveTime(text));//需要将毫秒转对应语言的时间日期
                datasetContainer.addView(view);
                return;
            case OTHER_CATEGORY:
                imageView.setImageDrawable(getDrawable(R.drawable.sell_fill0_wght700_grad0_opsz48));
                textView.setText(R.string.category);
                break;
            case OTHER_ISSUING_BANK:
                imageView.setImageDrawable(getDrawable(R.drawable.location_city_fill0_wght700_grad0_opsz48));
                textView.setText(R.string.issuing_bank);
                break;
        }
        textView_right.setText(text);
        datasetContainer.addView(view);
    }

    private void addEyeView(View view) {
        RelativeLayout relativeLayout = (RelativeLayout) view;
//addImageButton
        MaterialButton myButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle); // context是上下文对象
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        myButton.setLayoutParams(params);
        myButton.setIconResource(R.drawable.visibility_off_fill0_wght700_grad0_opsz48);
        //添加到relativeLayout
        relativeLayout.addView(myButton);
        //默认不可见
        MaterialTextView materialTextView = view.findViewById(R.id.item_textView_right);
        materialTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        myButton.setOnClickListener(v -> showHidePassword(materialTextView, myButton));
    }

    private boolean isShowPassword = false;

    private void showHidePassword(MaterialTextView textView, MaterialButton button) {
        if (isShowPassword) {
            // 显示星号
            textView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            button.setIconResource(R.drawable.visibility_off_fill0_wght700_grad0_opsz48);
            isShowPassword = false;
        } else {
            // 显示明文
            textView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            button.setIconResource(R.drawable.visibility_fill0_wght700_grad0_opsz48);
            isShowPassword = true;
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
            switch (item.getItemId()) {
                case R.id.editor:
                    startActivity(new Intent(DetailActivity.this, ModifyDataset.class).putExtra(INTENT_KEY_DATASET_ID, datasetEntity.id).putExtra(INTENT_KEY_PACKAGE_ID, datasetEntity.package_id).putExtra(INTENT_KEY_POSITION, position));
                    break;
                case R.id.delete:
                    new MaterialAlertDialogBuilder(DetailActivity.this).setTitle(getString(R.string.delete_account_position, position+1)).setMessage(R.string.delete_sure).setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).setPositiveButton(R.string.ok, (dialog, which) -> {
                        MyApplication.appExecutors.diskIO().execute(() -> {
                            if (binding.linearLayoutContainer.getChildCount() == 1) {
                                //只有一个账户，直接删除数据库
                                MyDB.getDb().getPackageDao().deletePackageEntityById(datasetEntity.package_id);
                                finish();
                                return;
                            }
                            MyDB.getDb().getDatasetDao().delete(datasetEntity);
                            MyApplication.appExecutors.mainThread().execute(() -> {
                                v.setAnimation(AnimationUtils.loadAnimation(DetailActivity.this, R.anim.scale_down));
                                binding.linearLayoutContainer.removeView(v);
                                Toast.makeText(MyApplication.applicationContext, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                            });
                        });
                    }).show();
                    break;
            }
            return false;
        });
        popup.show();
    }
}