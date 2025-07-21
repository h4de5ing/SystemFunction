# Android System Function Library

A library that encapsulates commonly used system functions for Android system applications.

## Features

- Provides access to hidden system APIs with `@UnsupportedAppUsage` annotation
- Compatible with Android 15 and below
- Pre-built binary distribution for easy integration
- Requires system privileges and system signature

## Quick Start

### Prerequisites
- System signature (platform.jks)
- `android.uid.system` shared user ID

### 1. Add repository to settings.gradle

```groovy
maven { 
    url 'https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository' 
}
```

### 2. Add dependency to build.gradle

```groovy
implementation 'com.android.systemlib:systemlib:1.0-20221223'
```

### 3. Configure signing

```groovy
signingConfigs {
    sign {
        storeFile file("platform.jks")
        storePassword 'android'
        keyAlias 'android'
        keyPassword 'android'
    }
}
```

### 4. Set shared user ID in AndroidManifest.xml

```xml
<manifest android:sharedUserId="android.uid.system" />
```

## Usage Example

```java
// Example of using system functions
SystemLibHelper helper = new SystemLibHelper(context);
helper.callHiddenSystemApi();
```

## Developer Notes

1. **System Signature Required**:
   - You must have the platform signature file (platform.jks)
   - The APK must be signed with this signature

2. **System Privileges**:
   - Your app must declare `android:sharedUserId="android.uid.system"`
   - Your app must be installed in the system partition

3. **Version Compatibility**:
   - The library maintains separate implementations for Android 12 and below
   - Check version compatibility before upgrading

4. **ProGuard Rules**:
```proguard
-keep class com.android.systemlib.** { *; }
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the [Apache License 2.0](LICENSE) - see the [LICENSE](LICENSE) file for details.
