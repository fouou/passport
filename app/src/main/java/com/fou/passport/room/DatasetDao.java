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

    @Query("select * from dataset_entity")
    List<DatasetEntity> getDatasetEntityList();

    @Query("select * from dataset_entity where id= :id")
    DatasetEntity getDatasetEntityById(int id);

    @Query("delete from dataset_entity")
    void deleteAll();

    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE id = :id")
    DatasetAndMetadata getDatasetAndMetadataInstance(int id);

    @Transaction
    @Query("SELECT * FROM meta_data_entity WHERE dataset_id = :dataset_id")
    List<MetadataEntity> getMetadataEntityListByDatasetId(int dataset_id);

    @Transaction
    @Query("SELECT * FROM dataset_entity")
    List<DatasetAndMetadata> getDatasetAndMetadataList();

    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE package_id = :package_id")
    List<DatasetAndMetadata> getDatasetAndMetadataListById(int package_id);

}
