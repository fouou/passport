package com.fou.passport.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fou.passport.ui.categoryfragment.CategoryFragment;
import com.fou.passport.ui.listfragment.ListFragment;
import com.fou.passport.ui.settingfragment.SettingsFragment;

/**
 * 主界面三页：密码列表、分类、设置，支持左右滑动切换。
 */
public class MainPagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_LIST = 0;
    public static final int PAGE_CATEGORY = 1;
    public static final int PAGE_SETTINGS = 2;

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_CATEGORY:
                return new CategoryFragment();
            case PAGE_SETTINGS:
                return new SettingsFragment();
            default:
                return ListFragment.newInstance("", "");
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
