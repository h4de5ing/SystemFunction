# Android 网络系统服务 API 总结

> 适用范围：System App（平台签名或 sharedUserId）通过隐藏 API / 反射调用  
> 服务获取方式：`getSystemService("netstats")` / `getSystemService("netpolicy")` / `getSystemService("network_watchlist")`

---

## 目录

1. [NetworkStatsManager](#1-networkstatsmanager)
2. [NetworkPolicyManager](#2-networkpolicymanager)
3. [NetworkWatchlistManager](#3-networkwatchlistmanager)
4. [所需权限汇总](#4-所需权限汇总)

---

## 1. NetworkStatsManager

**包名**：`android.app.usage.NetworkStatsManager`  
**服务名**：`netstats`（API 23+）  
**用途**：查询设备/用户/应用的网络流量统计数据，注册流量阈值回调

### 1.1 公开 SDK 方法（API 23+，普通 App 可用）

| 方法 | API | 返回值 | 说明 |
|------|-----|--------|------|
| `querySummaryForDevice(int networkType, String subscriberId, long startTime, long endTime)` | 23 | `NetworkStats.Bucket` | 查询指定时间段内整台设备的流量汇总（单个 Bucket） |
| `querySummaryForUser(int networkType, String subscriberId, long startTime, long endTime)` | 23 | `NetworkStats.Bucket` | 查询当前用户下所有 UID 的流量汇总 |
| `querySummary(int networkType, String subscriberId, long startTime, long endTime)` | 23 | `NetworkStats` | 查询当前用户的流量汇总，按 UID/状态/计费等拆分，返回多个 Bucket |
| `queryDetails(int networkType, String subscriberId, long startTime, long endTime)` | 23 | `NetworkStats` | 查询当前用户的时序流量历史（按时间桶，聚合 state） |
| `queryDetailsForUid(int networkType, String subscriberId, long startTime, long endTime, int uid)` | 23 | `NetworkStats` | 查询指定 UID 的时序流量历史 |
| `queryDetailsForUidTag(int networkType, String subscriberId, long startTime, long endTime, int uid, int tag)` | 24 | `NetworkStats` | 查询指定 UID + Tag 的流量历史 |
| `queryDetailsForUidTagState(int networkType, String subscriberId, long startTime, long endTime, int uid, int tag, int state)` | 28 | `NetworkStats` | 最细粒度查询：UID + Tag + State |
| `registerUsageCallback(int networkType, String subscriberId, long thresholdBytes, UsageCallback callback, Handler handler)` | 24 | `void` | 注册流量阈值回调（带 Handler） |
| `registerUsageCallback(int networkType, String subscriberId, long thresholdBytes, UsageCallback callback)` | 24 | `void` | 注册流量阈值回调（无 Handler） |
| `unregisterUsageCallback(UsageCallback callback)` | 24 | `void` | 取消注册流量阈值回调 |

### 1.2 隐藏 API 方法（需反射或 stub 访问）

| 方法 | API | 说明 |
|------|-----|------|
| `setPollOnOpen(boolean)` | 28 | 控制打开统计会话时是否触发一次 poll 刷新 |
| `setPollForce(boolean)` | 28 | 强制 poll（测试用） |
| `setAugmentWithSubscriptionPlan(boolean)` | 28 | 控制是否用套餐信息补充统计数据 |
| `querySummaryForDevice(NetworkTemplate, long, long)` | 28 | 通过 NetworkTemplate 查询设备流量汇总 |
| `registerUsageCallback(NetworkTemplate, int, long, UsageCallback, Handler)` | 28 | 通过 NetworkTemplate 注册流量回调 |
| `querySummary(NetworkTemplate, long, long)` | 29 | 通过 NetworkTemplate 查询用户汇总 |
| `queryDetailsForUid(NetworkTemplate, long, long, int)` | 29 | 通过 NetworkTemplate 查询 UID 历史 |
| `queryDetailsForUidTagState(NetworkTemplate, long, long, int, int, int)` | 29 | 通过 NetworkTemplate 查询 UID+Tag+State |

### 1.3 System API 方法（仅系统组件）

| 方法 | API | 权限 | 说明 |
|------|-----|------|------|
| `registerNetworkStatsProvider(String tag, NetworkStatsProvider provider)` | 30 | `NETWORK_STATS_PROVIDER` | 向系统注册自定义流量统计提供者 |
| `unregisterNetworkStatsProvider(NetworkStatsProvider provider)` | 30 | `NETWORK_STATS_PROVIDER` | 取消注册统计提供者 |

### 1.4 关键参数说明

- **networkType**：`ConnectivityManager.TYPE_MOBILE` / `TYPE_WIFI`
- **subscriberId**：SIM 订阅 ID（API 29+ 受隐私限制，可传 `null` 表示全部移动网络）
- **startTime / endTime**：Unix 毫秒时间戳（`System.currentTimeMillis()`）
- **tag**：流量标签，通常用 `NetworkStats.Bucket.TAG_NONE`
- **state**：`NetworkStats.Bucket.STATE_ALL` / `STATE_DEFAULT` / `STATE_FOREGROUND`

### 1.5 所需权限

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<!-- 或通过 Usage Access 授权 -->
```

---

## 2. NetworkPolicyManager

**包名**：`android.net.NetworkPolicyManager`  
**服务名**：`netpolicy`  
**全部方法均为隐藏 API**，需系统签名或反射  
**用途**：Data Saver 控制、UID 网络策略管理、套餐计划、网络使用限额、回调监听

### 2.1 服务获取

```kotlin
// 方式1：通过 getSystemService
val npm = getSystemService("netpolicy") as NetworkPolicyManager

// 方式2：静态工厂（反射）
val npm = NetworkPolicyManager.from(context)
```

### 2.2 UID 策略与 Data Saver

| 方法 | 权限 | 说明 |
|------|------|------|
| `setUidPolicy(int uid, int policy)` | `MANAGE_NETWORK_POLICY` | 替换指定 UID 的全部策略位（`POLICY_NONE` / `POLICY_REJECT_METERED_BACKGROUND` / `POLICY_ALLOW_METERED_BACKGROUND`） |
| `addUidPolicy(int uid, int policy)` | `MANAGE_NETWORK_POLICY` | 追加策略位（OR 操作） |
| `removeUidPolicy(int uid, int policy)` | `MANAGE_NETWORK_POLICY` | 清除策略位 |
| `getUidPolicy(int uid)` | `MANAGE_NETWORK_POLICY` | 获取 UID 当前策略位掩码 |
| `getUidsWithPolicy(int policy)` | `MANAGE_NETWORK_POLICY` | 获取拥有指定策略的所有 UID 数组 |
| `setRestrictBackground(boolean)` | `MANAGE_NETWORK_POLICY` | 开启/关闭全局 Data Saver（省流量模式） |
| `getRestrictBackground()` | `MANAGE_NETWORK_POLICY` | 获取 Data Saver 全局开关状态 |
| `getRestrictBackgroundStatus(int uid)` | Network Stack only | 获取指定 UID 的 Data Saver 状态（`RESTRICT_BACKGROUND_STATUS_*`） |

**策略常量**：
- `POLICY_NONE = 0` — 无限制
- `POLICY_REJECT_METERED_BACKGROUND = 1` — 计费网络禁止后台流量（黑名单）
- `POLICY_ALLOW_METERED_BACKGROUND = 4` — Data Saver 开启时允许后台流量（白名单）

### 2.3 UID 网络封锁查询（API 31+）

| 方法 | 权限 | 说明 |
|------|------|------|
| `isUidNetworkingBlocked(int uid, boolean meteredNetwork)` | `OBSERVE_NETWORK_POLICY` | 查询 UID 在当前策略/防火墙规则下是否被封网 |
| `isUidRestrictedOnMeteredNetworks(int uid)` | `OBSERVE_NETWORK_POLICY` | 查询 UID 是否被限制在计费网络上 |

### 2.4 全局网络策略管理

| 方法 | 权限 | 说明 |
|------|------|------|
| `setNetworkPolicies(NetworkPolicy[] policies)` | `MANAGE_NETWORK_POLICY` | 替换全部全局网络策略（含计费周期/预警/限额/模板） |
| `getNetworkPolicies()` | `MANAGE_NETWORK_POLICY` + `READ_PHONE_STATE` | 获取当前所有网络策略 |
| `factoryReset(String subscriber)` | `NETWORK_SETTINGS`（API 30+）/ `CONNECTIVITY_INTERNAL`（API 23-29） | 恢复网络策略出厂默认值 |
| `getMultipathPreference(Network network)` | Network Stack only | 获取指定网络的多路径偏好（`MULTIPATH_PREFERENCE_*`） |

### 2.5 监听器与回调注册

| 方法 | API | 权限 | 说明 |
|------|-----|------|------|
| `registerListener(INetworkPolicyListener)` | 21+ | `CONNECTIVITY_INTERNAL` 或 `OBSERVE_NETWORK_POLICY` | 注册低级别 Binder 策略变更监听 |
| `unregisterListener(INetworkPolicyListener)` | 21+ | 同上 | 取消注册 |
| `registerSubscriptionCallback(SubscriptionCallback)` | 30+ | `OBSERVE_NETWORK_POLICY` | 注册套餐计划/覆盖变更回调 |
| `unregisterSubscriptionCallback(SubscriptionCallback)` | 30+ | `OBSERVE_NETWORK_POLICY` | 取消注册 |
| `registerNetworkPolicyCallback(Executor, NetworkPolicyCallback)` | 31+ | `OBSERVE_NETWORK_POLICY` | 注册 UID 封锁状态变更回调 |
| `unregisterNetworkPolicyCallback(NetworkPolicyCallback)` | 31+ | `OBSERVE_NETWORK_POLICY` | 取消注册 |

### 2.6 套餐计划与运营商覆盖（API 30+）

> 调用者需为运营商特权应用，或持有 `MANAGE_SUBSCRIPTION_PLANS` 权限

| 方法 | API | 说明 |
|------|-----|------|
| `setSubscriptionPlans(int subId, SubscriptionPlan[], String callingPackage)` | 30-32 | 设置订阅套餐计划 |
| `setSubscriptionPlans(int subId, SubscriptionPlan[], long expirationDurationMillis, String callingPackage)` | 33+ | 设置套餐计划（带过期时间） |
| `getSubscriptionPlans(int subId, String callingPackage)` | 30+ | 获取当前套餐计划 |
| `getSubscriptionPlan(NetworkTemplate template)` | 33+ | 根据 NetworkTemplate 获取主套餐 |
| `setSubscriptionOverride(int subId, int overrideMask, int overrideValue, long timeoutMillis, String callingPackage)` | 30 | 临时覆盖套餐状态（计费/拥堵） |
| `setSubscriptionOverride(int subId, int overrideMask, int overrideValue, int[] networkTypes, long timeoutMillis, String callingPackage)` | 31-32 | 按网络类型覆盖套餐状态 |
| `setSubscriptionOverride(int subId, int overrideMask, int overrideValue, int[] networkTypes, long expirationDurationMillis, String callingPackage)` | 33+ | 同上（参数名更名） |
| `notifyStatsProviderWarningReached()` | 33+ | 通知服务统计提供者到达预警阈值 |
| `notifyStatsProviderLimitReached()` | 33+ | 通知服务统计提供者到达限额阈值 |

### 2.7 计费周期工具方法（本地静态，无 Binder 调用，无需权限）

| 方法 | API | 说明 |
|------|-----|------|
| `computeLastCycleBoundary(long currentTime, NetworkPolicy policy)` | 21-26 | 计算上一个计费周期边界 |
| `computeNextCycleBoundary(long currentTime, NetworkPolicy policy)` | 21-26 | 计算下一个计费周期边界 |
| `cycleIterator(NetworkPolicy policy)` | 27+ | 迭代策略的计费周期起止时间对 |
| `resolveNetworkId(WifiConfiguration config)` | 27+ | 将 Wi-Fi 配置规范化为策略匹配用的网络 ID |
| `resolveNetworkId(String ssid)` | 27+ | 将 SSID 规范化 |

### 2.8 调试工具方法（本地静态，无需权限）

| 方法 | API | 说明 |
|------|-----|------|
| `uidRulesToString(int uidRules)` | 24+ | 将 UID 规则位掩码转为可读字符串 |
| `uidPoliciesToString(int uidPolicies)` | 26+ | 将 UID 策略位掩码转为可读字符串 |
| `blockedReasonsToString(int blockedReasons)` | 31+ | 将封锁原因掩码转为可读字符串 |
| `allowedReasonsToString(int allowedReasons)` | 31+ | 将允许原因掩码转为可读字符串 |
| `isUidValidForPolicy(Context, int uid)` | 21+ | 判断 UID 是否可以应用 UID 策略 |

### 2.9 进程状态策略工具方法（本地静态，无需权限）

| 方法 | API | 说明 |
|------|-----|------|
| `isProcStateAllowedWhileIdleOrPowerSaveMode(int procState)` | 26-30 | 是否在 Idle/省电模式下允许联网 |
| `isProcStateAllowedWhileIdleOrPowerSaveMode(UidState uidState)` | 31+ | 同上（UidState 版本） |
| `isProcStateAllowedWhileIdleOrPowerSaveMode(int procState, int capability)` | 31+ | 同上（含 capability 版本） |
| `isProcStateAllowedWhileOnRestrictBackground(int procState)` | 26-33 | 是否在 Data Saver 下允许联网 |
| `isProcStateAllowedWhileOnRestrictBackground(UidState uidState)` | 31+ | 同上（UidState 版本） |
| `isProcStateAllowedWhileOnRestrictBackground(int procState, int capabilities)` | 34 | 同上（含 capability 版本） |
| `isProcStateAllowedWhileInLowPowerStandby(UidState uidState)` | 33+ | 是否在低功耗待机模式下允许联网 |
| `getDefaultProcessNetworkCapabilities(int procState)` | 34 | 获取进程状态默认的网络能力集 |

---

## 3. NetworkWatchlistManager

**包名**：`android.net.NetworkWatchlistManager`  
**服务名**：`network_watchlist`（API 28+）  
**整个类均为隐藏 API**（`@hide`，API 28+，`@SystemApi` MODULE_LIBRARIES）  
**用途**：管理可疑网络访问监控名单（Watchlist），基于差分隐私技术生成报告

### 3.1 原理说明

NetworkWatchlistManager 监控设备的 DNS 查询和 TCP/UDP 连接，对照一个可疑域名/IP 黑名单（Watchlist 配置文件）。  
命中记录经过差分隐私处理后，以聚合报告形式存储，报告每 12 小时生成一次。

### 3.2 公开方法（框架源码中 Java public，全部为隐藏 API）

| 方法 | 权限 / 限制 | 说明 |
|------|------------|------|
| `reportWatchlistIfNecessary()` | 无权限检查（任意调用者） | 触发一次 Watchlist 报告生成检查；如距上次报告不足 12 小时则为 no-op |
| `reloadWatchlist()` | Binder 层要求调用者 UID 为 `SYSTEM_UID` | 从磁盘重新加载 Watchlist 配置文件到内存 |
| `getWatchlistConfigHash()` | 无权限检查 | 返回当前 Watchlist 配置文件的 SHA-256 摘要（可为 null） |

### 3.3 底层 AIDL 接口方法（`INetworkWatchlistManager`，仅 SYSTEM_UID 可调）

| 方法 | 说明 |
|------|------|
| `startWatchlistLogging()` | 启动 netd 的网络连接事件日志采集（Watchlist 监控开始） |
| `stopWatchlistLogging()` | 停止日志采集 |
| `reloadWatchlist()` | 同 Manager 封装方法 |
| `reportWatchlistIfNecessary()` | 同 Manager 封装方法 |
| `getWatchlistConfigHash()` | 同 Manager 封装方法 |

### 3.4 测试方法（仅内部/Shell）

| 方法 | 说明 |
|------|------|
| `NetworkWatchlistService.forceReportWatchlistForTest(long lastReportTime)` | 强制生成测试报告；仅在非生产配置下有效，生产签名返回 false |

### 3.5 配置文件格式

Watchlist 配置文件默认路径：`/system/etc/network_watchlist.xml`

```xml
<network-watchlist version="1">
    <md5-blacklist>
        <!-- 可疑域名的 MD5 哈希 -->
        <domain-blacklist>
            <hash value="..."/>
        </domain-blacklist>
        <!-- 可疑 IP 的 MD5 哈希 -->
        <ip-blacklist>
            <hash value="..."/>
        </ip-blacklist>
    </md5-blacklist>
    <sha256-blacklist>
        <domain-blacklist>
            <hash value="..."/>
        </domain-blacklist>
        <ip-blacklist>
            <hash value="..."/>
        </ip-blacklist>
    </sha256-blacklist>
</network-watchlist>
```

---

## 4. 所需权限汇总

| 权限 | 相关服务 | 说明 |
|------|---------|------|
| `android.permission.PACKAGE_USAGE_STATS` | NetworkStatsManager | 查询设备级/其他 App 流量统计 |
| `android.permission.MANAGE_NETWORK_POLICY` | NetworkPolicyManager | UID 策略读写、全局网络策略管理、Data Saver 控制 |
| `android.permission.OBSERVE_NETWORK_POLICY` | NetworkPolicyManager | 只读监听策略变更/封锁状态查询（API 30+） |
| `android.permission.NETWORK_SETTINGS` | NetworkPolicyManager | `factoryReset()`（API 30+） |
| `android.permission.CONNECTIVITY_INTERNAL` | NetworkPolicyManager | 低级别监听器注册（API 21-29） |
| `android.permission.MANAGE_SUBSCRIPTION_PLANS` | NetworkPolicyManager | 非运营商应用操作套餐计划 |
| `android.permission.NETWORK_STATS_PROVIDER` | NetworkStatsManager | 注册自定义流量统计提供者 |

> 上述权限均为 signature 级或系统预装权限，普通应用无法申请，需要平台签名或 `android:sharedUserId="android.uid.system"`。

---

## 5. 代码示例

### 查询 App 流量统计

```kotlin
val statsManager = getSystemService("netstats") as NetworkStatsManager
val end = System.currentTimeMillis()
val start = end - 7 * 24 * 3600 * 1000L  // 最近 7 天

val stats = statsManager.queryDetailsForUid(
    ConnectivityManager.TYPE_WIFI,
    null,
    start, end,
    Process.myUid()
)
val bucket = NetworkStats.Bucket()
while (stats.hasNextBucket()) {
    stats.getNextBucket(bucket)
    Log.d("Stats", "rx=${bucket.rxBytes} tx=${bucket.txBytes}")
}
stats.close()
```

### 开启 Data Saver 并设置 App 白名单

```kotlin
// 需要 MANAGE_NETWORK_POLICY 权限
val npm = getSystemService("netpolicy") as NetworkPolicyManager
npm.setRestrictBackground(true)  // 开启全局 Data Saver
// 将指定 App 加入 Data Saver 白名单
npm.addUidPolicy(targetUid, NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND)
```

### 监听 UID 网络策略变更

```kotlin
val npm = getSystemService("netpolicy") as NetworkPolicyManager
// API 31+，需要 OBSERVE_NETWORK_POLICY
npm.registerNetworkPolicyCallback(mainExecutor, object : NetworkPolicyManager.NetworkPolicyCallback() {
    override fun onUidBlockedReasonChanged(uid: Int, blockedReasons: Int) {
        Log.d("Policy", "uid=$uid blocked=${NetworkPolicyManager.blockedReasonsToString(blockedReasons)}")
    }
})
```

### 检查 Watchlist 配置哈希

```kotlin
val watchlist = getSystemService("network_watchlist") as NetworkWatchlistManager
val hash = watchlist.getWatchlistConfigHash()
Log.d("Watchlist", "config hash=${hash?.let { it.joinToString("") { b -> "%02x".format(b) } }}")
```
