package com.fou.passport.room;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.fou.passport.MyApplication;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

@Database(entities = {PackageEntity.class, DatasetEntity.class,MetadataEntity.class}, version = 4, exportSchema = false)
public abstract class MyDB extends RoomDatabase {
    public abstract DatasetDao getDatasetDao();

    public abstract PackageDao getPackageDao();

    public abstract MetadataDao getMetadataDao();

    public static volatile MyDB myDB;

    final static byte[] passphrase = SQLiteDatabase.getBytes(("userEnteredPassphrase").toCharArray());
    final static SupportFactory factory = new SupportFactory(passphrase);


    public static synchronized MyDB getDb() {
        if (myDB == null) {
            myDB = Room.databaseBuilder(MyApplication.applicationContext,
                            MyDB.class, "myDatabase")
                    //TODO Thread operate and database migrations
                    // .allowMainThreadQueries()
                    //.addMigrations(MIGRATION_1_2) // 添加迁移路径
                    //.fallbackToDestructiveMigration() // 允许破坏性迁移
                    .openHelperFactory(factory)
                    .build();
        }
        return myDB;
    }

    public static void destroyUserAndBookDb() {
        myDB = null;
    }


}
