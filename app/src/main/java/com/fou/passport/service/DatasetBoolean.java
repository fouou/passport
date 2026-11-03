package com.fou.passport.service;

import com.fou.passport.room.DatasetEntity;

/**
 * 保存每个账户与传入数据的比较结果
 */
public class DatasetBoolean {
    public DatasetEntity datasetEntity;//当前账户对象
    //public boolean isNeedUpdate;//删掉当前dataset然后newDataset
    public boolean isNeedNewDataset;//直接newDataset
    public boolean isKey_wordMatching;//
    public DatasetBoolean(DatasetEntity datasetEntity) {
        this.datasetEntity = datasetEntity;
    }

    @Override
    public String toString() {
        return "DatasetBoolean{isNeedUpdate=" +
                ", isNeedNewDataset=" + isNeedNewDataset +
                '}';
    }
}
