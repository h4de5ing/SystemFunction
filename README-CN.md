# Android 系统功能封装库

面向生产环境的 Kotlin 库，封装了 Android 系统应用中常用的隐藏系统 API，提供统一的、感知版本差异的接口，用于需要平台签名或系统权限的操作。内置对 Android 12 至 Android 16（API 31–36）的兼容适配。

> **前提条件**：平台签名（`platform.jks`）+ `android.uid.system` 共享用户 ID

## 适合谁使用

本库面向需要调用标准 SDK API 无法满足的特权能力的团队：

- **Android OEM / ODM 工程师** — 开发随定制 ROM 预装的系统应用
- **企业设备管理开发者** — 实现 MDM/EMM 解决方案或 Kiosk 系统
- **系统应用开发者** — 需要静默安装、设备策略控制或硬件级别访问
- **AOSP 定制团队** — 通过特权应用而非修改框架层来扩展平台能力

## 支持的使用场景

- 无需用户交互，静默安装、卸载、隐藏、冻结或限制应用
- 通过 DPM 控制锁屏、密码策略和 Kiosk 模式
- 在系统级别切换 NFC、以太网、传感器隐私开关和 USB 数据
- 通过代码注入触摸、滚动和键盘事件
- 在 A/B 分区设备上通过 `UpdateEngine` 推送 OTA 升级
- 读写 `Settings.Global/System/Secure` 和系统属性
- 管理 WiFi 配置（含密码）并配置网络代理
- 获取普通应用无法访问的真实设备标识符（IMEI、SN、出厂 MAC）
- 为任意已安装应用授予或撤销运行时权限及 AppOps 模式

## 注意事项与安全说明

- **不适用于普通消费者应用。** 本库所有 API 均需系统 UID 和平台签名，普通 Play Store 应用无法使用。
- **不是 Root 方案。** 本库在 AOSP 权限模型内工作，不利用漏洞也不绕过 SELinux。使用前需从厂商获取 `platform.jks` 或自行构建 ROM。
- **隐藏 API 稳定性无保证。** 带 `@UnsupportedAppUsage` 注解的接口可能在 Android 大版本升级时被移除或修改。本库的各版本兼容模块正是为此而设计，但新版 Android 可能仍需新的适配。
- **部分 DPM 操作不可逆。** 恢复出厂（`wipeDate`）、密码最大错误次数、Kiosk 锁定任务模式等，若误操作可能导致设备无法使用。请务必先在非生产设备上测试。
- **传感器隐私 API 仅支持 Android 12–13。** 用于麦克风/摄像头硬件开关的 `ISensorPrivacyManager` 接口已在 Android 14 中移除，请勿在 API 34+ 上调用 `disableSensor()`。

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

## 仓库结构

```
SystemFunction/
├── SystemFunction_src/     # 库的完整源码及所有版本兼容模块
│   ├── SystemLib/          # 核心库（主入口，统一 API 层）
│   ├── Android12/          # API 31–32 兼容实现
│   ├── Android13/          # API 33 兼容实现
│   ├── Android14/          # API 34 兼容实现
│   ├── Android15/          # API 35 兼容实现
│   └── Android16/          # API 36 兼容实现
├── SystemLib_repository/   # 预编译 Maven 仓库（AAR 产物，可直接引用）
├── API_REFERENCE.md        # 完整 API 参考文档（中文）
└── API_REFERENCE_EN.md     # 完整 API 参考文档（英文）
```

### SystemLib_repository — 预编译 Maven 产物

`SystemLib_repository` 目录是一个本地 Maven 仓库，包含所有模块的预编译 AAR，无需自行构建即可直接使用。

可用产物（版本号格式均为 `1.0-<日期>`）：

