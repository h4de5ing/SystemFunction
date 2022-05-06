package com.android.systemfunction;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * 设备管理器 主要用户防止用户停止和卸载本应用
 */
public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        Log.e(">>>>>>>>>", "onReceive");
    }

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        Log.e(">>>>>>>>>", "onEnabled");
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        Log.e(">>>>>>>>>", "onDisabled");
    }
}
