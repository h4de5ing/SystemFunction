# Android 系统服务功能说明文档

> 基于 `dumpsys -l` 输出整理，部分服务为 MediaTek 平台特有。

## 1. 核心系统服务

| 服务名称 | 功能说明 |
|---------|----------|
| `activity` | 管理 Activity 生命周期、任务栈和启动模式 |
| `activity_task` | 管理 Activity 任务栈、最近任务列表 |
| `window` | 管理窗口布局、层级、动画及输入事件分发 |
| `input` | 处理原始输入事件（触摸、按键）并分发给目标窗口 |
| `input_method` | 管理输入法（软键盘）的显示、隐藏和切换 |
| `display` | 管理物理/逻辑显示屏的配置、模式及内容 |
| `power` | 控制系统电源状态、唤醒锁、屏幕亮度等 |
| `alarm` | 设置和管理定时闹钟任务 |
| `jobscheduler` | 调度后台作业，优化电池和资源使用 |
| `content` | 提供 ContentProvider 跨进程数据访问支持 |
| `package` | 管理应用安装、卸载、权限和包信息 |
| `user` | 管理多用户、工作资料及用户切换 |
| `system_update` | 负责系统 OTA 更新相关操作 |

## 2. 硬件相关服务

| 服务名称 | 功能说明 |
|---------|----------|
| `android.hardware.cameraservice.service.ICameraService/default` | 相机硬件服务，控制相机设备预览、拍照等 |
| `android.hardware.sensorservice.ISensorManager/default` | 传感器管理服务（加速度计、陀螺仪等） |
| `android.hardware.gnss.IGnss/default` | GNSS（GPS）定位硬件服务 |
| `android.hardware.light.ILights/default` | LED 背光、指示灯控制服务 |
| `android.hardware.memtrack.IMemtrack/default` | 内存跟踪服务，统计硬件内存使用 |
| `android.hardware.power.IPower/default` | 电源管理 HAL，控制 CPU/GPU 调频、休眠等 |
| `android.hardware.vibrator.IVibrator/default` | 单马达振动控制服务 |
| `android.hardware.vibrator.IVibratorManager/default` | 多马达振动管理服务 |
| `android.hardware.neuralnetworks.IDevice/mtk-*` | MTK 神经网络（NPU/GPU）加速服务 |
| `sensorservice` | Android 传感器框架核心服务 |
| `inputflinger` | 底层输入事件读取与分发器（Native 服务） |
| `memtrack.proxy` | 内存跟踪代理服务 |
| `vibrator_manager` | 振动器管理服务（上层接口） |

## 3. 网络与连接服务

| 服务名称 | 功能说明 |
|---------|----------|
| `connectivity` | 管理网络连接状态（WiFi、移动数据、以太网等） |
| `wifi` | WiFi 扫描、连接、配置管理 |
| `wifip2p` | WiFi Direct（点对点）服务 |
| `wifiscanner` | WiFi 扫描服务（独立于连接） |
| `wifinl80211` | 基于 nl80211 的 WiFi 驱动接口 |
| `ethernet` | 以太网连接管理 |
| `bluetooth_manager` | 蓝牙适配器管理和权限控制 |
| `nfc` | 近场通信（NFC）服务 |
| `vpn_management` | VPN 连接管理 |
| `tethering` | 网络热点共享（USB/蓝牙/WiFi） |
| `mdns` | 组播 DNS（Bonjour/AirPlay 等）服务发现 |
| `servicediscovery` | NSD 网络服务发现 |
| `pac_proxy` | 代理自动配置（PAC）服务 |
| `ipsec` | IPsec VPN 服务 |

## 4. 电话与短信服务

