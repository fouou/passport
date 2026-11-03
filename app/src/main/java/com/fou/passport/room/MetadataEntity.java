package com.fou.passport.room;

import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

/**
 *
 * ***** keyWords ******
 *
 * Username (用户名)
 * Email (邮箱)
 * Password (密码)
 * Category (分类)
 * Website (网站) - for passwords, bank cards, certificates or licenses
 * Application (应用)
 * Two-factor authentication (两步验证)
 * Attachment (附件)
 * Account association (账户关联)
 * Creation time (创建时间)
 * Modification history (修改历史记录)
 * Card number (卡号)
 * Issuing bank (开户行)
 * Verification number (for credit cards only) (验证号（仅信用卡）)
 * Phone number (手机号码)
 * Name (名称)
 * Code (编码)
 */
@Entity(tableName = "meta_data_entity",
        foreignKeys = @ForeignKey(parentColumns = "id", childColumns = "dataset_id", entity = DatasetEntity.class, onDelete = ForeignKey.CASCADE),
        indices = {@Index("dataset_id")})
public class MetadataEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public int xml_id;
    @ColumnInfo
    public int auto_fill_type;
    @ColumnInfo
    public String auto_fill_value_text;
    @ColumnInfo
    public boolean auto_fill_value_toggle;
    @ColumnInfo
    public String auto_fill_value_list;//如果不填写，即使有默认值也都为0
    @ColumnInfo
    public long auto_fill_value_date;
    @ColumnInfo
    public int dataset_id;
    @ColumnInfo
    public int package_id;
    @ColumnInfo
    public String key_word;


    public MetadataEntity() {
    }


    @Ignore
    public MetadataEntity(String key_word,String auto_fill_value_text){
        this.xml_id=-1;
        this.auto_fill_type=View.AUTOFILL_TYPE_TEXT;
        this.key_word=key_word;
        this.auto_fill_value_text=auto_fill_value_text;
    }

    /**
     * edittext
     * @param key_word
     * @param auto_fill_value_text
     */
    @Ignore
    public MetadataEntity(String key_word ,String auto_fill_value_text ,int dataset_id ,int package_id){
        this.xml_id=-1;
        this.auto_fill_type=View.AUTOFILL_TYPE_TEXT;
        this.key_word=key_word;
        this.auto_fill_value_text=auto_fill_value_text;
        this.dataset_id=dataset_id;
        this.package_id=package_id;
    }

    @Ignore
    public MetadataEntity(String key_word, int xml_id, int auto_fill_type, String auto_fill_value_text, boolean auto_fill_value_toggle, String auto_fill_value_list, long auto_fill_value_date, int dataset_id, int package_id) {
        this.xml_id = xml_id;
        this.auto_fill_type = auto_fill_type;
        this.auto_fill_value_text = auto_fill_value_text;
        this.auto_fill_value_toggle = auto_fill_value_toggle;
        this.auto_fill_value_list = auto_fill_value_list;
        this.auto_fill_value_date = auto_fill_value_date;
        this.dataset_id = dataset_id;
        this.package_id = package_id;
        this.key_word = key_word;
    }

    @Ignore
    public static AutofillId getAutofillId(String auto_fill_id) {
        if (auto_fill_id != null && auto_fill_id.length() > 0) {
            return new Gson()
                    .fromJson(auto_fill_id, new TypeToken<AutofillId>() {
                    }.getType());
        }
        return null;
    }

    @Ignore
    public static String getAutofillIdStr(AutofillId auto_fill_id) {
        if (auto_fill_id != null) {
            return new Gson().toJson(auto_fill_id);
        }
        return null;
    }

    @Ignore
    public static AutofillValue getAutofillValue(String auto_fill_value) {
        if (auto_fill_value != null && auto_fill_value.length() > 0) {
            return new Gson()
                    .fromJson(auto_fill_value, new TypeToken<AutofillValue>() {
                    }.getType());
        }
        return null;
    }

    @Ignore
    public static String getAutofillValueStr(AutofillValue auto_fill_value) {
        if (auto_fill_value != null) {
            return new Gson().toJson(auto_fill_value);
        }
        return null;
    }

}
