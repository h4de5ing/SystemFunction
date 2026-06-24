# ApkInstaller 静默安装器设计文档

> 分析 [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) 与
> [universal-installer](https://github.com/pass-with-high-score/universal-installer) 两个开源 APK 安装器，
> 提炼可吸收的实现思路，落地为本项目（`com.android.systemlib`）下的 `ApkInstaller.kt`。

---

## 1. 需求与目标

### 1.1 我的现状
本项目是 OEM/特权 MDM 平台，应用以 **system 签名 / 系统权限** 运行（`SystemFunction` 子模块里的 `SystemLib` 即特权封装层）。
现有的静默安装实现位于 `SystemFunction/SystemFunction_src/SystemLib/src/main/java/com/android/systemlib/Syslib.kt`，
顶层函数 `installAPK(context, apkFilePath, change)`：

- 仅支持 `.apk` 与 `.xapk` 两种格式；
- 使用公开的 `context.packageManager.packageInstaller` 创建 `PackageInstaller.Session`；
- 通过 `PendingIntent.getBroadcast` + 动态注册 `BroadcastReceiver` 接收安装结果；
- 处理了 `STATUS_PENDING_USER_ACTION`（拉起确认界面）的情况。

因为是 system 签名，安装调用本身已经具备特权身份，通常不会触发用户确认弹窗，已经能“静默”。但实现上仍有明显短板。

### 1.2 本次需求
1. **吸收两个开源项目支持的各种后缀的 app 文件安装**：`.apk`、`.apks`（APKMirror / App Bundle 拆分包）、`.xapk`、`.apkm`，以及多 APK 压缩包；
2. **优先使用 system 权限静默安装**——借助系统签名身份走 `PackageInstaller` 会话，无需任何用户交互；
3. **不要使用 root 或 Shizuku 等第三方特权方式**（不引入 `libsu` / Shizuku / Dhizuku / `app_process` 提权）；
4. 把上述思路整理成文档，并尽量直接封装一个 `ApkInstaller.kt` 放在 `com.android.systemlib` 包下，替代 / 增强现有 `installAPK`。

### 1.3 设计约束
- 运行身份：**system 签名应用（system UID）**。这是“静默”的根本来源——`PackageManager` 对 system UID 的安装调用不注入 `STATUS_PENDING_USER_ACTION`。
- 因此凡是依赖 root / shell(2000) / Shizuku binder 包装 / `app_process su` 的提权路径，**一律不吸收**，只吸收与“特权身份 + 会话构造 + 多格式解析 + 结果处理”相关的纯逻辑。
- `SystemLib` 模块已通过 `compileOnly(files("libs/classes.jar"))` 暴露了 Android 隐藏 API 桩（`IPackageInstaller`、`IPackageInstallerSession`、`IPackageManager`、`IIntentSender`、`ServiceManager`、`PackageInstaller` 隐藏构造器、`SessionParams.installFlags/abiOverride` 公开字段等），可直接调用，无需反射（少数版本差异处用反射兜底）。

---

## 2. 两个开源项目分析

### 2.1 InstallerX-Revived

整体是分层很干净的 Kotlin 工程（`domain/data/framework/core/hidden-api`），安装管线：
`解析来源 → 检测类型 → 解析(分析) → 预处理/分组/去重 → 选择最优 split → 签名/策略/黑名单校验 → 派发到对应 Authorizer 仓库 → 构建 PackageInstaller 会话 → 写入 APK → 本地 IntentSender 提交 → 校验结果`。

#### 支持的格式
`DataType` 枚举：`APK / APKS / APKM / XAPK / MULTI_APK / MULTI_APK_ZIP / MODULE_ZIP / ...`。
检测器 `FileTypeDetector` 把文件当作 `ZipFile` 打开后按优先级判定：
- XAPK：含 `manifest.json` 且带 `package_name`/`version_code`/`split_apks`/`expansions`；
- APKM：含 `info.json` 且带 `pname`/`versioncode`；
- APK：含 `AndroidManifest.xml`；
- APKS：含 `toc.pb` 或 `base.apk` / `base-master.*`；
- 否则若含任意 `.apk` 条目 → `MULTI_APK_ZIP`。
> 注意：Android 14+ 调用 `ZipPathValidator.clearCallback()`，避免压缩包内非法路径导致解析崩溃——值得吸收。

解析策略 `AnalysisStrategy`（策略模式）+ `UnifiedContainerAnalyser` 调度，**只打开一次 ZipFile**：
`SingleApkStrategy / ApksStrategy / ApkmStrategy / XApkStrategy / MultiApkZipStrategy / ModuleStrategy`。

#### 安装模式与派发（重点）
`Authorizer` 枚举：`Global / None / Root / Shizuku / Dhizuku / Customize`。`AppInstallerRepositoryImpl.resolveRepo()` 派发：
- `Shizuku` → `ShizukuAppInstallerRepoImpl`
- `Dhizuku` → `DhizukuAppInstallerRepoImpl`
- `None` → 系统应用走 `SystemAppInstallerRepoImpl`，否则 `NoneAppInstallerRepoImpl`
- 其余（Root/Customize）→ `ProcessAppInstallerRepoImpl`（`app_process` 提权）

**最值得吸收的抽象**：`System / Shizuku / Dhizuku / Root / Customize` **全部继承自 `IBinderAppInstallerRepoImpl`**，整套特权安装逻辑只写一份，子类只重写一个方法：
```kotlin
protected abstract suspend fun iBinderWrapper(iBinder: IBinder): IBinder
```
- `SystemAppInstallerRepoImpl`：`iBinderWrapper = { it }`（恒等），因为应用本身就是 system，直接在进程内用原始系统服务 binder。

**System（非 root、非 shizuku）静默路径**正是我们要的：
1. `ServiceManager.getService("package")` → `IPackageManager.Stub.asInterface(...)` → `.packageInstaller.asBinder()` → `IPackageInstaller.Stub.asInterface(...)`；
2. 通过 **隐藏构造器** `PackageInstaller(IPackageInstaller, callerPackageName, null/userId)` 反射构造 `PackageInstaller`，从而能设置 `callerPackageName` 与 `userId`（公开 `context.packageInstaller` 不允许）；
3. 构建 `SessionParams`、`createSession`、`openSession`、写 APK、`commit`。

> 与 root/shizuku 的区别**仅仅是 `iBinderWrapper`**：System 用原始 binder，其余把每次 binder transact 代理到外部特权进程。会话构造、split 写入、提交、结果校验全部共用。这印证了我们的思路：system 身份下，公开/隐藏 `PackageInstaller` 路径本身已足够静默，无需任何外部提权。

#### PackageInstaller 会话用法要点
- **模式**：批量/多 APK 用 `MODE_FULL_INSTALL`；单包有 `base.apk` 用 `MODE_FULL_INSTALL`，纯 split 增量更新用 `MODE_INHERIT_EXISTING`（一个会话内只能有一个 base，否则抛异常）；
- `setOriginatingUid / setAppPackageName / setInstallerPackageName(API34+) / setInstallLocation / setInstallReason / setPackageSource(API33+) / setSize`；
- 隐藏字段 `installFlags`：组合 `INSTALL_REPLACE_EXISTING`、按需 `INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS`、`INSTALL_ALLOW_DOWNGRADE`、`INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK` 等；
- 隐藏字段 `abiOverride`：设为 base APK 解析出的实际架构字符串（如 `arm64-v8a`），**用于触发 Houdini 二进制翻译**，让架构不匹配也能装；
- **split 写入**：按 `packageName` 分组，每组一个会话；`session.openWrite(name, 0, size)` 拷流 + `fsync`。

#### 结果回调——避免“卡在 pending user action”（核心）
两套机制：
1. **`LocalIntentReceiver`**：直接实现 `IIntentSender.Stub`（隐藏接口），用隐藏构造器 `IntentSender(IIntentSender)` 包成真正的 `IntentSender`，`session.commit(receiver.getIntentSender())` 通过 Kotlin `Channel` **同步在进程内**回传结果——不依赖 `PendingIntent` 广播、不拉起任何 Activity。这是“自包含静默安装”的关键：结果不离开进程。
2. **特权身份本身**避免确认：root/shizuku/system 以 system/shell/root 身份调用，`PackageManager` 不注入 `STATUS_PENDING_USER_ACTION`；
3. **兜底自动批准**：万一系统仍要求确认，调用隐藏 `IPackageInstaller.setPermissionsResult(sessionId, granted)` 程序化批准/拒绝，失败再 `abandonSession`。

#### split 选择
`SplitNameUtil.parseSplitMetadata`：剥离 `split_config.`/`config.`/`split-`/`base-` 前缀后分类为 `ARCHITECTURE / DENSITY / LANGUAGE / FEATURE`。
`SelectOptimalSplitsUseCase`：按设备 `supportedArchitectures / supportedDensities / supportedLocales` 选最优 ABI/density/locale split；`.dm` 与 module 始终选中。

#### 签名/版本/错误处理
- 待装 APK 签名：用官方 `com.android.apksig.ApkVerifier` 提取 V1–V4 + 证书 lineage；
- 已装签名：`PackageManager.hasSigningCertificate(pkg, sha256, CERT_INPUT_SHA256)`（API28+）；
- `InstallErrorType.fromLegacyCode`：把 `INSTALL_FAILED_*`/`INSTALL_PARSE_FAILED_*`（-1 ~ -131）+ OEM 码（如 HyperOS -1000、FRP -9477）映射成可读枚举；
- 读取隐藏 `EXTRA_LEGACY_STATUS` 拿到精确 legacy 码。

#### hidden-api / 反射
`hidden-api/` 模块是编译期 `@hide` 桩。运行期 `ReflectionProvider` 缓存反射。关键点：`PackageInstaller` 隐藏构造器、`SessionParams.installFlags/abiOverride` 字段、`PackageInstaller.Session.mSession` 字段（重新包装会话 binder）、`IntentSender(IIntentSender)` 构造器、`AssetManager.addAssetPath`。

> 该项目 `AGENTS.md` 明确：**优先用原生 AIDL API 而非 shell 命令**，全程不用 `pm install`，只用 PackageInstaller 会话。这与本项目“system 权限”思路完全一致。

### 2.2 universal-installer

Kotlin + Compose 多模块工程（`:app` 手机、`:tv` 电视、`:core` 共享引擎），**安装底层依赖 Ackpine 库**（`ru.solrudev.ackpine`），特权路径靠 **Shizuku + libsu**。

#### 支持的格式
扩展名集合：`apk / apks / xapk / apkm / apk+ / zip`。bundle（`apks/xapk/apkm/zip`）交给 Ackpine 的 `ZippedApkSplits.getApksForUri()` 枚举并按类型分类 split（`Base/Libs/Localization/ScreenDensity/Feature/Other`），再 `.validate().filterCompatible(context)`。

值得吸收的小技巧：
- **零条目回退**：bundle 解析出 0 条目时，回退按单 APK 重新解析（修 F-Droid 那种其实是单 APK 却命名成 `.zip` 的包）；
- **无 `.apk` 后缀的单包**：Ackpine 的 `File.isApk` 会拒绝没有 `.apk` 后缀的文件，于是先拷贝到缓存改名 `*.apk` 再解析；
- **`reclassifyOtherSplit`**：Ackpine 把名字带额外后缀（如 `.v2` 签名方案标记）的 split 误判为 `Other`，这里手动按 `.` 拆 token 重新匹配 ABI/dpi/locale。

#### 安装模式与派发
四个后端，都继承 `BaseInstallController`，只重写 `createSession()`：
| 后端 | 控制器 | 机制 | 静默 |
|---|---|---|---|
| Default(system) | `DefaultInstallController` | Ackpine `PackageInstaller.createSession` + `Confirmation.IMMEDIATE` | 否（系统确认弹窗） |
| Root | `RootInstallController` | Ackpine + `libsu {}` | 是 |
| Shizuku | `ShizukuInstallController` | Ackpine + `shizuku {}` | 是 |
| 目标用户(手动) | `ManualInstallController` | 绕过 Ackpine | 是 |

> 关键结论：**该项目并没有实现“system 签名应用”的静默安装**。它的静默路径就是 Shizuku / root。Default 路径（应用自身 UID）必有确认弹窗。所以对我们最有价值的不是它的 Ackpine 集成，而是它**绕过 Ackpine 的两份纯 `PackageInstaller` 会话实现**。

#### 纯 PackageInstaller 会话实现（重点吸收）
**(a) `core/install/ApkInstaller.kt`** —— TV 路径、不依赖 Ackpine，最适合手写参考：
- `SessionParams(MODE_FULL_INSTALL)`，不设 reason/location；
- `writeBundle()`：`ZipInputStream` 遍历，每个 `.apk` 条目 `session.openWrite(name, 0, size)` 拷流 + `fsync`，size 用 -1 流式（zip 条目 size 不可靠）；
- **`PendingIntent` 广播回调**：注册 `BroadcastReceiver` 监听 per-session action `"...STATUS.$sessionId"`（`RECEIVER_NOT_EXPORTED`），`PendingIntent.getBroadcast(..., FLAG_MUTABLE|FLAG_UPDATE_CURRENT)`——**`FLAG_MUTABLE` 必需**，系统才能挂 `EXTRA_INTENT`；
- `STATUS_PENDING_USER_ACTION` → 取 `Intent.EXTRA_INTENT` 加 `FLAG_ACTIVITY_NEW_TASK` 拉起，**不结束**，继续等后续终态；
- `suspendCancellableCoroutine` + `invokeOnCancellation { unregisterReceiver }`，协程友好、防泄漏；
- 任意 `Throwable` → `pm.abandonSession(sessionId)`。

**(b) `ManualTargetedInstaller.kt`** —— 通过 Shizuku 给**指定 userId** 静默安装，最接近“真静默”：
- `HiddenApiHacks.createPackageInstallerForUser(context, userId, overrideInstallerPackageName)` 反射构造绑定到指定 userId 的 `PackageInstaller`，并把 installer 包名设为 `com.android.shell`（shell 身份才静默）；
- **关键教训**：会话**不用 app 的 PendingIntent 提交**，而是用 Shizuku `newProcess` 跑 `pm install-commit <sessionId>`——因为用 app 的 PendingIntent 提交一个 shell 拥有的会话，Android 11+ 会抛 `"Session does not belong to uid"`。**会话归属者必须自己提交。**
- 成功判定：解析 stdout 的 `"Success"`。

**(c) `RootTargetedInstaller.kt`** —— 纯 `pm` shell：`pm install-create --user <id> -r` → 正则提取 sessionId → 逐 split `pm install-write -S <size> <sid> <name> <path>`（旧 pm 用 `cat | pm install-write -S ... -` 管道兜底）→ `pm install-commit` → stdout 含 `"Success"`。

> 注意 (b)(c) 都依赖 root/Shizuku，**不吸收其提权方式**。但要吸收其教训与判定模式：
> - **会话归属者必须自己提交**（我们 system 身份下用本地 `IntentSender` 提交，天然满足）；
> - **`pm`/shell 的 exit code 不可信**，必须同时校验 stdout 的 `"Success"` token（我们走 AIDL 不用 shell，但 `EXTRA_LEGACY_STATUS` 同理要读）。

#### split 智能选择 `applySmartPick`
- Libs：ackpine 的 `arm64_v8a` 与设备 `Build.SUPPORTED_ABIS` 的 `arm64-8a`（下划线 vs 短横）做归一后取优先级最低的；
- Density：与 `displayMetrics.densityDpi` 绝对差最近；
- Locale：匹配 `LocaleListCompat.getDefault()`，并始终保留 `en`；
- Base/Feature/Other 始终选中；用户可在预览页手动勾选。

#### OBB 处理（三策略，安装成功后跑前台 WorkManager）
`ObbExtractor.scan()` 只走 zip local header（不读内容）收集 `.obb` 条目，按文件名约定（`main.1.com.foo.obb`）。策略：Pre-Android11 直写；Shizuku ready 用 shell `cat > path`；有 SAF 授权用 `DocumentFile`；否则提示 `ACTION_OPEN_DOCUMENT_TREE` 并 deep-link 到 `Android/obb/<pkg>/`，授权按包持久化复用。
> OBB 写入需要 `obb` 目录写权限，shell/system 身份才能直写。我们 system 身份可直写 `/sdcard/Android/obb/<pkg>/`（需权限），这部分可吸收。

#### APK 元信息读取 `ApkMetadataReader.kt`
拷到临时文件用 `PackageManager.getPackageArchiveInfo`，**关键**：设置 `appInfo.sourceDir = publicSourceDir = tempFile` 后 `loadIcon/loadLabel` 才可用。bundle 用 Ackpine 找 `Apk.Base` 再读其 URI。还要 `catch Throwable`（非 Exception）绕开 Android 14/15 的 `aconfig_flags.pb` `ExceptionInInitializerError`。

#### 架构模式
- `BaseInstallController` 抽象基类持有完整会话生命周期（进度/终态/历史/源文件删除），子类只重写 `createSession()`——与 InstallerX 的 `IBinderAppInstallerRepoImpl` 异曲同工；
- `InstallerBackendFactory` 接口把所有“碰特权”的操作收口到一个类，便于测试；
- `SessionDataRepository` 用 ~17 个 `combine` 流驱动单一 UI 状态；
- 错误分类 `InstallErrorHelper`：`Aborted/Blocked/Conflict/Incompatible/Invalid/Storage/Timeout/Exceptional/Generic`，清晰可复用。

---

## 3. 可吸收的实现思路（已过滤掉 root/Shizuku）

> 以下均符合“system 权限静默、不依赖第三方特权”约束。

1. **多格式统一解析（策略模式）**：`apk / apks / xapk / apkm` 全部按 ZIP 处理；用类型检测器（看 `manifest.json`/`info.json`/`AndroidManifest.xml`/`base.apk`/`toc.pb`）判定格式，再分发到对应解析策略。一次 `ZipFile` 打开贯穿解析。
2. **split 分类 + 智能选择**：剥离 `split_config.`/`config.`/`split-`/`base-` 前缀，按 ABI / density / locale / feature 分类；按设备 `Build.SUPPORTED_ABIS`、`densityDpi`、`LocaleListCompat.getDefault()` 选最优 split；base/feature 始终装。`.dm` 始终装。
3. **一个会话装一个包的所有 split**：按 `packageName` 分组，base + splits 写同一会话，`openWrite` + `fsync`，一次 `commit`。纯 split 增量更新用 `MODE_INHERIT_EXISTING`，否则 `MODE_FULL_INSTALL`。
4. **本地 `IntentSender` 同步回传结果（吸收 InstallerX `LocalIntentReceiver`）**：实现 `IIntentSender.Stub`，用 `IntentSender(IIntentSender)` 构造，`session.commit(...)` 通过 `Channel`/回调在进程内同步拿结果，**取代 `PendingIntent` 广播**——更干净、无广播注册/注销、无 `RECEIVER_NOT_EXPORTED` 兼容分支。本模块桩里有 `IIntentSender` 与 `IntentSender(IIntentSender)`，可直接用。
5. **自动批准兜底（吸收 `setPermissionsResult`）**：拿到 `IPackageInstaller`（反射读公开 `PackageInstaller.mInstaller`，或 `ServiceManager.getService("package")` → `IPackageManager.packageInstaller`），若仍收到 `STATUS_PENDING_USER_ACTION`，调 `IPackageInstaller.setPermissionsResult(sessionId, true)` 程序化批准，失败再 `abandonSession`。system 身份下几乎不会走到，但作为兜底。
6. **隐藏构造器构造特权 `PackageInstaller`（吸收 InstallerX）**：用 `PackageInstaller(IPackageInstaller, callerPackageName, userId)` 设置 `callerPackageName`/`userId`，实现“安装来源归属”与“指定用户安装”——这是公开 `context.packageInstaller` 做不到的。本模块桩里该 3 参构造器可直接调用（API 31+ 的 4 参版本用反射兜底）。
7. **会话参数增强**：`setInstallReason`（`INSTALL_REASON_USER` 会刷新桌面图标）、`setInstallLocation`、`setOriginatingUid`、隐藏 `installFlags` 加 `INSTALL_REPLACE_EXISTING`、隐藏 `abiOverride` 触发二进制翻译。
8. **错误码精确映射**：读隐藏 `EXTRA_LEGACY_STATUS`，把 `INSTALL_FAILED_*`/`INSTALL_PARSE_FAILED_*` 映射成可读枚举，而非只回传裸 status。
9. **APK 元信息预读（吸收 `ApkMetadataReader`）**：`getPackageArchiveInfo` + 设置 `sourceDir/publicSourceDir` 读包名/版本/图标，做安装前版本比较（降级提示）与签名一致性预检（`hasSigningCertificate`）。
10. **健壮性细节**：Android 14+ `ZipPathValidator.clearCallback()`；zip 条目 size 不可靠时 `openWrite(..., -1)` 流式写；`catch Throwable` 包住 `getPackageArchiveInfo`；零条目回退单 APK；无 `.apk` 后缀单包先改名缓存。
11. **OBB 处理（system 可直写）**：bundle 内 `.obb` 解压到 `/sdcard/Android/obb/<pkg>/`（system 身份有写权限），按文件名约定放置，安装成功后做。
12. **架构收口**：单一入口 + 策略分发 + 统一会话生命周期，把所有隐藏 API 调用集中，便于维护与测试。

---

## 4. ApkInstaller.kt 设计方案

落地文件：`SystemFunction/SystemFunction_src/SystemLib/src/main/java/com/android/systemlib/ApkInstaller.kt`
包：`com.android.systemlib`（与现有 `Syslib.kt` 同包，可与 `installAPK` 共存，逐步替换）。

### 4.1 对外 API
```kotlin
sealed class InstallResult {
    data class Success(val packageName: String, val message: String) : InstallResult()
    data class Failure(val status: Int, val legacyStatus: Int, val message: String, val packageName: String) : InstallResult()
    data class PendingUserAction(val confirmIntent: Intent) : InstallResult() // 走自动批准失败时的最后兜底
}

object ApkInstaller {
    /** 安装任意支持的格式文件，system 静默优先。 */
    fun install(
        context: Context,
        filePath: String,
        userId: Int = Process.myUserHandle().identifier,
        installerPackageName: String? = null,   // 安装来源归属，null=本应用包名
        onProgress: ((percent: Int, message: String) -> Unit)? = null
    ): InstallResult

    /** 卸载（system 静默）。 */
    fun uninstall(context: Context, packageName: String, userId: Int = ...): InstallResult
}
```

### 4.2 内部分层
- **格式检测 `detectFormat(file)`**：返回 `APK / APKS / XAPK / APKM / MULTI_APK_ZIP`。
- **解析 `parseSplits(file, format, context)`**：返回 `List<SplitEntry(name, type, abi, density, locale, isBase, openStream)>`。XAPK 读 `manifest.json` 的 `split_apks[].file/.id`；APKM 读 `info.json`；APKS/MULTI 直接枚举 `.apk` 条目。
- **智能选择 `selectOptimal(splits, context)`**：按设备 ABI/density/locale 过滤，保留 base/feature/`.dm`。
- **会话构建 `openPrivilegedSession(...)`**：优先隐藏构造器 `PackageInstaller(IPackageInstaller, callerPkg, userId)`；失败回退公开 `context.packageManager.packageInstaller`。
- **写入 `writeSplits(session, splits, onProgress)`**：`openWrite` + 拷流 + `fsync`。
- **提交 `commitAndAwait(session, ...)`**：`LocalIntentSender`（`IIntentSender.Stub`）同步拿结果；遇 `STATUS_PENDING_USER_ACTION` → `setPermissionsResult(sessionId, true)` 兜底 → 仍失败返回 `PendingUserAction`。
- **结果映射 `mapResult(intent)`**：读 `EXTRA_STATUS` + 反射读 `EXTRA_LEGACY_STATUS` → `InstallResult`。

### 4.3 静默性保证（无 root/shizuku）
- 应用 system 签名 → `PackageInstaller` 调用即特权身份 → 系统不注入 `STATUS_PENDING_USER_ACTION`；
- 提交用进程内 `IIntentSender`，结果同步回传，不拉 Activity、不发广播；
- 兜底 `setPermissionsResult(true)` 程序化批准；
- 全程不调 `su`/`pm`/Shizuku/`app_process`。

### 4.4 与现有 `installAPK` 的关系
- `ApkInstaller.install` 覆盖 `installAPK` 全部能力并扩展（多格式、split、来源归属、精确错误码）；
- 现有 `installAPK`/`installXAPK` 可保留作为薄封装委派给 `ApkInstaller`，或直接替换；`change(-1,msg)` 回调风格映射到 `InstallResult.Failure`。

### 4.5 风险与版本兼容
- `IIntentSender.send` 的参数签名随版本演进；本模块桩为 7 参（Android 10 期）。实现 `IIntentSender.Stub` 时按桩签名重写，binder transact 由系统侧路由；若极端版本不兼容，回退到 `PendingIntent` 广播（即现有 `installAPK` 的方式）作为第二兜底。
- 隐藏构造器 `PackageInstaller(IPackageInstaller, String, int)` 在 API 31+ 实际为 4 参（多 `String attributionTag`）；用反射先尝试 4 参再回退 3 参。
- `EXTRA_LEGACY_STATUS` 常量未必在桩里，用反射按常量名读其 int 值，失败则置 -1。

---

## 5. 各格式支持一览

| 后缀 | 实质 | 解析方式 | split 处理 |
|------|------|----------|-----------|
| `.apk` | 单 APK | 直接 `PackageInstaller` 会话 | 单条 `base.apk` |
| `.apks` | ZIP(base + splits) | 枚举 `.apk`；识别 `base.apk`/`base-master` | 智能选择 ABI/density/locale |
| `.xapk` | ZIP + `manifest.json` | 读 `manifest.json` 的 `split_apks[].file/.id` | 同上；含 `expansions` 时另处理 OBB |
| `.apkm` | ZIP + `info.json` | 读 `info.json`(`pname`/`versioncode`) | 同上；含 `.dm` 一并装 |
| 多 APK ZIP | ZIP 内多个独立 APK | 每个独立 APK 视为单包，分别建会话 | 按 `packageName` 分组 |

---

## 6. 后续演进
- 引入 `apksig` 做安装前签名一致性预检（V1–V4 + lineage），避免装到一半才因签名冲突失败；
- OEM 错误码扩展（HyperOS/FRP 等）加入 `InstallErrorType` 映射；
- OBB 解压作为安装成功后的可选步骤（system 可直写 `Android/obb/<pkg>/`）；
- 多包批量安装的进度聚合与取消（`abandonSession`）；
- 可选把 `installAPK` 标 `@Deprecated` 委派给 `ApkInstaller`，统一入口。
