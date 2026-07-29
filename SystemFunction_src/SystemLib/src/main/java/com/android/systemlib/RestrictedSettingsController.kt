package com.android.systemlib

import android.content.Context
import android.os.Build
import android.os.ServiceManager
import android.util.Log
import com.android.internal.app.IAppOpsService

/**
 * Android 13+ “受限制的设置”控制器。
 *
 * 通过文件管理器或浏览器侧载 APK 时，PackageInstaller 可能将
 * ACCESS_RESTRICTED_SETTINGS 设为 MODE_IGNORED，导致无障碍服务即使已经写入
 * Settings.Secure 也无法启用或绑定。
 *
 * 该操作需要 MANAGE_APP_OPS_MODES，仅适用于具有平台权限的系统应用。
 */
object RestrictedSettingsController {
    private const val TAG = "RestrictedSettings"

    // AppProtoEnums.APP_OP_ACCESS_RESTRICTED_SETTINGS，Android 13+。
    private const val OP_ACCESS_RESTRICTED_SETTINGS = 119

    data class AllowResult(
        val supported: Boolean,
        val succeeded: Boolean,
        val errorMessage: String? = null,
    )

    /**
     * 仅允许当前应用包访问受限制的设置，不设置 UID mode，避免影响共享 UID 的其他应用。
     */
    fun allowForSelf(context: Context): AllowResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return AllowResult(supported = false, succeeded = true)
        }

        return try {
            val appOpsService = IAppOpsService.Stub.asInterface(
                ServiceManager.getService(Context.APP_OPS_SERVICE),
            )
            appOpsService.setMode(
                OP_ACCESS_RESTRICTED_SETTINGS,
                context.applicationInfo.uid,
                context.packageName,
                MODE_ALLOWED,
            )
            Log.i(TAG, "已允许本应用访问受限制的设置")
            AllowResult(supported = true, succeeded = true)
        } catch (e: Exception) {
            Log.e(TAG, "允许受限制的设置失败: ${e.message}", e)
            AllowResult(
                supported = true,
                succeeded = false,
                errorMessage = e.message ?: e.javaClass.simpleName,
            )
        }
    }
}
