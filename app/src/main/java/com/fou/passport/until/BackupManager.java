package com.fou.passport.until;

import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.room.MyDB;
import com.fou.passport.room.PackageEntity;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 备份数据模型与数据库读写：导出时把整库拍平成 JSON，导入时重建包/账户/字段。
 */
public final class BackupManager {
    private static final Gson GSON = new Gson();

    private BackupManager() {
    }

    public static class BackupData {
        public int version = 1;
        public List<BackupPackage> packages = new ArrayList<>();
    }

    public static class BackupPackage {
        public String application_id;
        public String certificates_hash;
        public String app_name;
        public List<BackupDataset> datasets = new ArrayList<>();
    }

    public static class BackupDataset {
        public List<BackupMetadata> metadata = new ArrayList<>();
    }

    public static class BackupMetadata {
        public int xml_id = -1;
        public int auto_fill_type;
        public String auto_fill_value_text;
        public boolean auto_fill_value_toggle;
        public String auto_fill_value_list;
        public long auto_fill_value_date;
        public String key_word;
    }

    /** 备份中没有可导出的账户时抛出，便于界面给出明确提示。 */
    public static class EmptyBackupException extends Exception {
    }

    /**
     * 导出：从数据库读取全部数据，加密后写入输出流。
     */
    public static void export(OutputStream out, String password) throws Exception {
        BackupData data = collect();
        if (data.packages.isEmpty()) {
            throw new EmptyBackupException();
        }
        String encrypted = BackupCrypto.encrypt(GSON.toJson(data), password);
        out.write(encrypted.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * 导入：读入文件并解密解析。
     */
    public static BackupData read(InputStream in, String password) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        String decrypted = BackupCrypto.decrypt(buffer.toString("UTF-8"), password);
        BackupData data = GSON.fromJson(decrypted, BackupData.class);
        if (data == null) {
            throw new IOException("invalid backup content");
        }
        return data;
    }

    /**
     * 读取整库数据（需在 IO 线程调用）。
     */
    public static BackupData collect() {
        BackupData data = new BackupData();
        for (PackageEntity packageEntity : MyDB.getDb().getPackageDao().getPackageEntityList()) {
            BackupPackage backupPackage = new BackupPackage();
            backupPackage.application_id = packageEntity.application_id;
            backupPackage.certificates_hash = packageEntity.certificates_hash;
            backupPackage.app_name = packageEntity.app_name;

            List<DatasetAndMetadata> datasetAndMetadataList = MyDB.getDb()
                    .getDatasetDao().getDatasetAndMetadataListById(packageEntity.id);
            for (DatasetAndMetadata datasetAndMetadata : datasetAndMetadataList) {
                BackupDataset backupDataset = new BackupDataset();
                for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                    BackupMetadata backupMetadata = new BackupMetadata();
                    backupMetadata.xml_id = metadataEntity.xml_id;
                    backupMetadata.auto_fill_type = metadataEntity.auto_fill_type;
                    backupMetadata.auto_fill_value_text = metadataEntity.auto_fill_value_text;
                    backupMetadata.auto_fill_value_toggle = metadataEntity.auto_fill_value_toggle;
                    backupMetadata.auto_fill_value_list = metadataEntity.auto_fill_value_list;
                    backupMetadata.auto_fill_value_date = metadataEntity.auto_fill_value_date;
                    backupMetadata.key_word = metadataEntity.key_word;
                    backupDataset.metadata.add(backupMetadata);
                }
                backupPackage.datasets.add(backupDataset);
            }
            data.packages.add(backupPackage);
        }
        return data;
    }

    /**
     * 写回数据库（需在 IO 线程调用）。
     *
     * @param overwrite true 先清空本机数据再导入；false 追加合并到同包名的应用
     */
    public static void restore(BackupData data, boolean overwrite) {
        MyDB.getDb().runInTransaction(() -> {
            if (overwrite) {
                MyDB.getDb().getMetadataDao().deleteAll();
                MyDB.getDb().getDatasetDao().deleteAll();
                MyDB.getDb().getPackageDao().deleteAll();
            }
            for (BackupPackage backupPackage : data.packages) {
                if (backupPackage == null) {
                    continue;
                }
                int packageId = resolvePackageId(backupPackage);
                if (backupPackage.datasets == null) {
                    continue;
                }
                for (BackupDataset backupDataset : backupPackage.datasets) {
                    if (backupDataset == null) {
                        continue;
                    }
                    int datasetId = (int) MyDB.getDb().getDatasetDao()
                            .insert(new DatasetEntity(packageId))[0];
                    if (backupDataset.metadata == null) {
                        continue;
                    }
                    List<MetadataEntity> metadataEntityList = new ArrayList<>();
                    for (BackupMetadata backupMetadata : backupDataset.metadata) {
                        if (backupMetadata == null) {
                            continue;
                        }
                        MetadataEntity metadataEntity = new MetadataEntity();
                        metadataEntity.xml_id = backupMetadata.xml_id;
                        metadataEntity.auto_fill_type = backupMetadata.auto_fill_type;
                        metadataEntity.auto_fill_value_text = backupMetadata.auto_fill_value_text;
                        metadataEntity.auto_fill_value_toggle = backupMetadata.auto_fill_value_toggle;
                        metadataEntity.auto_fill_value_list = backupMetadata.auto_fill_value_list;
                        metadataEntity.auto_fill_value_date = backupMetadata.auto_fill_value_date;
                        metadataEntity.key_word = backupMetadata.key_word;
                        metadataEntity.package_id = packageId;
                        metadataEntity.dataset_id = datasetId;
                        metadataEntityList.add(metadataEntity);
                    }
                    if (!metadataEntityList.isEmpty()) {
                        MyDB.getDb().getMetadataDao()
                                .insert(metadataEntityList.toArray(new MetadataEntity[0]));
                    }
                }
            }
        });
    }

    /**
     * 追加导入时复用已有应用，避免同一应用出现多份。
     */
    private static int resolvePackageId(BackupPackage backupPackage) {
        if (backupPackage.application_id != null) {
            PackageEntity existing = MyDB.getDb().getPackageDao().getPackageEntity(
                    backupPackage.application_id, backupPackage.certificates_hash);
            if (existing != null) {
                return existing.id;
            }
        }
        PackageEntity packageEntity = new PackageEntity(backupPackage.application_id,
                backupPackage.certificates_hash, backupPackage.app_name);
        return (int) MyDB.getDb().getPackageDao().insert(packageEntity)[0];
    }
}
