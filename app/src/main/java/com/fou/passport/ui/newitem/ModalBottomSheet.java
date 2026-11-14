package com.fou.passport.ui.newitem;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModalBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = "ModalBottomSheet";
    private List<ApplicationInfo> applicationInfoList_all;
    private RecyclerView recyclerView;
    private TextInputEditText textInputEditText;
    private LinearProgressIndicator linearProgressIndicator;
    private AdapterBottomSheet adapter;
    private List<ApplicationInfo> applicationInfoList_adapter;
    private OnBottomSheetDoneButtonClickListener onBottomSheetDoneButtonClickListener;
    private AdapterBottomSheet.OnBottomSheetItemClickListener onBottomSheetItemClickListener;
    private ApplicationInfoListIsAssignedValueListener applicationInfoListIsAssignedValueListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_newitem, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) {
            return;
        }
        bindViews(view);
        setBottomSheetView();
    }

    private void setBottomSheetView() {
        // 获取所有安装的应用程序的信息,耗时，异步
        //    TODO 可能需要权限，或者无法获得应用程序列表,API30（android11）以上
        MyApplication.appExecutors.diskIO().execute(() -> {
            applicationInfoList_all = MyApplication.applicationContext.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES);
            if (applicationInfoList_all == null || applicationInfoList_all.isEmpty()) {
                return;
            }
            //向接口传递参数
            applicationInfoListIsAssignedValueListener.fetchApplicationInfoList(applicationInfoList_all);
            applicationInfoList_adapter = new ArrayList<>(applicationInfoList_all);
            // 将应用程序列表排序
            Collections.sort(applicationInfoList_adapter, new ApplicationInfo.DisplayNameComparator(MyApplication.applicationContext.getPackageManager()));
            // 显示应用程序列表
            MyApplication.appExecutors.mainThread().execute(() -> {
                linearProgressIndicator.hide();//隐藏进度条
                setAdapter(applicationInfoList_adapter);//展示recyclerView列表
                //加载期间用户已经输入的内容，加载完成后立即生效
                Editable currentText = textInputEditText.getText();
                applyFilter(currentText == null ? "" : currentText.toString());
            });
        });
    }


    /**
     * 监听搜索框。提前绑定，避免列表加载完成前的输入被丢弃。
     */
    private void textChangeListener() {
        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s == null ? "" : s.toString());
            }
        });
    }

    /**
     * 按当前输入过滤应用列表；列表尚未加载完成时直接返回，加载完成后会再次应用当前输入。
     */
    private void applyFilter(String query) {
        if (applicationInfoList_all == null || applicationInfoList_all.isEmpty()
                || adapter == null || applicationInfoList_adapter == null) {
            return;
        }
        PackageManager packageManager = MyApplication.applicationContext.getPackageManager();
        String keyword = query.trim().toLowerCase();
        applicationInfoList_adapter.clear();//清空adapter
        for (ApplicationInfo applicationInfo : applicationInfoList_all) {
            if (applicationInfo != null) {//TODO 出现过空指针，所以加了这个
                String app_name = applicationInfo.loadLabel(packageManager).toString();
                if (app_name.trim().toLowerCase().contains(keyword)) {
                    applicationInfoList_adapter.add(applicationInfo);
                }
            }
        }
        //数字，字母顺序排序
        Collections.sort(applicationInfoList_adapter, new ApplicationInfo.DisplayNameComparator(packageManager));
        adapter.notifyDataSetChanged();
    }

    /**
     * 显示应用程序列表
     *
     * @param applicationInfoList
     */
    private void setAdapter(List<ApplicationInfo> applicationInfoList) {
        adapter = new AdapterBottomSheet(applicationInfoList);
        //bottomSheetItem点击回调，不在此处实现，传递给NewItem
        adapter.getPackageEntityAboutClickedItem(onBottomSheetItemClickListener);
        if (getContext()!=null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }

    }


    /**
     * @param view
     */
    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView_bottomSheet);
        textInputEditText = view.findViewById(R.id.textInputEditText);
        linearProgressIndicator = view.findViewById(R.id.linearProgressIndicator);
        MaterialButton button_close = view.findViewById(R.id.close);
        MaterialButton button_done = view.findViewById(R.id.done);
        //搜索框监听要在加载应用列表之前绑定，加载期间输入的名称不会被丢掉
        textChangeListener();
        button_close.setOnClickListener(v -> {
            dismiss();
        });

        //自定义输入（点bottomsheet的对号来的）
        button_done.setOnClickListener(v -> {
            String bottomSheet_textInputEditText = textInputEditText.getText().toString().trim();
            //啥也没输
            if (bottomSheet_textInputEditText.equals("")) {
                dismiss();
                return;
            }
            //输入了，让接口处理（newItem实现）
            onBottomSheetDoneButtonClickListener.onClick(bottomSheet_textInputEditText);
            dismiss();
        });
    }

    public void setApplicationInfoListIsAssignedValueListener(ApplicationInfoListIsAssignedValueListener applicationInfoListIsAssignedValueListener){
        this.applicationInfoListIsAssignedValueListener=applicationInfoListIsAssignedValueListener;
    }

    public void OnBottomSheetItemClickListener(AdapterBottomSheet.OnBottomSheetItemClickListener onBottomSheetItemClickListener) {
        this.onBottomSheetItemClickListener = onBottomSheetItemClickListener;
    }

    public void setDoneButtonClick(OnBottomSheetDoneButtonClickListener onBottomSheetDoneButtonClickListener) {
        this.onBottomSheetDoneButtonClickListener = onBottomSheetDoneButtonClickListener;
    }


    /**
     * 点击BottomSheetDoneButton的回调接口
     */
    interface OnBottomSheetDoneButtonClickListener {
        /**
         * @param bottom_sheet_edittext_text 点击DoneButton时输入框中的内容
         *                                   已经对其进行处理，不为 “”
         */
        void onClick(String bottom_sheet_edittext_text);
    }

    /**
     * applicationInfoList_all 被赋值的时候的回调
     */
    interface ApplicationInfoListIsAssignedValueListener{
        /**
         *
         * @param applicationInfoList 拿到applicationInfoList_all
         */
        void fetchApplicationInfoList(List<ApplicationInfo> applicationInfoList);
    }
}

