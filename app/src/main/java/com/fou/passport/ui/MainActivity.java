package com.fou.passport.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fou.passport.R;
import com.fou.passport.databinding.ActivityMainBinding;
import com.fou.passport.service.BrowserHelper;
import com.fou.passport.ui.listfragment.ListFragment;
import com.fou.passport.ui.settingfragment.SettingsFragment;
import com.fou.passport.until.Log;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private Fragment listFragment, messageFragment, myFragment ,settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //数据恢复
        getSaveInstanceState(savedInstanceState);
        //跳转到自动填充设置页面
        startSettingAutofillActivity();
        //底部导航栏的选择监听器，只监听从未未选中到选中状态
        setBottomNavigation();
        //展示初始显示页面
        if (savedInstanceState==null) {
            showListFragment();
        }
    }

    private void getSaveInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.MainActivityy("savedInstanceState != null");
            FragmentManager fragmentManager = getSupportFragmentManager();
            ListFragment listFragment = (ListFragment) fragmentManager.findFragmentByTag(ListFragment.class.getSimpleName());
            SettingsFragment settingsFragment = (SettingsFragment) fragmentManager.findFragmentByTag(SettingsFragment.class.getSimpleName());
            // 如果找到了对应的 Fragment，则重新赋值
            if (listFragment != null) {
                this.listFragment = listFragment;
            }
            if (settingsFragment != null) {
                this.settingsFragment = settingsFragment;
            }
        }
    }

    private void showListFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        hideAllFrg(fragmentTransaction);
        if (listFragment==null) {
            listFragment=ListFragment.newInstance("","");
            fragmentTransaction.add(R.id.fragmentContainerView, listFragment,ListFragment.class.getSimpleName());
        }
        fragmentTransaction.show(listFragment).commit();
    }

    private void setBottomNavigation() {
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //每一个fragmentTransaction提交之后将不能再使用，所以要放在这里，不能再类里面去定义，防止重复使用同一个fragmentTransaction
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                switch (item.getItemId()) {
                    case R.id.navigation_password_list://passwordList
                        hideAllFrg(fragmentTransaction);
                        if (listFragment==null) {
                            listFragment=ListFragment.newInstance("","");
                            fragmentTransaction.add(R.id.fragmentContainerView, listFragment, ListFragment.class.getSimpleName());
                        }
                        fragmentTransaction.show(listFragment).commit();
                        break;
                    case R.id.navigation_folder://Categories
                        break;
                    case R.id.navigation_dialpad://Authenticator
                        break;
                    case R.id.navigation_settings://settings
                        hideAllFrg(fragmentTransaction);
                        if (settingsFragment==null) {
                            settingsFragment= new SettingsFragment();
                            fragmentTransaction.add(R.id.fragmentContainerView, settingsFragment, SettingsFragment.class.getSimpleName());
                        }
                        fragmentTransaction.show(settingsFragment).commit();
                        break;
                }
                return true;
            }
        });
    }

    public void startSettingAutofillActivity() {
        AutofillManager mAutofillManager=getSystemService(AutofillManager.class);
        if (!mAutofillManager.isAutofillSupported()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.unsupport_service)
                    .setMessage(R.string.no_support_service)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                    }).show();
        } else if (mAutofillManager != null && !mAutofillManager.hasEnabledAutofillServices()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.turn_on_service)
                    .setMessage(R.string.turn_on_service_subscribe)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                        intent.setData(Uri.parse("package:com.fou.passport.service.MyAutofillService2"));
                        startActivity(intent);
                    }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).show();
        }
    }

    private void hideAllFrg(FragmentTransaction fragmentTransaction) {
        if (null != listFragment) {
            fragmentTransaction.hide(listFragment);
        }
        if (null != settingsFragment) {
            fragmentTransaction.hide(settingsFragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}