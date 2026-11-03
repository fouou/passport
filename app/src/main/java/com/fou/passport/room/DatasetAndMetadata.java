package com.fou.passport.room;

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;

public class DatasetAndMetadata {
    @Embedded
    public DatasetEntity datasetEntity;
    @Relation(parentColumn = "id",entityColumn = "dataset_id",entity = MetadataEntity.class)
    public List<MetadataEntity> metadataEntityList;
}
