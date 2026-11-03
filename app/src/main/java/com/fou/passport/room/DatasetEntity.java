package com.fou.passport.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "dataset_entity", foreignKeys = @ForeignKey(parentColumns = "id",
        childColumns = "package_id", entity = PackageEntity.class, onDelete = ForeignKey.CASCADE),indices = {@Index("package_id")})

public class DatasetEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "package_id")
    public int package_id;

    public DatasetEntity() {
    }

    @Ignore
    public DatasetEntity(@NonNull int package_id) {
        this.package_id = package_id;
    }
}
