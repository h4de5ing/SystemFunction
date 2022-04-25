package com.android.systemfunction.utils

import androidx.appcompat.widget.AppCompatCheckBox

fun AppCompatCheckBox.checked(change: ((Boolean) -> Unit)) {
    this.setOnCheckedChangeListener { _, isChecked ->
        change(isChecked)
    }
}