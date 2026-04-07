# SystemFunction 源码工程

本目录包含 Android 系统功能封装库的完整源码——一个面向生产环境的 Kotlin 库，为需要系统权限的 Android 应用提供统一的、感知版本差异的 API 层。

## 架构

```
SystemFunction_src/
├── SystemLib/      # 核心库——所有调用方的统一入口
├── Android12/      # API 31–32 专属实现
├── Android13/      # API 33 专属实现
├── Android14/      # API 34 专属实现
├── Android15/      # API 35 专属实现
├── Android16/      # API 36 专属实现
└── t/              # 内部测试/调试模块
```

**依赖关系：**

```
你的 App
    └── systemlib（SystemLib 模块）
            ├── android12
            ├── android13
            ├── android14
            ├── android15
            └── android16
```

`SystemLib` 是统一 API 层，对外暴露稳定的接口，在运行时根据 `Build.VERSION.SDK_INT` 自动分发到对应的版本实现模块。调用方无需直接依赖 `Android12`–`Android16`。

## Android 版本兼容范围

| 模块 | API Level | Android 版本 |
|------|-----------|-------------|
| Android12 | 31, 32 | Android 12, 12L |
| Android13 | 33 | Android 13 |
| Android14 | 34 | Android 14 |
| Android15 | 35 | Android 15 |
| Android16 | 36 | Android 16 |

核心 `SystemLib` 模块的 `minSdk = 21`，`compileSdk = 36`。

## 源文件说明（SystemLib 模块）

| 文件 | 职责 |
|------|------|
| `SystemLib.kt` | Launcher、状态栏、设备信息、时间/时区、系统属性 |
| `Syslib.kt` | 应用管理、网络、存储、电源、OTA、辅助功能、USB |
| `DPM.kt` | 设备策略管理——管理员激活、锁屏、Kiosk、证书 |
| `InputLib.kt` | 输入事件注入（触摸、滚动、键盘） |
| `Oops.kt` | AppOps 权限模式、运行时权限授予、电池优化管理 |
| `DeviceUtils.kt` | 设备类型检测（TV、Wear、Auto） |
| `X.kt` | 图像转换工具类、系统设置读写 |
| `NtpClient.kt` | NTP 时间同步 |
| `oemconfig.kt` | OEMConfig 限制 XML 解析 |
| `Logger.java` | 内部日志工具 |

## 各版本实现要点

### Android12（API 31–32）
- `disableSensor12` — 通过 `ISensorPrivacyManager.setIndividualSensorPrivacy` 控制传感器隐私
- `disableEthernet12` — 通过 `IEthernetManager.Trackstop/Trackstart` 控制以太网
- 屏保（Dream）管理 — 使用 `IDreamManager`
- 锁屏禁用 — 使用 `ILockSettings` + `LockPatternUtils`
- 无线调试 — 使用 `IAdbManager.allowWirelessDebugging`
- 通知监听授权 — 使用 `INotificationManager.setNotificationListenerAccessGrantedForUser`

### Android13（API 33）
- `disableSensor13` — 通过 `ISensorPrivacyManager.setToggleSensorPrivacy` 控制传感器隐私
- `disableEthernet13` — 通过 `IEthernetManager.setEthernetEnabled` 控制以太网
- `cameraListener` — 通过 `ICameraService` 监听摄像头和手电筒状态
- `setLock` — 通过 `IDevicePolicyManager.lockNow`（2参数）锁定设备

### Android14（API 34）
- `setProfileOwner14` — 参数数量相比 API 33 从3个减少为2个
- `setLock14` — `lockNow` 新增第3个参数 `callerPackageName`
- `lockScreen` — 通过 `SurfaceComposer.setPowerMode` 实现只息屏不锁屏

### Android15（API 35）
- `setPackagesSuspendedAsUser15` — 新增2个参数，共9个参数（相比 API 34）
- `enableNFC15` / `disableNFC15` — `INfcAdapter` 已移除，改为通过 `NfcAdapter` 反射调用

### Android16（API 36）
- `setActiveAdmin16` — 新增第4个参数 `callerPackageName`
- `setPackagesSuspendedAsUser16` — 与 Android15 相同的9参数签名

## 构建

### 环境要求
- Android Studio Ladybug 或更新版本
- JDK 22
- 已安装 Android SDK API 36
- 设备运行需要平台签名文件（`platform.jks`）

### 构建库

```bash
./gradlew :SystemLib:assembleRelease
```

### 发布到本地 Maven 仓库

将 AAR 产物发布到 `../SystemLib_repository`：

```bash
./gradlew :SystemLib:publishReleasePublicationToMavenRepository
./gradlew :Android12:publishReleasePublicationToMavenRepository
./gradlew :Android13:publishReleasePublicationToMavenRepository
./gradlew :Android14:publishReleasePublicationToMavenRepository
./gradlew :Android15:publishReleasePublicationToMavenRepository
./gradlew :Android16:publishReleasePublicationToMavenRepository
```

## 开发注意事项

**系统签名是必要条件。** 所有调用 `IPackageManager`、`IDevicePolicyManager`、`IPowerManager`、`IInputManager` 等 AIDL 服务的 API，都要求调用方持有系统 UID（`android.uid.system`）并使用平台签名。

**AIDL 接口稳定性。** 本库使用了带 `@UnsupportedAppUsage` 注解的接口和 AIDL 隐藏接口。版本专属模块存在的原因正是这些接口在不同 Android 大版本间会发生变化。升级目标 API Level 后请务必验证行为。

**DPM 前提条件。** `DPM.kt` 中的所有方法都要求应用已激活为设备管理员或 Profile Owner。如需无人值守激活，需在 ROM 的 `config.xml` 中配置默认监管组件：

```xml
<!-- frameworks/base/core/res/res/values/config.xml -->
<string name="config_defaultSupervisionProfileOwnerComponent" translatable="false">
    com.your.package/com.your.package.AdminReceiver
</string>
```

AOSP 源码参考：
- `frameworks/base/core/java/android/app/admin/DevicePolicyManager.java`
- `frameworks/base/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java`

**已知限制。** `X.kt` 中的 `copyDir()` 目前只遍历并打印目录结构，**不会实际复制文件**。

## 完整 API 参考

- 中文版：[API_REFERENCE.md](../API_REFERENCE.md)
- 英文版：[API_REFERENCE_EN.md](../API_REFERENCE_EN.md)