| 服务名称 | 功能说明 |
|---------|----------|
| `phone` | 标准电话服务（呼叫状态、网络注册等） |
| `phoneEx` | MTK 扩展电话服务 |
| `telephony.registry` | 电话状态注册服务（信号、运营商等） |
| `telephony.mtkregistry` | MTK 电话状态注册扩展 |
| `telephony_ims` | IMS（VoLTE/WiFi 通话）服务 |
| `isms` | 短信发送/接收服务接口 |
| `isub` | 订阅信息（SIM 卡）服务 |
| `iphonesubinfo` | 设备/手机标识信息（IMEI、MEID） |
| `iphonesubinfoEx` | MTK 扩展设备标识服务 |
| `mtkIms` | MTK IMS 实现服务 |
| `imtksms` | MTK 短信管理服务 |
| `mtksimphonebook` | MTK SIM 卡电话本管理 |
| `simphonebook` | 通用 SIM 卡电话本管理 |

## 5. 媒体与音频服务

| 服务名称 | 功能说明 |
|---------|----------|
| `media.audio_flinger` | 音频流混音与输出（AudioFlinger） |
| `media.audio_policy` | 音频路由策略管理 |
| `media.player` | 媒体播放器服务（NuPlayer/MediaPlayer） |
| `media.camera` | 相机 HAL 服务 |
| `media.camera.proxy` | 相机服务代理（用于权限等） |
| `media.extractor` | 媒体提取器（解析容器格式） |
| `media.metrics` | 媒体性能指标收集 |
| `media.resource_manager` | 媒体资源（解码器、内存）分配管理 |
| `media_session` | 媒体会话控制（播放、暂停、通知栏控制） |
| `media_router` | 媒体路由（投屏、蓝牙耳机） |
| `media_projection` | 屏幕录制/投影服务 |
| `audio` | 高级音频管理（音量、焦点、策略） |
| `soundtrigger` | 声音触发（Always On 语音命令）服务 |
| `soundtrigger_middleware` | 声音触发中间件 |

## 6. 存储与文件服务

| 服务名称 | 功能说明 |
|---------|----------|
| `mount` | 存储设备挂载（内部、外部 SD 卡） |
| `diskstats` | 磁盘 I/O 统计信息 |
| `storaged` | 存储监控守护进程（预测寿命、健康度） |
| `storaged_pri` | 优先存储监控服务 |
| `storagestats` | 各应用/用户存储空间统计 |
| `incremental` | 增量文件系统服务（用于运行时安装） |
| `file_integrity` | 文件完整性校验服务 |

## 7. 安全与权限服务

| 服务名称 | 功能说明 |
|---------|----------|
| `permission` | 权限管理核心服务（授予、撤销、运行时权限） |
| `permission_checker` | 权限检查辅助服务 |
| `permissionmgr` | MTK 权限管理增强服务 |
| `android.security.keystore2.IKeystoreService/default` | 密钥库服务（硬件级密钥存储） |
| `android.hardware.security.keymint.IKeyMintDevice/default` | KeyMint 硬件密钥管理 |
| `android.security.legacykeystore` | 旧版密钥库兼容服务 |
| `attestation_verification` | 密钥证明验证服务 |
| `auth` | 用户认证服务（锁屏密码、指纹、人脸） |
| `biometric` | 生物识别（指纹/人脸）统一管理 |
| `trust` | 信任代理（解锁设备保持信任状态） |
| `oem_lock` | OEM 解锁状态管理 |
| `platform_compat` | 平台兼容性（针对旧应用的行为变更） |
| `safety_center` | 安全中心服务（隐私、安全建议） |
| `rollback` | 应用回滚（安全更新后回退） |

## 8. 性能与监控服务

| 服务名称 | 功能说明 |
|---------|----------|
| `procstats` | 进程统计信息（内存使用时长等） |
| `meminfo` | 内存信息（PSS、RSS、共享库等） |
| `gfxinfo` | GPU 渲染性能信息 |
| `cpuinfo` | CPU 频率、负载信息 |
| `diskstats` | 磁盘 I/O 统计 |
| `batterystats` | 电池使用统计（各应用耗电） |
| `anrmanager` | ANR（应用无响应）记录与处理 |
| `looper_stats` | Looper 消息队列统计 |
| `processinfo` | 进程基本信息（PID、UID、优先级） |
| `stats` | 系统统计服务（Metrics 收集） |
| `statsbootstrap` | 统计服务启动引导 |
| `statscompanion` | 统计辅助服务 |
| `binder_calls_stats` | Binder 调用统计 |
| `tracing.proxy` | 系统跟踪代理（Systrace/Perfetto） |
| `performance_hint` | 性能提示服务（游戏/高负载场景优化） |

