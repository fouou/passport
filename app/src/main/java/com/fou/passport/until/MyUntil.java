package com.fou.passport.until;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MyUntil {

    /**
     * 毫秒字符串转当前系统语言的时间日期
     * @param dateString
     * @return
     */
    public static String getSaveTime(String dateString) {
        long dateLong = Long.parseLong(dateString); // 获取 Long 对象
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
        return dateFormat.format(new Date(dateLong));
    }


    /**
     * set<String>集合转字符串（字符串之间用空格相连，且经行了排序）
     * 例 :
     * Set<String> set = new HashSet<>();
     * set.add("orange");
     * set.add("banana");
     * set.add("apple");
     * turnSetToString(set)==>"apple banana orange",
     * @param stringSet
     * @return
     */
    public static String turnSetToString(Set<String> stringSet){
        if (stringSet==null || stringSet.isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>(stringSet);
        Collections.sort(list);
        return String.join(" ", list);//字符串之间用空格相连
    }
}
