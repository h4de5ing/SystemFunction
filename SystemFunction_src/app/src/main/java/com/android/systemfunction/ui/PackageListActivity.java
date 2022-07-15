package com.android.systemfunction.ui;

import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.android.mdmsdk.PackageTypeEnum;
import com.android.systemfunction.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackageListActivity extends AppCompatActivity {
    String name = PackageTypeEnum.PERSISTENT.name();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_list);
        findViewById(R.id.read).setOnClickListener(v -> {
            String persistent = Settings.Global.getString(getContentResolver(), name);
            List<String> list = getList(persistent);
            for (String s : list) {
                System.out.println(s);
            }
            if (list.lastIndexOf("com.guoshi.httpcanary") > -1) {
                System.out.println("在白名单中...");
            } else {
                System.out.println("没找到...");
            }
        });
        findViewById(R.id.write).setOnClickListener(v ->
                Settings.Global.putString(getContentResolver(),
                        name, Arrays.asList(
                                "com.guoshi.httpcanary",
                                "com.zhihu.android",
                                "com.coolapk.market").toString()));
        findViewById(R.id.clean).setOnClickListener(v -> Settings.Global.putString(getContentResolver(), name, ""));
    }

    public static List<String> getList(String message) {
        List<String> list = new ArrayList<>();
        try {
            int length = message.length();
            String newMessage = message.substring(1, length - 1);
            for (String s : newMessage.split(",")) {
                list.add(s.trim());
            }
        } catch (Exception e) {
        }
        return list;
    }

}