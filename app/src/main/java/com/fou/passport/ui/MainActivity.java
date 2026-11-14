package com.fou.passport.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.fou.passport.R;
import com.fou.passport.databinding.ActivityMainBinding;
import com.fou.passport.until.AppLock;
import com.fou.passport.until.AutofillCompat;
import com.fou.passport.until.MyPreferences;
import com.fou.passport.until.SystemAuthHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 主界面：密码列表、分类、设置三页左右滑动切换，底部导航保持同步。
 */
public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyPreferences preferences = MyPreferences.getInstance(this);
        //系统已有锁屏时直接用系统指纹/密码解锁，不再强制设置应用密码
        if (!preferences.hasAnyCredential() && !SystemAuthHelper.isDeviceSecure(this)) {
            startActivity(new Intent(this, CreateAccount.class));
            finish();
            return;
        }
        //冷启动必须解锁后才能进入主界面
        if (!AppLock.isUnlocked()) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViewPager();
        setupBottomNavigation();
        startSettingAutofillActivity();
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new MainPagerAdapter(this));
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                syncBottomNavigation(position);
            }
        });
    }

    /**
     * 滑动切页后同步底部导航选中状态；setChecked 不会再次触发选择回调。
     */
    private void syncBottomNavigation(int position) {
        int itemId;
        switch (position) {
            case MainPagerAdapter.PAGE_CATEGORY:
                itemId = R.id.navigation_folder;
                break;
            case MainPagerAdapter.PAGE_SETTINGS:
                itemId = R.id.navigation_settings;
                break;
            default:
                itemId = R.id.navigation_password_list;
                break;
        }
        binding.bottomNavigationView.getMenu().findItem(itemId).setChecked(true);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int position;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_folder) {
                position = MainPagerAdapter.PAGE_CATEGORY;
            } else if (itemId == R.id.navigation_settings) {
                position = MainPagerAdapter.PAGE_SETTINGS;
            } else if (itemId == R.id.navigation_password_list) {
                position = MainPagerAdapter.PAGE_LIST;
            } else {
                return false;
            }
            if (binding.viewPager.getCurrentItem() != position) {
                binding.viewPager.setCurrentItem(position, true);
            }
            return true;
        });
    }

    public void startSettingAutofillActivity() {
        //Android 8.0 以下没有系统自动填充框架，只能作为密码本使用
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, R.string.no_support_service, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AutofillCompat.isSupported(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.unsupport_service)
                    .setMessage(R.string.no_support_service)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                    }).show();
        } else if (!MyPreferences.getInstance(this).isOurAutofillServiceEnabled()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.turn_on_service)
                    .setMessage(R.string.turn_on_service_subscribe)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).show();
        }
    }
}
