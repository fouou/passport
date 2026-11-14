package com.fou.passport.service;

import android.content.res.XmlResourceParser;

import com.fou.passport.MyApplication;
import com.fou.passport.R;
import com.fou.passport.until.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class BrowserHelper {
    private static final String[] BROWSERS = {
            "alook.browser",
            "com.amazon.cloud9",
            "com.android.browser",
            "com.android.chrome",
            "com.android.htmlviewer",
            "com.avast.android.secure.browser",
            "com.avg.android.secure.browser",
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.brave.browser_default",
            "com.brave.browser_dev",
            "com.brave.browser_nightly",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "com.cookiegames.smartcookie",
            "com.cookiejarapps.android.smartcookieweb",
            "com.ecosia.android",
            "com.google.android.apps.chrome",
            "com.google.android.apps.chrome_dev",
            "com.jamal2367.styx",
            "com.kiwibrowser.browser",
            "com.microsoft.emmx",
            "com.microsoft.emmx.beta",
            "com.microsoft.emmx.canary",
            "com.microsoft.emmx.dev",
            "com.mmbox.browser",
            "com.mmbox.xbrowser",
            "com.mycompany.app.soulbrowser",
            "com.naver.whale",
            "com.opera.browser",
            "com.opera.browser.beta",
            "com.opera.mini.native",
            "com.opera.mini.native.beta",
            "com.opera.touch",
            "com.qwant.liberty",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta",
            "com.stoutner.privacybrowser.free",
            "com.stoutner.privacybrowser.standard",
            "com.vivaldi.browser",
            "com.vivaldi.browser.snapshot",
            "com.vivaldi.browser.sopranos",
            "com.yandex.browser",
            "com.z28j.feel",
            "idm.internet.download.manager",
            "idm.internet.download.manager.adm.lite",
            "idm.internet.download.manager.plus",
            "io.github.forkmaintainers.iceraven",
            "mark.via",
            "mark.via.gp",
            "net.slions.fulguris.full.download",
            "net.slions.fulguris.full.download.debug",
            "net.slions.fulguris.full.playstore",
            "net.slions.fulguris.full.playstore.debug",
            "org.adblockplus.browser",
            "org.adblockplus.browser.beta",
            "org.bromite.bromite",
            "org.bromite.chromium",
            "org.chromium.chrome",
            "org.codeaurora.swe.browser",
            "org.gnu.icecat",
            "org.mozilla.fenix",
            "org.mozilla.fenix.nightly",
            "org.mozilla.fennec_aurora",
            "org.mozilla.fennec_fdroid",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.reference.browser",
            "org.mozilla.rocket",
            "org.torproject.torbrowser",
            "org.torproject.torbrowser_alpha",
            "org.ungoogled.chromium.extensions.stable",
            "org.ungoogled.chromium.stable",
            "us.spotco.fennec_dos",
            "com.huawei.browser"};

    /**
     * 是否为需要兼容模式处理的浏览器。
     */
    public static boolean isBrowser(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String browser : BROWSERS) {
            if (browser.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static void delete(String package_name){
        try {
            // 读取 XML 文件
            InputStream inputStream = MyApplication.applicationContext.getResources().openRawResource(R.xml.service_configuration);
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputStream);
            // 找到需要删除的元素
            NodeList elements = doc.getElementsByTagName("compatibility-package");

            for (int i = 0; i < elements.getLength(); i++) {
                Node element = elements.item(i);
                NamedNodeMap attributes = element.getAttributes();
                Node nameAttr = attributes.getNamedItem("android:name");
                Log.Special_1(nameAttr.getNodeValue());
                if (nameAttr != null && nameAttr.getNodeValue().equals(package_name)) {
                    // 删除该元素
                    element.getParentNode().removeChild(element);
                    break; // 找到需要删除的元素后就退出循环
                }
            }
            // 保存修改后的 XML 文件
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(MyApplication.applicationContext.getResources().openRawResourceFd(R.xml.service_configuration).createOutputStream());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
