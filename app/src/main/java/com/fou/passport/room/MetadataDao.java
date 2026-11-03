package com.fou.passport.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(MetadataEntity... metadataEntities);

    @Delete
    void delete(MetadataEntity... metadataEntities);

    @Update
    void update(MetadataEntity... metadataEntities);

    @Query("select * from meta_data_entity")
    List<MetadataEntity> getMetadataEntityList();

    @Query("delete from meta_data_entity")
    void deleteAll();

    @Query("delete from meta_data_entity WHERE dataset_id = :dataset_id")
    void deleteByDatasetId(int dataset_id);

    @Query("select * from meta_data_entity WHERE dataset_id = :dataset_id")
    List<MetadataEntity> getMetadataEntityListByDatasetId(int dataset_id);

    @Query("select * from meta_data_entity WHERE package_id = :package_id")
    List<MetadataEntity> getMetadataEntityListByPackageId(int package_id);

    //Return null when no PackageEntity be queried
    @Query("SELECT * FROM meta_data_entity " +
            "WHERE package_id = :package_id " +
            "AND key_word = :key_word " +
            "AND auto_fill_value_text = :text " +
            "LIMIT 1")
    MetadataEntity getMetadataEntityByText(int package_id,String key_word ,String text);
}
