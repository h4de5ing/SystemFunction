package com.android.android14

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.ActivityTaskManager
import android.app.WindowConfiguration
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * 一键分屏启动器（system app 版）。
 *
 * 关键时序（v4 修复）：
 *   1. 手动点击时，先保留当前 SystemTest Activity 作为临时 split anchor，不再提前移除它。
 *      否则虽然能先后拉起两个应用，但 `FLAG_ACTIVITY_LAUNCH_ADJACENT` 没有锚点可挂，
 *      最终只会看到两个应用顺序启动，而不会真正进入分屏。
 *   2. 先把 primary 以 `MULTI_WINDOW + LAUNCH_ADJACENT` 启到当前 Activity 的对侧，
 *      形成过渡态 [primary, SystemTest]。
 *   3. 等当前 Activity 确认进入 multi-window 后，再从当前 stage 内启动 secondary。
 *      这样 secondary 会继承当前 stage，把 SystemTest 这半边替换掉。
 *   4. 后台/开机场景没有现成 Activity 可当锚点，只能保留 direct-start 的 best-effort 路径。
 *
 * 最终效果：上/左 = primary，下/右 = secondary。
 */
object SplitScreenLauncher14 {

    private const val TAG = "SplitScreenLauncher"

    private const val POLL_INTERVAL_MS = 80L
    private const val POLL_TIMEOUT_MS = 2500L
    private const val PRE_LAUNCH_DELAY_MS = 120L

    fun launchSplitPair(
        context: Context,
        primary: ComponentName,
        secondary: ComponentName,
        onMessage: ((String) -> Unit)? = null,
    ): Boolean {
        val appContext = context.applicationContext
        if (!ActivityTaskManager.supportsSplitScreenMultiWindow(appContext)) {
            onMessage?.invoke("当前系统不支持分屏")
            return false
        }
        if (!isResolvable(appContext, primary) || !isResolvable(appContext, secondary)) {
            onMessage?.invoke("目标应用不存在或未安装：${primary.flattenToShortString()} / ${secondary.flattenToShortString()}")
            return false
        }

        return if (context is Activity && !context.isFinishing) {
            launchFromCallerStage(context, primary, secondary, onMessage)
        } else {
            launchFromBackgroundContext(appContext, primary, secondary, onMessage)
        }
    }

    private fun launchFromCallerStage(
        activity: Activity,
        primary: ComponentName,
        secondary: ComponentName,
        onMessage: ((String) -> Unit)?,
    ): Boolean {
        val handler = Handler(Looper.getMainLooper())

        try {
            startPrimaryAdjacentFromCaller(activity, primary)
        } catch (t: Throwable) {
            Log.e(TAG, "launchFromCallerStage: start primary failed", t)
            onMessage?.invoke("启动 ${primary.flattenToShortString()} 失败: ${t.message}")
            return false
        }

        handler.postDelayed({
            waitForSplitReady(activity, primary, POLL_TIMEOUT_MS, handler) { ok ->
                if (!ok) {
                    Log.w(TAG, "caller 未进入分屏，取消 secondary 替换")
                    onMessage?.invoke("未能进入分屏，请重试")
                    return@waitForSplitReady
                }
                try {
                    startSecondaryInCallerStage(activity, secondary)
                } catch (t: Throwable) {
                    Log.e(TAG, "launchFromCallerStage: start secondary failed", t)
                    onMessage?.invoke("启动 ${secondary.flattenToShortString()} 失败: ${t.message}")
                }
            }
        }, PRE_LAUNCH_DELAY_MS)

        return true
    }

