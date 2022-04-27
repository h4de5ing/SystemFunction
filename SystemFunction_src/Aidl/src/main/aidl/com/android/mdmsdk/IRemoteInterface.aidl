// IRemoteInterface.aidl
package com.android.mdmsdk;

// Declare any non-default types here with import statements

interface IRemoteInterface {
    void setDisable(in String key,in boolean disable);
    boolean isDisable(in String key);
    void removeWifi(in String ssid);
    void deviceManager(in String packageName,in String className,in boolean isRemove);
    void defaultLauncher(in String packageName,in boolean isClean);
    void shutdown(in boolean isReboot);
    void resetDevice();
    void packageManager(in String[] list,in int type);
    String[] getPackages(in int type);
}