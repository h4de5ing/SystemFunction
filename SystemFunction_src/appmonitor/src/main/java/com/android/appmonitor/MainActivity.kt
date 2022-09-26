package com.android.appmonitor

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var tv: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = findViewById(R.id.tv)
        tv?.movementMethod = ScrollingMovementMethod()
        try {
            startService(Intent(this, ForegroundService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTv(message: String) {
        runOnUiThread {
            tv!!.append(message + "\n")
            val offset =
                tv!!.lineCount * tv!!.lineHeight - tv!!.height + 10
            if (offset > 10000) tv!!.text = "" else tv!!.scrollTo(0, offset)
        }
    }
}