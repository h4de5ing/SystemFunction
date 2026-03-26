# SystemFunction API 参考文档

> 包路径：`com.android.systemlib`
> 需要系统签名或 Device Owner / Profile Owner 权限才能调用大多数接口。

---

## 目录

1. [Launcher / 桌面管理](#1-launcher--桌面管理)
2. [状态栏 / 导航栏控制](#2-状态栏--导航栏控制)
3. [设备信息获取](#3-设备信息获取)
4. [时间 / 时区](#4-时间--时区)
5. [电源管理（开关机 / 屏幕）](#5-电源管理开关机--屏幕)
6. [DPM（设备策略管理）](#6-dpm设备策略管理)
7. [应用管理（安装 / 卸载 / 隐藏 / 冻结）](#7-应用管理安装--卸载--隐藏--冻结)
8. [存储管理](#8-存储管理)
9. [网络（WiFi / 以太网 / 代理 / NFC）](#9-网络wifi--以太网--代理--nfc)
10. [输入注入（触摸 / 键盘 / 滚动）](#10-输入注入触摸--键盘--滚动)
11. [Sensor 隐私控制](#11-sensor-隐私控制)
12. [系统设置读写](#12-系统设置读写)
13. [辅助功能服务](#13-辅助功能服务)
14. [OTA 升级](#14-ota-升级)
15. [日志 / BugReport](#15-日志--bugreport)
16. [USB 管理](#16-usb-管理)
17. [屏保（Dream）管理](#17-屏保dream管理)
18. [设备类型检测](#18-设备类型检测)
19. [图像工具类](#19-图像工具类)
20. [OEMConfig 限制条目解析](#20-oemconfig-限制条目解析)

---

## 1. Launcher / 桌面管理

> 文件：`SystemLib.kt`, `Syslib.kt`

### `getDefaultLauncher(context: Context): String?`

获取当前默认 Launcher 的包名。

### `getDefaultLauncherName(context: Context): String?`

获取当前默认 Launcher 的应用名称。

### `isDefaultLauncher(context: Context): Boolean`

判断调用方应用是否是当前默认 Launcher。

### `getSystemDefaultLauncher(context: Context): String?`

获取具有系统属性（FLAG_SYSTEM）的 Launcher 包名。

### `getSystemDefaultLauncher2(context: Context): ComponentName?`

获取具有系统属性的 Launcher 的 ComponentName。

### `getAllLaunchers(context: Context): MutableList<Pair<String, String>>`

获取系统中所有 Launcher，返回 `Pair(应用名, 包名)` 列表。

### `getAllLaunchers2(context: Context): MutableList<Pair<String, String>>`

同上，内部实现略有差异。

### `setDefaultLauncher(context: Context, packageName: String)`

**需要系统权限**。静默设置默认桌面（通过 `IPackageManager.replacePreferredActivity`）。

### `setDefaultLauncher(context: Context, componentName: ComponentName)`

**需要系统权限**。通过 ComponentName 静默设置默认桌面。

### `clearDefaultLauncher(context: Context, packageName: String)`

静默清除指定包名的默认桌面设置。

### `cleanDefaultLauncher(context: Context)`

清空所有 Launcher 的 preferredActivity 设置（所有已注册桌面）。

### `setHomeActivity(className: ComponentName)`

通过 `IPackageManager` 设置 Home Activity（需要系统权限）。

---

## 2. 状态栏 / 导航栏控制

> 文件：`SystemLib.kt`, `Syslib.kt`

### 状态栏控制常量

| 常量                            | 值                  | 说明              |
| ----------------------------- | ------------------ | --------------- |
| `DISABLE_NONE`                | 0x00000000         | 不禁用任何东西         |
| `DISABLE_EXPAND`              | 0x00010000         | 禁用展开状态栏         |
| `DISABLE_NOTIFICATION_ICONS`  | 0x00020000         | 禁用通知图标          |
| `DISABLE_NOTIFICATION_ALERTS` | 0x00040000         | 禁用通知提示          |
| `DISABLE_SYSTEM_INFO`         | 0x00100000         | 禁用 WiFi/电池等系统图标 |
| `DISABLE_HOME`                | 0x00200000         | 禁用并隐藏 Home 键    |
| `DISABLE_RECENT`              | 0x01000000         | 禁用并隐藏 Recent 键  |
| `DISABLE_BACK`                | 0x00400000         | 禁用并隐藏 Back 键    |
| `DISABLE_CLOCK`               | 0x00800000         | 禁用状态栏时间         |
| `STATUS_DISABLE_NAVIGATION`   | BACK\|HOME\|RECENT | 禁用整个导航栏         |

### `setStatusBarInt(context: Context, status: Int)`

通过反射调用 `StatusBarManager.disable(status)` 设置状态栏禁用项，同时自动调用 `setStatusBar2`。传 `DISABLE_NONE` 恢复全部。

### `setStatusBar2(context: Context, status: Int)`

调用 `StatusBarManager.disable2(status)`，控制快速设置面板、系统图标、通知栏等。

### `setGestural()`

**需要系统权限**。通过 `IOverlayManager` 将导航栏切换为手势导航模式。

### `set3Buttons()`

**需要系统权限**。通过 `IOverlayManager` 将导航栏切换为三按钮模式。

---

## 3. 设备信息获取

> 文件：`SystemLib.kt`

### `getSN(): String`

获取设备 SN 号。优先读取 `persist.radio.sn`，其次调用 `Build.getSerial()`。

### `getSystemVersion(): String`

获取系统版本号，优先读 `ro.product.version`，其次 `ro.build.display.id`。

### `getSystemPropertyString(key: String): String?`

读取 `getprop` 中的属性值。

### `setSystemPropertyString(key: String, value: String)`

**需要 SELinux 权限**。设置系统属性值。

### `getImeis(context: Context): Pair<String, String>`

获取双卡设备的 IMEI1 和 IMEI2，返回 `Pair(imei1, imei2)`。

### `getSubscriberId(context: Context): String`

获取 SIM 卡的 IMSI（subscriberId）。

### `getWifiMac(context: Context): String`

获取 WiFi MAC 地址。如果系统返回 `02:00:00:00:00:00` 则尝试通过 `IWifiManager` 获取真实地址。

### `getBTMac(context: Context): String`

获取蓝牙 MAC 地址。

### `getSDCard(): Triple<Long, Long, Long>`

获取外部存储（SD卡）的 `Triple(已用, 可用, 总量)` 字节数。

### `getRomMemorySize(context: Context): Triple<Long, Long, Long>`

获取 RAM 的 `Triple(已用, 可用, 总量)` 字节数。

### `getTotalRam(): Long`

获取设备总 RAM 大小（单位：GB）。

### `getBatteryCapacity(context: Context): Int`

通过反射 `PowerProfile.getBatteryCapacity()` 获取电池容量（mAh）。

### `getFactoryMacAddresses(): String`

通过 `IWifiManager` 获取出厂 WiFi MAC 地址。

### `isNetAvailable(context: Context): Boolean`

判断当前网络是否可用（具备 `NET_CAPABILITY_VALIDATED`）。

### `ping(): Int`

ping `www.baidu.com` 3次，成功返回 `200`，失败返回 `404`。

### `isRoot(): Boolean`

检测设备是否有 root 权限（尝试执行 `su` 命令）。

### `getDefaultIpAddresses(): String`

遍历所有网络接口，返回第一个可用的 IPv4 地址。

### `getDefaultIpAddresses(context: Context): String`

通过 `ConnectivityManager.getLinkProperties` 获取当前活跃网络的 IPv4 地址（Android 6+）。

### `getStorageStats(context: Context, storageUuid: UUID, packageName: String): LongArray`

获取指定应用的存储占用，返回 `LongArray(cacheBytes, appBytes, dataBytes)`。

---

## 4. 时间 / 时区

> 文件：`SystemLib.kt`

### `getNtpTime(url: String, timeout: Int, time: (Long) -> Unit)`

异步从 NTP 服务器获取时间戳（毫秒）并在主线程回调。

- `url` 默认 `ntp.aliyun.com`
- 其他可选：`ntp1.nim.ac.cn`、`edu.ntp.org.cn`

### `setTime(context: Context, time: Long)`

**需要 `SET_TIME` 权限**。通过 `AlarmManager.setTime()` 设置系统时间（Unix 时间戳，毫秒）。

### `setTimeZone(context: Context, zone: String)`

**需要 `SET_TIME` 权限**。设置系统时区，`zone` 格式示例：`Asia/Shanghai`。

### `getAllSystemZone(): Array<String>?`

获取系统支持的所有时区 ID 数组。

### `setConfiguration(language: String): Boolean`

设置系统语言（通过 `LocalePicker.updateLocale`）。`language` 支持格式：`zh`、`zh-CN`、`zh-Hant-TW`。

---

## 5. 电源管理（开关机 / 屏幕）

> 文件：`Syslib.kt`, `Android14.kt`

### `shutdown()`

**需要系统权限**。通过 `IPowerManager.shutdown()` 关机。

### `reboot()`

**需要系统权限**。通过 `IPowerManager.reboot()` 重启。

### `reset(context: Context)`

恢复出厂设置（发送 `android.intent.action.FACTORY_RESET` 广播）。

### `goToSleep()`

**需要系统权限**。通过 `IPowerManager.goToSleep()` 使设备进入休眠（息屏锁屏）。

### `goToSleep(context: Context)`

通过反射调用 `PowerManager.goToSleep()` 使设备休眠。

### `wakeUp(context: Context)`

**需要系统权限**。通过 `IPowerManager.wakeUp()` 点亮屏幕。

### `isScreenOn(): Boolean`

通过 `IPowerManager.isInteractive` 判断屏幕是否亮起。

### `uptimeMillis(): Long`

返回开机以来（不含休眠）的毫秒数。

### `elapsedRealtime(): Long`

返回开机以来（含休眠）的毫秒数。

### `lockScreen(mode: Int = 0)` *(Android14.kt)*

**需要 Android 14+**。通过 `SurfaceComposer.setPowerMode()` 只息屏不锁屏（`mode=0` 关闭，`mode=2` 开启）。

---

## 6. DPM（设备策略管理）

> 文件：`DPM.kt`
> 以下方法均需要 `DevicePolicyManager` 相关权限（Profile Owner 或 Device Owner）。

### 激活 / 注销

#### `setActiveAdmin(componentName: ComponentName)`

静默激活设备管理器（兼容 Android 12–16）。

#### `removeActiveAdmin(componentName: ComponentName)`

取消激活设备管理器。

#### `setProfileOwner(componentName: ComponentName)`

激活 Admin 后，将其设为 Profile Owner（兼容 Android 12–14）。

#### `setActiveProfileOwner(componentName: ComponentName)`

组合调用 `setActiveAdmin` + `setProfileOwner`。

#### `getProfileOwnerAsUser(): ComponentName`

获取当前设备的 Profile Owner ComponentName。

#### `clearProfileOwner(componentName: ComponentName)`

清除 Profile Owner。

#### `isAdminActive(context: Context, componentName: ComponentName): Boolean`

判断指定 ComponentName 是否已激活设备管理器。

#### `isProfileOwnerApp(context: Context, packageName: String): Boolean`

判断指定包名是否已是 Profile Owner。

#### `getProfileOwnerComponent(context: Context): String`

读取系统内置的默认 supervision Profile Owner 组件名（来自 `config.xml`）。

#### `openProfileOwner(activity: Activity, componentName: ComponentName)`

打开系统提供的 Provision Managed Profile 界面（用于引导用户授权）。

#### `setAdmin(activity: Activity, componentName: ComponentName)`

弹出设备管理员申请界面（用户手动授权）。

### 锁屏 / 密码策略

#### `lock(callerPackageName: String): Boolean`

立即锁定设备（兼容 Android 12–14）。

#### `setDisableLockScreen(context: Context, oldPassword: String, isDisable: Boolean, change: (String) -> Unit)`

禁用/启用锁屏，需要提供旧密码（只支持 PIN 和密码类型）。

#### `getCredentialType(): Int`

获取当前锁屏凭据类型：`-1`=无/滑动, `1`=图案, `3`=PIN, `4`=密码。

#### `resetPassword(context: Context, password: String, change: (String) -> Unit)`

重置锁屏密码。

#### `getPasswordQuality(context: Context, admin: ComponentName): Int`

获取当前密码强度策略值。

#### `setPasswordQuality(context: Context, admin: ComponentName, quality: Int)`

设置密码强度。`quality` 参考 `DevicePolicyManager.PASSWORD_QUALITY_*` 常量。

#### `getPasswordQualityList(context: Context): List<Pair<String, Int>>`

返回所有 `PASSWORD_QUALITY_*` 常量的名称和值列表，方便枚举。

#### `setPasswordMinimumLength(context: Context, admin: ComponentName, length: Int)`

设置密码最小长度。

#### `setPasswordExpirationTimeout(context: Context, admin: ComponentName, timeout: Long)`

设置密码过期时间（毫秒）。

#### `setMaximumTimeToLock(context: Context, admin: ComponentName, timeMs: Long)`

设置屏幕自动锁定时间（毫秒，0 表示立即）。

#### `setMaximumFailedPasswordsForWipe(admin: ComponentName, context: Context, num: Int, change: (String) -> Unit)`

设置密码最大错误次数，超出后自动恢复出厂（0 表示取消限制）。

#### `getMaximumFailedPasswordsForWipe(admin: ComponentName, context: Context): Int`

获取当前设置的密码最大错误次数。

### 功能禁用

#### `setCameraDisabled(context: Context, componentName: ComponentName, isDisable: Boolean)`

禁用/启用摄像头。

#### `getCameraDisabled(context: Context, componentName: ComponentName): Boolean`

查询摄像头是否被禁用。

#### `setScreenCaptureDisabled(context: Context, componentName: ComponentName, isDisable: Boolean)`

禁止/允许截图。

#### `getScreenCaptureDisabled(context: Context, componentName: ComponentName): Boolean`

查询截图是否被禁止。

#### `setStatusBarDisabled(context: Context, componentName: ComponentName, isDisable: Boolean)`

**需要 Android 6+**。通过 DPM 禁用/启用状态栏。

#### `disableMDM(context: Context, componentName: ComponentName, key: String, isDisable: Boolean, change: (Boolean) -> Unit)`

通过 `UserManager` 限制键禁用功能（如 `UserManager.DISALLOW_CAMERA`）。

#### `isDisableDMD(context: Context, key: String): Boolean`

查询指定 UserManager 限制键是否已生效。

### 代理设置

#### `setGlobalProxy(admin: ComponentName, host: String, exclusionList: List<String>)`

通过 `IDevicePolicyManager` 设置全局代理。`host` 格式：`127.0.0.1:8080`。

#### `clearGlobalProxy(admin: ComponentName)`

清除全局代理（传空字符串）。

#### `setRecommendedGlobalProxy(context: Context, admin: ComponentName, proxyInfo: ProxyInfo)`

设置推荐全局代理（独立于系统全局代理）。

### 其他

#### `setDelegatedScopes(context: Context, componentName: ComponentName, packageName: String)`

**需要 Android 8+**。授权指定包名禁止卸载的委托权限。

#### `setSystemUpdatePolicy(context: Context, componentName: ComponentName, policy: SystemUpdatePolicy)`

设置系统更新策略（如冻结更新）。

#### `wipeDate(context: Context)`

通过 DPM 清除外部存储数据。

#### `kiosk(context: Activity, admin: ComponentName, packages: Array<String>): Boolean`

启动 Kiosk 模式（锁定任务模式），仅允许指定包名应用运行。

#### `installCer(context: Context, admin: ComponentName, file: String, error: (String) -> Unit, success: (String) -> Unit)`

静默安装 CA 证书（`.cer` 格式）。

#### `bugreport(context: Context, admin: ComponentName)`

通过 DPM 触发系统日志收集。

---

## 7. 应用管理（安装 / 卸载 / 隐藏 / 冻结）

> 文件：`Syslib.kt`

### 安装

#### `installAPK(context: Context, apkFilePath: String, change: ((Int, String) -> Unit))`

静默安装 APK 或 XAPK。回调 `change(code, message)`：

- `0` 成功
- `-1` 参数/初始化错误
- `-2` 文件复制失败
- `-3` 提交失败
- `-4` 安装失败（设备端可查看）
- `-5` XAPK 相关错误

### 卸载

#### `uninstall(context: Context, packageName: String)`

静默卸载应用（通过 `PackageInstaller.uninstall`）。

### 隐藏 / 禁用

#### `hiddenAPP(packageName: String, isHidden: Boolean)`

**需要系统权限**。隐藏应用（图标不可见，`pm list packages` 也不可见）。

> 注意：可能导致系统 app 丢失，不建议对系统应用使用。

#### `isHiddenAPP(packageName: String): Boolean`

查询应用是否被隐藏。

#### `disableApp(context: Context, componentName: ComponentName, isDisable: Boolean)`

禁用/启用组件（图标不显示，`pm list packages` 可见）。

#### `disableAppUser(context: Context, componentName: ComponentName, isDisable: Boolean)`

以用户维度禁用/恢复默认状态（`COMPONENT_ENABLED_STATE_DISABLED_USER`）。

#### `setDisableAPP(context: Context, packageName: ComponentName, isDisable: Boolean)`

启用/禁用组件（逻辑与 `disableApp` 相同，参数语义一致）。

#### `isDisableAPP(context: Context, packageName: ComponentName): Boolean`

查询组件是否处于 `DISABLED` 状态。

### 冻结 / 暂停

#### `suspendedAPP(packageName: String, isHidden: Boolean)`

**需要系统权限**。暂停应用（图标变灰，不可使用），兼容 Android 12–16。

#### `isSuspendedAPP(packageName: String): Boolean`

查询应用是否处于暂停状态。

#### `isSuspended(context: Context, packageName: String): Boolean`

通过 `ApplicationInfo.FLAG_SUSPENDED` 标志位判断是否暂停。

### 卸载保护

#### `disUninstallAPP(packageName: String, isDisable: Boolean)`

**需要系统权限**。禁止/允许卸载指定应用（通过 `IPackageManager.setBlockUninstallForUser`）。

#### `isDisUninstallAPP(packageName: String): Boolean`

查询应用是否被禁止卸载。

### 应用信息

#### `isSystemAPP(context: Context, packageName: String): Boolean`

判断是否是系统应用（`FLAG_SYSTEM`）。

#### `canUninstall(context: Context, packageName: String): Boolean`

判断应用是否可以卸载（`FLAG_INSTALLED`）。

#### `getPkgList(): MutableList<String>`

通过执行 `pm list packages` 命令获取所有包名。

#### `getAllPackages(): List<String>`

通过 `IPackageManager.allPackages` 获取所有包名。

#### `getPackageDataDir(packageName: String): String`

获取指定应用的数据目录路径。

#### `isFirstBoot(): Boolean`

判断设备是否刚刷机或恢复出厂设置。

#### `canBackup(context: Context, packageName: String): Boolean`

判断应用是否允许备份（`FLAG_ALLOW_BACKUP`）。

#### `grant(context: Context, packageName: String, permName: String)`

**需要系统权限**。通过 `IPackageManager.grantRuntimePermission` 静默授予运行时权限。

### 电池优化

#### `isPowerSaveWhitelistApp(packageName: String): Boolean`

判断应用是否在电池优化白名单中。

#### `getPowerSaveWhitelistApp(): List<String>`

获取当前电池优化白名单应用列表。

### 任务管理

#### `removeTask(taskId: Int): Boolean`

通过 `IActivityManager.removeTask()` 结束指定任务。

#### `amTask(taskId: Int)`

通过 `IActivityTaskManager.removeTask()` 结束指定任务。

### 分屏 / 多屏

#### `enterSplitScreen(context: Context, component1: ComponentName, component2: ComponentName)`

启动分屏显示，`component1` 在上/左，`component2` 在下/右。

#### `enterMultiScreen(context: Context, component1: ComponentName)`

将指定 Activity 启动到第二块屏幕（displayId=0 为第一块）。

---

## 8. 存储管理

> 文件：`Syslib.kt`

### `unmount(context: Context)`

卸载所有可移动存储设备。

### `getStorage(context: Context, onChange: (list: List<String>) -> Unit)`

获取所有 USB 存储设备的路径列表，并打印容量信息。

### `registerStorageListener(onChange: ((String?, String?, Int?, Int?) -> Unit))`

注册存储设备变化监听器，回调参数：`(volumeId, path, type, state)`。

### `unregisterStorageListener()`

注销存储设备变化监听器。

### `getVolumes(): List<Triple<String, Int, Int>>`

获取所有存储卷信息，返回 `List<Triple(id, type, state)>`。

### `mount(id: String)`

挂载指定存储卷（通过卷 ID）。

### `unmount(id: String)`

卸载指定存储卷（通过卷 ID）。

#### 存储卷状态常量

| 常量                       | 值   | 说明   |
| ------------------------ | --- | ---- |
| `VOLUME_TYPE_PUBLIC`     | 0   | 公共存储 |
| `VOLUME_STATE_UNMOUNTED` | 0   | 未挂载  |
| `VOLUME_STATE_MOUNTED`   | 2   | 已挂载  |

### `clearApplicationUserData(packageName: String, onChange: ((String, Boolean) -> Unit))`

清除指定应用的全部数据，回调 `(packageName, success)`。

---

## 9. 网络（WiFi / 以太网 / 代理 / NFC）

> 文件：`Syslib.kt`, `SystemLib.kt`

### WiFi

#### `removeWifiConfig(context: Context, ssid: String): Boolean`

根据 SSID 移除 WiFi 配置。

#### `removeWifiConfig(context: Context, config: WifiConfiguration): Boolean`

根据 WifiConfiguration 对象移除 WiFi 配置。

#### `addWifiConfig(context: Context, config: WifiConfiguration): Int`

添加 WiFi 配置，返回 networkId（-1 失败）。

#### `addWifi(context: Context, ssid: String, pass: String)` *(Android 10+)*

创建仅对当前 App 生效的 WiFi 连接（不写入系统配置）。

#### `getPrivilegedConfiguredNetworks(context: Context): List<WifiConfiguration>`

**需要系统权限**。通过 `IWifiManager` 获取所有已保存的 WiFi 配置（含密码）。

### 热点

#### `setHotSpotDisabled(isDisable: Boolean): Boolean`

禁用个人热点（调用 `IWifiManager.stopSoftAp()`）。启用请传 `false`（当前实现只支持关闭）。

#### `isHotSpotDisabled(): Boolean`

查询个人热点是否已关闭（`wifiApEnabledState == 13` 为关闭）。

### 以太网

#### `disableEthernet(disable: Boolean, isSupported: (Boolean) -> Unit)`

禁用/启用以太网，兼容 Android 12–13。`isSupported` 回调是否支持该操作。

#### `addEthernetListener(change: ((String, Boolean) -> Unit))`

注册以太网接口状态变化监听，回调 `(interfaceName, isAvailable)`。

#### `removeEthernetListener()`

注销以太网监听。

#### `isAvailable(face: String): Boolean`

查询指定以太网接口是否可用。

#### `hasEthernetInterface(): Boolean`

判断当前设备的 `IEthernetManager` 是否支持禁用控制接口。

#### `hasEthernetListenerInterface(): Boolean`

判断当前设备的 `IEthernetManager` 是否支持添加监听接口。

### 代理

#### `setHttpProxy(context: Context, host: String)`

设置 HTTP 代理（写入 Settings.Global `http_proxy`，格式 `host:port`）。

#### `setGlobalProxy(context: Context, proxyInfo: ProxyInfo)`

设置全局代理（写入 Settings.Global 多个字段）。支持 PAC 文件 URL。

### NFC

#### `enableNFC()`

启用 NFC（兼容 Android 12–15）。

#### `disableNFC()`

禁用 NFC（兼容 Android 12–15）。

### USB 数据

#### `setUSBDataDisabled(context: Context, isDisable: Boolean)`

禁用/启用 USB 数据传输（关闭 ADB 并写入 vendor 属性）。

#### `isUSBDataDisabled(context: Context): Boolean`

查询 USB 数据传输是否被禁用。

### ADB

#### `getAdbWirelessPort(): Int`

获取无线 ADB 端口（默认 5555）。需要 Android 12+。

#### `allowWirelessDebugging(alwaysAllow: Boolean, ssid: String)` *(Android12.kt)*

允许指定 SSID 的网络进行无线调试。

#### `startAdbd()` *(Android12.kt)*

启动 ADB 守护进程（设置系统属性 `ctl.start=adbd`）。

#### `stopAdbd()` *(Android12.kt)*

停止 ADB 守护进程（设置系统属性 `ctl.stop=adbd`）。

---

## 10. 输入注入（触摸 / 键盘 / 滚动）

> 文件：`InputLib.kt`
> **需要系统权限**，无系统权限可改用无障碍服务实现。

### `injectInit()`

初始化输入注入，必须在其他 inject 方法前调用（获取 `IInputManager` 实例）。

### `injectMotionEvent(action: Int, x: Float, y: Float)`

在子线程注入触摸事件（调用 `injectMotionEvent2`）。

- `action`：`MotionEvent.ACTION_DOWN` / `ACTION_UP` / `ACTION_MOVE`

### `injectMotionEvent2(action: Int, x: Float, y: Float)`

直接注入触摸事件（同步，使用 `IInputManager.injectInputEvent`，source=TOUCHSCREEN）。

### `injectScrollEvent(x: Float, y: Float, deltaY: Float)`

注入滚动事件（source=MOUSE，axis=VSCROLL）。

### `injectKeyEvent(action: Int, key: String, code: Int)`

根据 JS keycode 注入键盘事件（经过 `JS_KEYCODE_TO_ANDROID` 映射表转换）。

### `injectKeyEvent(keyCode: Int)`

根据 Android `KeyEvent.KEYCODE_*` 直接注入按键事件（使用 `Instrumentation.sendKeyDownUpSync`）。

### `injectEvent(inputEvent: InputEvent)`

注入任意 `InputEvent` 对象（通用接口）。

---

## 11. Sensor 隐私控制

> 文件：`Syslib.kt`（统一入口）, `Android12.kt`, `Android13.kt`
> 仅支持 Android 12–13（API 31–33）。

### `disableSensor(isDisable: Boolean, sensor: Int)`

禁用/启用指定传感器，自动选择 API 版本兼容实现。

- `sensor = 1`：麦克风
- `sensor = 2`：相机
- `sensor = 3`：其他传感器

### `addIndividualSensorPrivacyListener(change: (Boolean) -> Unit)` *(Android13.kt)*

注册传感器隐私变化监听（Android 13 专用）。

### `removeIndividualSensorPrivacyListener()` *(Android13.kt)*

注销传感器隐私变化监听。

---

## 12. 系统设置读写

> 文件：`Syslib.kt`, `X.kt`

### `getSystemGlobal(context: Context, key: String): String`

读取 `Settings.Global` 中的字符串值。

### `setSystemGlobal(context: Context, key: String, value: String): Boolean`

写入 `Settings.Global` 中的字符串值。

### `getAllSettings(context: Context): List<Pair<String, String>>`

枚举 `Settings.Global`、`Settings.System`、`Settings.Secure` 中所有可读的键值对。

### `getSettings(context: Context, uri: Uri): Pair<String, String>?`

通过 URI 查询单条设置，返回 `Pair(name, value)`。

### `getUriFor(context: Context, uri: Uri): List<Pair<String, String>>`

通过 URI 查询多条设置，返回 `List<Pair(name, value)>`。

### `putSettings(context: Context, uri: Uri, name: String, value: String)`

更新指定设置条目（update 操作）。

### `insertSettings(context: Context, uri: Uri, name: String, value: String)`

插入一条新设置条目（insert 操作）。

### `putAllSettings(context: Context, uri: Uri, list: List<Pair<String, String>>)`

批量更新设置条目（`applyBatch` 操作）。

---

## 13. 辅助功能服务

> 文件：`Syslib.kt`

### `enableAccessibilityService(context: Context, packageName: String, accessibilityService: String)`

**需要 `WRITE_SECURE_SETTINGS` 权限**。启用指定辅助功能服务（追加到已启用列表）。

### `disableAccessibilityService(context: Context, packageName: String, accessibilityService: String)`

**需要 `WRITE_SECURE_SETTINGS` 权限**。从已启用列表中移除指定辅助功能服务。

### `enabledAccessibilityServices(context: Context, enable: Boolean)`

**需要 `WRITE_SECURE_SETTINGS` 权限**。启用/禁用当前应用的辅助服务总开关。

---

## 14. OTA 升级

> 文件：`Syslib.kt`

### `ota(file: File, onStatusUpdate: ((Int, Float) -> Unit), onErrorCode: ((Int) -> Unit))`

**需要 A/B 分区设备**。通过 `UpdateEngine` 执行本地 OTA 升级包。

- `file`：OTA zip 文件（本地路径）
- `onStatusUpdate(status, percent)`：升级进度回调，`status` 见下方常量
- `onErrorCode(errorCode)`：完成回调，`errorCode=0` 为成功

### `getUpdateStatus(status: Int): String`

将 `UpdateEngine.UpdateStatusConstants` 状态码转为中文说明字符串。

### `getUpdateError(errorCode: Int): String`

将 `UpdateEngine.ErrorCodeConstants` 错误码转为中文说明字符串。

---

## 15. 日志 / BugReport

> 文件：`Syslib.kt`

### `bugreport()`

通过 `IActivityManager.requestBugReport(0)` 触发系统日志收集，完成后在通知栏生成分享入口。输出路径：`/data/user_de/0/com.android.shell/files/bugreports`。

### `bugReportManager(context: Context, onFinished: (String) -> Unit)`

**需要 `DUMP` 权限**（Android 11+）。通过 `BugreportManager` 捕获日志，输出到应用外部文件目录，完成后回调文件路径。

---

## 16. USB 管理

> 文件：`Syslib.kt`

### `disableUsbPermissionDialogs(device: UsbDevice, packageName: String?, uid: Int, error: (String) -> Unit)`

通过 `IUsbManager` 静默授予 USB 设备权限，并绑定默认处理 App，不再弹出权限确认对话框。

- `packageName = null` 表示清除绑定

---

## 17. 屏保（Dream）管理

> 文件：`Android12.kt`

### `getDreamPackage(context: Context)`

获取并打印所有已安装的屏保 App（实现了 `DreamService.SERVICE_INTERFACE` 的服务）。

### `dream()`

启动屏保（如当前已在屏保中则不操作）。

### `setDefaultDreamTime(context: Context, time: Long)`

通过反射调用 `PowerManager.dream(time)` 延迟启动屏保。

### `testDream(componentName: ComponentName)`

通过指定 ComponentName 启动测试屏保（调试用）。

### `getDreamComponents(): ComponentName?`

获取当前已设置的屏保 ComponentName。

### `setDreamComponents(componentName: ComponentName)`

设置屏保的 ComponentName。

### `grantNotificationListenerAccessGranted12(serviceComponent: ComponentName)` *(Android12.kt)*

**需要系统权限**。静默授予指定组件通知监听权限。

---

## 18. 设备类型检测

> 文件：`DeviceUtils.kt`

### `isTelevision(context: Context): Boolean`

判断设备是否为 Android TV（具有 `android.software.leanback` feature）。

### `isWear(context: Context): Boolean`

判断设备是否为 Android Wear（具有 `android.hardware.type.watch` feature）。

### `isAuto(context: Context): Boolean`

判断设备是否为 Android Auto（具有 `android.hardware.type.automotive` feature）。

---

## 19. 图像工具类

> 文件：`X.kt`

### `drawable2ByteArray(icon: Drawable): ByteArray`

将 Drawable 转换为 PNG ByteArray（先转 Bitmap，再压缩为 PNG，缩放至 72x72）。

### `bitmap2ByteArray(bitmap: Bitmap): ByteArray`

将 Bitmap 压缩为 PNG ByteArray。

### `byteArray2Drawable(byteArray: ByteArray): Drawable?`

将 ByteArray 反序列化为 BitmapDrawable。

### `byteArray2Bitmap(byteArray: ByteArray): Bitmap?`

将 ByteArray 解码为 Bitmap。

### `drawable2Bitmap(icon: Drawable): Bitmap`

将 Drawable 转换为 72x72 Bitmap。

### `Bitmap.thumbnail(): Bitmap`

Bitmap 扩展方法，将 Bitmap 缩放为 72x72。

### `File.toBitmap(maxWidth: Int, maxHeight: Int): Bitmap?`

从文件路径按指定最大尺寸加载并缩放 Bitmap（自动计算 inSampleSize 防止 OOM）。

### `write2File(name: String, content: String, append: Boolean)`

写入字符串到文件（`append=true` 追加，`false` 覆盖）。

### `copyDir(src: String, des: String)`

递归遍历并打印目录结构（当前实现仅做遍历，无实际复制）。

### `Activity.toast(message: String)`

在 Activity 中快速显示 Toast。

---

## 20. OEMConfig 限制条目解析

> 文件：`oemconfig.kt`

### `loadRestrictionElement(context: Context, xml: XmlResourceParser): RestrictionEntry?`

从 XML 中解析一个 `<restriction>` 标签，返回 `RestrictionEntry`。支持类型：

- `TYPE_STRING`、`TYPE_CHOICE`、`TYPE_INTEGER`
- `TYPE_MULTI_SELECT`、`TYPE_BOOLEAN`
- `TYPE_BUNDLE`、`TYPE_BUNDLE_ARRAY`（递归解析子节点）

---

## 附录：相机监听（Android13.kt）

### `cameraListener(onStatusChanged: (Int, String) -> Unit, onTorchStatusChanged: (Int, String) -> Unit)`

通过 `ICameraService.addListener()` 监听摄像头状态变化和手电筒状态变化。

---

## 权限说明

| 功能类别               | 所需权限/条件                                    |
| ------------------ | ------------------------------------------ |
| DPM 相关操作           | Profile Owner 或 Device Owner               |
| 状态栏禁用              | `android.permission.STATUS_BAR` 或系统签名      |
| 静默安装/卸载            | `android.permission.INSTALL_PACKAGES`      |
| 输入注入               | `android.permission.INJECT_EVENTS`（系统签名）   |
| Settings.Global 写入 | `android.permission.WRITE_SECURE_SETTINGS` |
| 开关机/重启             | `android.permission.REBOOT` + 系统签名         |
| 系统属性读写             | 系统签名 + SELinux 规则                          |
| 电池优化白名单            | 系统签名                                       |
| 应用隐藏/冻结            | 系统签名                                       |
| OTA 升级             | `android.permission.OTA_UPDATE` + A/B 分区   |
