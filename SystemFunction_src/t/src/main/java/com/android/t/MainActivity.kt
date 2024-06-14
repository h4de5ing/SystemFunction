package com.android.t

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.android.systemlib.setMode

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val targetPackageName = "com.isaac.devmanager"
        val cb = findViewById<Switch>(R.id.test)
        cb.setOnCheckedChangeListener { _, isChecked ->
            //92 https://android.googlesource.com/platform/frameworks/base/+/727e195ee8be4e9f2ac3f4c47c9c2bfb1e8916e9/core/proto/android/app/enums.proto#202
            setMode(
                this,
                92,
                targetPackageName,
                if (isChecked) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_IGNORED
            )
        }
        findViewById<Button>(R.id.test2).setOnClickListener {
            val intent = Intent()
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }
        findViewById<Button>(R.id.test3).setOnClickListener {
            val uri = Uri.parse("package:${targetPackageName}")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            startActivity(intent)
        }
    }
}