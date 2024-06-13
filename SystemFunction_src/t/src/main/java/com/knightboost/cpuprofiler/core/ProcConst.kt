package com.knightboost.cpuprofiler.core

object ProcConst {
    const val PROC_TERM_MASK: Int = 0xff

    const val PROC_ZERO_TERM: Int = 0

    const val PROC_SPACE_TERM: Int = ' '.code

    const val PROC_TAB_TERM: Int = '\t'.code

    const val PROC_NEWLINE_TERM: Int = '\n'.code

    const val PROC_COMBINE: Int = 0x100

    const val PROC_PARENS: Int = 0x200

    const val PROC_QUOTES: Int = 0x400

    const val PROC_CHAR: Int = 0x800

    const val PROC_OUT_STRING: Int = 0x1000

    const val PROC_OUT_LONG: Int = 0x2000

    const val PROC_OUT_FLOAT: Int = 0x4000
}