| 产物          | Group ID                | 说明               |
| ----------- | ----------------------- | ---------------- |
| `systemlib` | `com.android.systemlib` | 核心库，大多数项目只需引用此产物 |
| `android12` | `com.android.android12` | API 31–32 兼容层    |
| `android13` | `com.android.android13` | API 33 兼容层       |
| `android14` | `com.android.android14` | API 34 兼容层       |
| `android15` | `com.android.android15` | API 35 兼容层       |
| `android16` | `com.android.android16` | API 36 兼容层       |
| `hideapi`   | `com.android.hideapi`   | 隐藏 API stub jar  |
| `mdmsdk`    | `com.android.mdmsdk`    | MDM SDK          |

> **典型用法**：只添加 `systemlib` 依赖即可，它已内部聚合所有版本兼容模块。

### SystemFunction_src — 源码工程

源码工程是标准的 Android 多模块 Gradle 项目。本地构建并发布：

```bash
cd SystemFunction_src
./gradlew :SystemLib:publishReleasePublicationToMavenRepository
```

执行后会将更新后的 AAR 写入 `SystemLib_repository`。

## 按功能快速查看示例

以下是常见调用模式。所有示例均假设应用已使用平台签名并声明 `android.uid.system`。

**静默安装 APK（带进度回调）**
```kotlin
installAPK(context, "/sdcard/app.apk") { code, message ->
    when (code) {
        0  -> Log.i(TAG, "安装成功")
        -4 -> Log.e(TAG, "安装失败: $message")
    }
}
```

**静默设置默认 Launcher**
```kotlin
setDefaultLauncher(context, "com.example.launcher")
```

**锁定状态栏（隐藏所有内容）**
```kotlin
setStatusBarInt(context, STATUS_DISABLE_NAVIGATION or DISABLE_EXPAND or DISABLE_NOTIFICATION_ICONS)
// 恢复
setStatusBarInt(context, DISABLE_NONE)
```

**冻结 / 暂停应用**
```kotlin
suspendedAPP("com.example.app", true)   // 冻结
suspendedAPP("com.example.app", false)  // 恢复
```

**静默授予运行时权限**
```kotlin
grant(context, "com.example.app", android.Manifest.permission.CAMERA)
// 或一键授予所有已申请权限
grantPermission(context, "com.example.app")
```

**设置 AppOps 模式（例如允许用量统计访问）**
```kotlin
// 操作码 43 = APP_OP_GET_USAGE_STATS
setMode(context, 43, "com.example.app", MODE_ALLOWED)
```

**从 NTP 服务器同步并设置系统时间**
```kotlin
getNtpTime("ntp.aliyun.com", 3000) { timestamp ->
    setTime(context, timestamp)
}
```

**DPM — 激活管理员并进入 Kiosk 模式**
```kotlin
val admin = ComponentName(packageName, "$packageName.AdminReceiver")
setActiveProfileOwner(admin)
kiosk(activity, admin, arrayOf("com.example.kiosk"))
```

**注入触摸事件**
```kotlin
injectInit()
injectMotionEvent(MotionEvent.ACTION_DOWN, 540f, 960f)
injectMotionEvent(MotionEvent.ACTION_UP,   540f, 960f)
```

**A/B 设备 OTA 升级**
```kotlin
val otaFile = File("/sdcard/update.zip")
ota(otaFile,
    onStatusUpdate = { status, percent ->
        Log.i(TAG, "${getUpdateStatus(status)} — ${(percent * 100).toInt()}%")
    },
    onErrorCode = { code ->
        if (code == 0) Log.i(TAG, "OTA 完成，即将重启…")
        else Log.e(TAG, "OTA 失败: ${getUpdateError(code)}")
    }
)
```

**禁用以太网**
```kotlin
disableEthernet(true) { supported ->
    if (!supported) Log.w(TAG, "当前设备不支持以太网禁用操作")
}
```

**获取电池优化状态**
```kotlin
when (getBatteryOptimization(context, "com.example.app")) {
    MODE_UNRESTRICTED -> // 白名单，不受任何后台限制
    MODE_OPTIMIZED    -> // Android 默认优化行为
    MODE_RESTRICTED   -> // 后台受到严格限制
}
```

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
