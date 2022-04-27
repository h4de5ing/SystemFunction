package com.android.systemfunction.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.android.systemfunction.BuildConfig
import com.android.systemfunction.utils.isDisableScreenCapture
import com.android.systemfunction.utils.isDisableScreenShot
import com.android.systemfunction.utils.setBooleanChange

//TODO 会抢占一些dialog的焦点，导致dialog无法点击
//https://blog.csdn.net/weixin_43766753/article/details/108350589
class OnePixelWindowService : Service() {
    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    private var mOnePixelView: View? = null
    private var mWindowManager: WindowManager? = null
    override fun onCreate() {
        super.onCreate()
        addOnePixelView()
        setBooleanChange { disableScreen(it) }
    }

    private val WINDOW_SIZE = if (BuildConfig.DEBUG) 10 else 1
    private val WINDOW_COLOR: Int = Color.TRANSPARENT
    private val params = WindowManager.LayoutParams()
    private fun addOnePixelView() {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = mWindowManager!!.defaultDisplay
//        val params = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        params.format = PixelFormat.RGBA_8888
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_SECURE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        params.gravity = Gravity.START or Gravity.TOP
        params.x = 0
        params.y = 0
//        params.width = WINDOW_SIZE
//        params.height = WINDOW_SIZE
        params.width = display.width
        params.height = display.height//TODO 需要在加一个状态栏和导航栏的高度
        mOnePixelView = View(this)//改成TextView就可以一直悬浮了
//        mOnePixelView!!.text = "根据安全规则，禁止截屏和录屏"
//        mOnePixelView!!.setTextColor(Color.RED)
        mOnePixelView!!.setBackgroundColor(WINDOW_COLOR)
        mWindowManager!!.addView(mOnePixelView, params)
        disableScreen(isDisableScreenShot || isDisableScreenCapture)
    }

    private fun disableScreen(isDisable: Boolean) {
        println("是否禁用:${isDisable}")
        setFlags(
            params,
            if (isDisable) WindowManager.LayoutParams.FLAG_SECURE
            else 0,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        updateView()
    }

    private fun setFlags(params: WindowManager.LayoutParams, flags: Int, mask: Int) {
        try {
            params.flags = params.flags and mask.inv() or (flags and mask)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOnPixelView()
    }

    private fun updateView() = mWindowManager!!.updateViewLayout(mOnePixelView, params)

    private fun removeOnPixelView() {
        if (mOnePixelView != null && mWindowManager != null) {
            mWindowManager?.removeViewImmediate(mOnePixelView)
            mOnePixelView = null
        }
    }
}