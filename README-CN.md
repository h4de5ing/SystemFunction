# Android 系统功能封装库

封装了Android系统应用中常用功能的库。

## 功能特性

- 提供对带有`@UnsupportedAppUsage`注解的系统隐藏API的访问
- 兼容Android 15及以下版本
- 提供预编译的二进制文件，方便集成
- 需要系统权限和系统签名

## 快速开始

### 前提条件
- 系统签名文件(platform.jks)
- 配置`android.uid.system`共享用户ID

### 1. 在settings.gradle中添加仓库

```groovy
maven { url 'https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository' }
//国内替代版本
maven { url 'https://gitee.com/lex1992/system-function/raw/master/SystemLib_repository' }
```

### 2. 在build.gradle中添加依赖

```groovy
implementation 'com.android.systemlib:systemlib:1.0-20221223'
```

### 3. 配置签名信息

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

### 4. 在AndroidManifest.xml中设置共享用户ID

```xml
<manifest android:sharedUserId="android.uid.system" />
```

## 使用示例

```java
// 使用系统功能的示例
SystemLibHelper helper = new SystemLibHelper(context);
helper.callHiddenSystemApi();
```

## 开发者注意事项

1. **需要系统签名**:
   - 必须拥有平台签名文件(platform.jks)
   - APK必须使用此签名进行签名

2. **系统权限要求**:
   - 应用必须声明`android:sharedUserId="android.uid.system"`
   - 应用必须安装在系统分区

3. **版本兼容性**:
   - 库为Android 15及以下版本维护了独立的实现
   - 升级前请检查版本兼容性

4. **ProGuard规则**:
```proguard
-keep class com.android.systemlib.** { *; }
```

5. **国内开发者特别提示**:
   - 如需在国内网络环境下使用，建议将仓库镜像到国内代码托管平台
   - 系统签名文件需要从设备厂商获取

## 贡献指南

1. Fork本仓库
2. 创建特性分支
3. 提交您的修改
4. 推送分支
5. 创建Pull Request

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。
