package com.android.systemlib

import android.app.Instrumentation
import android.content.Context.INPUT_SERVICE
import android.hardware.input.IInputManager
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import java.lang.reflect.Method

private const val TAG = "InputLib"

private var iInput: IInputManager? = null

// Cached reflection method for InputEvent.setDisplayId(int) — hidden API
private var setDisplayIdMethod: Method? = null

private fun setDisplayId(event: InputEvent, displayId: Int) {
    if (displayId < 0) return
    try {
        if (setDisplayIdMethod == null) {
            setDisplayIdMethod = InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        }
        setDisplayIdMethod!!.invoke(event, displayId)
    } catch (e: Exception) {
        Log.e(TAG, "setDisplayId failed", e)
    }
}

fun injectInit() {
    try {
        iInput = IInputManager.Stub.asInterface(ServiceManager.getService(INPUT_SERVICE))
        Log.i(TAG, "IInputManager acquired")
    } catch (e: RemoteException) {
        Log.e(TAG, "injectInit failed", e)
    }
}

// Bug fix: track downTime across ACTION_DOWN/MOVE/UP so drags work correctly.
// Original InputLib.kt called SystemClock.uptimeMillis() for both downTime and eventTime
// on every call, meaning each MOVE/UP got a fresh downTime — breaking drag gestures.
private var touchDownTime = 0L

/**
 * Inject a touch/mouse motion event.
 *
 * @param action    MotionEvent.ACTION_DOWN / ACTION_MOVE / ACTION_UP
 * @param x         Absolute X in device pixels
 * @param y         Absolute Y in device pixels
 * @param displayId Target display ID. Pass -1 (default) for the default display.
 *                  Pass the VirtualDisplay ID to route input to a virtual display.
 */
fun injectMotionEvent2(action: Int, x: Float, y: Float, displayId: Int = -1) {
    val now = SystemClock.uptimeMillis()

    // Bug fix: preserve downTime for the lifetime of a touch gesture.
    if (action == MotionEvent.ACTION_DOWN) touchDownTime = now
    val downTime = if (touchDownTime > 0) touchDownTime else now

    val pointerProperties = arrayOfNulls<PointerProperties>(1).apply {
        this[0] = PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
    }
    val pointerCoords = arrayOfNulls<PointerCoords>(1).apply {
        this[0] = PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1.0f
            size = 1.0f
        }
    }

    val event = try {
        MotionEvent.obtain(
            downTime, now, action, 1,
            pointerProperties, pointerCoords,
            0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create MotionEvent", e)
        return
    }

    setDisplayId(event, displayId)

    try {
        iInput?.injectInputEvent(event, 0)
    } catch (e: RemoteException) {
        Log.e(TAG, "injectMotionEvent2 failed: action=$action x=$x y=$y", e)
    } finally {
        event.recycle()
    }

    if (action == MotionEvent.ACTION_UP) touchDownTime = 0L
}

/**
 * Inject a scroll event (mouse wheel).
 *
 * @param deltaY    Normalized scroll amount (positive = scroll down).
 *                  Tip: pass -browserDeltaY / 120f from the browser wheel event.
 * @param displayId Target display ID, or -1 for default display.
 */
fun injectScrollEvent(x: Float, y: Float, deltaY: Float, displayId: Int = -1) {
    val now = SystemClock.uptimeMillis()
    val pointerProperties = arrayOfNulls<PointerProperties>(1).apply {
        this[0] = PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
    }
    val pointerCoords = arrayOfNulls<PointerCoords>(1).apply {
        this[0] = PointerCoords().apply {
            this.x = x
            this.y = y
            setAxisValue(MotionEvent.AXIS_VSCROLL, deltaY)
        }
    }
    val event = MotionEvent.obtain(
        now, now, MotionEvent.ACTION_SCROLL, 1,
        pointerProperties, pointerCoords,
        0, 0, 1f, 1f, 0, 0,
        InputDevice.SOURCE_MOUSE, 0
    )

    setDisplayId(event, displayId)

    try {
        iInput?.injectInputEvent(event, 0)
    } catch (e: RemoteException) {
        Log.e(TAG, "injectScrollEvent failed", e)
    } finally {
        event.recycle()
    }
}

/**
 * Inject a keyboard event using a JS keyCode mapped to an Android KeyEvent keycode.
 *
 * @param action  KeyEvent.ACTION_DOWN or KeyEvent.ACTION_UP
 * @param code    JS browser keyCode (e.g. 13 = Enter, 65 = A)
 */
fun injectKeyEvent(action: Int, code: Int) {
    val androidKeyCode = JS_KEYCODE_TO_ANDROID[code]
    if (androidKeyCode == null) {
        Log.d(TAG, "No Android mapping for JS keyCode=$code")
        return
    }
    val now = SystemClock.uptimeMillis()
    val event = KeyEvent(
        now, now, action, androidKeyCode, 0, 0,
        KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
        InputDevice.SOURCE_KEYBOARD
    )
    try {
        iInput?.injectInputEvent(event, 0)
    } catch (e: Exception) {
        Log.e(TAG, "injectKeyEvent failed: code=$code->$androidKeyCode", e)
    }
}

