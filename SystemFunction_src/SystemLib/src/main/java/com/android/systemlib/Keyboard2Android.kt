package com.android.systemlib

import android.view.KeyEvent

/**
 * https://android.googlesource.com/platform/frameworks/native/+/master/include/android/keycodes.h
 */
val JS_KEYCODE_TO_ANDROID = mapOf(
    // === 控制键 ===
    8 to KeyEvent.KEYCODE_DEL,        // Backspace (退格键)
    9 to KeyEvent.KEYCODE_TAB,         // Tab (制表键)
    12 to KeyEvent.KEYCODE_CLEAR,      // Clear (清除键，小键盘中央)
    13 to KeyEvent.KEYCODE_ENTER,      // Enter (回车键)
    16 to KeyEvent.KEYCODE_SHIFT_LEFT, // Left Shift (左Shift键)
    17 to KeyEvent.KEYCODE_CTRL_LEFT,  // Left Ctrl (左控制键)
    18 to KeyEvent.KEYCODE_ALT_LEFT,   // Left Alt (左Alt键)
    19 to KeyEvent.KEYCODE_BREAK,      // Pause/Break (暂停/中断键)
    20 to KeyEvent.KEYCODE_CAPS_LOCK,  // Caps Lock (大写锁定键)
    27 to KeyEvent.KEYCODE_ESCAPE,     // Esc (退出键)
    32 to KeyEvent.KEYCODE_SPACE,      // Space (空格键)

    // === 导航键 ===
    33 to KeyEvent.KEYCODE_PAGE_UP,    // Page Up (向上翻页键)
    34 to KeyEvent.KEYCODE_PAGE_DOWN,  // Page Down (向下翻页键)
    35 to KeyEvent.KEYCODE_MOVE_END,   // End (行尾键)
    36 to KeyEvent.KEYCODE_MOVE_HOME,  // Home (行首键)
    37 to KeyEvent.KEYCODE_DPAD_LEFT,  // Left Arrow (左方向键)
    38 to KeyEvent.KEYCODE_DPAD_UP,    // Up Arrow (上方向键)
    39 to KeyEvent.KEYCODE_DPAD_RIGHT, // Right Arrow (右方向键)
    40 to KeyEvent.KEYCODE_DPAD_DOWN,  // Down Arrow (下方向键)
    44 to KeyEvent.KEYCODE_SYSRQ,      // Print Screen (打印屏幕键)
    45 to KeyEvent.KEYCODE_INSERT,     // Insert (插入键)
    46 to KeyEvent.KEYCODE_FORWARD_DEL,// Delete (删除键)

    // === 数字键 ===
    48 to KeyEvent.KEYCODE_0,          // 0
    49 to KeyEvent.KEYCODE_1,          // 1
    50 to KeyEvent.KEYCODE_2,          // 2
    51 to KeyEvent.KEYCODE_3,          // 3
    52 to KeyEvent.KEYCODE_4,          // 4
    53 to KeyEvent.KEYCODE_5,          // 5
    54 to KeyEvent.KEYCODE_6,          // 6
    55 to KeyEvent.KEYCODE_7,          // 7
    56 to KeyEvent.KEYCODE_8,          // 8
    57 to KeyEvent.KEYCODE_9,          // 9

    // === 特殊符号键 ===
    59 to KeyEvent.KEYCODE_SEMICOLON,  // ; (分号，Firefox兼容)
    61 to KeyEvent.KEYCODE_EQUALS,     // = (等号，Firefox兼容)
    163 to KeyEvent.KEYCODE_POUND,     // # (井号/英镑键)
    173 to KeyEvent.KEYCODE_MINUS,     // - (减号，Firefox兼容)

    // === 字母键 ===
    65 to KeyEvent.KEYCODE_A,          // A
    66 to KeyEvent.KEYCODE_B,          // B
    67 to KeyEvent.KEYCODE_C,          // C
    68 to KeyEvent.KEYCODE_D,          // D
    69 to KeyEvent.KEYCODE_E,          // E
    70 to KeyEvent.KEYCODE_F,          // F
    71 to KeyEvent.KEYCODE_G,          // G
    72 to KeyEvent.KEYCODE_H,          // H
    73 to KeyEvent.KEYCODE_I,          // I
    74 to KeyEvent.KEYCODE_J,          // J
    75 to KeyEvent.KEYCODE_K,          // K
    76 to KeyEvent.KEYCODE_L,          // L
    77 to KeyEvent.KEYCODE_M,          // M
    78 to KeyEvent.KEYCODE_N,          // N
    79 to KeyEvent.KEYCODE_O,          // O
    80 to KeyEvent.KEYCODE_P,          // P
    81 to KeyEvent.KEYCODE_Q,          // Q
    82 to KeyEvent.KEYCODE_R,          // R
    83 to KeyEvent.KEYCODE_S,          // S
    84 to KeyEvent.KEYCODE_T,          // T
    85 to KeyEvent.KEYCODE_U,          // U
    86 to KeyEvent.KEYCODE_V,          // V
    87 to KeyEvent.KEYCODE_W,          // W
    88 to KeyEvent.KEYCODE_X,          // X
    89 to KeyEvent.KEYCODE_Y,          // Y
    90 to KeyEvent.KEYCODE_Z,          // Z

    // === 系统功能键 ===
    91 to KeyEvent.KEYCODE_META_LEFT,  // Left Meta (左Windows/Command键)
    92 to KeyEvent.KEYCODE_META_RIGHT, // Right Meta (右Windows/Command键)
    93 to KeyEvent.KEYCODE_MENU,       // Context Menu (上下文菜单键)

    // === 数字小键盘 ===
    96 to KeyEvent.KEYCODE_NUMPAD_0,   // Numpad 0 (小键盘0)
    97 to KeyEvent.KEYCODE_NUMPAD_1,   // Numpad 1 (小键盘1)
    98 to KeyEvent.KEYCODE_NUMPAD_2,   // Numpad 2 (小键盘2)
    99 to KeyEvent.KEYCODE_NUMPAD_3,   // Numpad 3 (小键盘3)
    100 to KeyEvent.KEYCODE_NUMPAD_4,  // Numpad 4 (小键盘4)
    101 to KeyEvent.KEYCODE_NUMPAD_5,  // Numpad 5 (小键盘5)
    102 to KeyEvent.KEYCODE_NUMPAD_6,  // Numpad 6 (小键盘6)
    103 to KeyEvent.KEYCODE_NUMPAD_7,  // Numpad 7 (小键盘7)
    104 to KeyEvent.KEYCODE_NUMPAD_8,  // Numpad 8 (小键盘8)
    105 to KeyEvent.KEYCODE_NUMPAD_9,  // Numpad 9 (小键盘9)
    106 to KeyEvent.KEYCODE_NUMPAD_MULTIPLY, // Numpad * (小键盘乘号)
    107 to KeyEvent.KEYCODE_NUMPAD_ADD,      // Numpad + (小键盘加号)
    108 to KeyEvent.KEYCODE_NUMPAD_COMMA,    // Numpad , (小键盘逗号)
    109 to KeyEvent.KEYCODE_NUMPAD_SUBTRACT, // Numpad - (小键盘减号)
    110 to KeyEvent.KEYCODE_NUMPAD_DOT,      // Numpad . (小键盘点号)
    111 to KeyEvent.KEYCODE_NUMPAD_DIVIDE,   // Numpad / (小键盘除号)

    // === 功能键 ===
    112 to KeyEvent.KEYCODE_F1,        // F1
    113 to KeyEvent.KEYCODE_F2,        // F2
    114 to KeyEvent.KEYCODE_F3,        // F3
    115 to KeyEvent.KEYCODE_F4,        // F4
    116 to KeyEvent.KEYCODE_F5,        // F5
    117 to KeyEvent.KEYCODE_F6,        // F6
    118 to KeyEvent.KEYCODE_F7,        // F7
    119 to KeyEvent.KEYCODE_F8,        // F8
    120 to KeyEvent.KEYCODE_F9,        // F9
    121 to KeyEvent.KEYCODE_F10,       // F10
    122 to KeyEvent.KEYCODE_F11,       // F11
    123 to KeyEvent.KEYCODE_F12,       // F12

    // === 锁定键 ===
    144 to KeyEvent.KEYCODE_NUM_LOCK,    // Num Lock (数字锁定键)
    145 to KeyEvent.KEYCODE_SCROLL_LOCK, // Scroll Lock (滚动锁定键)

    // === 符号键 ===
    186 to KeyEvent.KEYCODE_SEMICOLON,    // ; (分号)
    187 to KeyEvent.KEYCODE_EQUALS,       // = (等号)
    188 to KeyEvent.KEYCODE_COMMA,        // , (逗号)
    189 to KeyEvent.KEYCODE_MINUS,        // - (减号)
    190 to KeyEvent.KEYCODE_PERIOD,       // . (句号)
    191 to KeyEvent.KEYCODE_SLASH,        // / (斜杠)
    192 to KeyEvent.KEYCODE_GRAVE,        // ` (反引号)
    219 to KeyEvent.KEYCODE_LEFT_BRACKET, // [ (左方括号)
    220 to KeyEvent.KEYCODE_BACKSLASH,    // \ (反斜杠)
    221 to KeyEvent.KEYCODE_RIGHT_BRACKET,// ] (右方括号)
    222 to KeyEvent.KEYCODE_APOSTROPHE    // ' (单引号)
)

