package com.android.t

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import com.android.systemlib.setSystemPropertyString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val scope = MainScope()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.test).setOnClickListener {
            scope.launch(Dispatchers.IO) {
                setSystemPropertyString("persist.sys.vin", "UDUMX1AERA006335")
            }
        }
    }
}