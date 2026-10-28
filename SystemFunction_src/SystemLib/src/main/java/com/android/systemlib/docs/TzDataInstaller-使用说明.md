# TzDataInstaller 时区数据更新

`com.android.systemlib.TzDataInstaller`：在不刷机的情况下，把时区更新包（`distro.zip`）安装到设备上，
写入 `/data/misc/zoneinfo/staged`，**重启后生效**。从 TzUpdater App 的 `TzInstaller.java` 移植而来。

## 1. 支持范围

**仅支持 Android 8.1 / 9 / 10（API 27 / 28 / 29）**，其他版本的 `install()` 会直接报错。

| 系统 | 包格式（distro_version） | 系统内置版本读取位置 | 安装方式 |
|---|---|---|---|
| 8.1 (API 27) | `001.001\|...` | `/system/usr/share/zoneinfo/tzdata` 文件头 | RulesManager；服务未开启时退回 `UPDATE_TZDATA` 广播 |
| 9 (API 28) | `002.001\|...` | `/system/usr/share/zoneinfo/tzdata` 文件头 | 只有 RulesManager（系统已删除 `TzDataInstallReceiver`） |
| 10 (API 29) | `003.001\|...` | `/apex/com.android.tzdata/etc/tz/tz_version` | 只有 RulesManager |

- 不同版本的包**不能混用**：设备要求格式的 major 版本完全一致，ICU 资源前缀也要和设备的 ICU 版本一致
  （8.1 是 `icudt58l`，9 是 `icudt60l`，10 是 `icudt63l`）。
- **RulesManager 的前提**：framework 配置 `config_enableUpdateableTimeZoneRules=true`，否则 RulesManagerService 不会启动。
  9 和 10 必须开启（通过 OTA）；8.1 没开启也可以，会自动改走广播。
  检查命令：`adb shell service list | grep timezone`。
- 建议同时保持 `config_timeZoneRulesUpdateTrackingEnabled=false`。

## 2. 权限与清单

SystemLib 的 `AndroidManifest.xml` 已经声明好，合并到宿主 App 后自动生效：

| 内容 | 用途 |
|---|---|
| `UPDATE_TIME_ZONE_RULES` | RulesManager 安装、卸载 |
| `QUERY_TIME_ZONE_RULES` | Android 10 查询 RulesState |
| `UPDATE_CONFIG` | 8.1 发送 `UPDATE_TZDATA` 广播 |
| `TzDistroProvider`（authority `${applicationId}.systemlib.tzdistro`，不导出） | 8.1 广播方式下给 system_server 读取 zip |

以上权限都是 `signature|privileged`，**宿主 App 必须用平台密钥签名**，否则会抛出 SecurityException。

## 3. API

```kotlin
val tz = TzDataInstaller(context)

tz.isSupported()            // 是否是 8.1 / 9 / 10
tz.getStatus()              // Status：系统内置 / 已安装 / 待生效版本、ICU 版本、RulesManager 是否可用，toString() 可直接显示
tz.readDistroVersion(file)  // 读取 zip 中的 distro_version
tz.precheck(dv, force)      // 检查包是否适合本机，null 表示通过
tz.extractBundledDistro()   // 从 assets 中取出适合本机的最新包（见第 4 节）

tz.install(file, force = false, configUpdateVersion = null, callbacks)  // 异步安装
tz.uninstall(callbacks)     // 卸载，重启后恢复系统内置数据（需要 RulesManager）
```

- `install()` / `uninstall()` 在内部的单线程中执行，同一时间只能运行一个操作，**回调在后台线程**，更新 UI 需要切回主线程。
- `force = true`：重新安装相同或更旧的版本，但仍然不能低于系统内置版本。
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
| 9 | `distro_android9.zip` 或 `distro_android9_*.zip` |
| 10 | `distro_android10.zip` 或 `distro_android10_*.zip` |

> **注意 Android 9 的包**：现在的 `tzdistro.py` 只能生成 android8（1.1）和 android10（3.1）两种包，
> 还不能生成 Android 9 需要的 2.1 格式（ICU `icudt60l`、Android 9 格式的 tzlookup.xml）。
> 工具类支持在 9 上安装，但要先给 `tzdistro.py` 增加 android9 预设，才有包可装。

## 5. 常见错误

| 提示 | 原因 |
|---|---|
| `this Android version is not supported` | 不是 8.1 / 9 / 10 |
| `distro format ... does not fit this device` | 包的目标版本不对 |
| `RulesManagerService is not running` | 9 / 10 上没有开启 `config_enableUpdateableTimeZoneRules` |
| `rules ... are older than the system's ...` | 包的规则比系统内置的旧 |
| `already installed/staged` | 已经是相同或更新的版本，需要的话传 `force = true` |
| `SecurityException` | 宿主 App 没有用平台密钥签名 |
| `No staged distro after 30 s`（8.1） | 广播被忽略，多半是 VERSION 不够大 |

排查日志：

```
adb logcat -s timezone.RulesManagerService TZDataInstallReceiver ConfigUpdateInstallReceiver tzdatacheck
```
