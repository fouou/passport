package com.fou.passport.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "package_entity")
public class PackageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo
    public String application_id;

    @ColumnInfo
    public String certificates_hash;

    @ColumnInfo
    public String app_name;

    public PackageEntity(String application_id, String certificates_hash ,String app_name) {
        this.application_id = application_id;
        this.certificates_hash = certificates_hash;
        this.app_name=app_name;
    }
}