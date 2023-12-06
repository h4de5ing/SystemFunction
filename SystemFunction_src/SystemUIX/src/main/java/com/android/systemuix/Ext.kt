package com.android.systemuix

import kotlin.properties.Delegates

interface DisableInfoChange {
    fun disableChange()
}


var disableInfoChange: DisableInfoChange? = null
var changeInfo: String by Delegates.observable(" ") { _, _, _ ->
    disableInfoChange?.disableChange()
}

fun updateInfoAfterChange(change: () -> Unit) {
    disableInfoChange = object : DisableInfoChange {
        override fun disableChange() {
            change()
        }
    }
}