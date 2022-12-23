package com.android.systemfunction.ui

import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.systemfunction.databinding.ActivityTestBinding
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val path = File("/sdcard/Download")
        SDCardListener(path).startWatching()
        binding.start.setOnClickListener {
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