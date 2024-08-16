package com.android.droidwall.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FireWallUtils {
    public static final String TAG = "";

    public void BlackListMode(INetworkManagementService networkService, Context context) {
        FireWallUtils.setFirewallEnabled(networkService, false); //黑名单模式
        List<String> whitelistApp = new ArrayList<>();
        whitelistApp.add("com.android.chrome");
        PackageManager pm = context.getPackageManager();
        for (String pkgName : whitelistApp) {
            int uid = FireWallUtils.getUidFromPackageName(pm, pkgName); //获取app的uid
            if (uid > 0) {
                FireWallUtils.setFirewallUidRule(networkService, 0, uid, 0); //重置 uid rule
                FireWallUtils.setFirewallUidRule(networkService, 0, uid, 2); //设置uid为黑名单
            }
        }
    }

    public void DisableMobileMode(INetworkManagementService networkService, Context context) {
        FireWallUtils.setFirewallEnabled(networkService, false); //黑名单模式
        List<String> whitelistApp = new ArrayList<>();
        whitelistApp.add("com.iflytek.inputmethod");//com.iflytek.inputmethod
        PackageManager pm = context.getPackageManager();
        for (String pkgName : whitelistApp) {
            int uid = FireWallUtils.getUidFromPackageName(pm, pkgName);
            if (uid > 0) {
//                FireWallUtils.setFirewallUidChainRule(networkService, uid, 0, false); //(networkType == 1) ? WIFI : MOBILE; , 禁止此uid连mobile
            }
        }
    }

    public static void setFirewallEnabled(INetworkManagementService networkService, boolean enable) {
        try {
            networkService.setFirewallEnabled(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setFirewallEnabled RemoteException e:" + Log.getStackTraceString(e));
        } catch (Exception e) {
            Log.e(TAG, "setFirewallEnabled Exception e:" + Log.getStackTraceString(e));
        }
    }

    public static void setFirewallUidRule(INetworkManagementService networkService, int chain, int uid, int rule) {
        try {
            networkService.setFirewallUidRule(chain, uid, rule);
        } catch (RemoteException e) {
            Log.e(TAG, "setFirewallUidRule RemoteException e:" + Log.getStackTraceString(e));
        } catch (Exception e) {
            Log.e(TAG, "setFirewallUidRule Exception e:" + Log.getStackTraceString(e));
        }
    }

    public static int getUidFromPackageName(PackageManager pm, String pkgName) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, PackageManager.MATCH_ALL);
            return appInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
        }
        return -1;
    }
}
