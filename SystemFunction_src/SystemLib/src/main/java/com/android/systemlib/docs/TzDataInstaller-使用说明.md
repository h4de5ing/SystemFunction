# TzDataInstaller 时区数据更新

`com.android.systemlib.TzDataInstaller`：在不刷机的情况下，把时区更新包（`distro.zip`）安装到设备上，
写入 `/data/misc/zoneinfo/staged`，**重启后生效**。从 TzUpdater App 的 `TzInstaller.java` 移植而来。

## 1. 支持范围

**仅支持 Android 8.1 / 10（API 27 / 29）**，其他版本的 `install()` 会直接报错。

| 系统 | 包格式（distro_version） | 安装方式 | 前提 |
|---|---|---|---|
| 8.1 (API 27) | `001.001\|...` | 一律走 `UPDATE_TZDATA` 广播 → `TzDataInstallReceiver` | 无，不看任何 framework 配置 |
| 10 (API 29) | `003.001\|...` | RulesManager（系统已没有 `TzDataInstallReceiver`） | `config_enableUpdateableTimeZoneRules=true` |

- 不同版本的包**不能混用**：设备要求格式的 major 版本完全一致，ICU 资源前缀也要和设备的 ICU 版本一致
  （8.1 是 `icudt58l`，10 是 `icudt63l`）。
- **Android 10 的开关校验**：`config_enableUpdateableTimeZoneRules` 是 RulesManagerService 的总开关（AOSP config.xml 原文 "master switch"），
  SystemServer 只在它为 true 时启动服务。工具类通过 `Resources.getSystem()` 读取这个值，**不是 true 就直接报错**，不会尝试其他方式。
  需要通过 OTA 打开。检查命令：`adb shell service list | grep timezone`。
- `config_timeZoneRulesUpdateTrackingEnabled` 控制的是"由 Google 时区数据 APK 更新来触发检查"，和本工具无关，**保持 false 即可**。
- 版本判断统一用 `android.util.TimeUtils.getTimeZoneDatabaseVersion()`（当前实际在用的规则版本，8.1 和 10 上都是公开 API），
  不读取系统分区或 APEX 中的文件。设备一旦由 tzdata APEX 机制负责更新，就不会使用本工具。

## 2. 权限与清单

SystemLib 的 `AndroidManifest.xml` 已经声明好，合并到宿主 App 后自动生效：

| 内容 | 用途 |
|---|---|
| `UPDATE_TIME_ZONE_RULES` | RulesManager 安装、卸载 |
| `QUERY_TIME_ZONE_RULES` | Android 10 查询 RulesState |
| `UPDATE_CONFIG` | 8.1 发送 `UPDATE_TZDATA` 广播 |
| `TzDistroProvider`（authority `${applicationId}.systemlib.tzdistro`，不导出，**默认禁用**） | 8.1 广播方式下给 system_server 读取 zip；只在 8.1 发送广播前由工具类动态启用，Android 10 和不使用本功能的宿主 App 不会启用 |

以上权限都是 `signature|privileged`，**宿主 App 必须用平台密钥签名**，否则会抛出 SecurityException。

## 3. API

```kotlin
val tz = TzDataInstaller(context)

tz.isSupported()              // 是否是 8.1 / 10
tz.getStatus()                // Status：在用 / 已安装 / 待生效版本、ICU 版本、开关和 RulesManager 状态，toString() 可直接显示
tz.currentRulesVersion()      // 当前在用的规则版本（TimeUtils.getTimeZoneDatabaseVersion()）
tz.isUpdateableRulesEnabled() // config_enableUpdateableTimeZoneRules 的值
tz.checkRulesManager()        // Android 10 的开关校验，null 表示通过
tz.readDistroVersion(file)    // 读取 zip 中的 distro_version
tz.precheck(dv, force)        // 检查包是否适合本机，null 表示通过
tz.extractBundledDistro()     // 从 assets 中取出适合本机的最新包（见第 4 节）

tz.install(file, force = false, configUpdateVersion = null, callbacks)  // 异步安装
tz.uninstall(callbacks)       // 卸载，重启后恢复系统内置数据（仅 Android 10）
```

- `install()` / `uninstall()` 在内部的单线程中执行，同一时间只能运行一个操作，**回调在后台线程**，更新 UI 需要切回主线程。
- `force = true`：跳过"比在用版本旧"和"已安装/待生效"的检查。如果包比系统内置的还旧，仍然会被系统拒绝（`ERROR_INSTALL_RULES_TOO_OLD`）。
- `configUpdateVersion`：只对 8.1 广播方式有效。默认按 `(年 × 100 + 字母序号) × 1000 + 修订号` 计算（2026e r1 → 202605001）。
  接收器只接受比上次更大的 VERSION，如果 logcat 出现 `new version is <= current version`，传一个更大的值。

示例：

```kotlin
val tz = TzDataInstaller(context)
val file = tz.extractBundledDistro() ?: File("/sdcard/distro.zip")
tz.install(file, callbacks = TzDataInstaller.Callbacks(
    onStatus = { Log.i(TAG, it) },
    onError = { Log.e(TAG, "tz install failed: $it") },
    onSuccess = {
        Log.i(TAG, it)
        // 已暂存，重启后生效；需要时调用 Syslib.kt 中的 reboot()
    },
))
```

## 4. 内置包命名

`extractBundledDistro()` 按本机版本在 assets 根目录中查找，同一个前缀有多个文件时，读取 zip 内的 `distro_version` 取最新的一个：

| 系统 | 文件名 |
|---|---|
| 8.1 | `distro_android8.zip` 或 `distro_android8_*.zip` |
| 10 | `distro_android10.zip` 或 `distro_android10_*.zip` |

## 5. 常见错误

| 提示 | 原因 |
|---|---|
| `this Android version is not supported` | 不是 8.1 / 10 |
| `distro format ... does not fit this device` | 包的目标版本不对 |
| `config_enableUpdateableTimeZoneRules is not true` | Android 10 上没有通过 OTA 打开这个开关 |
| `RulesManagerService ... is not running` | 开关是 true 但服务没有启动（不应该出现，请查 system_server 日志） |
| `rules ... are older than the rules in use ...` | 包的规则比当前在用的旧 |
| `ERROR_INSTALL_RULES_TOO_OLD` | 包的规则比系统内置的旧（系统拒绝） |
| `already installed/staged` | 已经是相同或更新的版本，需要的话传 `force = true` |
| `SecurityException` | 宿主 App 没有用平台密钥签名 |
| `No staged distro after 30 s`（8.1） | 广播被忽略，多半是 VERSION 不够大 |

排查日志：

```
adb logcat -s timezone.RulesManagerService TZDataInstallReceiver ConfigUpdateInstallReceiver tzdatacheck
```
