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
- isHomeKeyDisable()
- setRecentKeyDisable() 禁用最近任务按键
- isRecentKeyDisable()
- setBackKeyDisable() 禁用返回按键
- isBackKeyDisable()
- setNavigaBarDisable() 禁用导航栏
- isNavigaBarDisable()
- setStatusBarDisable() 禁用状态栏
- isStatusBarDisable()
- setBluetoothDisable() 禁用蓝牙
- isBluetoothDisabled()
- setHotSpotDisabled() 禁用热点
- isHotSpotDisabled()
- setWifiDisabled() 禁止wifi
- isWifiDisabled()
- setGPSDisabled() 禁止GPS
- isGPSDisable()
- setUSBDataDisabled() 禁止USB数据传输
- isUSBDataDisabled()
- setDataConnectivityDisabled() 禁止移动数据网络
- isDataConnectivityDisabled()
- setScreenShotDisable() 禁止截屏
- isScreenShot()
- setScreenCaptureDisabled() 禁止录屏
- isScreenCaptureDisabled()
- setTFCardDisabled() 禁止TF卡
- isTFCardDisabled()
- setCallPhoneDisabled() 禁止拨打电话
- isCallPhoneDisabled()
- disableSms() 禁止短信
- isSmsDisable
- setMicrophoneDisable 禁止麦克风
- isMicrophoneDisable
- setRestoreFactoryDisabled 禁止恢复出厂设置
- isRestoreFactoryDisable
- setSystemUpdateDisabled 禁止系统升级
- isSystemUpdateDisabled
- removeWifiConfig 移除wifi配置
- activeDeviceManager 激活设备管理器
- removeActiveDeviceAdmin 移除设备管理器
- setDefaultLauncher 设置默认Launcher
- clearDefaultLauncher 移除默认Launcher



