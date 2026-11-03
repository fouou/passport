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
package com.fou.passport.until;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.fou.passport.MyApplication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Helper class for security checks.
 */
public final class SecurityHelper {

    /**
     * 通过包名获取该包的签名文件
     *
     * @param packageName
     * @return
     * @throws Exception
     */
    public static String getCertificatesHash(String packageName) {
        if (packageName==null || packageName.trim().equals("")) {
            return null;
        }
        try {
            PackageManager pm = MyApplication.applicationContext.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            ArrayList hashes = new ArrayList<>(info.signatures.length);
            for (Signature sig : info.signatures) {
                byte[] cert = sig.toByteArray();
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(cert);
                hashes.add(toHexString(md.digest()));
            }
            Collections.sort(hashes);
            StringBuilder hash = new StringBuilder();
            for (int i = 0; i < hashes.size(); i++) {
                hash.append(hashes.get(i));
            }
            return hash.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将SHA-256 哈希值转为十六进制字符串
     *
     * @param bytes
     * @return
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
