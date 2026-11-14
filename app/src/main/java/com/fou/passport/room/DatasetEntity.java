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

    /**
     * 软删除标记：为 1 时进入回收站，不参与列表展示、自动填充与备份。
     */
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    public boolean is_deleted;

    /**
     * 进入回收站的时间戳。
     */
    @ColumnInfo(name = "deleted_at", defaultValue = "0")
    public long deleted_at;

    /**
     * 是否收藏（设置页的快速访问列表）。
     */
    @ColumnInfo(name = "favorite", defaultValue = "0")
    public boolean favorite;


    public DatasetEntity() {
    }

    @Ignore
    public DatasetEntity(@NonNull int package_id) {
        this.package_id = package_id;
    }
}
