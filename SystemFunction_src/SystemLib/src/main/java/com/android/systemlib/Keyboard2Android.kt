package com.android.systemlib

import android.view.KeyEvent
val JS_KEYCODE_TO_ANDROID = mapOf(
    // 控制键
    8 to KeyEvent.KEYCODE_DEL,    // Backspace
    9 to KeyEvent.KEYCODE_TAB,    // Tab
    13 to KeyEvent.KEYCODE_ENTER, // Enter
    16 to KeyEvent.KEYCODE_SHIFT_LEFT, // Shift (通用)
    17 to KeyEvent.KEYCODE_CTRL_LEFT,  // Ctrl (通用)
    18 to KeyEvent.KEYCODE_ALT_LEFT,   // Alt (通用)
    27 to KeyEvent.KEYCODE_ESCAPE,     // Esc
    32 to KeyEvent.KEYCODE_SPACE,     // Space

    // 导航键
    33 to KeyEvent.KEYCODE_PAGE_UP,   // Page Up
    34 to KeyEvent.KEYCODE_PAGE_DOWN, // Page Down
    35 to KeyEvent.KEYCODE_MOVE_END,  // End
    36 to KeyEvent.KEYCODE_MOVE_HOME, // Home
    37 to KeyEvent.KEYCODE_DPAD_LEFT, // Left Arrow
    38 to KeyEvent.KEYCODE_DPAD_UP,   // Up Arrow
    39 to KeyEvent.KEYCODE_DPAD_RIGHT,// Right Arrow
    40 to KeyEvent.KEYCODE_DPAD_DOWN, // Down Arrow
    45 to KeyEvent.KEYCODE_INSERT,    // Insert
    46 to KeyEvent.KEYCODE_FORWARD_DEL, // Delete

    // 数字键 (0-9)
    48 to KeyEvent.KEYCODE_0,
    49 to KeyEvent.KEYCODE_1,
    50 to KeyEvent.KEYCODE_2,
    51 to KeyEvent.KEYCODE_3,
    52 to KeyEvent.KEYCODE_4,
    53 to KeyEvent.KEYCODE_5,
    54 to KeyEvent.KEYCODE_6,
    55 to KeyEvent.KEYCODE_7,
    56 to KeyEvent.KEYCODE_8,
    57 to KeyEvent.KEYCODE_9,

    // 字母键 (A-Z)
    65 to KeyEvent.KEYCODE_A,
    66 to KeyEvent.KEYCODE_B,
    67 to KeyEvent.KEYCODE_C,
    68 to KeyEvent.KEYCODE_D,
    69 to KeyEvent.KEYCODE_E,
    70 to KeyEvent.KEYCODE_F,
    71 to KeyEvent.KEYCODE_G,
    72 to KeyEvent.KEYCODE_H,
    73 to KeyEvent.KEYCODE_I,
    74 to KeyEvent.KEYCODE_J,
    75 to KeyEvent.KEYCODE_K,
    76 to KeyEvent.KEYCODE_L,
    77 to KeyEvent.KEYCODE_M,
    78 to KeyEvent.KEYCODE_N,
    79 to KeyEvent.KEYCODE_O,
    80 to KeyEvent.KEYCODE_P,
    81 to KeyEvent.KEYCODE_Q,
    82 to KeyEvent.KEYCODE_R,
    83 to KeyEvent.KEYCODE_S,
    84 to KeyEvent.KEYCODE_T,
    85 to KeyEvent.KEYCODE_U,
    86 to KeyEvent.KEYCODE_V,
    87 to KeyEvent.KEYCODE_W,
    88 to KeyEvent.KEYCODE_X,
    89 to KeyEvent.KEYCODE_Y,
    90 to KeyEvent.KEYCODE_Z,

    // 数字小键盘
    96 to KeyEvent.KEYCODE_NUMPAD_0,
    97 to KeyEvent.KEYCODE_NUMPAD_1,
    98 to KeyEvent.KEYCODE_NUMPAD_2,
    99 to KeyEvent.KEYCODE_NUMPAD_3,
    100 to KeyEvent.KEYCODE_NUMPAD_4,
    101 to KeyEvent.KEYCODE_NUMPAD_5,
    102 to KeyEvent.KEYCODE_NUMPAD_6,
    103 to KeyEvent.KEYCODE_NUMPAD_7,
    104 to KeyEvent.KEYCODE_NUMPAD_8,
    105 to KeyEvent.KEYCODE_NUMPAD_9,
    106 to KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
    107 to KeyEvent.KEYCODE_NUMPAD_ADD,
    109 to KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
    110 to KeyEvent.KEYCODE_NUMPAD_DOT,
    111 to KeyEvent.KEYCODE_NUMPAD_DIVIDE,

    // 功能键
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

    // 符号键
    186 to KeyEvent.KEYCODE_SEMICOLON,    // ;
    187 to KeyEvent.KEYCODE_EQUALS,       // =
    188 to KeyEvent.KEYCODE_COMMA,        // ,
    189 to KeyEvent.KEYCODE_MINUS,        // -
    190 to KeyEvent.KEYCODE_PERIOD,       // .
    191 to KeyEvent.KEYCODE_SLASH,        // /
    192 to KeyEvent.KEYCODE_GRAVE,        // `
    219 to KeyEvent.KEYCODE_LEFT_BRACKET, // [
    220 to KeyEvent.KEYCODE_BACKSLASH,    // \
    221 to KeyEvent.KEYCODE_RIGHT_BRACKET,// ]
    222 to KeyEvent.KEYCODE_APOSTROPHE    // '
)

