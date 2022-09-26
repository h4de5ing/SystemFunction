package com.android.appmonitor

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {
    private var tv: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = findViewById(R.id.tv)
        tv?.movementMethod = ScrollingMovementMethod()
        try {
            Thread {
                getTemp().forEach {
                    updateTv("${it.first} -> ${it.second}")
                }
            }.start()
            getTemp()
            startService(Intent(this, ForegroundService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTemp(): List<Pair<String, String>> {
        val allTemp = mutableListOf<Pair<String, String>>()
        try {
            val file = File("/sys/class/thermal/")
            val list = file.listFiles()
            list?.let { files ->
                files.sortBy { it.name }
                files.forEach {
                    if (it.name.contains("thermal_zone")) {
//                    println("dir:${it.absoluteFile}")
                        val type = getTemp(it.absolutePath + File.separator + "type")
                        val temp = getTemp(it.absolutePath + File.separator + "temp")
                        allTemp.add(Pair(type, temp))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return allTemp
    }

    private fun getTemp(path: String): String {
        var result = ""
        try {
            result = path.stream().reader().readString()
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return result
    }

    fun String.stream() = FileInputStream(this)
    fun Reader.readString(): String {
        val sb = StringBuilder()
        forEachLine { sb.append(it) }
        return sb.toString()
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