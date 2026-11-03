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

import static android.app.assist.AssistStructure.ViewNode;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import com.fou.passport.until.Log;
import com.fou.passport.until.Type;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientParser {
    private final List<AssistStructure> mStructures;
    private AssistStructure lastAssistStructure;

    public ClientParser(@NonNull List<AssistStructure> structures
            , AssistStructure lastAssistStructure) {
        mStructures = structures;
        this.lastAssistStructure = lastAssistStructure;
    }

    public ClientParser(@NonNull AssistStructure structure) {
        this(ImmutableList.of(structure) ,null);
    }

    /**
     * onFillRequest专用node解析器 只填充当前页面，
     * 防止由于登录注册页面切换导致几个structure出现在一起，从而使得填充了登录表单没填充注册表单导致无法触发保存
     *
     * @param processor
     */
    public void parseLastStructure(NodeProcessor processor) {
        int nodes = lastAssistStructure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode viewNode = lastAssistStructure.getWindowNodeAt(i).getRootViewNode();
            traverseRoot(viewNode, processor);
        }
    }

    /**
     * 内部方法
     *
     * @param processor
     */
    public void parse(NodeProcessor processor) {
        for (AssistStructure structure : mStructures) {
            int nodes = structure.getWindowNodeCount();
            for (int i = 0; i < nodes; i++) {
                ViewNode viewNode = structure.getWindowNodeAt(i).getRootViewNode();
                traverseRoot(viewNode, processor);
            }
        }
    }

    /**
     * 保存node专用的解析器，去除 因为重复提交表单而使得structure中存在的大量相同表单中的node
     *
     * @param processor
     * @return
     */
    public List<ViewNode> noRepeatParse() {
        Set<String> editTextNode = new HashSet<>();
        Set<Integer> otherNode = new HashSet<>();
        List<ViewNode> viewNodes = new ArrayList<>();
        parse(node -> {
            if (node.getAutofillType() == View.AUTOFILL_TYPE_TEXT) {
                String key_word = Type.getKey(node);
                if (key_word != null
                        && !node.getAutofillValue().getTextValue().toString().trim().equals("")) {
                    if (editTextNode.add(key_word)) {
                        viewNodes.add(node);
                    }
                }
            } else {
                if (node.getId() != -1 && otherNode.add(node.getId())) {
                    viewNodes.add(node);
                }
            }
        });
        return viewNodes;
    }

    /**
     * 只收集有自动填充功能的
     *
     * @param viewNode
     * @param processor
     */
    private void traverseRoot(ViewNode viewNode, NodeProcessor processor) {
        //To filter out the views that have the capability of automatic filling
        if (viewNode.getAutofillType() != View.AUTOFILL_TYPE_NONE) {
            processor.processNode(viewNode);
        }

        int childrenSize = viewNode.getChildCount();
        if (childrenSize > 0) {
            for (int i = 0; i < childrenSize; i++) {
                traverseRoot(viewNode.getChildAt(i), processor);
            }
        }
    }

    public interface NodeProcessor {
        void processNode(ViewNode node);
    }
}
