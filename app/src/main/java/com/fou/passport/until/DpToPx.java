package com.fou.passport.until;

import android.content.res.Resources;

public class DpToPx {
    /**
     * 将 dp 转换为 px
     *
     * @param dpValue dp 值
     * @return 对应的 px 值
     */
    public static int dpToPx(float dpValue) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }
}
