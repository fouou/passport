/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fou.passport.service;
import android.view.View;
import android.widget.RemoteViews;

import com.fou.passport.R;

/**
 * This is a class containing helper methods for building Autofill Datasets and Responses.
 */
public final class RemoteViewsHelper {
    private RemoteViewsHelper() {
    }

    public static RemoteViews viewsWithAuth(String packageName, String text) {
        return remoteViews(packageName, text, true);
    }

    public static RemoteViews viewsWithNoAuth(String packageName, String text) {
        return remoteViews(packageName, text, false);
    }

    private static RemoteViews remoteViews(String packageName, String remoteViewsText, boolean authRequired) {
        RemoteViews presentation = new RemoteViews(packageName,
                R.layout.multidataset_service_list_item);
        presentation.setTextViewText(R.id.text, remoteViewsText);
        //需要验证身份才能取用的数据集显示副标题，普通数据集不显示
        presentation.setViewVisibility(R.id.subtitle, authRequired ? View.VISIBLE : View.GONE);
        return presentation;
    }
}