/**
 * Inject a key down+up by Android keycode directly (e.g. for Home/Back shortcuts).
 * When displayId >= 0, routes the event to that display via IInputManager (supports virtual display).
 * Falls back to Instrumentation when IInputManager is unavailable.
 */
fun injectKeyEventByAndroidCode(keyCode: Int, displayId: Int = -1) {
    val now = SystemClock.uptimeMillis()
    val im = iInput
    if (im != null) {
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val event = KeyEvent(
                now, now, action, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD
            )
            setDisplayId(event, displayId)
            try {
                im.injectInputEvent(event, 0)
            } catch (e: Exception) {
                Log.e(TAG, "injectKeyEventByAndroidCode failed: keyCode=$keyCode", e)
            }
        }
    } else {
        // Fallback: Instrumentation does not support displayId
        try {
            Instrumentation().sendKeyDownUpSync(keyCode)
        } catch (e: Exception) {
            Log.e(TAG, "injectKeyEventByAndroidCode fallback failed: keyCode=$keyCode", e)
        }
    }
}

/**
 * Raw injection — pass any InputEvent directly.
 */
fun injectEvent(inputEvent: InputEvent) {
    try {
        iInput?.injectInputEvent(inputEvent, 0)
    } catch (e: Exception) {
        Log.e(TAG, "injectEvent failed", e)
    }
}

/**
 * Complete JS keyCode → Android KeyEvent mapping.
 * Source: MDN keyCode values + Android keycodes.h
 * 完整 JS keyCode → Android KeyEvent 映射表（按 JS keyCode 升序）
 * 来源：MDN + Android keycodes.h
 * https://android.googlesource.com/platform/frameworks/native/+/master/include/android/keycodes.h
 */
