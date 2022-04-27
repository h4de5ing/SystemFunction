package com.android.systemfunction.utils;

import android.annotation.SuppressLint;

import java.lang.reflect.Field;

public class TestUtils {
    //https://www.cnblogs.com/pandans/p/3288847.html
    public static int getInternalString() {
        int id = -1;
        try {
            @SuppressLint("PrivateApi") Class c = Class.forName("com.android.internal.R$string");
            Object obj = c.newInstance();
            Field field = c.getField("config_defaultSupervisionProfileOwnerComponent");
            id = field.getInt(obj);
        } catch (Exception e) {
        }
        return id;
    }
}
