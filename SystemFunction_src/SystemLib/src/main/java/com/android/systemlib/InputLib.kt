package com.android.systemlib

import android.content.Context.INPUT_SERVICE
import android.hardware.input.IInputManager
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties

//主要封装一些模拟[键盘,鼠标,触摸,滑动]事件,以上事件均需要System权限,如果没有System权限可以采用辅助功能实现
fun String.logI() = println(this)
private var iInput: IInputManager? = null
fun injectInit() {
    try {
        iInput = IInputManager.Stub.asInterface(ServiceManager.getService(INPUT_SERVICE))
    } catch (e: RemoteException) {
        e.printStackTrace()
    }
}

fun injectMotionEvent(action: Int, x: Float, y: Float) {
    val now = SystemClock.uptimeMillis()
    val event = MotionEvent.obtain(now, now, action, x, y, 0)
    try {
        iInput?.injectInputEvent(event, 0)
        val actionStr = when (action) {
            MotionEvent.ACTION_DOWN -> "按下"
            MotionEvent.ACTION_UP -> "抬起"
            MotionEvent.ACTION_MOVE -> "移动"
            else -> "未知动作"
        }
        "注入鼠标点击事件成功,$actionStr,$x,$y".logI()
    } catch (e: RemoteException) {
        e.printStackTrace()
    } finally {
        event.recycle()
    }
}

fun injectScrollEvent(x: Float, y: Float, deltaY: Float) {
    val now = SystemClock.uptimeMillis()
    val pointerProperties = arrayOfNulls<PointerProperties>(1)
    pointerProperties[0] = PointerProperties()
    pointerProperties[0]!!.id = 0
    pointerProperties[0]!!.toolType = MotionEvent.TOOL_TYPE_MOUSE

    val pointerCoords = arrayOfNulls<PointerCoords>(1)
    pointerCoords[0] = PointerCoords()
    pointerCoords[0]!!.x = x
    pointerCoords[0]!!.y = y
    pointerCoords[0]!!.setAxisValue(MotionEvent.AXIS_VSCROLL, deltaY)
    val event = MotionEvent.obtain(
        now,
        now,
        MotionEvent.ACTION_SCROLL,
        1,
        pointerProperties,
        pointerCoords,
        0,
        0,
        1.0f,
        1.0f,
        0,
        0,
        InputDevice.SOURCE_MOUSE,
        0
    )

    try {
        iInput?.injectInputEvent(event, 0)
//        "注入滚动事件成功,$x,$y".logI()
    } catch (e: RemoteException) {
        e.printStackTrace()
    } finally {
        event.recycle()
    }
}

fun injectKeyEvent(action: Int, key: String, code: Int) {
    try {
        if (code > 0) {
            val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
            val events = keyCharacterMap.getEvents(charArrayOf(key[0]))
            val keyCode = events[0].keyCode
            val downTime = SystemClock.uptimeMillis()
            val event = KeyEvent(
                downTime,
                SystemClock.uptimeMillis(),
                action,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD
            )
            "注入键盘事件成功,key=$key,code=$code->${keyCode}".logI()
            iInput?.injectInputEvent(event, 0)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 根据Android keycode注入
 */
fun injectKeyEvent(action: Int, keyCode: Int) {
    try {
        if (keyCode > 0) {
            val downTime = SystemClock.uptimeMillis()
            val event = KeyEvent(
                downTime,
                SystemClock.uptimeMillis(),
                action,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD
            )
            "注入KeyCode事件成功,keyCode=$keyCode".logI()
            iInput?.injectInputEvent(event, 0)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}