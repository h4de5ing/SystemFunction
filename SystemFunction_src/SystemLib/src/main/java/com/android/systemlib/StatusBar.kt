package com.android.systemlib

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

/**
 * 状态栏控制器
 * IStatusBarService.aidl
 * StatusBarManager.java
 */
// ======================== disable() 常量（第一组标志） ========================

/**
 * 无禁用项，恢复所有状态栏功能
 */
const val DISABLE_NONE = 0x00000000

/**
 * 禁用展开通知栏/快捷设置面板
 */
const val DISABLE_EXPAND = 0x00010000

/**
 * 禁用通知图标显示（状态栏右侧的通知图标区域）
 */
const val DISABLE_NOTIFICATION_ICONS = 0x00020000

/**
 * 禁用通知提醒（禁止横幅通知弹出，即“Heads-up”通知）
 */
const val DISABLE_NOTIFICATION_ALERTS = 0x00040000

/**
 * 已废弃，禁用通知滚动文本（Ticker）
 * @deprecated 不再使用
 */
@Deprecated("不再使用")
const val DISABLE_NOTIFICATION_TICKER = 0x00080000

/**
 * 禁用系统状态信息区域（电池、信号、Wi-Fi 等图标）
 */
const val DISABLE_SYSTEM_INFO = 0x00100000

/**
 * 禁用 Home 键（导航栏上的 Home 按钮会被隐藏/不可用）
 */
const val DISABLE_HOME = 0x00200000

/**
 * 禁用返回键
 */
const val DISABLE_BACK = 0x00400000

/**
 * 禁用时钟显示
 */
const val DISABLE_CLOCK = 0x00800000

/**
 * 禁用最近任务键（概览按钮）
 */
const val DISABLE_RECENT = 0x01000000

/**
 * 禁用搜索框（如某些设备上的搜索栏）
 */
const val DISABLE_SEARCH = 0x02000000

/**
 * 禁用进行中的通话气泡提示（状态栏上的通话 Chip）
 */
const val DISABLE_ONGOING_CALL_CHIP = 0x04000000

/**
 * 已废弃，同时禁用 Home 和 Recent 键（等同于 DISABLE_HOME | DISABLE_RECENT）
 * @deprecated 使用 [DISABLE_HOME] 和 [DISABLE_RECENT] 组合替代
 */
@Deprecated("使用 DISABLE_HOME 和 DISABLE_RECENT 组合")
const val DISABLE_NAVIGATION = DISABLE_BACK or DISABLE_HOME or DISABLE_RECENT

/**
 * 所有 disable 标志的掩码（用于校验或获取当前所有禁用项）
 */
const val DISABLE_MASK =
    DISABLE_EXPAND or DISABLE_NOTIFICATION_ICONS or DISABLE_NOTIFICATION_ALERTS or DISABLE_NOTIFICATION_TICKER or DISABLE_SYSTEM_INFO or DISABLE_RECENT or DISABLE_HOME or DISABLE_BACK or DISABLE_CLOCK or DISABLE_SEARCH or DISABLE_ONGOING_CALL_CHIP


// ======================== disable2() 常量（第二组标志） ========================

/**
 * 第二组无禁用项
 */
const val DISABLE2_NONE = 0x00000000

/**
 * 禁用快捷设置面板（Quick Settings），但通知栏仍可展开
 * 值=1
 */
const val DISABLE2_QUICK_SETTINGS = 1

/**
 * 禁用系统图标（如电池、信号等），注意与 [DISABLE_SYSTEM_INFO] 不同，
 * 此标志属于第二组
 * 值=2
 */
const val DISABLE2_SYSTEM_ICONS = 1 shl 1

/**
 * 禁用整个通知栏（无法展开任何面板）
 * 值=4
 */
const val DISABLE2_NOTIFICATION_SHADE = 1 shl 2

/**
 * 禁用全局操作菜单（长按电源键等弹出的关机/重启菜单）
 * 值=8
 */
const val DISABLE2_GLOBAL_ACTIONS = 1 shl 3

/**
 * 禁用旋转建议提示（当用户旋转设备时弹出的建议按钮）
 * 值=16
 */
const val DISABLE2_ROTATE_SUGGESTIONS = 1 shl 4

/**
 * 所有 disable2 标志的掩码
 */
const val DISABLE2_MASK =
    DISABLE2_QUICK_SETTINGS /* 注释调这个，不然关机对话框无法弹出or DISABLE2_SYSTEM_ICONS*/ or DISABLE2_NOTIFICATION_SHADE or DISABLE2_GLOBAL_ACTIONS or DISABLE2_ROTATE_SUGGESTIONS

fun getStatusBarHeight(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= 30) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowMetrics = wm.currentWindowMetrics
        val windowInsets = windowMetrics.windowInsets
        val insets =
            windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
        insets.top
    } else {//TODO 以前怎么获取的高度
        0
    }
}