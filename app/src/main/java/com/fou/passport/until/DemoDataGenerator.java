package com.fou.passport.until;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceManager;

import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 调试用演示数据：一键生成用于应用商店截图的示例账户。
 * 只在调试包里通过设置页入口调用，正式包不显示入口。
 */
public final class DemoDataGenerator {

    private static final String KEY_DEMO_PACKAGE_IDS = "demo_package_ids";

    private DemoDataGenerator() {
    }

    private static final class Account {
        final String appId;
        final String appName;
        final boolean favorite;
        final boolean deleted;
        final String[] keyValues;

        Account(String appId, String appName, boolean favorite, boolean deleted, String... keyValues) {
            this.appId = appId;
            this.appName = appName;
            this.favorite = favorite;
            this.deleted = deleted;
            this.keyValues = keyValues;
        }
    }

    /**
     * 生成演示账户，重复调用会先清除旧的演示数据。
     *
     * @return 生成的账户数量
     */
    public static int generate(Context context) {
        Context appContext = context.getApplicationContext();
        clear(appContext);
        PackageManager packageManager = appContext.getPackageManager();
        MyDB db = MyDB.getDb();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        Set<String> demoPackageIds = new HashSet<>();
        long now = System.currentTimeMillis();
        int count = 0;
        int index = 0;
        for (Account account : demoAccounts()) {
            String certificatesHash = SecurityHelper.getCertificatesHash(account.appId);
            int packageId;
            PackageEntity packageEntity = db.getPackageDao().getPackageEntity(account.appId, certificatesHash);
            if (packageEntity == null) {
                String appName = account.appName;
                try {
                    appName = packageManager.getApplicationInfo(account.appId, 0)
                            .loadLabel(packageManager).toString();
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                PackageEntity newPackage = new PackageEntity(account.appId, certificatesHash, appName);
                long[] packageIds = db.getPackageDao().insert(newPackage);
                packageId = (int) packageIds[0];
                demoPackageIds.add(String.valueOf(packageId));
            } else {
                packageId = packageEntity.id;
            }

            DatasetEntity datasetEntity = new DatasetEntity(packageId);
            datasetEntity.favorite = account.favorite;
            long[] datasetIds = db.getDatasetDao().insert(datasetEntity);
            int datasetId = (int) datasetIds[0];

            List<MetadataEntity> metadataEntities = new ArrayList<>();
            for (int i = 0; i + 1 < account.keyValues.length; i += 2) {
                metadataEntities.add(new MetadataEntity(account.keyValues[i], account.keyValues[i + 1], datasetId, packageId));
            }
            //创建时间：依次往前推，截图里时间不会完全一样
            metadataEntities.add(new MetadataEntity(Type.OTHER_CREATE_TIME,
                    String.valueOf(now - index * 86400000L), datasetId, packageId));
            db.getMetadataDao().insert(metadataEntities.toArray(new MetadataEntity[0]));

            if (account.deleted) {
                db.getDatasetDao().softDelete(datasetId, now - 2 * 86400000L);
            }
            count++;
            index++;
        }
        preferences.edit().putStringSet(KEY_DEMO_PACKAGE_IDS, demoPackageIds).apply();
        return count;
    }

    /**
     * 删除演示生成器创建的账户（按应用包级联删除账户和字段）。
     */
    public static void clear(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        Set<String> demoPackageIds = preferences.getStringSet(KEY_DEMO_PACKAGE_IDS, new HashSet<>());
        if (demoPackageIds == null || demoPackageIds.isEmpty()) {
            return;
        }
        for (String packageId : demoPackageIds) {
            try {
                MyDB.getDb().getPackageDao().deletePackageEntityById(Integer.parseInt(packageId));
            } catch (NumberFormatException ignored) {
            }
        }
        preferences.edit().remove(KEY_DEMO_PACKAGE_IDS).apply();
    }

    private static List<Account> demoAccounts() {
        List<Account> accounts = new ArrayList<>();
        //密码类
        accounts.add(new Account("com.android.chrome", "Chrome", true, false,
                Type.USERNAME, "alex.chen@gmail.com",
                Type.EMAIL, "alex.chen@gmail.com",
                Type.PASSWORD, "Chrome@2024",
                Type.OTHER_WEBSITE, "accounts.google.com"));
        accounts.add(new Account("com.tencent.mm", "微信", true, false,
                Type.USERNAME, "wx_alexchen",
                Type.PHONE, "+86 138 0013 8000",
                Type.PASSWORD, "WeChat@2024"));
        accounts.add(new Account("com.eg.android.AlipayGphone", "支付宝", false, false,
                Type.USERNAME, "138 0013 8000",
                Type.PASSWORD, "Wallet@2024"));
        accounts.add(new Account("com.taobao.taobao", "淘宝", false, false,
                Type.USERNAME, "alex_shop",
                Type.PASSWORD, "Wallet@2024"));
        accounts.add(new Account("com.zhihu.android", "知乎", false, false,
                Type.USERNAME, "alexchen",
                Type.EMAIL, "alex.chen@outlook.com",
                Type.PASSWORD, "12345678"));
        accounts.add(new Account("org.telegram.messenger", "Telegram", false, false,
                Type.PHONE, "+86 138 0013 8000",
                Type.PASSWORD, "Telegram@2024"));
        accounts.add(new Account("cn.wps.moffice_eng", "WPS Office", false, false,
                Type.USERNAME, "659389824",
                Type.PASSWORD, "Wps@2024"));
        //银行卡（带卡号、开户行、CVV，模板页会归到银行卡分区）
        accounts.add(new Account("com.eg.android.AlipayGphone", "支付宝", false, false,
                Type.CARD, "6222 0210 0123 4567",
                Type.OTHER_ISSUING_BANK, "招商银行",
                Type.CVV, "328"));
        //其他：只有账号和邮箱，没有密码
        accounts.add(new Account("com.hihonor.photos", "相册", false, false,
                Type.USERNAME, "alexchen",
                Type.EMAIL, "alex.chen@outlook.com"));
        //回收站
        accounts.add(new Account("com.taobao.taobao", "淘宝", false, true,
                Type.USERNAME, "old_taobao",
                Type.PASSWORD, "taobao123"));
        return accounts;
    }
}
