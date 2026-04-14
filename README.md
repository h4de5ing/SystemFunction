# Android System Function Library

A production-grade Kotlin library that wraps commonly used hidden system APIs for Android system applications. It provides a unified, version-aware interface for operations that require platform signature or system privileges, with built-in compatibility coverage from Android 12 through Android 16 (API 31–36).

> **Requires**: platform signature (`platform.jks`) + `android.uid.system` shared user ID

## Who Is This For?

This library is designed for teams building privileged Android applications where standard SDK APIs are insufficient:

- **Android OEM / ODM engineers** building system apps bundled with custom ROMs
- **Enterprise device management developers** implementing MDM/EMM solutions or kiosk systems
- **System app developers** needing silent installs, device policy control, or hardware-level access
- **AOSP customization teams** extending platform behavior through privileged apps rather than framework changes

## Supported Scenarios

- Silently install, uninstall, hide, freeze, or restrict apps without user interaction
- Control device lock screen, password policy, and kiosk mode via DPM
- Toggle NFC, Ethernet, sensor privacy switches, and USB data at the system level
- Inject touch, scroll, and keyboard events programmatically
- Push OTA updates on A/B partition devices via `UpdateEngine`
- Read and write `Settings.Global/System/Secure` and system properties
- Manage WiFi profiles (including credentials) and configure network proxies
- Get real device identifiers (IMEI, SN, factory MAC) that are hidden from normal apps
- Grant or revoke runtime permissions and AppOps modes for any installed package

## Non-Goals and Security Caveats

- **Not for consumer apps.** Every API in this library requires system UID and platform signature. Standard Play Store apps cannot use it.
- **Not a root solution.** This library works within the AOSP permission model — it does not exploit vulnerabilities or bypass SELinux. You need a manufacturer-provided `platform.jks` or a custom ROM build.
- **Hidden API stability is not guaranteed.** Interfaces marked `@UnsupportedAppUsage` can be removed or changed by Google at any major Android release. The per-version compatibility modules in this library exist to manage that churn, but new Android versions may require new stubs.
- **DPM operations are irreversible in some cases.** Factory wipe (`wipeDate`), max failed password attempts, and kiosk lock task mode can render a device unusable if called incorrectly. Test on non-production hardware first.
- **Sensor privacy APIs (Android 12–13 only).** The `ISensorPrivacyManager` interface used for mic/camera kill switch was removed in Android 14. Do not use `disableSensor()` on API 34+.

## Feature Modules

| Module                      | Description                                                                                                           |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Launcher                    | Get/set default launcher, list all launchers                                                                          |
| Status Bar / Navigation Bar | Disable expand, icons, clock, navigation buttons; switch gesture/3-button mode                                        |
| Device Info                 | SN, IMEI, MAC, RAM, battery capacity, IP address, root detection                                                      |
| Time / Timezone             | NTP sync, set system time/timezone, change system language                                                            |
| Power Management            | Shutdown, reboot, factory reset, screen on/off, sleep/wake                                                            |
| DPM (Device Policy)         | Activate admin, set Profile/Device Owner, lock screen, password policy, camera/screenshot disable, kiosk mode         |
| App Management              | Silent install/uninstall (APK & XAPK), hide, freeze/suspend, block uninstall, grant permissions, battery optimization |
| Storage                     | Mount/unmount volumes, list USB storage, clear app data                                                               |
| Network                     | WiFi config, hotspot control, Ethernet enable/disable, HTTP/global proxy, NFC enable/disable                          |
| Input Injection             | Inject touch, scroll, and key events via `IInputManager`                                                              |
| Sensor Privacy              | Toggle microphone/camera kill switch (Android 12–13)                                                                  |
| System Settings             | Read/write `Settings.Global/System/Secure`                                                                            |
| Accessibility               | Enable/disable accessibility services                                                                                 |
| OTA Update                  | A/B partition OTA via `UpdateEngine` with progress callback                                                           |
| Bug Report                  | Trigger system bug report via `IActivityManager` or `BugreportManager`                                                |
| USB Management              | Grant USB device permission silently, bind default handler app                                                        |
| Dream (Screensaver)         | Get/set screensaver component, start screensaver                                                                      |
| Device Type Detection       | Detect TV, Wear OS, Android Auto                                                                                      |
| Image Utilities             | Convert between `Drawable`, `Bitmap`, and `ByteArray`                                                                 |
| AppOps / Permissions        | Set/check `AppOpsManager` modes, grant runtime permissions, manage battery optimization                               |
| OEMConfig                   | Parse OEMConfig restriction XML entries                                                                               |

