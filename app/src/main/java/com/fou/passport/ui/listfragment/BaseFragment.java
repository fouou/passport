package com.fou.passport.ui.listfragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public abstract class BaseFragment extends Fragment {

    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(setLayout(), container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onBinding(view);
        onBegin(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (view!=null) {
            onBegin(view);
        }
    }

    public abstract int setLayout();
    public abstract int onBinding(View view);
    public abstract int onBegin(View view);

}