## 9. 显示与图形服务

| 服务名称 | 功能说明 |
|---------|----------|
| `SurfaceFlinger` | 图形合成服务（将多个窗口图层合成为显示帧） |
| `SurfaceFlingerAIDL` | SurfaceFlinger 的 AIDL 接口封装 |
| `DockObserver` | 底座（Dock）状态监听，控制显示输出 |
| `gpu` | GPU 渲染相关服务（调试、性能） |
| `graphicsstats` | 图形性能统计 |
| `color_display` | 色彩显示调整（色温、色彩模式） |
| `wallpaper` | 壁纸管理服务 |
| `wallpaper_effects_generation` | 动态壁纸效果生成 |
| `overlay` | 运行时资源叠加（主题、字体替换） |
| `virtualdevice` | 虚拟显示设备（投屏、录屏） |

## 10. 应用管理服务

| 服务名称 | 功能说明 |
|---------|----------|
| `app_binding` | 应用绑定服务（管理 Service 连接） |
| `app_hibernation` | 应用休眠管理（冻结后台应用） |
| `app_integrity` | 应用完整性验证 |
| `app_search` | 应用索引与搜索服务 |
| `appops` | 应用操作记录与限制（后台启动、位置等） |
| `appwidget` | 桌面小部件管理 |
| `shortcut` | 快捷方式管理（动态、固定） |
| `launcherapps` | 启动器应用列表管理 |
| `sdk_sandbox` | SDK 沙箱运行环境 |
| `rollback` | 应用回滚服务（用于安全更新） |
| `background_install_control` | 后台安装控制（静默安装限制） |
| `otadexopt` | OTA 后 dex 优化服务 |
| `webviewupdate` | WebView 组件更新服务 |

## 11. 位置与时间服务

| 服务名称 | 功能说明 |
|---------|----------|
| `location` | 融合定位服务（GPS、网络、传感器） |
| `android.frameworks.location.altitude.IAltitudeService/default` | 海拔高度服务（气压计等） |
| `time_detector` | 自动时间检测（NITZ/网络） |
| `time_zone_detector` | 自动时区检测 |
| `location_time_zone_manager` | 根据地理位置更新时区 |
| `network_time_update_service` | 网络时间同步（SNTP） |

## 12. 输入法与文本服务

| 服务名称 | 功能说明 |
|---------|----------|
| `textservices` | 拼写检查、文本预测服务 |
| `textclassification` | 文本分类（识别 URL、电话、地址等） |
| `texttospeech` | 文字转语音服务 |
| `speech_recognition` | 语音识别服务 |
| `selection_toolbar` | 文本选择工具栏（复制、粘贴） |
| `grammatical_inflection` | 语法屈折形式（性别、单复数）处理 |

## 13. 系统 UI 服务

| 服务名称 | 功能说明 |
|---------|----------|
| `statusbar` | 状态栏管理（图标、通知、系统图标） |
| `notification` | 通知管理（显示、取消、渠道） |
| `dreams` | 屏保（互动屏保）服务 |
| `accessibility` | 无障碍服务（TalkBack、放大手势等） |
| `uimode` | UI 模式（车载、夜间、电视等） |
| `emergency_affordance` | 紧急呼叫按钮/手势 |

## 14. 电源与热管理服务

| 服务名称 | 功能说明 |
|---------|----------|
| `battery` | 电池状态（电量、充电、温度） |
| `batteryproperties` | 电池属性（容量、电压、技术） |
| `deviceidle` | 设备空闲模式（Doze/App Standby） |
| `thermalservice` | 温度监控与限频调节 |
| `power_hal_mgr_service` | 电源 HAL 管理器 |
| `powerstats` | 电源功耗统计 |
| `reboot_readiness` | 重启就绪检查（阻止因更新/应用而重启） |

