package com.android.systemlib

import android.app.Activity
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import java.io.*

/**
 * 不需要System权限调用的接口,普通权限可以调用的工具类
 */
fun String.stream() = FileInputStream(this)
fun InputStream.buffered() = BufferedInputStream(this)
fun InputStream.reader(charset: String) = InputStreamReader(this, charset)
fun Reader.readLines(): List<String> {
    val result = arrayListOf<String>()
    forEachLine { result.add(it) }
    return result
}

fun Reader.readString(): String {
    val sb = StringBuilder()
    forEachLine { sb.append(it) }
    return sb.toString()
}


fun getAllSettings(context: Context): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    try {
        val global = Settings.Secure::class.java
        global.declaredFields.forEach {
            try {
                val name = it.get(global)
                val uri = Settings.Global.getUriFor("$name")
                val pair = getSettings(context, uri)
                if (pair != null) list.add(pair)
                //Settings.Global.getString(context.contentResolver, "${name}")
            } catch (e: Exception) {
            }
        }

        val system = Settings.System::class.java
        system.declaredFields.forEach {
            try {
                val name = it.get(system)
                val uri = Settings.System.getUriFor("$name")
                val pair = getSettings(context, uri)
                if (pair != null) list.add(pair)
            } catch (e: Exception) {
            }
        }
        val secure = Settings.Secure::class.java
        secure.declaredFields.forEach {
            try {
                val name = it.get(secure)
                val uri = Settings.Secure.getUriFor("$name")
                val pair = getSettings(context, uri)
                if (pair != null) list.add(pair)
            } catch (e: Exception) {
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun getDeclaredFields(context: Context, clazz: Class<T>): Pair<String, String>? {
    var pair: Pair<String, String>? = null
    try {
        clazz.declaredFields.forEach {
            try {
                val name = it.get(clazz)
                val uri = Settings.Global.getUriFor("$name")
                pair = getSettings(context, uri)
            } catch (e: Exception) {
            }
        }
    } catch (e: Exception) {
    }
    return pair
}

fun getSettings(context: Context, uri: Uri): Pair<String, String>? {
    var pair: Pair<String, String>? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    if (cursor != null) {
        while (cursor.moveToNext()) {
            println("${uri}:${cursor.getString(1)}:${cursor.getString(2)}")
            pair = Pair(cursor.getString(1), cursor.getString(2))
        }
        cursor.close()
    }
    return pair
}

/**
 * 通过url 获取第1个和第2个字段
 */
fun getUriFor(context: Context, uri: Uri): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    if (cursor != null && cursor.moveToFirst()) {
        while (cursor.moveToNext()) {
            val name = cursor.getString(1)
            val value = cursor.getString(2)
            if (!TextUtils.isEmpty(name)) list.add(Pair(name, "$value"))
        }
        cursor.close()
    }
    return list
}

/**
 * 修改一条设置
 * @param context 上下文
 * @param uri  content://settings/global
 * @param name 设置名称 wifi_on
 * @param value 1 开  0 关
 * adb shell content query --uri content://settings/global
 * adb shell content query --uri content://settings/global/wifi_on
 * 写成功后状态不生效,需要重启才能生效
 */
fun putSettings(context: Context, uri: Uri, name: String, value: String) {
//    val uri = Settings.Global.getUriFor(name)//content://settings/global/wifi_on
    val values = ContentValues()
    values.put("name", name)
    values.put("value", value)
    try {
        val result = context.contentResolver.update(uri, values, "name=?", arrayOf(name))
        println("更新结果:${result}")
    } catch (e: Exception) {
        println("$uri 更新失败 ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 批量插入设置信息
 */
fun putAllSettings(context: Context, uri: Uri, list: List<Pair<String, String>>) {
    try {
        val ops = arrayListOf<ContentProviderOperation>()
        for (i in list.indices) {
            val item = list.get(i)
            ops.add(
                ContentProviderOperation
                    .newUpdate(uri)
                    .withSelection("name=?", arrayOf(item.first))
                    .withValue("name", item.first)
                    .withValue("value", item.second)
                    .withYieldAllowed(true)
                    .build()
            )
        }
        val result = context.contentResolver.applyBatch(Settings.AUTHORITY, ops)
        result.forEach {
            println("更新结果:${it}")
        }
    } catch (e: Exception) {
        println("$uri 更新失败 ${e.message}")
        e.printStackTrace()
    }
}


/**
 * 递归遍历拷贝目录
 */
fun copyDir(src: String, des: String) {
    try {
        val fileTree: FileTreeWalk = File(src).walk()
        fileTree.forEach {
            println("${it.name}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun write2File(name: String, content: String, append: Boolean) {
    var writer: BufferedWriter? = null
    try {
        writer = BufferedWriter(FileWriter(name, append))
        writer.write(content)
    } catch (e: Exception) {
    } finally {
        closeQuietly(writer)
    }
}

fun closeQuietly(autoCloseable: AutoCloseable?) {
    try {
        autoCloseable?.close()
    } catch (unused: Exception) {
        unused.printStackTrace()
    }
}

fun Activity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}