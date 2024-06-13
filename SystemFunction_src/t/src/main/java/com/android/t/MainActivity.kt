package com.android.t

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.systemlib.grant


class MainActivity : AppCompatActivity() {
    private var tv: TextView? = null
    private var processCPUTime = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = findViewById(R.id.tv)
        findViewById<Button>(R.id.test).setOnClickListener {
//            processCPUTime = 0L
//            AndroidAppProcessLoader(this) {
//                it.forEach {
//                    processCPUTime += it.stat().utime()
//                    processCPUTime += it.stat().stime()
//                }
//                tv?.text = "$processCPUTime"
//            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
//            grant(
//                this,
//                "com.guoshi.httpcanary",
//                "android.permission.WRITE_EXTERNAL_STORAGE"
//            )
            grant(this, "com.guoshi.httpcanary", "android.permission.WRITE_EXTERNAL_STORAGE")
        }
    }
}