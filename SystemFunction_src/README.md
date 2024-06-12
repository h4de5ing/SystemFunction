# 系统功能实现Demo

- System权限+系统签名才能实现的功能  
- 调用SystemLib的app必须申明为系统App+系统签名才能正常使用
- SystemLib 为系统层功能的抽象层,如果这个接口是Android5~Android12都有的接口，那么直接由SystemLib实现，如果这个接口每个版本的实现方式不一样，那么就采用对应版本的实现
Android12 保存Android12特有接口    
...  
- App依赖->SystemLib(抽象层)->(Android5、Android6、Android7、Android8、Android9、Android10、Android11、Android12)
- DPM.kt中的接口必须要setActiveProfileOwner激活以后才能正常使用。具体请参考[官方文档](https://developer.android.com/guide/topics/admin/device-admin)


- 源码请参考
```
./base/core/java/android/app/admin/DevicePolicyManager.java
./base/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java
```
- AOSP配置中默认DPM的包名配置
```
frameworks\base\core\res\res\values\config.xml
<string name="config_defaultSupervisionProfileOwnerComponent" translatable="false">com.android.systemfunction/com.android.systemfunction.AdminReceiver</string>
```


- etc
  * 其他重要的系统服务
  * KeyChainSystemService
  * TelecomLoaderService
  * NsdManager 内网服务器发现服务

  * apn 设置可以参考ApnEditor.java ApnPreference.java ApnSettings.java