## 15. 备份与恢复服务

| 服务名称 | 功能说明 |
|---------|----------|
| `backup` | 应用数据备份与恢复（Google 云端备份） |
| `device_config` | 设备配置存储（remote config） |
| `rollback` | 系统回滚（用于 A/B 分区） |
| `persistent_data_block` | 持久数据块（OEM 解锁信息等） |

## 16. USB 与串口服务

| 服务名称 | 功能说明 |
|---------|----------|
| `usb` | USB 模式管理（MTP、PTP、调试、充电） |
| `serial` | 串口（UART）服务 |

## 17. 多媒体通信服务

| 服务名称 | 功能说明 |
|---------|----------|
| `media_communication` | 媒体通信（VoIP、实时音视频） |
| `telecom` | 电信服务（通话 UI、默认拨号） |
| `vow_bridge` | VoWiFi 桥接服务 |
| `midi` | MIDI 音乐设备接口服务 |

## 18. 开发者与调试服务

| 服务名称 | 功能说明 |
|---------|----------|
| `adb` | ADB 调试桥服务 |
| `bugreport` | 生成系统错误报告 |
| `logcat` | 日志缓冲区读取服务 |
| `testharness` | 测试框架辅助服务 |
| `incidentcompanion` | 错误报告伴随服务（Incidentd） |
| `system_server_dumper` | 系统服务器状态导出 |

## 19. MTK 平台特有服务

| 服务名称 | 功能说明 |
|---------|----------|
| `duraspeed` | MTK 后台应用冻结/加速技术 |
| `smartratswitch` | 智能网络类型切换（4G/5G/WiFi 策略） |
| `vendor.mediatek.framework.mtksf_ext.IMtkSF_ext/default` | MTK SurfaceFlinger 扩展（显示增强） |
| `mtkIms` | MTK IMS 协议栈服务 |
| `imtksms` | MTK 短信增强服务 |
| `mtksimphonebook` | MTK SIM 卡电话本 |
| `capctrl` | MTK 流量控制服务 |
| `dataloader_manager` | 数据加载管理器 |
| `power_hal_mgr_service` | MTK 电源 HAL 管理 |
| `external_vibrator_service` | 外部振动器（如游戏手柄）服务 |
| `smartspace` | MTK 智能空间（场景识别） |

## 20. 其他辅助服务

| 服务名称 | 功能说明 |
|---------|----------|
| `dropbox` | 存储系统错误日志（DropboxManager） |
| `clipboard` | 剪贴板服务 |
| `country_detector` | 根据地理位置检测国家码 |
| `restrictions` | 应用限制服务（家长控制） |
| `role` | 角色管理（默认浏览器、助手等） |
| `search` | 全局搜索框架 |
| `search_engine_service` | 搜索引擎选择服务 |
| `people` | 联系人/人物数据管理 |
| `companiondevice` | 配套设备（手表、耳机）管理 |
| `nearby` | 附近设备发现与连接（Fast Pair） |
| `secure_element` | eSE（嵌入式安全元件）服务 |
| `slice` | 应用切片（搜索结果中的交互片段） |
| `tare` | 资源使用记账（待机活动管理） |
| `uri_grants` | URI 临时权限授予管理 |
| `device_lock` | 设备锁屏策略 |
| `device_policy` | 设备管理器（MDM 策略） |
| `device_state` | 设备状态（折叠、底座等） |
| `persistent_data_block` | 持久数据块（解锁记录） |

---

**说明**：
- 服务名称中的 `/default` 表示 HIDL/AIDL 定义的硬件服务实例。
- 带 `mtk` 前缀的服务为 MediaTek 平台特有，可能在非 MTK 设备上不存在。
- 功能描述基于 Android 通用实现及公开文档，具体行为可能随系统版本或厂商定制变化。