## Android Version Compatibility

| Feature                                 | API 31 (12)                  | API 32 (12L) | API 33 (13)                | API 34 (14) | API 35 (15) | API 36 (16) |
| --------------------------------------- |:----------------------------:|:------------:|:--------------------------:|:-----------:|:-----------:|:-----------:|
| Sensor Privacy (mic/camera toggle)      | Yes                          | Yes          | Yes                        | -           | -           | -           |
| Ethernet enable/disable                 | Yes (`Trackstop/Trackstart`) | Yes          | Yes (`setEthernetEnabled`) | Yes         | Yes         | Yes         |
| Lock screen (screen-off only, no lock)  | -                            | -            | -                          | Yes         | Yes         | Yes         |
| `setProfileOwner` (4-param)             | -                            | -            | -                          | Yes         | Yes         | Yes         |
| `setActiveAdmin` (4-param)              | -                            | -            | -                          | -           | -           | Yes         |
| `setPackagesSuspendedAsUser` (9-param)  | -                            | -            | -                          | -           | Yes         | Yes         |
| NFC via `INfcAdapter`                   | Yes                          | Yes          | Yes                        | Yes         | -           | -           |
| NFC via `NfcAdapter` reflection         | -                            | -            | -                          | -           | Yes         | Yes         |
| Wireless debugging (`IAdbManager`)      | Yes                          | Yes          | Yes                        | Yes         | Yes         | Yes         |
| `lockNow` (2-param)                     | Yes                          | Yes          | Yes                        | -           | -           | -           |
| `lockNow` (3-param, with callerPackage) | -                            | -            | -                          | Yes         | Yes         | Yes         |

## Repository Structure

```
SystemFunction/
├── SystemFunction_src/     # Full source code of the library and all version-specific modules
│   ├── SystemLib/          # Core library (main entry point, unified API surface)
│   ├── Android12/          # API 31–32 compatibility implementations
│   ├── Android13/          # API 33 compatibility implementations
│   ├── Android14/          # API 34 compatibility implementations
│   ├── Android15/          # API 35 compatibility implementations
│   └── Android16/          # API 36 compatibility implementations
├── SystemLib_repository/   # Pre-built Maven repository (AAR artifacts, ready to consume)
├── API_REFERENCE.md        # Full API reference (Chinese)
└── API_REFERENCE_EN.md     # Full API reference (English)
```

### SystemLib_repository — Pre-built Maven Artifacts

The `SystemLib_repository` directory is a local Maven repository. It contains pre-compiled AAR artifacts for all modules so you can consume the library without building from source.

Available artifacts (all versioned `1.0-<date>`):

| Artifact | Group ID | Description |
|----------|----------|-------------|
| `systemlib` | `com.android.systemlib` | Core library — main dependency for most projects |
| `android12` | `com.android.android12` | API 31–32 compatibility layer |
| `android13` | `com.android.android13` | API 33 compatibility layer |
| `android14` | `com.android.android14` | API 34 compatibility layer |
| `android15` | `com.android.android15` | API 35 compatibility layer |
| `android16` | `com.android.android16` | API 36 compatibility layer |
| `hideapi` | `com.android.hideapi` | Hidden API stub jar |
| `mdmsdk` | `com.android.mdmsdk` | MDM SDK |

> **Typical usage**: add `systemlib` only. It already bundles all version-specific modules internally.

### SystemFunction_src — Source Code

The source project is a standard Android multi-module Gradle project. To build and publish locally:

```bash
cd SystemFunction_src
./gradlew :SystemLib:publishReleasePublicationToMavenRepository
```

This publishes the updated AARs back into `SystemLib_repository`.

## Quick Start by Feature

A selection of common call patterns. All snippets assume your app is signed with the platform key and declares `android.uid.system`.

**Silent APK install with progress callback**
```kotlin
installAPK(context, "/sdcard/app.apk") { code, message ->
    when (code) {
        0  -> Log.i(TAG, "Install succeeded")
        -4 -> Log.e(TAG, "Install failed: $message")
    }
}
```

**Set default launcher silently**
```kotlin
setDefaultLauncher(context, "com.example.launcher")
```

