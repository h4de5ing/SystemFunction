# Android 系统应用常用功能指北

## 注意：调用系统隐藏接口或者@UnsupportedAppUsage标注的接口，需要你拥有System权限以及APP使用系统签名，如果你具体此条件，请继续往下看

## 本仓库模块说明

- SystemLib_src 系统功能封装库的源码 app 
  - AndroidManifest.xml 配置android:sharedUserId="android.uid.system"  
  - app build.gradle sign的签名文件必须是对应平台的系统签名 
  - SystemLib 兼容android12以下的api 
  - Android12 兼容Android12特有的api
  
- SystemLib_repository 系统封装库的二进制文件，方便第三方使用，基于SystemLib_src源码打包

## 系统应用开发准备工作

settings.gradle

```groovy
    maven { url 'https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository' }
```

build.gradle

```groovy
//添加系统签名
signingConfigs {
    sign {
        storeFile file("platform.jks")
        storePassword('android')
        keyAlias('android')
        keyPassword('android')
    }
}
//添加库依赖
implementation 'com.android.systemlib:systemlib:1.0-20221223' //请根据需求升级版本号

```

AndroidManifest.xml

```xml

<manifest android:sharedUserId="android.uid.system" />

```