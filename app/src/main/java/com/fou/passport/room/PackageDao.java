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
public interface PackageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(PackageEntity... PackageEntities);

    @Delete
    void delete(PackageEntity... PackageEntities);

    @Update
    void update(PackageEntity... PackageEntities);
//Return null when no PackageEntity be queried
    @Query("select * from package_entity WHERE application_id= :application_id AND certificates_hash= :certificates_hash LIMIT 1")
    PackageEntity getPackageEntity(String application_id,String certificates_hash);
//Return a empty list(List<PackageEntity>.isEmpty==true) when no PackageEntity be queried
    //只返回还有可用账户的应用，账户全部进回收站的应用不再显示
    @Query("select * from package_entity where id in (select distinct package_id from dataset_entity where is_deleted = 0)")
    List<PackageEntity> getPackageEntityList();

    //回收站里账户可能全部已删除，需要不受过滤影响的全量应用列表
    @Query("select * from package_entity")
    List<PackageEntity> getAllPackageEntityList();

    @Query("delete from Package_entity")
    void deleteAll();

    @Query("SELECT * from Package_entity WHERE id= :id")
    PackageEntity getPackageEntityById(int id);


    @Query("delete from Package_entity WHERE id= :id")
    void deletePackageEntityById(int id);

    //模糊匹配
    @Query("SELECT * from Package_entity WHERE app_name LIKE :app_name")
    List<PackageEntity> getPackageEntityByApp_name(String app_name);

    @Transaction
    @Query("SELECT * FROM package_entity")
    List<PackageAndDataset> getPackageAndDatasetList();

    @Transaction
    @Query("SELECT * FROM dataset_entity WHERE package_id= :package_id AND is_deleted = 0")
    List<DatasetEntity> getDatasetEntityList(int package_id);


}
