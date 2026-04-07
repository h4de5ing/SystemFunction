# Android System Function Library

A Kotlin library that wraps commonly used hidden system APIs for Android system applications. It provides a unified, version-aware interface for operations that require platform signature or system privileges.

> **Requires**: platform signature (`platform.jks`) + `android.uid.system` shared user ID

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
