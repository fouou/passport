package com.fou.passport.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DatasetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(DatasetEntity... datasetEntities);

    @Delete
    void delete(DatasetEntity... datasetEntities);

    @Update
    void update(DatasetEntity... datasetEntities);

    @Query("select * from dataset_entity where is_deleted = 0")
    List<DatasetEntity> getDatasetEntityList();

    @Query("select * from dataset_entity where id= :id")
    DatasetEntity getDatasetEntityById(int id);

    /**
     * 软删除：账户进入回收站，可恢复。
     */
    @Query("update dataset_entity set is_deleted = 1, deleted_at = :timestamp where id = :id")
    void softDelete(int id, long timestamp);

    /**
     * 从回收站恢复账户。
     */
    @Query("update dataset_entity set is_deleted = 0, deleted_at = 0 where id = :id")
    void restore(int id);

    /**
     * 收藏/取消收藏，收藏的账户会出现在设置页的快速访问列表。
     */
    @Query("update dataset_entity set favorite = :favorite where id = :id")
    void setFavorite(int id, boolean favorite);

    @Query("delete from dataset_entity")
    void deleteAll();

    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE id = :id")
    DatasetAndMetadata getDatasetAndMetadataInstance(int id);

    @Transaction
    @Query("SELECT * FROM meta_data_entity WHERE dataset_id = :dataset_id")
    List<MetadataEntity> getMetadataEntityListByDatasetId(int dataset_id);

    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE is_deleted = 0 ORDER BY id DESC")
    List<DatasetAndMetadata> getDatasetAndMetadataList();

    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE package_id = :package_id AND is_deleted = 0")
    List<DatasetAndMetadata> getDatasetAndMetadataListById(int package_id);

    /**
     * 回收站列表，按删除时间倒序。
     */
    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE is_deleted = 1 ORDER BY deleted_at DESC")
    List<DatasetAndMetadata> getDeletedDatasetAndMetadataList();

    /**
     * 快速访问：已收藏的账户。
     */
    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE is_deleted = 0 AND favorite = 1 ORDER BY id DESC")
    List<DatasetAndMetadata> getFavoriteDatasetAndMetadataList();

}
