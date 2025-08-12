package com.android.systemlib

import android.view.KeyEvent

/**
 * 完整 JS keyCode → Android KeyEvent 映射表（按 JS keyCode 升序）
 * 来源：MDN + Android keycodes.h
 * https://android.googlesource.com/platform/frameworks/native/+/master/include/android/keycodes.h
 */
val JS_KEYCODE_TO_ANDROID = mapOf(
    3   to KeyEvent.KEYCODE_BREAK,        // Cancel
    8   to KeyEvent.KEYCODE_DEL,          // Backspace
    9   to KeyEvent.KEYCODE_TAB,          // Tab
    12  to KeyEvent.KEYCODE_CLEAR,        // Clear
    13  to KeyEvent.KEYCODE_ENTER,        // Enter
    16  to KeyEvent.KEYCODE_SHIFT_LEFT,   // Shift
    17  to KeyEvent.KEYCODE_CTRL_LEFT,    // Control
    18  to KeyEvent.KEYCODE_ALT_LEFT,     // Alt
    19  to KeyEvent.KEYCODE_BREAK,        // Pause
    20  to KeyEvent.KEYCODE_CAPS_LOCK,    // CapsLock
    27  to KeyEvent.KEYCODE_ESCAPE,       // Escape
    32  to KeyEvent.KEYCODE_SPACE,        // Space
    33  to KeyEvent.KEYCODE_PAGE_UP,      // PageUp
    34  to KeyEvent.KEYCODE_PAGE_DOWN,    // PageDown
    35  to KeyEvent.KEYCODE_MOVE_END,     // End
    36  to KeyEvent.KEYCODE_MOVE_HOME,    // Home
    37  to KeyEvent.KEYCODE_DPAD_LEFT,    // ArrowLeft
    38  to KeyEvent.KEYCODE_DPAD_UP,      // ArrowUp
    39  to KeyEvent.KEYCODE_DPAD_RIGHT,   // ArrowRight
    40  to KeyEvent.KEYCODE_DPAD_DOWN,    // ArrowDown
    41  to KeyEvent.KEYCODE_STAR,         // Select
    44  to KeyEvent.KEYCODE_SYSRQ,        // PrintScreen
    45  to KeyEvent.KEYCODE_INSERT,       // Insert
    46  to KeyEvent.KEYCODE_FORWARD_DEL,  // Delete
    47  to KeyEvent.KEYCODE_HELP,         // Help
    48  to KeyEvent.KEYCODE_0,            // Digit0
    49  to KeyEvent.KEYCODE_1,            // Digit1
    50  to KeyEvent.KEYCODE_2,            // Digit2
    51  to KeyEvent.KEYCODE_3,            // Digit3
    52  to KeyEvent.KEYCODE_4,            // Digit4
    53  to KeyEvent.KEYCODE_5,            // Digit5
    54  to KeyEvent.KEYCODE_6,            // Digit6
    55  to KeyEvent.KEYCODE_7,            // Digit7
    56  to KeyEvent.KEYCODE_8,            // Digit8
    57  to KeyEvent.KEYCODE_9,            // Digit9
    58  to KeyEvent.KEYCODE_SEMICOLON,    // Colon
    59  to KeyEvent.KEYCODE_SEMICOLON,    // Semicolon
    60  to KeyEvent.KEYCODE_SHIFT_RIGHT,         // Less
    61  to KeyEvent.KEYCODE_EQUALS,       // Equal
    62  to KeyEvent.KEYCODE_SPACE,      // Greater
    63  to KeyEvent.KEYCODE_SLASH,        // Question
    64  to KeyEvent.KEYCODE_AT,           // At
    65  to KeyEvent.KEYCODE_A,            // KeyA
    66  to KeyEvent.KEYCODE_B,            // KeyB
    67  to KeyEvent.KEYCODE_C,            // KeyC
    68  to KeyEvent.KEYCODE_D,            // KeyD
    69  to KeyEvent.KEYCODE_E,            // KeyE
    70  to KeyEvent.KEYCODE_F,            // KeyF
    71  to KeyEvent.KEYCODE_G,            // KeyG
    72  to KeyEvent.KEYCODE_H,            // KeyH
    73  to KeyEvent.KEYCODE_I,            // KeyI
    74  to KeyEvent.KEYCODE_J,            // KeyJ
    75  to KeyEvent.KEYCODE_K,            // KeyK
    76  to KeyEvent.KEYCODE_L,            // KeyL
    77  to KeyEvent.KEYCODE_M,            // KeyM
    78  to KeyEvent.KEYCODE_N,            // KeyN
    79  to KeyEvent.KEYCODE_O,            // KeyO
    80  to KeyEvent.KEYCODE_P,            // KeyP
    81  to KeyEvent.KEYCODE_Q,            // KeyQ
    82  to KeyEvent.KEYCODE_R,            // KeyR
    83  to KeyEvent.KEYCODE_S,            // KeyS
    84  to KeyEvent.KEYCODE_T,            // KeyT
    85  to KeyEvent.KEYCODE_U,            // KeyU
    86  to KeyEvent.KEYCODE_V,            // KeyV
    87  to KeyEvent.KEYCODE_W,            // KeyW
    88  to KeyEvent.KEYCODE_X,            // KeyX
    89  to KeyEvent.KEYCODE_Y,            // KeyY
    90  to KeyEvent.KEYCODE_Z,            // KeyZ
    91  to KeyEvent.KEYCODE_META_LEFT,    // MetaLeft
    92  to KeyEvent.KEYCODE_META_RIGHT,   // MetaRight
    93  to KeyEvent.KEYCODE_MENU,         // ContextMenu
    96  to KeyEvent.KEYCODE_NUMPAD_0,     // Numpad0
    97  to KeyEvent.KEYCODE_NUMPAD_1,     // Numpad1
    98  to KeyEvent.KEYCODE_NUMPAD_2,     // Numpad2
    99  to KeyEvent.KEYCODE_NUMPAD_3,     // Numpad3
    100 to KeyEvent.KEYCODE_NUMPAD_4,     // Numpad4
    101 to KeyEvent.KEYCODE_NUMPAD_5,     // Numpad5
    102 to KeyEvent.KEYCODE_NUMPAD_6,     // Numpad6
    103 to KeyEvent.KEYCODE_NUMPAD_7,     // Numpad7
    104 to KeyEvent.KEYCODE_NUMPAD_8,     // Numpad8
    105 to KeyEvent.KEYCODE_NUMPAD_9,     // Numpad9
    106 to KeyEvent.KEYCODE_NUMPAD_MULTIPLY, // NumpadMultiply
    107 to KeyEvent.KEYCODE_NUMPAD_ADD,      // NumpadAdd
    108 to KeyEvent.KEYCODE_NUMPAD_COMMA,    // NumpadComma
    109 to KeyEvent.KEYCODE_NUMPAD_SUBTRACT, // NumpadSubtract
    110 to KeyEvent.KEYCODE_NUMPAD_DOT,      // NumpadDecimal
    111 to KeyEvent.KEYCODE_NUMPAD_DIVIDE,   // NumpadDivide
    112 to KeyEvent.KEYCODE_F1,           // F1
    113 to KeyEvent.KEYCODE_F2,           // F2
    114 to KeyEvent.KEYCODE_F3,           // F3
    115 to KeyEvent.KEYCODE_F4,           // F4
    116 to KeyEvent.KEYCODE_F5,           // F5
    117 to KeyEvent.KEYCODE_F6,           // F6
    118 to KeyEvent.KEYCODE_F7,           // F7
    119 to KeyEvent.KEYCODE_F8,           // F8
    120 to KeyEvent.KEYCODE_F9,           // F9
    121 to KeyEvent.KEYCODE_F10,          // F10
    122 to KeyEvent.KEYCODE_F11,          // F11
    123 to KeyEvent.KEYCODE_F12,          // F12
    144 to KeyEvent.KEYCODE_NUM_LOCK,     // NumLock
    145 to KeyEvent.KEYCODE_SCROLL_LOCK,  // ScrollLock
    160 to KeyEvent.KEYCODE_POUND,        // ^
    161 to KeyEvent.KEYCODE_PLUS,         // !
    162 to KeyEvent.KEYCODE_MENU,         // ;
    163 to KeyEvent.KEYCODE_POUND,        // #
    164 to KeyEvent.KEYCODE_STAR,         // $
    165 to KeyEvent.KEYCODE_MINUS,        // %
    166 to KeyEvent.KEYCODE_CLEAR,        // &
    167 to KeyEvent.KEYCODE_LEFT_BRACKET, // (
    168 to KeyEvent.KEYCODE_RIGHT_BRACKET,// )
    169 to KeyEvent.KEYCODE_APOSTROPHE,   // '
    170 to KeyEvent.KEYCODE_GRAVE,        // `
    171 to KeyEvent.KEYCODE_PLUS,         // +
    172 to KeyEvent.KEYCODE_MINUS,        // -
    173 to KeyEvent.KEYCODE_EQUALS,       // =
    174 to KeyEvent.KEYCODE_LEFT_BRACKET, // [
    175 to KeyEvent.KEYCODE_RIGHT_BRACKET,// ]
    176 to KeyEvent.KEYCODE_BACKSLASH,    // \
    177 to KeyEvent.KEYCODE_SEMICOLON,    // ;
    178 to KeyEvent.KEYCODE_APOSTROPHE,   // '
    179 to KeyEvent.KEYCODE_COMMA,        // ,
    180 to KeyEvent.KEYCODE_PERIOD,       // .
    181 to KeyEvent.KEYCODE_SLASH,        // /
    186 to KeyEvent.KEYCODE_SEMICOLON,    // Semicolon
    187 to KeyEvent.KEYCODE_EQUALS,       // Equal
    188 to KeyEvent.KEYCODE_COMMA,        // Comma
    189 to KeyEvent.KEYCODE_MINUS,        // Minus
    190 to KeyEvent.KEYCODE_PERIOD,       // Period
    191 to KeyEvent.KEYCODE_SLASH,        // Slash
    192 to KeyEvent.KEYCODE_GRAVE,        // Backquote
    219 to KeyEvent.KEYCODE_LEFT_BRACKET, // BracketLeft
    220 to KeyEvent.KEYCODE_BACKSLASH,    // Backslash
    221 to KeyEvent.KEYCODE_RIGHT_BRACKET,// BracketRight
    222 to KeyEvent.KEYCODE_APOSTROPHE    // Quote
)

