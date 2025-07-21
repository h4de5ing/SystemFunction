# 系统功能实现Demo

## 概述
- System权限+系统签名才能实现的功能  
- 调用SystemLib的app必须申明为系统App+系统签名才能正常使用 
- 架构：App依赖 → SystemLib(抽象层) → (Android5~Android14实现)

## SystemLib模块详细功能

### 1. 设备策略管理 (DPM.kt)
- 设备管理员激活/取消激活
- 配置文件所有者设置
- 设备管理功能(禁用摄像头、禁止截图、密码策略等)
- 应用管理(隐藏/暂停应用)
- 网络代理设置
- 证书安装

> **注意**：DPM.kt中的接口必须要setActiveProfileOwner激活以后才能正常使用。具体请参考[官方文档](https://developer.android.com/guide/topics/admin/device-admin)
### AOSP DPM默认配置
```
frameworks\base\core\res\res\values\config.xml
<string name="config_defaultSupervisionProfileOwnerComponent" translatable="false">com.android.systemfunction/com.android.systemfunction.AdminReceiver</string>
```
### AOSP源码
```
./base/core/java/android/app/admin/DevicePolicyManager.java
./base/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java
```


### 2. 输入事件模拟 (InputLib.kt)
- 模拟鼠标点击事件
- 模拟滚动事件
- 模拟键盘事件
- 输入服务初始化

### 3. 系统功能封装 (SystemLib.kt)
- Launcher管理
- 状态栏控制
- 系统属性操作
- 设备信息获取(SN号、IMEI、MAC地址等)
- 存储和内存信息
- 网络状态检测
- 时间和时区设置
- 屏幕和配置管理
- 分屏显示功能

### 4. 系统底层操作 (Syslib.kt)
- 网络管理(WiFi配置、以太网控制)
- 应用管理(静默安装/卸载、权限控制)
- 设备控制(USB、NFC、传感器)
- 存储管理(挂载/卸载存储设备)
- 系统更新(OTA升级)
- 电池优化管理
- 辅助功能控制
- 代理设置

## 使用要求
- 需要系统权限才能使用大部分功能
- 部分功能需要特定Android版本支持
- 使用前请确保已获取必要的权限

## 注意事项
- 修改系统设置可能会影响设备稳定性
- 部分功能在不同Android版本上行为可能不同
- 生产环境使用前请充分测试

### 其他重要系统服务
- KeyChainSystemService
- TelecomLoaderService
- NsdManager 内网服务器发现服务
- APN设置参考：ApnEditor.java, ApnPreference.java, ApnSettings.java

## 示例代码
```kotlin
// 获取设备SN号
val sn = SystemLib.getSN()

// 设置默认Launcher
SystemLib.setDefaultLauncher(context, "com.example.launcher")

// 禁用USB数据传输
Syslib.setUSBDataDisabled(context, true)

// 模拟点击事件
InputLib.injectMotionEvent(x, y)
```

## 版本支持
- 支持Android 5.0 (API 21) 及以上版本
- 部分功能需要更高API级别

## 依赖
- Kotlin协程
- AndroidX核心库
