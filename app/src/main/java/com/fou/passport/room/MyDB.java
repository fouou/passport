package com.fou.passport.room;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.fou.passport.MyApplication;
import com.fou.passport.until.DbKeyStore;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

@Database(entities = {PackageEntity.class, DatasetEntity.class, MetadataEntity.class},
        version = 5, exportSchema = true)
public abstract class MyDB extends RoomDatabase {
    public abstract DatasetDao getDatasetDao();

    public abstract PackageDao getPackageDao();

    public abstract MetadataDao getMetadataDao();

    public static volatile MyDB myDB;

    /**
     * v4 -> v5：为账户增加软删除、删除时间与收藏标记，保证回收站与快速访问可用且不丢失旧数据。
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE dataset_entity ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE dataset_entity ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE dataset_entity ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static synchronized MyDB getDb() {
        if (myDB == null) {
            SupportOpenHelperFactory factory = new SupportOpenHelperFactory(DbKeyStore.getPassphrase(MyApplication.applicationContext));
            myDB = Room.databaseBuilder(MyApplication.applicationContext,
                            MyDB.class, "myDatabase")
                    .addMigrations(MIGRATION_4_5)
                    //版本升级时结构变更不再直接崩溃；数据可回退重建
                    .fallbackToDestructiveMigration()
                    .openHelperFactory(factory)
                    .build();
        }
        return myDB;
    }

    public static synchronized void destroyUserAndBookDb() {
        if (myDB != null) {
            myDB.close();
            myDB = null;
        }
    }
}
