package com.android.systemfunction

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.android.systemlib.activateDeviceManager
import com.android.systemlib.isNavigaBarDisable
import com.android.systemlib.removeActiveDeviceAdmin

class MainActivity : AppCompatActivity() {
    private var iv: ImageView? = null
    private var isToggle = false
    private lateinit var start: Button
    private lateinit var get: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        iv = findViewById(R.id.iv)
        start = findViewById<Button>(R.id.start)
        get = findViewById<Button>(R.id.get)
        start.setOnClickListener {
//            iv?.apply { this.setImageBitmap(takeScreenShot()) }
            isToggle = !isToggle
            start.text = if (isToggle) "启用" else "禁用"
//            activateDeviceManager(MainActivity@ this, BuildConfig.APPLICATION_ID,AdminReceiver::class.java.name)
            if (isToggle) {

                activateDeviceManager(
                    this,
                    "com.android.edittext",
                    "com.android.edittext.AdminReceiver"
                )
            } else {
                removeActiveDeviceAdmin(
                    this, "com.android.edittext", "com.android.edittext.AdminReceiver"
                )
            }
        }
        get.setOnClickListener {
            println(isNavigaBarDisable(MainActivity@ this))
        }
    }

    fun setStatusBar(isDisable: Boolean) {
        if (isDisable) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}