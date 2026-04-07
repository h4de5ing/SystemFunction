# Android 系统功能封装库

封装了 Android 系统应用中常用隐藏系统 API 的 Kotlin 库，提供统一的、感知版本差异的接口，用于需要平台签名或系统权限的操作。

> **前提条件**：平台签名（`platform.jks`）+ `android.uid.system` 共享用户 ID

## 功能模块

| 模块             | 说明                                                      |
| -------------- | ------------------------------------------------------- |
| Launcher / 桌面  | 获取/设置默认桌面，列举所有 Launcher                                 |
| 状态栏 / 导航栏      | 禁用展开栏、图标、时钟、导航键；切换手势/三按钮模式                              |
| 设备信息           | SN、IMEI、MAC、RAM、电池容量、IP 地址、Root 检测                      |
| 时间 / 时区        | NTP 对时、设置系统时间/时区、修改系统语言                                 |
| 电源管理           | 关机、重启、恢复出厂、屏幕开关、休眠/唤醒                                   |
| DPM（设备策略）      | 激活管理员、设置 Profile/Device Owner、锁屏、密码策略、禁用摄像头/截图、Kiosk 模式 |
| 应用管理           | 静默安装/卸载（APK & XAPK）、隐藏、冻结/暂停、禁止卸载、授予权限、电池优化             |
| 存储管理           | 挂载/卸载存储卷、列出 USB 存储、清除应用数据                               |
| 网络             | WiFi 配置、热点控制、以太网启用/禁用、HTTP/全局代理、NFC 开关                  |
| 输入注入           | 通过 `IInputManager` 注入触摸、滚动、按键事件                         |
| Sensor 隐私      | 切换麦克风/摄像头硬件开关（Android 12–13）                            |
| 系统设置           | 读写 `Settings.Global/System/Secure`                      |
| 辅助功能           | 启用/禁用辅助功能服务                                             |
| OTA 升级         | A/B 分区 OTA（通过 `UpdateEngine`，带进度回调）                     |
| 日志 / BugReport | 通过 `IActivityManager` 或 `BugreportManager` 触发日志收集       |
| USB 管理         | 静默授予 USB 设备权限、绑定默认处理应用                                  |
| 屏保（Dream）      | 获取/设置屏保组件，启动屏保                                          |
| 设备类型检测         | 检测 TV、Wear OS、Android Auto                              |
| 图像工具           | `Drawable`、`Bitmap`、`ByteArray` 互转                      |
| AppOps / 权限    | 设置/检查 `AppOpsManager` 模式，授予运行时权限，管理电池优化                 |
| OEMConfig      | 解析 OEMConfig 限制 XML 条目                                  |

## Android 版本兼容矩阵

| 功能                                | API 31 (12)               | API 32 (12L) | API 33 (13)             | API 34 (14) | API 35 (15) | API 36 (16) |
| --------------------------------- |:-------------------------:|:------------:|:-----------------------:|:-----------:|:-----------:|:-----------:|
| Sensor 隐私（麦克风/摄像头开关）              | 是                         | 是            | 是                       | -           | -           | -           |
| 以太网启用/禁用                          | 是（`Trackstop/Trackstart`） | 是            | 是（`setEthernetEnabled`） | 是           | 是           | 是           |
| 只息屏不锁屏                            | -                         | -            | -                       | 是           | 是           | 是           |
| `setProfileOwner`（4参数）            | -                         | -            | -                       | 是           | 是           | 是           |
| `setActiveAdmin`（4参数）             | -                         | -            | -                       | -           | -           | 是           |
| `setPackagesSuspendedAsUser`（9参数） | -                         | -            | -                       | -           | 是           | 是           |
| NFC（`INfcAdapter`）                | 是                         | 是            | 是                       | 是           | -           | -           |
| NFC（`NfcAdapter` 反射）              | -                         | -            | -                       | -           | 是           | 是           |
| 无线调试（`IAdbManager`）               | 是                         | 是            | 是                       | 是           | 是           | 是           |
| `lockNow`（2参数）                    | 是                         | 是            | 是                       | -           | -           | -           |
| `lockNow`（3参数，含调用方包名）             | -                         | -            | -                       | 是           | 是           | 是           |

## 快速开始

### 前提条件

- 从设备厂商获取平台签名文件（`platform.jks`）
- APK 必须使用平台签名进行签名
- 应用必须声明 `android.uid.system` 共享用户 ID

### 1. 在 `settings.gradle` 中添加仓库

```groovy
dependencyResolutionManagement {
    repositories {
        maven { url 'https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository' }
        // 国内镜像
        maven { url 'https://gitee.com/lex1992/system-function/raw/master/SystemLib_repository' }
    }
}
```

### 2. 在 `build.gradle` 中添加依赖

```groovy
dependencies {
    implementation 'com.android.systemlib:systemlib:1.0-20221223'
}
```

### 3. 在 `build.gradle` 中配置签名

```groovy
android {
    signingConfigs {
        release {
            storeFile file("platform.jks")
            storePassword 'android'
            keyAlias 'android'
            keyPassword 'android'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### 4. 在 `AndroidManifest.xml` 中设置共享用户 ID

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    android:sharedUserId="android.uid.system">
    ...
</manifest>
```

### 5. 添加 ProGuard 规则

```proguard
-keep class com.android.systemlib.** { *; }
```

## 开发者注意事项

**系统签名**：必须从设备厂商获取 `platform.jks`。APK 需使用平台签名并安装到系统分区（或在 eng/userdebug 构建上通过 `adb install` 推送）。

**API 稳定性**：本库大量使用带 `@UnsupportedAppUsage` 注解的接口或 AIDL 隐藏接口，这些接口可能随 Android 版本变化。升级目标 Android 版本后请务必测试。

**版本特定模块**：`Android12.kt` 至 `Android16.kt` 包含各 API 级别的差异实现。`Syslib.kt` / `SystemLib.kt` 中的统一入口会自动分发到正确的实现。

**已知限制**：`X.kt` 中的 `copyDir()` 目前只遍历目录树并打印路径，**不会实际复制文件**。请勿依赖此方法执行文件复制操作。

## 问题报告

提交 Issue 时请包含以下信息：

- Android 版本和 API 级别（如 Android 14 / API 34）
- 设备型号和 ROM 版本（如相关）
- 完整的 logcat 输出
- 最小可复现代码

## 贡献指南

1. Fork 本仓库
2. 创建特性分支
3. 提交修改（附有意义的 commit message）
4. 推送分支并创建 Pull Request

## 许可证

[Apache License 2.0](LICENSE)
