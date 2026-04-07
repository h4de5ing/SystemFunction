# SystemFunction Source Code

This directory contains the full source code for the Android System Function Library — a production-grade Kotlin library that provides a unified, version-aware API surface for system-privileged Android applications.

## Architecture

```
SystemFunction_src/
├── SystemLib/      # Core library — main entry point for all consumers
├── Android12/      # API 31–32 specific implementations
├── Android13/      # API 33 specific implementations
├── Android14/      # API 34 specific implementations
├── Android15/      # API 35 specific implementations
├── Android16/      # API 36 specific implementations
└── t/              # Internal test/scratch module
```

**Dependency graph:**

```
Your App
    └── systemlib (SystemLib module)
            ├── android12
            ├── android13
            ├── android14
            ├── android15
            └── android16
```

`SystemLib` is the unified API layer. It exposes a single, stable set of entry points and internally dispatches to the correct version-specific module at runtime based on `Build.VERSION.SDK_INT`. Callers never need to interact with `Android12`–`Android16` directly.

## Android Version Compatibility

| Module | API Level | Android Version |
|--------|-----------|-----------------|
| Android12 | 31, 32 | Android 12, 12L |
| Android13 | 33 | Android 13 |
| Android14 | 34 | Android 14 |
| Android15 | 35 | Android 15 |
| Android16 | 36 | Android 16 |

The core `SystemLib` module sets `minSdk = 21` and `compileSdk = 36`.

## Source Files (SystemLib)

| File | Responsibility |
|------|---------------|
| `SystemLib.kt` | Launcher, status bar, device info, time/timezone, system properties |
| `Syslib.kt` | App management, network, storage, power, OTA, accessibility, USB |
| `DPM.kt` | Device Policy Manager — admin activation, lock screen, kiosk, certificates |
| `InputLib.kt` | Input event injection (touch, scroll, keyboard) |
| `Oops.kt` | AppOps mode control, runtime permission grants, battery optimization |
| `DeviceUtils.kt` | Device type detection (TV, Wear, Auto) |
| `X.kt` | Image conversion utilities, system settings read/write |
| `NtpClient.kt` | NTP time synchronization |
| `oemconfig.kt` | OEMConfig restriction XML parser |
| `Logger.java` | Internal logging utility |

## Version-Specific Implementation Notes

### Android12 (API 31–32)
- `disableSensor12` — sensor privacy via `ISensorPrivacyManager.setIndividualSensorPrivacy`
- `disableEthernet12` — ethernet control via `IEthernetManager.Trackstop/Trackstart`
- Screensaver (Dream) management — `IDreamManager`
- Lock screen disable — `ILockSettings` + `LockPatternUtils`
- Wireless debugging — `IAdbManager.allowWirelessDebugging`
- Notification listener grant — `INotificationManager.setNotificationListenerAccessGrantedForUser`

### Android13 (API 33)
- `disableSensor13` — sensor privacy via `ISensorPrivacyManager.setToggleSensorPrivacy`
- `disableEthernet13` — ethernet control via `IEthernetManager.setEthernetEnabled`
- `cameraListener` — camera and torch status monitoring via `ICameraService`
- `setLock` — device lock via `IDevicePolicyManager.lockNow` (2-param)

### Android14 (API 34)
- `setProfileOwner14` — 2-param variant (parameter count changed from API 33)
- `setLock14` — 3-param `lockNow` with `callerPackageName`
- `lockScreen` — screen-off-only (no lock) via `SurfaceComposer.setPowerMode`

### Android15 (API 35)
- `setPackagesSuspendedAsUser15` — 9-param variant (2 new params vs API 34)
- `enableNFC15` / `disableNFC15` — `INfcAdapter` removed; uses `NfcAdapter` reflection

### Android16 (API 36)
- `setActiveAdmin16` — 4-param variant with `callerPackageName`
- `setPackagesSuspendedAsUser16` — same 9-param signature as Android 15

## Building

### Requirements
- Android Studio Ladybug or later
- JDK 22
- Android SDK with API 36 installed
- System signature keystore (`platform.jks`) for running on device

### Build the library

```bash
./gradlew :SystemLib:assembleRelease
```

### Publish to local Maven repository

Publishes all AAR artifacts to `../SystemLib_repository`:

```bash
./gradlew :SystemLib:publishReleasePublicationToMavenRepository
./gradlew :Android12:publishReleasePublicationToMavenRepository
./gradlew :Android13:publishReleasePublicationToMavenRepository
./gradlew :Android14:publishReleasePublicationToMavenRepository
./gradlew :Android15:publishReleasePublicationToMavenRepository
./gradlew :Android16:publishReleasePublicationToMavenRepository
```

## Developer Notes

**System signature is mandatory.** All APIs that call into `IPackageManager`, `IDevicePolicyManager`, `IPowerManager`, `IInputManager`, and similar AIDL services require the calling app to hold system UID (`android.uid.system`) and be signed with the platform key.

**AIDL interface stability.** These libraries call `@UnsupportedAppUsage`-annotated and AIDL hidden interfaces. The per-version modules exist precisely because these interfaces change between major Android releases. Always verify behavior after targeting a new API level.

**DPM prerequisite.** All `DPM.kt` methods require the app to be activated as a Device Admin or Profile Owner first. For unattended activation, configure the default supervision component in the ROM's `config.xml`:

```xml
<!-- frameworks/base/core/res/res/values/config.xml -->
<string name="config_defaultSupervisionProfileOwnerComponent" translatable="false">
    com.your.package/com.your.package.AdminReceiver
</string>
```

AOSP source references:
- `frameworks/base/core/java/android/app/admin/DevicePolicyManager.java`
- `frameworks/base/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java`

**Known limitation.** `copyDir()` in `X.kt` currently traverses the directory tree and prints paths only — it does not copy files. Do not use it for file copy operations.

## Full API Reference

See [API_REFERENCE_EN.md](../API_REFERENCE_EN.md) for the complete English API reference, or [API_REFERENCE.md](../API_REFERENCE.md) for the Chinese version.
