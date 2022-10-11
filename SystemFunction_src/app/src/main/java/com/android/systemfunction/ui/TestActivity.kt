package com.android.systemfunction.ui

import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.systemfunction.R
import kotlinx.android.synthetic.main.activity_test.*
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val path = File("/sdcard/Download")
        SDCardListener(path).startWatching()
        start.setOnClickListener {
            File("/sdcard/Download/a.txt").createNewFile()
        }
    }

    class SDCardListener(path: File) : FileObserver(path, ALL_EVENTS) {
        override fun onEvent(event: Int, path: String?) {
            val declaredFields = this.javaClass.declaredFields
//            var errMessage = ""
//            for (field in declaredFields) {
//                try {
//                    field.isAccessible = true
//                    val name = field.name
//                    val value = field[this] as Int
//                    if (value == event) {
//                        errMessage = "$name [$value]"
//                    }
//                } catch (e: Exception) {
//                }
//            }
            println("$event -> $path")
        }
    }
}