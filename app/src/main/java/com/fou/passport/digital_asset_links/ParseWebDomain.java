package com.fou.passport.digital_asset_links;

import android.content.Context;

import com.fou.passport.service.ClientParser;
import com.fou.passport.until.Log;
import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ParseWebDomain {
    public String webDomain;
    private ClientParser parser;

    public ParseWebDomain(ClientParser parser) {
        this.parser = parser;
    }

    /**
     * getBeanDigitalAssetLinksList
     * @return
     */
    public List<BeanDigitalAssetLinks> getBeanDigitalAssetLinksList() {
        try {
            URL assetLinksUrl = new URL("https://" + getCanonicalDomain(webDomain) + "/.well-known/assetlinks.json");
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(assetLinksUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<BeanDigitalAssetLinks>>() {
                }.getType();
                List<BeanDigitalAssetLinks> list = gson.fromJson(Objects.requireNonNull(response.body()).string(), listType);
                if (!list.isEmpty()) {
                    return list;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get the canonical domain using Guava
     *
     * @param domain
     * @return
     */
    public static String getCanonicalDomain(String domain) {
        InternetDomainName idn = InternetDomainName.from(domain);
        while (idn != null && !idn.isTopPrivateDomain()) {
            idn = idn.parent();
        }
        return idn == null ? "" : idn.toString();
    }

    /**
     * isWebView?
     * @param
     * @return
     */
    public boolean isWeb() {
        parser.parseLastStructure(node -> {
            if (node.getWebDomain()!=null) {
                webDomain=node.getWebDomain();
            }
        });
        if (webDomain!=null) {
            return true;
        }
        return false;
    }

}
