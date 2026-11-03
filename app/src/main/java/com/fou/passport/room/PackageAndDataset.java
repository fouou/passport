package com.fou.passport.room;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class PackageAndDataset {
    @Embedded
    public PackageEntity packageEntity;
    @Relation(parentColumn = "id",entityColumn = "package_id",entity = DatasetEntity.class)
    public List<DatasetEntity> datasetEntityList;
}
