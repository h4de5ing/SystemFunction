# 系统功能实现Demo

- 一些需要具备系统权限+系统签名才能实现的功能  
- 调用SystemLib的app必须申明为系统App+系统签名才能正常使用  
- 测试一些系统功能在App中的实现  

app为调用系统层功能  

SystemLib 为系统层功能的抽象层,如果这个接口是Android5~Android12都有的接口，那么直接由SystemLib实现，如果这个接口每个版本的实现方式不一样，那么就采用对应版本的实现
Android12 保存Android12特有接口    
Android11 保存Android11特有接口  
...  
App依赖->SystemLib(抽象层)->(Android5、Android6、Android7、Android8、Android9、Android10、Android11、Android12)



./base/core/java/android/app/admin/DevicePolicyManager.java
./base/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java


config_defaultSupervisionProfileOwnerComponent
frameworks\base\core\res\res\values\config.xml

mdm配置
config_defaultSupervisionProfileOwnerComponent
com.android.systemfunction/com.android.systemfunction.AdminReceiver
PackageManager 
isAutoRevokeWhitelisted  撤销权限


那些比较重要的系统服务
KeyChainSystemService
TelecomLoaderService

apn 设置可以参考ApnEditor.java ApnPreference.java ApnSettings.java