**Lock down the status bar (hide everything)**
```kotlin
setStatusBarInt(context, STATUS_DISABLE_NAVIGATION or DISABLE_EXPAND or DISABLE_NOTIFICATION_ICONS)
// Restore
setStatusBarInt(context, DISABLE_NONE)
```

**Freeze / suspend an app**
```kotlin
suspendedAPP("com.example.app", true)   // suspend
suspendedAPP("com.example.app", false)  // restore
```

**Grant a runtime permission silently**
```kotlin
grant(context, "com.example.app", android.Manifest.permission.CAMERA)
// or grant all requested permissions at once
grantPermission(context, "com.example.app")
```

**Set AppOps mode (e.g. allow usage stats access)**
```kotlin
// OP code 43 = APP_OP_GET_USAGE_STATS
setMode(context, 43, "com.example.app", MODE_ALLOWED)
```

**Set system time from NTP**
```kotlin
getNtpTime("ntp.aliyun.com", 3000) { timestamp ->
    setTime(context, timestamp)
}
```

**DPM — activate admin and enter kiosk mode**
```kotlin
val admin = ComponentName(packageName, "$packageName.AdminReceiver")
setActiveProfileOwner(admin)
kiosk(activity, admin, arrayOf("com.example.kiosk"))
```

**Inject a touch event**
```kotlin
injectInit()
injectMotionEvent(MotionEvent.ACTION_DOWN, 540f, 960f)
injectMotionEvent(MotionEvent.ACTION_UP,   540f, 960f)
```

**OTA update on A/B device**
```kotlin
val otaFile = File("/sdcard/update.zip")
ota(otaFile,
    onStatusUpdate = { status, percent ->
        Log.i(TAG, "${getUpdateStatus(status)} — ${(percent * 100).toInt()}%")
    },
    onErrorCode = { code ->
        if (code == 0) Log.i(TAG, "OTA complete, rebooting…")
        else Log.e(TAG, "OTA failed: ${getUpdateError(code)}")
    }
)
```

**Disable Ethernet**
```kotlin
disableEthernet(true) { supported ->
    if (!supported) Log.w(TAG, "This device does not support Ethernet disable")
}
```

**Get battery optimization state**
```kotlin
when (getBatteryOptimization(context, "com.example.app")) {
    MODE_UNRESTRICTED -> // whitelist — no restrictions
    MODE_OPTIMIZED    -> // default Android behavior
    MODE_RESTRICTED   -> // background heavily restricted
}
```

## Quick Start

### Prerequisites

- Platform signature keystore (`platform.jks`) from the device manufacturer
- Your app must be signed with the platform key
- Your app must declare `android.uid.system` as shared user ID

### 1. Add repository to `settings.gradle`

```groovy
dependencyResolutionManagement {
    repositories {
        maven { url 'https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository' }
        // Mirror for users in China
        maven { url 'https://gitee.com/lex1992/system-function/raw/master/SystemLib_repository' }
    }
}
```

### 2. Add dependency to `build.gradle`

```groovy
dependencies {
    implementation 'com.android.systemlib:systemlib:1.0-20221223'
}
```

### 3. Configure signing in `build.gradle`

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

### 4. Set shared user ID in `AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    android:sharedUserId="android.uid.system">
    ...
</manifest>
```

### 5. Add ProGuard rule

```proguard
-keep class com.android.systemlib.** { *; }
```

## Developer Notes

**System signature**: You must obtain `platform.jks` from your device manufacturer. The APK must be signed with the platform key and installed on the system partition (or pushed via `adb install` on eng/userdebug builds).

**API stability**: Most APIs in this library use `@UnsupportedAppUsage`-annotated or AIDL hidden interfaces. They may change between Android versions. Always test after upgrading the target Android version.

**Version-specific modules**: `Android12.kt` through `Android16.kt` contain implementations that differ per API level. The unified entry point in `Syslib.kt` / `SystemLib.kt` dispatches to the correct implementation automatically.

**Known limitation**: `copyDir()` in `X.kt` currently only traverses the directory tree and prints paths — it does not actually copy files. Do not rely on it for file copy operations.

## Issue Reporting

When filing an issue, include:

- Android version and API level (e.g. Android 14 / API 34)
- Device model and ROM variant (if relevant)
- Complete logcat output
- Minimal reproducible code

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes with a descriptive message
4. Push and open a Pull Request

## License

[Apache License 2.0](LICENSE)
