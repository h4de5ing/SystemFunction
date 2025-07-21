# System Function Implementation Demo

## Overview
- Requires system permissions + system signature  
- Apps calling SystemLib must be declared as system apps with system signature to work properly
- Architecture: App dependency → SystemLib (abstraction layer) → (Android5~Android14 implementation)

## SystemLib Module Detailed Features

### 1. Device Policy Management (DPM.kt)
- Device admin activation/deactivation
- Profile owner configuration
- Device management features (disable camera, block screenshots, password policies, etc.)
- Application management (hide/pause apps)
- Network proxy settings
- Certificate installation

> **Note**: Interfaces in DPM.kt require setActiveProfileOwner activation to work properly. Refer to [official documentation](https://developer.android.com/guide/topics/admin/device-admin)

### AOSP DPM Default Configuration
```
frameworks\base\core\res\res\values\config.xml
<string name="config_defaultSupervisionProfileOwnerComponent" translatable="false">com.android.systemfunction/com.android.systemfunction.AdminReceiver</string>
```

### AOSP Source Code
```
./base/core/java/android/app/admin/DevicePolicyManager.java
./base/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java
```

### 2. Input Event Simulation (InputLib.kt)
- Simulate mouse click events
- Simulate scroll events
- Simulate keyboard events
- Input service initialization

### 3. System Function Wrapper (SystemLib.kt)
- Launcher management
- Status bar control
- System property operations
- Device information retrieval (SN, IMEI, MAC address, etc.)
- Storage and memory information
- Network status detection
- Time and timezone settings
- Screen and configuration management
- Split-screen display functionality

### 4. System Low-level Operations (Syslib.kt)
- Network management (WiFi configuration, Ethernet control)
- Application management (silent install/uninstall, permission control)
- Device control (USB, NFC, sensors)
- Storage management (mount/unmount storage devices)
- System updates (OTA upgrades)
- Battery optimization management
- Accessibility control
- Proxy settings

## Requirements
- System permissions required for most features
- Some features require specific Android version support
- Ensure necessary permissions are obtained before use

## Notes
- Modifying system settings may affect device stability
- Some features may behave differently across Android versions
- Thorough testing required before production use

### Other Important System Services
- KeyChainSystemService
- TelecomLoaderService
- NsdManager (Local network server discovery service)
- APN settings reference: ApnEditor.java, ApnPreference.java, ApnSettings.java

## Example Code
```kotlin
// Get device serial number
val sn = SystemLib.getSN()

// Set default launcher
SystemLib.setDefaultLauncher(context, "com.example.launcher")

// Disable USB data transfer
Syslib.setUSBDataDisabled(context, true)

// Simulate click event
InputLib.injectMotionEvent(x, y)
```

## Version Support
- Supports Android 5.0 (API 21) and above
- Some features require higher API levels

## Dependencies
- Kotlin Coroutines
- AndroidX Core Library
