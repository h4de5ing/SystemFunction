package com.android.systemlib

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

/**
 * 无障碍服务开关控制器（基于 Settings.Secure 的启用/禁用）。
 *
 * 适用场景：应用作为系统级应用（持有 WRITE_SECURE_SETTINGS 权限）时，通过直接写
 * Settings.Secure 来启用/禁用指定无障碍服务，并在服务条目已残留但进程未运行
 * （如 adb install -r 替换安装、设备重启后作为 Launcher）时强制重写以触发系统重新绑定。
 * Android 13+ 启用服务前还会解除当前包因侧载产生的“受限制的设置”AppOp。
 *
 * 注意：本工具类只负责 Settings 层面的开关，不感知服务进程是否真正连接。
 * 调用方如需判断“服务是否真正生效”，应结合自身服务的 onServiceConnected 状态。
 */
object AccessibilityServiceController {
    private const val TAG = "AccessibilityServiceController"

    /**
     * 控制结果。
     *
     * @param alreadyInTargetState 操作前是否已处于目标状态（启用时仍会强制写入以触发系统重新绑定）
     * @param settingsWritten enabled_accessibility_services 是否写入成功
     * @param switchWritten accessibility_enabled 是否写入成功
     * @param errorMessage 失败时的错误信息
     */
    data class ToggleResult(
        val alreadyInTargetState: Boolean,
        val settingsWritten: Boolean,
        val switchWritten: Boolean,
        val errorMessage: String? = null,
    )

    /**
     * 获取无障碍服务的 ComponentName 扁平字符串（写入 Settings.Secure 的格式）。
     */
    fun getServiceId(
        context: Context,
        serviceClass: Class<out AccessibilityService>,
    ): String = ComponentName(context, serviceClass).flattenToString()

    /**
     * 服务是否已在系统已启用列表中（通过 AccessibilityManager 与 Settings 双重判断）。
     */
    fun isServiceEnabled(
        context: Context,
        serviceClass: Class<out AccessibilityService>,
    ): Boolean {
        val serviceId = getServiceId(context, serviceClass)
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledByManager = accessibilityManager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { it.id == serviceId }
            ?: false
        return enabledByManager || getEnabledServiceIds(context).contains(serviceId)
    }

    /**
     * 确保无障碍服务已启用。
     *
     * 始终写入 Settings.Secure，因为 putString 会触发系统重新绑定无障碍服务。仅检查
     * [isServiceEnabled] 就跳过写入会导致：当 Settings 中已残留服务 ID 但服务实际未运行时
     * （如 adb install -r 替换安装、设备重启后作为 Launcher），系统不会收到变更通知，
     * 服务永远不会被绑定。因此无论当前是否已启用，都强制幂等写入一次。
     */
    fun ensureServiceEnabled(
        context: Context,
        serviceClass: Class<out AccessibilityService>,
    ): ToggleResult = setServiceEnabled(context, serviceClass, enabled = true)

    /**
     * 禁用无障碍服务。若服务本就未启用则直接返回成功。
     */
    fun disableService(
        context: Context,
        serviceClass: Class<out AccessibilityService>,
    ): ToggleResult {
        val serviceId = getServiceId(context, serviceClass)
        if (!getEnabledServiceIds(context).contains(serviceId) &&
            !isServiceEnabled(context, serviceClass)
        ) {
            return ToggleResult(
                alreadyInTargetState = true,
                settingsWritten = true,
                switchWritten = true,
            )
        }
        return setServiceEnabled(context, serviceClass, enabled = false)
    }

    private fun setServiceEnabled(
        context: Context,
        serviceClass: Class<out AccessibilityService>,
        enabled: Boolean,
    ): ToggleResult {
        return try {
            if (enabled) {
                val restrictedSettingsResult =
                    RestrictedSettingsController.allowForSelf(context)
                if (!restrictedSettingsResult.succeeded) {
                    Log.w(
                        TAG,
                        "解除受限制的设置失败，继续尝试写入无障碍设置: " +
                            restrictedSettingsResult.errorMessage,
                    )
                }
            }

            val serviceId = getServiceId(context, serviceClass)
            val enabledServices = getEnabledServiceIds(context).toMutableSet()
            val alreadyEnabled = enabledServices.contains(serviceId)

            if (enabled && alreadyEnabled) {
                val servicesWithoutTarget = enabledServices
                    .filterNot { it == serviceId }
                    .joinToString(separator = ":")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    servicesWithoutTarget,
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    if (servicesWithoutTarget.isBlank()) 0 else 1,
                )
            }

            if (enabled) {
                enabledServices += serviceId
            } else {
                enabledServices -= serviceId
            }

            val encodedServices = enabledServices.joinToString(separator = ":")
            val settingsWritten = Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                encodedServices,
            )
            val switchWritten = Settings.Secure.putInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                if (enabledServices.isEmpty()) 0 else 1,
            )

            Log.d(
                TAG,
                "setServiceEnabled enabled=$enabled serviceId=$serviceId " +
                    "settingsWritten=$settingsWritten switchWritten=$switchWritten"
            )

            ToggleResult(
                alreadyInTargetState = alreadyEnabled == enabled,
                settingsWritten = settingsWritten,
                switchWritten = switchWritten,
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "写入安全设置失败: ${e.message}", e)
            ToggleResult(
                alreadyInTargetState = false,
                settingsWritten = false,
                switchWritten = false,
                errorMessage = e.message ?: e.javaClass.simpleName,
            )
        } catch (e: Exception) {
            Log.e(TAG, "切换无障碍服务失败: ${e.message}", e)
            ToggleResult(
                alreadyInTargetState = false,
                settingsWritten = false,
                switchWritten = false,
                errorMessage = e.message ?: e.javaClass.simpleName,
            )
        }
    }

    private fun getEnabledServiceIds(context: Context): LinkedHashSet<String> {
        val rawValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return rawValue
            .split(':')
            .filter { it.isNotBlank() }
            .toCollection(LinkedHashSet())
    }
}
