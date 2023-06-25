# 　MDM 二次开发手册

## 一 安装MDM服务MDM.apk

## 二 引入sdk

- 添加远程仓库地址 settings.gradle

```groovy
repositories {
    maven { url 'https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository' }
}
```

- 添加依赖

```groovy
dependencies {
    implementation 'com.android.mdmsdk:mdmsdk:1.0-20220427'
}
```

- 调用mdm接口

```kotlin
setHomeKeyDisabled(true) //禁用Home按键
val isDisable = isHomeKeyDisable() //检查home按键是否被禁用
```

## 三 接口说明

- bind(context) 绑定远程服务
- unbind(context) 解除绑定
- isBind() 判断服务是否绑定成功，如果无法绑定成功，1.检查mdm服务是否安装成功 2.检查mdm服务是否成功运行
- setHomeKeyDisabled() 禁用Home按键
- isHomeKeyDisable() 查询是否禁用Home键按键
- setRecentKeyDisable() 禁用最近任务按键
- isRecentKeyDisable() 查询是否禁用最近应用键按键
- setBackKeyDisable() 禁用返回按键
- isBackKeyDisable() 查询是否禁用返回键
- setNavigaBarDisable() 禁用导航栏
- isNavigaBarDisable() 查询是否禁用导航栏
- setStatusBarDisable() 禁用状态栏
- isStatusBarDisable() 查询是否禁用状态栏
- setBluetoothDisable() 禁用蓝牙
- isBluetoothDisabled() 查询是否禁用蓝牙设备
- setHotSpotDisabled() 禁用热点
- isHotSpotDisabled() 是否禁用设备个人热点
- setWifiDisabled() 禁止wifi
- isWifiDisabled() 查询是否禁止使用WiFi
- setGPSDisabled() 禁止GPS
- isGPSDisable() 查询是否禁用设备GPS
- setUSBDataDisabled() 禁止USB数据传输
- isUSBDataDisabled() 查询是否禁用USB数据传输
- setDataConnectivityDisabled() 禁止移动数据网络
- isDataConnectivityDisabled() 查询是否禁用移动数据网络
- setScreenShotDisable() 禁止截屏
- isScreenShot() 是否禁用设备截屏
- setScreenCaptureDisabled() 禁止录屏
- isScreenCaptureDisabled() 是否禁用设备录屏
- setTFCardDisabled() 禁止TF卡
- isTFCardDisabled() 禁用设备TF卡存储
- setCallPhoneDisabled() 禁止拨打电话
- isCallPhoneDisabled() 是否禁用拨打电话
- disableSms() 禁止短信
- isSmsDisable() 查询是否禁用短信
- setMicrophoneDisable() 禁止麦克风
- isMicrophoneDisable() 查询麦克风禁用状态
- setRestoreFactoryDisabled 禁止恢复出厂设置
- isRestoreFactoryDisable() 查询是否禁用设备恢复出厂设备
- setSystemUpdateDisabled 禁止系统升级
- isSystemUpdateDisabled() 查询系统升级功能禁用状态
- setInstallDisabled() 禁止安装应用
- isInstallDisabled() 是否禁止安装应用
- removeWifiConfig 移除wifi配置
- activeDeviceManager 静默激活设备管理器
- removeActiveDeviceAdmin 静默取消激活设备管理
- setDefaultLauncher 静默设置默认Launcher
- clearDefaultLauncher 静默移除默认Launcher
- addForbiddenInstallApp 添加禁止安装应用列表
- removeForbiddenInstallApp  移除禁止安装应用列表
- getForbiddenInstallAppList  获取被禁止安装应用列表接口
- addInstallPackageTrustList  添加应用安装白名单
- removeInstallPackageTrustList  移除应用安装白名单
- getInstallPackageTrustList  获取应用安装白名单
- addDisallowedUninstallPackages  添加禁止卸载应用列表
- removeDisallowedUninstallPackages  移除禁止卸载应用列表
- getDisallowedUninstallPackageList  获取被禁止卸载应用列表
- addPersistentApp  添加系统应用保活白名单
- removePersistentApp  移除系统应用保活白名单
- getPersistentApp  获取系统应用保活白名单
- setSuperWhiteListForSystem  添加受信任应用白名单
- removeSuperWhiteListForSystem  移除受信任应用白名单
- getSuperWhiteListForSystem  获取受信任应用白名单
- getDeviceInfo  获取设备信息
- setFileShareDisabled  禁止设备分享文件
- isFileShareDisabled  查询是否禁用设备分享文件



