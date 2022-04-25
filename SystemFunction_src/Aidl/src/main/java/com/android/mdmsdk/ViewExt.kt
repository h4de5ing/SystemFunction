package com.android.mdmsdk

import androidx.appcompat.widget.AppCompatCheckBox

fun AppCompatCheckBox.change(change: ((Boolean) -> Unit)) {
    this.setOnCheckedChangeListener { _, isChecked ->
        change(isChecked)
    }
}