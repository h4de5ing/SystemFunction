package com.android.systemfunction

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.android.systemlib.takeScreenShot2

class MainActivity : AppCompatActivity() {
    private var iv: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        iv = findViewById(R.id.iv)
        findViewById<Button>(R.id.start).setOnClickListener {
            iv?.apply { this.setImageBitmap(takeScreenShot2()) }
        }
    }
}