    private fun launchFromBackgroundContext(
        context: Context,
        primary: ComponentName,
        secondary: ComponentName,
        onMessage: ((String) -> Unit)?,
    ): Boolean {
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            try {
                startPrimary(context, primary)
                waitForForeground(context, primary, POLL_TIMEOUT_MS, handler) { ok ->
                    if (!ok) {
                        Log.w(TAG, "primary 未及时上前台，仍尝试发起 adjacent")
                    }
                    startSecondaryAdjacent(context, secondary)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "launchFromBackgroundContext failed", t)
                onMessage?.invoke("启动分屏异常: ${t.message}")
            }
        }, PRE_LAUNCH_DELAY_MS)

        return true
    }

    private fun newMultiWindowOptions(): ActivityOptions {
        val opts = ActivityOptions.makeBasic()
        try {
            ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                ).invoke(opts, WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW)
        } catch (t: Throwable) {
            Log.w(TAG, "setLaunchWindowingMode not available", t)
        }
        return opts
    }

    private fun startPrimary(context: Context, target: ComponentName) {
        val intent = Intent().apply {
            component = target
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
        }
        val opts = newMultiWindowOptions()
        Log.i(TAG, "startPrimary: ${target.flattenToShortString()}")
        context.startActivity(intent, opts.toBundle())
    }

    private fun startPrimaryAdjacentFromCaller(activity: Activity, target: ComponentName) {
        val intent = Intent().apply {
            component = target
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
        }
        val opts = newMultiWindowOptions()
        Log.i(TAG, "startPrimaryAdjacentFromCaller: ${target.flattenToShortString()}")
        activity.startActivity(intent, opts.toBundle())
    }

    private fun startSecondaryInCallerStage(activity: Activity, target: ComponentName) {
        val intent = Intent().apply {
            component = target
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        Log.i(TAG, "startSecondaryInCallerStage: ${target.flattenToShortString()}")
        activity.startActivity(intent)
        if (!activity.isFinishing) {
            activity.finish()
        }
    }

    private fun startSecondaryAdjacent(context: Context, target: ComponentName) {
        val intent = Intent().apply {
            component = target
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
        }
        val opts = newMultiWindowOptions()
        Log.i(TAG, "startSecondaryAdjacent: ${target.flattenToShortString()}")
        context.startActivity(intent, opts.toBundle())
    }

    private fun waitForForeground(
        context: Context,
        target: ComponentName,
        timeoutMs: Long,
        handler: Handler,
        callback: (Boolean) -> Unit,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        val atms = try {
            ActivityTaskManager.getInstance()
        } catch (_: Throwable) {
            null
        }

        val poll = object : Runnable {
            override fun run() {
                if (isOnTop(context, atms, target)) {
                    Log.i(TAG, "primary on top: ${target.flattenToShortString()}")
                    callback(true)
                    return
                }
                if (SystemClock.uptimeMillis() >= deadline) {
                    callback(false)
                    return
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.postDelayed(poll, POLL_INTERVAL_MS)
    }

    private fun waitForSplitReady(
        activity: Activity,
        primary: ComponentName,
        timeoutMs: Long,
        handler: Handler,
        callback: (Boolean) -> Unit,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        val atms = try {
            ActivityTaskManager.getInstance()
        } catch (_: Throwable) {
            null
        }

        val poll = object : Runnable {
            override fun run() {
                val inMultiWindow = try {
                    activity.isInMultiWindowMode
                } catch (_: Throwable) {
                    false
                }
                if (inMultiWindow && hasRunningTask(activity.applicationContext, atms, primary)) {
                    Log.i(
                        TAG, "caller entered split with primary: ${primary.flattenToShortString()}"
                    )
                    callback(true)
                    return
                }
                if (SystemClock.uptimeMillis() >= deadline) {
                    callback(false)
                    return
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.postDelayed(poll, POLL_INTERVAL_MS)
    }

    private fun isOnTop(
        context: Context,
        atms: ActivityTaskManager?,
        target: ComponentName,
    ): Boolean {
        try {
            atms?.getTasks(1, false, true)?.firstOrNull()?.let { top ->
                if (matches(top, target)) return true
            }
        } catch (_: Throwable) {
        }
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getRunningTasks(1).firstOrNull()?.let { top ->
                if (matches(top, target)) return true
            }
        } catch (_: Throwable) {
        }
        return false
    }

    private fun hasRunningTask(
        context: Context,
        atms: ActivityTaskManager?,
        target: ComponentName,
    ): Boolean {
        try {
            atms?.getTasks(10, false, true)?.forEach { task ->
                if (matches(task, target)) return true
            }
        } catch (_: Throwable) {
        }
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getRunningTasks(10).forEach { task ->
                if (matches(task, target)) return true
            }
        } catch (_: Throwable) {
        }
        return false
    }

    private fun matches(info: ActivityManager.RunningTaskInfo, target: ComponentName): Boolean {
        val top = info.topActivity ?: return false
        return top.packageName == target.packageName
    }

    private fun isResolvable(context: Context, component: ComponentName): Boolean {
        return try {
            context.packageManager.getActivityInfo(component, 0)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
