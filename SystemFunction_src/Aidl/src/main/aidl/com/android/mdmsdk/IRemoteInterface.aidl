// IRemoteInterface.aidl
package com.android.mdmsdk;

// Declare any non-default types here with import statements

interface IRemoteInterface {
    void setDisable(in String key,in boolean disable);
     boolean isDisable(in String key);
}