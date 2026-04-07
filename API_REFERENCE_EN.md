# SystemFunction API Reference

> Package: `com.android.systemlib`
> Most APIs require system signature or Device Owner / Profile Owner privileges.

---

## Table of Contents

1. [Launcher Management](#1-launcher-management)
2. [Status Bar / Navigation Bar](#2-status-bar--navigation-bar)
3. [Device Information](#3-device-information)
4. [Time / Timezone](#4-time--timezone)
5. [Power Management](#5-power-management)
6. [DPM (Device Policy Management)](#6-dpm-device-policy-management)
7. [App Management](#7-app-management)
8. [Storage Management](#8-storage-management)
9. [Network (WiFi / Ethernet / Proxy / NFC)](#9-network-wifi--ethernet--proxy--nfc)
10. [Input Injection](#10-input-injection)
11. [Sensor Privacy](#11-sensor-privacy)
12. [System Settings](#12-system-settings)
13. [Accessibility Services](#13-accessibility-services)
14. [OTA Update](#14-ota-update)
15. [Bug Report / Logging](#15-bug-report--logging)
16. [USB Management](#16-usb-management)
17. [Dream (Screensaver)](#17-dream-screensaver)
18. [Device Type Detection](#18-device-type-detection)
19. [Image Utilities](#19-image-utilities)
20. [AppOps / Permission Management](#20-appops--permission-management)
21. [OEMConfig Restriction Parsing](#21-oemconfig-restriction-parsing)

---

## 1. Launcher Management

> Files: `SystemLib.kt`, `Syslib.kt`

### `getDefaultLauncher(context: Context): String?`

Returns the package name of the current default launcher.

### `getDefaultLauncherName(context: Context): String?`

Returns the application label of the current default launcher.

### `isDefaultLauncher(context: Context): Boolean`

Returns `true` if the calling application is the current default launcher.

### `getSystemDefaultLauncher(context: Context): String?`

Returns the package name of the launcher that has the `FLAG_SYSTEM` flag set.

### `getSystemDefaultLauncher2(context: Context): ComponentName?`

Returns the `ComponentName` of the system-flagged launcher.

### `getAllLaunchers(context: Context): MutableList<Pair<String, String>>`

Returns a list of all installed launchers as `Pair(appLabel, packageName)`.

### `getAllLaunchers2(context: Context): MutableList<Pair<String, String>>`

Same as above with a slightly different internal implementation.

### `setDefaultLauncher(context: Context, packageName: String)`

> **Requires**: system signature / `IPackageManager.replacePreferredActivity`

Silently sets the default launcher by package name.

### `setDefaultLauncher(context: Context, componentName: ComponentName)`

> **Requires**: system signature

Silently sets the default launcher by `ComponentName`.

### `clearDefaultLauncher(context: Context, packageName: String)`

Silently clears the preferred activity setting for the given package.

### `cleanDefaultLauncher(context: Context)`

Clears all preferred activity settings for every registered launcher.

### `setHomeActivity(className: ComponentName)`

> **Requires**: system signature

Sets the Home Activity via `IPackageManager`.

---

## 2. Status Bar / Navigation Bar

> Files: `SystemLib.kt`, `Syslib.kt`

### Disable Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `DISABLE_NONE` | 0x00000000 | Re-enable everything |
| `DISABLE_EXPAND` | 0x00010000 | Prevent status bar from expanding |
| `DISABLE_NOTIFICATION_ICONS` | 0x00020000 | Hide notification icons |
| `DISABLE_NOTIFICATION_ALERTS` | 0x00040000 | Suppress notification alerts |
| `DISABLE_SYSTEM_INFO` | 0x00100000 | Hide WiFi/battery system icons |
| `DISABLE_HOME` | 0x00200000 | Disable and hide Home button |
| `DISABLE_RECENT` | 0x01000000 | Disable and hide Recents button |
| `DISABLE_BACK` | 0x00400000 | Disable and hide Back button |
| `DISABLE_CLOCK` | 0x00800000 | Hide status bar clock |
| `STATUS_DISABLE_NAVIGATION` | BACK\|HOME\|RECENT | Disable entire navigation bar |

### `setStatusBarInt(context: Context, status: Int)`

> **Requires**: `android.permission.STATUS_BAR` or system signature

Calls `StatusBarManager.disable(status)` via reflection and automatically calls `setStatusBar2`. Pass `DISABLE_NONE` to restore everything.

### `setStatusBar2(context: Context, status: Int)`

Calls `StatusBarManager.disable2(status)` to control quick settings panel, system icons, and notification shade.

### `setGestural()`

> **Requires**: system signature / `IOverlayManager`

Switches the navigation bar to gesture navigation mode.

### `set3Buttons()`

> **Requires**: system signature / `IOverlayManager`

Switches the navigation bar to 3-button mode.

---

## 3. Device Information

> File: `SystemLib.kt`

### `getSN(): String`

Returns the device serial number. Reads `persist.radio.sn` first, falls back to `Build.getSerial()`.

### `getSystemVersion(): String`

Returns the system version string. Reads `ro.product.version` first, falls back to `ro.build.display.id`.

### `getSystemPropertyString(key: String): String?`

Reads a system property value (equivalent to `getprop <key>`).

### `setSystemPropertyString(key: String, value: String)`

> **Requires**: system signature + SELinux policy

Sets a system property value.

### `getImeis(context: Context): Pair<String, String>`

> **Requires**: `READ_PHONE_STATE`

Returns IMEI1 and IMEI2 for dual-SIM devices as `Pair(imei1, imei2)`.

### `getSubscriberId(context: Context): String`

> **Requires**: `READ_PHONE_STATE`

Returns the IMSI (subscriber ID) of the SIM card.

### `getWifiMac(context: Context): String`

Returns the Wi-Fi MAC address. If the system returns `02:00:00:00:00:00`, attempts to retrieve the real address via `IWifiManager`.

### `getBTMac(context: Context): String`

Returns the Bluetooth MAC address.

### `getSDCard(): Triple<Long, Long, Long>`

Returns external storage (SD card) usage as `Triple(used, available, total)` in bytes.

### `getRomMemorySize(context: Context): Triple<Long, Long, Long>`

Returns RAM usage as `Triple(used, available, total)` in bytes.

### `getTotalRam(): Long`

Returns total RAM size in GB.

### `getBatteryCapacity(context: Context): Int`

Returns battery capacity in mAh via reflection on `PowerProfile.getBatteryCapacity()`.

### `getFactoryMacAddresses(): String`

Returns the factory Wi-Fi MAC address via `IWifiManager`.

### `isNetAvailable(context: Context): Boolean`

Returns `true` if the active network has `NET_CAPABILITY_VALIDATED`.

### `ping(): Int`

Pings `www.baidu.com` 3 times. Returns `200` on success, `404` on failure.

### `isRoot(): Boolean`

Returns `true` if the device has root access (attempts to run `su`).

### `getDefaultIpAddresses(): String`

Iterates all network interfaces and returns the first available IPv4 address.

### `getDefaultIpAddresses(context: Context): String`

Returns the IPv4 address of the active network via `ConnectivityManager.getLinkProperties` (Android 6+).

### `getStorageStats(context: Context, storageUuid: UUID, packageName: String): LongArray`

> **Min API**: 26

Returns app storage usage as `LongArray(cacheBytes, appBytes, dataBytes)`.

---

## 4. Time / Timezone

> File: `SystemLib.kt`

### `getNtpTime(url: String, timeout: Int, time: (Long) -> Unit)`

Asynchronously fetches a timestamp (milliseconds) from an NTP server and delivers it on the main thread.

- Default `url`: `ntp.aliyun.com`
- Alternatives: `ntp1.nim.ac.cn`, `edu.ntp.org.cn`

### `setTime(context: Context, time: Long)`

> **Requires**: `SET_TIME` permission

Sets the system time via `AlarmManager.setTime()`. `time` is a Unix timestamp in milliseconds.

### `setTimeZone(context: Context, zone: String)`

> **Requires**: `SET_TIME` permission

Sets the system timezone. Example `zone`: `Asia/Shanghai`.

### `getAllSystemZone(): Array<String>?`

Returns an array of all timezone IDs supported by the system.

### `setConfiguration(language: String): Boolean`

Sets the system language via `LocalePicker.updateLocale`. Supported `language` formats: `zh`, `zh-CN`, `zh-Hant-TW`.

---

## 5. Power Management

> Files: `Syslib.kt`, `Android14.kt`

### `shutdown()`

> **Requires**: system signature / `IPowerManager`

Powers off the device via `IPowerManager.shutdown()`.

### `reboot()`

> **Requires**: system signature / `IPowerManager`

Reboots the device via `IPowerManager.reboot()`.

### `reset(context: Context)`

Triggers a factory reset by broadcasting `android.intent.action.FACTORY_RESET`.

### `goToSleep()`

> **Requires**: system signature / `IPowerManager`

Puts the device to sleep (screen off + lock) via `IPowerManager.goToSleep()`.

### `goToSleep(context: Context)`

Puts the device to sleep via reflection on `PowerManager.goToSleep()`.

### `wakeUp(context: Context)`

> **Requires**: system signature / `IPowerManager`

Wakes up the screen via `IPowerManager.wakeUp()`.

### `isScreenOn(): Boolean`

Returns `true` if the screen is on, via `IPowerManager.isInteractive`.

### `uptimeMillis(): Long`

Returns milliseconds since boot, excluding time spent in deep sleep.

### `elapsedRealtime(): Long`

Returns milliseconds since boot, including time spent in deep sleep.

### `lockScreen(mode: Int = 0)` *(Android14.kt)*

> **Min API**: 34  
> **Note**: Turns off the screen without triggering the lock screen. `mode=0` turns off, `mode=2` turns on.

Controls display power state via `SurfaceComposer.setPowerMode()`.

---

## 6. DPM (Device Policy Management)

> File: `DPM.kt`  
> All methods require `DevicePolicyManager` privileges (Profile Owner or Device Owner).

### Activation / Deactivation

#### `setActiveAdmin(componentName: ComponentName)`

> **Compat**: API 31–36 (Android 16 adds a 4th `callerPackageName` parameter — handled automatically)

Silently activates the device admin.

#### `removeActiveAdmin(componentName: ComponentName)`

Deactivates the device admin.

#### `setProfileOwner(componentName: ComponentName)`

> **Compat**: API 31–36 (Android 14+ reduces parameters from 3 to 2 — handled automatically)

Sets the component as Profile Owner after activating it as admin.

#### `setActiveProfileOwner(componentName: ComponentName)`

Convenience method combining `setActiveAdmin` + `setProfileOwner`.

#### `getProfileOwnerAsUser(): ComponentName`

Returns the Profile Owner `ComponentName` for the current user.

#### `clearProfileOwner(componentName: ComponentName)`

Clears the Profile Owner.

#### `isAdminActive(context: Context, componentName: ComponentName): Boolean`

Returns `true` if the given component is an active device admin.

#### `isProfileOwnerApp(context: Context, packageName: String): Boolean`

Returns `true` if the given package is the Profile Owner.

#### `getProfileOwnerComponent(context: Context): String`

Reads the default supervision Profile Owner component name from system `config.xml`.

#### `openProfileOwner(activity: Activity, componentName: ComponentName)`

Opens the system's Provision Managed Profile UI to guide the user through authorization.

#### `setAdmin(activity: Activity, componentName: ComponentName)`

Shows the system device admin request dialog for manual user authorization.

### Lock Screen / Password Policy

#### `lock(callerPackageName: String): Boolean`

> **Compat**: API 31–33 uses 2-param `lockNow`; API 34+ uses 3-param version with caller package — handled automatically

Immediately locks the device. Returns `true` on success.

#### `setDisableLockScreen(context: Context, oldPassword: String, isDisable: Boolean, change: (String) -> Unit)`

Enables or disables the lock screen. Requires the current password (PIN or password type only).

#### `getCredentialType(): Int`

> **Min API**: 30

Returns the current lock screen credential type: `-1`=none/swipe, `1`=pattern, `3`=PIN, `4`=password.

#### `resetPassword(context: Context, password: String, change: (String) -> Unit)`

Resets the lock screen password.

#### `getPasswordQuality(context: Context, admin: ComponentName): Int`

Returns the current password quality policy value.

#### `setPasswordQuality(context: Context, admin: ComponentName, quality: Int)`

Sets the password quality policy. `quality` refers to `DevicePolicyManager.PASSWORD_QUALITY_*` constants.

#### `getPasswordQualityList(context: Context): List<Pair<String, Int>>`

Returns all `PASSWORD_QUALITY_*` constant names and values for enumeration.

#### `setPasswordMinimumLength(context: Context, admin: ComponentName, length: Int)`

Sets the minimum password length.

#### `setPasswordExpirationTimeout(context: Context, admin: ComponentName, timeout: Long)`

Sets the password expiration timeout in milliseconds.

#### `setMaximumTimeToLock(context: Context, admin: ComponentName, timeMs: Long)`

Sets the screen auto-lock timeout in milliseconds. `0` means lock immediately.

#### `setMaximumFailedPasswordsForWipe(admin: ComponentName, context: Context, num: Int, change: (String) -> Unit)`

Sets the maximum number of failed password attempts before wiping the device. `0` disables this policy.

#### `getMaximumFailedPasswordsForWipe(admin: ComponentName, context: Context): Int`

Returns the current maximum failed password attempts setting.

### Feature Restrictions

#### `setCameraDisabled(context: Context, componentName: ComponentName, isDisable: Boolean)`

Enables or disables the camera.

#### `getCameraDisabled(context: Context, componentName: ComponentName): Boolean`

Returns `true` if the camera is disabled.

#### `setScreenCaptureDisabled(context: Context, componentName: ComponentName, isDisable: Boolean)`

Enables or disables screen capture.

#### `getScreenCaptureDisabled(context: Context, componentName: ComponentName): Boolean`

Returns `true` if screen capture is disabled.

#### `setStatusBarDisabled(context: Context, componentName: ComponentName, isDisable: Boolean)`

> **Min API**: 23

Enables or disables the status bar via DPM.

#### `disableMDM(context: Context, componentName: ComponentName, key: String, isDisable: Boolean, change: (Boolean) -> Unit)`

Disables a feature using a `UserManager` restriction key (e.g. `UserManager.DISALLOW_CAMERA`).

#### `isDisableDMD(context: Context, key: String): Boolean`

Returns `true` if the given `UserManager` restriction key is active.

### Proxy

#### `setGlobalProxy(admin: ComponentName, host: String, exclusionList: List<String>)`

Sets a global proxy via `IDevicePolicyManager`. `host` format: `127.0.0.1:8080`.

#### `clearGlobalProxy(admin: ComponentName)`

Clears the global proxy.

#### `setRecommendedGlobalProxy(context: Context, admin: ComponentName, proxyInfo: ProxyInfo)`

Sets a recommended global proxy (independent of the system global proxy).

### Miscellaneous

#### `setDelegatedScopes(context: Context, componentName: ComponentName, packageName: String)`

> **Min API**: 26

Grants uninstall-block delegate permission to the given package.

#### `setSystemUpdatePolicy(context: Context, componentName: ComponentName, policy: SystemUpdatePolicy)`

Sets the system update policy (e.g. freeze updates).

#### `wipeDate(context: Context)`

Clears external storage data via DPM.

#### `kiosk(context: Activity, admin: ComponentName, packages: Array<String>): Boolean`

Starts kiosk mode (lock task mode), restricting the device to the specified packages.

#### `installCer(context: Context, admin: ComponentName, file: String, error: (String) -> Unit, success: (String) -> Unit)`

Silently installs a CA certificate (`.cer` format).

#### `bugreport(context: Context, admin: ComponentName)`

Triggers system log collection via DPM.

---

## 7. App Management

> File: `Syslib.kt`

### Installation

#### `installAPK(context: Context, apkFilePath: String, change: ((Int, String) -> Unit))`

> **Requires**: `INSTALL_PACKAGES`

Silently installs an APK or XAPK. Callback `change(code, message)`:

| Code | Meaning |
|------|---------|
| `0` | Success |
| `-1` | Parameter / initialization error |
| `-2` | File copy failed |
| `-3` | Commit failed |
| `-4` | Installation failed (check device logs) |
| `-5` | XAPK-related error |

### Uninstallation

#### `uninstall(context: Context, packageName: String)`

> **Requires**: `DELETE_PACKAGES`

Silently uninstalls an application via `PackageInstaller.uninstall`.

### Hide / Disable

#### `hiddenAPP(packageName: String, isHidden: Boolean)`

> **Requires**: system signature  
> **Warning**: Can cause system apps to disappear. Do not use on system packages.

Hides an application (invisible in launcher and `pm list packages`).

#### `isHiddenAPP(packageName: String): Boolean`

Returns `true` if the application is hidden.

#### `disableApp(context: Context, componentName: ComponentName, isDisable: Boolean)`

Disables or enables a component (hidden from launcher, but visible in `pm list packages`).

#### `disableAppUser(context: Context, componentName: ComponentName, isDisable: Boolean)`

Disables a component at user level (`COMPONENT_ENABLED_STATE_DISABLED_USER`).

#### `setDisableAPP(context: Context, packageName: ComponentName, isDisable: Boolean)`

Enables or disables a component (same as `disableApp`).

#### `isDisableAPP(context: Context, packageName: ComponentName): Boolean`

Returns `true` if the component is in `DISABLED` state.

### Freeze / Suspend

#### `suspendedAPP(packageName: String, isHidden: Boolean)`

> **Requires**: system signature  
> **Compat**: API 31–36 (parameter count differs per version — handled automatically)

Suspends an application (icon grayed out, app unusable).

#### `isSuspendedAPP(packageName: String): Boolean`

Returns `true` if the application is suspended.

#### `isSuspended(context: Context, packageName: String): Boolean`

Returns `true` if `ApplicationInfo.FLAG_SUSPENDED` is set for the application.

### Uninstall Protection

#### `disUninstallAPP(packageName: String, isDisable: Boolean)`

> **Requires**: system signature

Blocks or unblocks uninstallation of the given package via `IPackageManager.setBlockUninstallForUser`.

#### `isDisUninstallAPP(packageName: String): Boolean`

Returns `true` if uninstallation of the package is blocked.

### App Information

#### `isSystemAPP(context: Context, packageName: String): Boolean`

Returns `true` if the application has the `FLAG_SYSTEM` flag.

#### `canUninstall(context: Context, packageName: String): Boolean`

Returns `true` if the application is user-installed (`FLAG_INSTALLED`).

#### `getPkgList(): MutableList<String>`

Returns all package names by running `pm list packages`.

#### `getAllPackages(): List<String>`

Returns all package names via `IPackageManager.allPackages`.

#### `getPackageDataDir(packageName: String): String`

Returns the data directory path of the given application.

#### `isFirstBoot(): Boolean`

Returns `true` if the device has just been flashed or factory-reset.

#### `canBackup(context: Context, packageName: String): Boolean`

Returns `true` if the application allows backup (`FLAG_ALLOW_BACKUP`).

#### `grant(context: Context, packageName: String, permName: String)`

> **Requires**: system signature

Silently grants a runtime permission via `IPackageManager.grantRuntimePermission`.

### Battery Optimization

#### `isPowerSaveWhitelistApp(packageName: String): Boolean`

Returns `true` if the application is on the battery optimization whitelist.

#### `getPowerSaveWhitelistApp(): List<String>`

Returns the current battery optimization whitelist.

### Task Management

#### `removeTask(taskId: Int): Boolean`

Removes the specified task via `IActivityManager.removeTask()`.

#### `amTask(taskId: Int)`

Removes the specified task via `IActivityTaskManager.removeTask()`.

### Split Screen / Multi-Display

#### `enterSplitScreen(context: Context, component1: ComponentName, component2: ComponentName)`

Launches two activities in split-screen mode. `component1` appears on top/left.

#### `enterMultiScreen(context: Context, component1: ComponentName)`

Launches the specified activity on the secondary display.

---

## 8. Storage Management

> File: `Syslib.kt`

### `unmount(context: Context)`

Unmounts all removable storage devices.

### `getStorage(context: Context, onChange: (list: List<String>) -> Unit)`

Returns a list of paths for all connected USB storage devices.

### `registerStorageListener(onChange: ((String?, String?, Int?, Int?) -> Unit))`

Registers a storage device change listener. Callback parameters: `(volumeId, path, type, state)`.

### `unregisterStorageListener()`

Unregisters the storage device change listener.

### `getVolumes(): List<Triple<String, Int, Int>>`

Returns all storage volumes as `List<Triple(id, type, state)>`.

### `mount(id: String)`

Mounts the storage volume with the given volume ID.

### `unmount(id: String)`

Unmounts the storage volume with the given volume ID.

#### Volume State Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `VOLUME_TYPE_PUBLIC` | 0 | Public storage |
| `VOLUME_STATE_UNMOUNTED` | 0 | Not mounted |
| `VOLUME_STATE_MOUNTED` | 2 | Mounted |

### `clearApplicationUserData(packageName: String, onChange: ((String, Boolean) -> Unit))`

Clears all data for the given application. Callback: `(packageName, success)`.

---

## 9. Network (WiFi / Ethernet / Proxy / NFC)

> Files: `Syslib.kt`, `SystemLib.kt`

### WiFi

#### `removeWifiConfig(context: Context, ssid: String): Boolean`

Removes a saved Wi-Fi configuration by SSID.

#### `removeWifiConfig(context: Context, config: WifiConfiguration): Boolean`

Removes a saved Wi-Fi configuration by `WifiConfiguration` object.

#### `addWifiConfig(context: Context, config: WifiConfiguration): Int`

Adds a Wi-Fi configuration. Returns `networkId` (`-1` on failure).

#### `addWifi(context: Context, ssid: String, pass: String)`

> **Min API**: 29

Creates a Wi-Fi connection scoped to the current app (does not persist to system settings).

#### `getPrivilegedConfiguredNetworks(context: Context): List<WifiConfiguration>`

> **Requires**: system signature

Returns all saved Wi-Fi configurations including passwords via `IWifiManager`.

### Hotspot

#### `setHotSpotDisabled(isDisable: Boolean): Boolean`

Disables the personal hotspot via `IWifiManager.stopSoftAp()`. Enabling is not currently supported.

#### `isHotSpotDisabled(): Boolean`

Returns `true` if the personal hotspot is disabled (`wifiApEnabledState == 13`).

### Ethernet

#### `disableEthernet(disable: Boolean, isSupported: (Boolean) -> Unit)`

> **Compat**: API 31–32 uses `Trackstop/Trackstart`; API 33+ uses `setEthernetEnabled`

Enables or disables Ethernet. `isSupported` callback indicates whether the operation is supported on this device.

#### `addEthernetListener(change: ((String, Boolean) -> Unit))`

Registers an Ethernet interface state change listener. Callback: `(interfaceName, isAvailable)`.

#### `removeEthernetListener()`

Unregisters the Ethernet listener.

#### `isAvailable(face: String): Boolean`

Returns `true` if the specified Ethernet interface is available.

#### `hasEthernetInterface(): Boolean`

Returns `true` if the device's `IEthernetManager` supports the disable control interface.

#### `hasEthernetListenerInterface(): Boolean`

Returns `true` if the device's `IEthernetManager` supports the listener interface.

### Proxy

#### `setHttpProxy(context: Context, host: String)`

Sets the HTTP proxy by writing `http_proxy` to `Settings.Global`. Format: `host:port`.

#### `setGlobalProxy(context: Context, proxyInfo: ProxyInfo)`

Sets the global proxy by writing multiple fields to `Settings.Global`. Supports PAC file URLs.

### NFC

#### `enableNFC()`

> **Compat**: API 31–34 uses `INfcAdapter`; API 35+ uses `NfcAdapter` reflection

Enables NFC.

#### `disableNFC()`

> **Compat**: same as above

Disables NFC.

### USB Data

#### `setUSBDataDisabled(context: Context, isDisable: Boolean)`

Enables or disables USB data transfer (disables ADB and writes to vendor properties).

#### `isUSBDataDisabled(context: Context): Boolean`

Returns `true` if USB data transfer is disabled.

### ADB

#### `getAdbWirelessPort(): Int`

> **Min API**: 31

Returns the wireless ADB port (default: 5555).

#### `allowWirelessDebugging(alwaysAllow: Boolean, ssid: String)` *(Android12.kt)*

> **Min API**: 31

Allows wireless debugging on the specified SSID network.

#### `startAdbd()` *(Android12.kt)*

Starts the ADB daemon by setting system property `ctl.start=adbd`. Requires SELinux permission.

#### `stopAdbd()` *(Android12.kt)*

Stops the ADB daemon by setting system property `ctl.stop=adbd`.

---

## 10. Input Injection

> File: `InputLib.kt`  
> **Requires**: `INJECT_EVENTS` (system signature). Alternatively, use Accessibility Service without system signature.

### `injectInit()`

Initializes input injection. Must be called before any other inject method to obtain the `IInputManager` instance.

### `injectMotionEvent(action: Int, x: Float, y: Float)`

Injects a touch event on a background thread. `action`: `MotionEvent.ACTION_DOWN` / `ACTION_UP` / `ACTION_MOVE`.

### `injectMotionEvent2(action: Int, x: Float, y: Float)`

Injects a touch event synchronously (source=TOUCHSCREEN).

### `injectScrollEvent(x: Float, y: Float, deltaY: Float)`

Injects a scroll event (source=MOUSE, axis=VSCROLL).

### `injectKeyEvent(action: Int, key: String, code: Int)`

Injects a keyboard event using a JS keycode, converted via the `JS_KEYCODE_TO_ANDROID` mapping table.

### `injectKeyEvent(keyCode: Int)`

Injects a key press using an Android `KeyEvent.KEYCODE_*` value via `Instrumentation.sendKeyDownUpSync`.

### `injectEvent(inputEvent: InputEvent)`

Injects any `InputEvent` object (generic interface).

---

## 11. Sensor Privacy

> Files: `Syslib.kt` (unified entry), `Android12.kt`, `Android13.kt`  
> **Supported only on Android 12–13 (API 31–33)**. This interface was removed in API 34+.

### `disableSensor(isDisable: Boolean, sensor: Int)`

Toggles sensor privacy for the specified sensor. Automatically selects the correct API version implementation.

- `sensor = 1`: Microphone
- `sensor = 2`: Camera
- `sensor = 3`: Other sensors

### `addIndividualSensorPrivacyListener(change: (Boolean) -> Unit)` *(Android13.kt)*

> **Min API**: 33

Registers a sensor privacy change listener.

### `removeIndividualSensorPrivacyListener()` *(Android13.kt)*

Unregisters the sensor privacy change listener.

---

## 12. System Settings

> Files: `Syslib.kt`, `X.kt`

### `getSystemGlobal(context: Context, key: String): String`

Reads a value from `Settings.Global`.

### `setSystemGlobal(context: Context, key: String, value: String): Boolean`

> **Requires**: `WRITE_SECURE_SETTINGS`

Writes a value to `Settings.Global`.

### `getAllSettings(context: Context): List<Pair<String, String>>`

Enumerates all readable key-value pairs from `Settings.Global`, `Settings.System`, and `Settings.Secure`.

### `getSettings(context: Context, uri: Uri): Pair<String, String>?`

Queries a single setting by URI. Returns `Pair(name, value)`.

### `getUriFor(context: Context, uri: Uri): List<Pair<String, String>>`

Queries multiple settings by URI. Returns `List<Pair(name, value)>`.

### `putSettings(context: Context, uri: Uri, name: String, value: String)`

Updates a setting entry (update operation).

### `insertSettings(context: Context, uri: Uri, name: String, value: String)`

Inserts a new setting entry (insert operation).

### `putAllSettings(context: Context, uri: Uri, list: List<Pair<String, String>>)`

Batch-updates setting entries (`applyBatch` operation).

---

## 13. Accessibility Services

> File: `Syslib.kt`

### `enableAccessibilityService(context: Context, packageName: String, accessibilityService: String)`

> **Requires**: `WRITE_SECURE_SETTINGS`

Enables the specified accessibility service (appends to the enabled list).

### `disableAccessibilityService(context: Context, packageName: String, accessibilityService: String)`

> **Requires**: `WRITE_SECURE_SETTINGS`

Removes the specified accessibility service from the enabled list.

### `enabledAccessibilityServices(context: Context, enable: Boolean)`

> **Requires**: `WRITE_SECURE_SETTINGS`

Enables or disables the calling application's accessibility service master switch.

---

## 14. OTA Update

> File: `Syslib.kt`  
> **Requires**: A/B partition device (Virtual A/B also supported).

### `ota(file: File, onStatusUpdate: ((Int, Float) -> Unit), onErrorCode: ((Int) -> Unit))`

Applies a local OTA update package via `UpdateEngine`.

- `file`: Local OTA zip file path
- `onStatusUpdate(status, percent)`: Progress callback
- `onErrorCode(errorCode)`: Completion callback. `errorCode=0` means success.

### `getUpdateStatus(status: Int): String`

Converts an `UpdateEngine.UpdateStatusConstants` value to a human-readable string.

### `getUpdateError(errorCode: Int): String`

Converts an `UpdateEngine.ErrorCodeConstants` value to a human-readable string.

---

## 15. Bug Report / Logging

> File: `Syslib.kt`

### `bugreport()`

Triggers system log collection via `IActivityManager.requestBugReport(0)`. A share entry appears in the notification shade when complete. Output path: `/data/user_de/0/com.android.shell/files/bugreports`.

### `bugReportManager(context: Context, onFinished: (String) -> Unit)`

> **Requires**: `DUMP` permission  
> **Min API**: 30

Captures a bug report via `BugreportManager`, outputs to the app's external files directory, and delivers the file path in the callback.

---

## 16. USB Management

> File: `Syslib.kt`

### `disableUsbPermissionDialogs(device: UsbDevice, packageName: String?, uid: Int, error: (String) -> Unit)`

Silently grants USB device permission via `IUsbManager` and binds the given app as the default handler, suppressing the permission dialog.

- `packageName = null` clears the binding.

---

## 17. Dream (Screensaver)

> File: `Android12.kt`

### `getDreamPackage(context: Context)`

Prints all installed screensaver apps (services implementing `DreamService.SERVICE_INTERFACE`).

### `dream()`

Starts the screensaver. No-op if the screensaver is already active.

### `setDefaultDreamTime(context: Context, time: Long)`

Starts the screensaver after a delay via reflection on `PowerManager.dream(time)`.

### `testDream(componentName: ComponentName)`

Starts a specific screensaver by `ComponentName` (for debugging).

### `getDreamComponents(): ComponentName?`

Returns the currently configured screensaver `ComponentName` (first item in the list).

### `setDreamComponents(componentName: ComponentName)`

Sets the screensaver `ComponentName`.

### `grantNotificationListenerAccessGranted12(serviceComponent: ComponentName)` *(Android12.kt)*

> **Requires**: system signature

Silently grants notification listener access to the specified component.

---

## 18. Device Type Detection

> File: `DeviceUtils.kt`

### `isTelevision(context: Context): Boolean`

Returns `true` if the device is an Android TV (has `android.software.leanback` feature).

### `isWear(context: Context): Boolean`

Returns `true` if the device is a Wear OS device (has `android.hardware.type.watch` feature).

### `isAuto(context: Context): Boolean`

Returns `true` if the device is an Android Auto device (has `android.hardware.type.automotive` feature).

---

## 19. Image Utilities

> File: `X.kt`

### `drawable2ByteArray(icon: Drawable): ByteArray`

Converts a `Drawable` to a PNG `ByteArray` (scaled to 72x72 before compression).

### `bitmap2ByteArray(bitmap: Bitmap): ByteArray`

Compresses a `Bitmap` to a PNG `ByteArray`.

### `byteArray2Drawable(byteArray: ByteArray): Drawable?`

Deserializes a `ByteArray` to a `BitmapDrawable`.

### `byteArray2Bitmap(byteArray: ByteArray): Bitmap?`

Decodes a `ByteArray` to a `Bitmap`.

### `drawable2Bitmap(icon: Drawable): Bitmap`

Converts a `Drawable` to a 72x72 `Bitmap`.

### `Bitmap.thumbnail(): Bitmap`

Extension function. Scales a `Bitmap` to 72x72.

### `File.toBitmap(maxWidth: Int, maxHeight: Int): Bitmap?`

Loads and scales a `Bitmap` from a file path to fit within the specified dimensions. Automatically calculates `inSampleSize` to avoid OOM.

### `write2File(name: String, content: String, append: Boolean)`

Writes a string to a file. `append=true` appends; `append=false` overwrites.

### `copyDir(src: String, des: String)`

> **Known limitation**: Currently only traverses and prints the directory tree. **Does not actually copy files.** Do not use for file copy operations.

### `Activity.toast(message: String)`

Shows a `Toast` from an `Activity`.

---

## 20. AppOps / Permission Management

> File: `Oops.kt`

### AppOps Mode Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `MODE_ALLOWED` | 0 | Access allowed |
| `MODE_IGNORED` | 1 | Access denied, no crash |
| `MODE_ERRORED` | 2 | Access denied, throws exception |
| `MODE_DEFAULT` | 3 | Determined by caller |
| `MODE_FOREGROUND` | 4 | Allowed only in foreground |

### Battery Optimization Mode Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `MODE_UNRESTRICTED` | 1 | Unrestricted (whitelist) |
| `MODE_OPTIMIZED` | 2 | Optimized (default) |
| `MODE_RESTRICTED` | 3 | Restricted background |

### `setMode(context: Context, code: Int, packageName: String, mode: Int)`

> **Requires**: system signature / `IAppOpsService`

Sets the AppOps mode for the given package and operation code. Calls both `setMode` and `setUidMode`.

- `code`: AppOps operation code (see Appendix for the full table)

### `checkUsageStatsViaReflection(context: Context, opCode: Int, targetPackageName: String): Boolean`

Verifies the result of `setMode` by calling `AppOpsManager.checkOp` via reflection. Returns `true` if the result is `MODE_ALLOWED`.

### `grantNotificationListenerAccessGranted(context: Context, packageName: String)`

> **Requires**: system signature  
> **Compat**: API 30 and below use 2-param interface; API 31+ use 3-param interface — handled automatically

Finds notification listener services in the given package and silently grants access.

### `grantPermission(context: Context, packageName: String, permission: String)`

> **Requires**: system signature

Silently grants a single runtime (dangerous) permission to the given package.

### `grantPermission(context: Context, packageName: String)`

> **Requires**: system signature

Silently grants all requested runtime (dangerous) permissions to the given package.

### `getBatteryOptimization(context: Context, packageName: String): Int`

Returns the battery optimization state for the given package: `MODE_UNRESTRICTED`, `MODE_OPTIMIZED`, or `MODE_RESTRICTED`.

### `setBatteryOptimization(context: Context, packageName: String, mode: Int)`

> **Min API**: 23  
> **Requires**: system signature / `IDeviceIdleController`

Sets the battery optimization state for the given package:
- `MODE_UNRESTRICTED`: Add to whitelist, no background restrictions
- `MODE_OPTIMIZED`: Remove from whitelist, allow background
- `MODE_RESTRICTED`: Remove from whitelist, restrict background

### `getOpCode()`

Dumps all `AppOpsManager` operation codes supported on the current device to Logcat (tag: `OpCodeDumper`).

### `dumpAppOps(): String`

Exports the full AppOps operation code table for the current device (APP_OP_ constant names, integer codes, OPSTR_ strings) to `/sdcard/ops.txt` and returns the content as a string.

---

## 21. OEMConfig Restriction Parsing

> File: `oemconfig.kt`

### `loadRestrictionElement(context: Context, xml: XmlResourceParser): RestrictionEntry?`

Parses a single `<restriction>` element from XML, returning a `RestrictionEntry`. Supported types:

- `TYPE_STRING`, `TYPE_CHOICE`, `TYPE_INTEGER`
- `TYPE_MULTI_SELECT`, `TYPE_BOOLEAN`
- `TYPE_BUNDLE`, `TYPE_BUNDLE_ARRAY` (recursively parses child nodes)

---

## Appendix: Camera Listener (Android13.kt)

### `cameraListener(onStatusChanged: (Int, String) -> Unit, onTorchStatusChanged: (Int, String) -> Unit)`

> **Min API**: 33

Registers a camera status and torch status listener via `ICameraService.addListener()`.

---

## Appendix: AppOps Operation Code Table (Android 14 / API 34)

The full table is available in `SystemLib/src/main/java/com/android/systemlib/ops.txt`.

Call `dumpAppOps()` at runtime to generate the complete table for the current device's Android version.

---

## Permission Summary

| Feature | Required Permission / Condition |
|---------|---------------------------------|
| DPM operations | Profile Owner or Device Owner |
| Status bar disable | `android.permission.STATUS_BAR` or system signature |
| Silent install/uninstall | `android.permission.INSTALL_PACKAGES` |
| Input injection | `android.permission.INJECT_EVENTS` (system signature) |
| Settings.Global write | `android.permission.WRITE_SECURE_SETTINGS` |
| Shutdown/reboot | `android.permission.REBOOT` + system signature |
| System property read/write | System signature + SELinux policy |
| Battery optimization whitelist | System signature |
| App hide/suspend | System signature |
| OTA update | `android.permission.OTA_UPDATE` + A/B partition |
| AppOps mode set | System signature / `IAppOpsService` |
| Notification listener grant | System signature / `INotificationManager` |
| Battery optimization set | System signature / `IDeviceIdleController` |
