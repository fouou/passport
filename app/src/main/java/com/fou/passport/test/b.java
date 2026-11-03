package com.fou.passport.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class b {
    public static final int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
    public static final int TYPE_CLASS_TEXT = 0x00000001;
//<flag name="textPassword" value="0x00000081" />
    public static void main(String[] args) {
        System.out.println(TYPE_TEXT_VARIATION_PASSWORD | TYPE_CLASS_TEXT);
    }
}