val JS_KEYCODE_TO_ANDROID = mapOf(
    3   to KeyEvent.KEYCODE_BREAK,
    8   to KeyEvent.KEYCODE_DEL,
    9   to KeyEvent.KEYCODE_TAB,
    12  to KeyEvent.KEYCODE_CLEAR,
    13  to KeyEvent.KEYCODE_ENTER,
    16  to KeyEvent.KEYCODE_SHIFT_LEFT,
    17  to KeyEvent.KEYCODE_CTRL_LEFT,
    18  to KeyEvent.KEYCODE_ALT_LEFT,
    19  to KeyEvent.KEYCODE_BREAK,
    20  to KeyEvent.KEYCODE_CAPS_LOCK,
    27  to KeyEvent.KEYCODE_ESCAPE,
    32  to KeyEvent.KEYCODE_SPACE,
    33  to KeyEvent.KEYCODE_PAGE_UP,
    34  to KeyEvent.KEYCODE_PAGE_DOWN,
    35  to KeyEvent.KEYCODE_MOVE_END,
    36  to KeyEvent.KEYCODE_MOVE_HOME,
    37  to KeyEvent.KEYCODE_DPAD_LEFT,
    38  to KeyEvent.KEYCODE_DPAD_UP,
    39  to KeyEvent.KEYCODE_DPAD_RIGHT,
    40  to KeyEvent.KEYCODE_DPAD_DOWN,
    41  to KeyEvent.KEYCODE_STAR,
    44  to KeyEvent.KEYCODE_SYSRQ,
    45  to KeyEvent.KEYCODE_INSERT,
    46  to KeyEvent.KEYCODE_FORWARD_DEL,
    47  to KeyEvent.KEYCODE_HELP,
    48  to KeyEvent.KEYCODE_0,
    49  to KeyEvent.KEYCODE_1,
    50  to KeyEvent.KEYCODE_2,
    51  to KeyEvent.KEYCODE_3,
    52  to KeyEvent.KEYCODE_4,
    53  to KeyEvent.KEYCODE_5,
    54  to KeyEvent.KEYCODE_6,
    55  to KeyEvent.KEYCODE_7,
    56  to KeyEvent.KEYCODE_8,
    57  to KeyEvent.KEYCODE_9,
    58  to KeyEvent.KEYCODE_SEMICOLON,
    59  to KeyEvent.KEYCODE_SEMICOLON,
    60  to KeyEvent.KEYCODE_SHIFT_RIGHT,
    61  to KeyEvent.KEYCODE_EQUALS,
    62  to KeyEvent.KEYCODE_SPACE,
    63  to KeyEvent.KEYCODE_SLASH,
    64  to KeyEvent.KEYCODE_AT,
    65  to KeyEvent.KEYCODE_A,
    66  to KeyEvent.KEYCODE_B,
    67  to KeyEvent.KEYCODE_C,
    68  to KeyEvent.KEYCODE_D,
    69  to KeyEvent.KEYCODE_E,
    70  to KeyEvent.KEYCODE_F,
    71  to KeyEvent.KEYCODE_G,
    72  to KeyEvent.KEYCODE_H,
    73  to KeyEvent.KEYCODE_I,
    74  to KeyEvent.KEYCODE_J,
    75  to KeyEvent.KEYCODE_K,
    76  to KeyEvent.KEYCODE_L,
    77  to KeyEvent.KEYCODE_M,
    78  to KeyEvent.KEYCODE_N,
    79  to KeyEvent.KEYCODE_O,
    80  to KeyEvent.KEYCODE_P,
    81  to KeyEvent.KEYCODE_Q,
    82  to KeyEvent.KEYCODE_R,
    83  to KeyEvent.KEYCODE_S,
    84  to KeyEvent.KEYCODE_T,
    85  to KeyEvent.KEYCODE_U,
    86  to KeyEvent.KEYCODE_V,
    87  to KeyEvent.KEYCODE_W,
    88  to KeyEvent.KEYCODE_X,
    89  to KeyEvent.KEYCODE_Y,
    90  to KeyEvent.KEYCODE_Z,
    91  to KeyEvent.KEYCODE_META_LEFT,
    92  to KeyEvent.KEYCODE_META_RIGHT,
    93  to KeyEvent.KEYCODE_MENU,
    96  to KeyEvent.KEYCODE_NUMPAD_0,
    97  to KeyEvent.KEYCODE_NUMPAD_1,
    98  to KeyEvent.KEYCODE_NUMPAD_2,
    99  to KeyEvent.KEYCODE_NUMPAD_3,
    100 to KeyEvent.KEYCODE_NUMPAD_4,
    101 to KeyEvent.KEYCODE_NUMPAD_5,
    102 to KeyEvent.KEYCODE_NUMPAD_6,
    103 to KeyEvent.KEYCODE_NUMPAD_7,
    104 to KeyEvent.KEYCODE_NUMPAD_8,
    105 to KeyEvent.KEYCODE_NUMPAD_9,
    106 to KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
    107 to KeyEvent.KEYCODE_NUMPAD_ADD,
    108 to KeyEvent.KEYCODE_NUMPAD_COMMA,
    109 to KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
    110 to KeyEvent.KEYCODE_NUMPAD_DOT,
    111 to KeyEvent.KEYCODE_NUMPAD_DIVIDE,
    112 to KeyEvent.KEYCODE_F1,
    113 to KeyEvent.KEYCODE_F2,
    114 to KeyEvent.KEYCODE_F3,
    115 to KeyEvent.KEYCODE_F4,
    116 to KeyEvent.KEYCODE_F5,
    117 to KeyEvent.KEYCODE_F6,
    118 to KeyEvent.KEYCODE_F7,
    119 to KeyEvent.KEYCODE_F8,
    120 to KeyEvent.KEYCODE_F9,
    121 to KeyEvent.KEYCODE_F10,
    122 to KeyEvent.KEYCODE_F11,
    123 to KeyEvent.KEYCODE_F12,
    144 to KeyEvent.KEYCODE_NUM_LOCK,
    145 to KeyEvent.KEYCODE_SCROLL_LOCK,
    160 to KeyEvent.KEYCODE_POUND,
    161 to KeyEvent.KEYCODE_PLUS,
    162 to KeyEvent.KEYCODE_MENU,
    163 to KeyEvent.KEYCODE_POUND,
    164 to KeyEvent.KEYCODE_STAR,
    165 to KeyEvent.KEYCODE_MINUS,
    166 to KeyEvent.KEYCODE_CLEAR,
    167 to KeyEvent.KEYCODE_LEFT_BRACKET,
    168 to KeyEvent.KEYCODE_RIGHT_BRACKET,
    169 to KeyEvent.KEYCODE_APOSTROPHE,
    170 to KeyEvent.KEYCODE_GRAVE,
    171 to KeyEvent.KEYCODE_PLUS,
    172 to KeyEvent.KEYCODE_MINUS,
    173 to KeyEvent.KEYCODE_EQUALS,
    174 to KeyEvent.KEYCODE_LEFT_BRACKET,
    175 to KeyEvent.KEYCODE_RIGHT_BRACKET,
    176 to KeyEvent.KEYCODE_BACKSLASH,
    177 to KeyEvent.KEYCODE_SEMICOLON,
    178 to KeyEvent.KEYCODE_APOSTROPHE,
    179 to KeyEvent.KEYCODE_COMMA,
    180 to KeyEvent.KEYCODE_PERIOD,
    181 to KeyEvent.KEYCODE_SLASH,
    186 to KeyEvent.KEYCODE_SEMICOLON,
    187 to KeyEvent.KEYCODE_EQUALS,
    188 to KeyEvent.KEYCODE_COMMA,
    189 to KeyEvent.KEYCODE_MINUS,
    190 to KeyEvent.KEYCODE_PERIOD,
    191 to KeyEvent.KEYCODE_SLASH,
    192 to KeyEvent.KEYCODE_GRAVE,
    219 to KeyEvent.KEYCODE_LEFT_BRACKET,
    220 to KeyEvent.KEYCODE_BACKSLASH,
    221 to KeyEvent.KEYCODE_RIGHT_BRACKET,
    222 to KeyEvent.KEYCODE_APOSTROPHE
)
