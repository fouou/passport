package com.fou.passport.ui.listfragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.fou.passport.ui.newitem.NewItem;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private List<PackageEntity> packageEntityList_all;
    private SearchView searchView;
    private ListAdapter listAdapter;

    public ListFragment() {
        // Required empty public constructor
    }

    public static ListFragment newInstance(String param1, String param2) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        //按两次返回
        doubleClickBack();
    }

    /**
     * 再按一次退出
     */
    private long millis;

    private void doubleClickBack() {
        // 创建一个OnBackPressedCallback对象
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (searchView != null && searchView.isShowing()) {//收起searchview
                    searchView.hide();
                } else if (millis > System.currentTimeMillis()) {
                    getActivity().finish();
                } else {
                    millis = System.currentTimeMillis() + 2000; // 2秒后重置标志位
                    Toast.makeText(MyApplication.applicationContext, R.string.double_click_back, Toast.LENGTH_SHORT).show();
                }
            }
        };
        // 将OnBackPressedCallback对象添加到Fragment的OnBackPressedDispatcher中
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        searchView = view.findViewById(R.id.search_view);//返回收缩
        SearchBar searchBar = view.findViewById(R.id.search_bar);
        searchBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_listFragment_add:
                    getActivity().startActivity(new Intent(getActivity(), NewItem.class));
                    return true;
            }
            return false;
        });
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyApplication.appExecutors.diskIO().execute(() -> {
            packageEntityList_all = MyDB.getDb().getPackageDao().getPackageEntityList();
            MyApplication.appExecutors.mainThread().execute(() -> {
                if (packageEntityList_all.isEmpty()) {
                    //TODO Empty view
                }else {
                    //item_list
                    setItemList(view);
                    //searchView
                    setSearchView(view);
                }
            });
        });
    }

    private void setSearchView(@NonNull View view) {
        RecyclerView recyclerView_search_view = view.findViewById(R.id.recyclerView_search);
        LinearLayoutManager linearLayoutManager_search_view = new LinearLayoutManager(getActivity());
        //searchview中recyclerview的adapter的list
        List<PackageEntity> packageEntityList_adapter = new ArrayList<>(packageEntityList_all);
        ListAdapter_searchview adapter_search_view = new ListAdapter_searchview(getActivity(), packageEntityList_adapter);
        recyclerView_search_view.setLayoutManager(linearLayoutManager_search_view);
        recyclerView_search_view.setAdapter(adapter_search_view);
        Log.d("HAH",String.valueOf(searchView!=null));
        //搜索
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<PackageEntity> list = packageEntityList_all.stream().filter(packageEntity -> {
                    if (packageEntity.app_name != null) {
                        return packageEntity.app_name.toLowerCase().contains(s.toString().trim().toLowerCase());
                    }
                    return false;
                }).collect(Collectors.toList());
                if (!list.isEmpty()) {
                    packageEntityList_adapter.clear();
                    packageEntityList_adapter.addAll(list);
                    adapter_search_view.notifyDataSetChanged();
                } else {
                    //TODO 展示空列表view
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setItemList(@NonNull View view) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        listAdapter = new ListAdapter(getActivity(), packageEntityList_all);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(listAdapter);
    }

    /**
     * 刷新
     */
    @Override
    public void onResume() {
        super.onResume();
        if (packageEntityList_all==null || listAdapter==null) {
            return;
        }
        MyApplication.appExecutors.diskIO().execute(() -> {
            List<PackageEntity> packageEntityList_all_resume = MyDB.getDb().getPackageDao().getPackageEntityList();
            MyApplication.appExecutors.mainThread().execute(() -> {
                if (packageEntityList_all_resume.isEmpty()) {
                    //TODO empty view
                    return;
                } else {
                    packageEntityList_all.clear();
                    packageEntityList_all.addAll(packageEntityList_all_resume);
                    listAdapter.notifyDataSetChanged();
                }
            });
        });
    }
}

