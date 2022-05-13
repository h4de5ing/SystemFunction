package com.android.systemfunction.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.android.systemfunction.R
import kotlinx.android.synthetic.main.activity_loggerui.*
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


class Logger : AppCompatActivity() {
    private var recording = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loggerui)
        val readThread = ReadLogThread { }
        start.setOnClickListener {
            recording = !recording
            start.text = if (recording) "停止" else "开始"
            if (recording) readThread.start() else readThread.stopLog()
        }
        log.movementMethod = ScrollingMovementMethod()
    }

    private class ReadLogThread(private val callback: (message: String) -> Unit) : Thread() {

        private val out = FileOutputStream(File("/sdcard/1.log"))
        private var running = true
        fun stopLog() {
            running = false
        }

        override fun run() {
            super.run()
            try {
                var logcatProc = Runtime.getRuntime().exec("/system/bin/logcat *:e *:i")
                val reader = BufferedReader(InputStreamReader(logcatProc.inputStream), 1024)
                var line: String? = null
                while (running && reader.readLine().also {
                        line = "${it}\n"
                        callback.invoke("$line")
                    } != null) {
                    line?.apply { out.write(this.toByteArray()) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                out.close()
            }
        }
    }
}