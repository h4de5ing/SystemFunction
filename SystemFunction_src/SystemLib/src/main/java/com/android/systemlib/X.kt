package com.android.systemlib

import android.app.Activity
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter


/**
 * 不需要System权限调用的接口,普通权限可以调用的工具类
 */
//fun String.stream() = FileInputStream(this)
//fun InputStream.buffered() = BufferedInputStream(this)
//fun InputStream.reader(charset: String) = InputStreamReader(this, charset)
//fun Reader.readLines(): List<String> {
//    val result = arrayListOf<String>()
//    forEachLine { result.add(it) }
//    return result
//}
//
//fun Reader.readString(): String {
//    val sb = StringBuilder()
//    forEachLine { sb.append(it) }
//    return sb.toString()
//}


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
            } catch (_: Exception) {
            }
        }

        val system = Settings.System::class.java
        system.declaredFields.forEach {
            try {
                val name = it.get(system)
                val uri = Settings.System.getUriFor("$name")
                val pair = getSettings(context, uri)
                if (pair != null) list.add(pair)
            } catch (_: Exception) {
            }
        }
        val secure = Settings.Secure::class.java
        secure.declaredFields.forEach {
            try {
                val name = it.get(secure)
                val uri = Settings.Secure.getUriFor("$name")
                val pair = getSettings(context, uri)
                if (pair != null) list.add(pair)
            } catch (_: Exception) {
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun getDeclaredFields(context: Context, clazz: Class<Any>): Pair<String, String>? {
    var pair: Pair<String, String>? = null
    try {
        clazz.declaredFields.forEach {
            try {
                val name = it.get(clazz)
                val uri = Settings.Global.getUriFor("$name")
                pair = getSettings(context, uri)
            } catch (_: Exception) {
            }
        }
    } catch (_: Exception) {
    }
    return pair
}

fun getSettings(context: Context, uri: Uri): Pair<String, String>? {
    var pair: Pair<String, String>? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    if (cursor != null) {
        while (cursor.moveToNext()) {
            //println("${uri}:${cursor.getString(1)}:${cursor.getString(2)}")
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
            if (!TextUtils.isEmpty(name)) list.add(Pair(name, value))
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
 * content query --uri content://settings/global/install_app
 * content insert --uri content://settings/global --bind name:s:install_app --bind value:s:[com.android.systemfunction,com.android.otax,com.guoshi.httpcanary]
 * content update --uri content://settings/global --bind value:s:[com.android.otax] --where "name='install_app'"
 * content delete --uri content://settings/global --where "name='install_app'"
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
 * 插入一条信息到设置数据库中
 * @param context 上下文
 * @param uri 设置的uri
 * @param name 插入设置的name
 * @param value 插入设置的value值
 */
fun insertSettings(context: Context, uri: Uri, name: String, value: String) {
    val values = ContentValues()
    values.put("name", name)
    values.put("value", value)
    try {
        val result = context.contentResolver.insert(uri, values)
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
            val item = list[i]
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
            println(it.name)
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
    } catch (_: Exception) {
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

//序列化 Drawable->Bitmap->ByteArray
fun drawable2ByteArray(icon: Drawable): ByteArray {
    return bitmap2ByteArray(drawable2Bitmap(icon))
}

fun bitmap2ByteArray(bitmap: Bitmap): ByteArray {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return baos.toByteArray()
}

//反序列化 ByteArray->Bitmap->Drawable
fun byteArray2Drawable(byteArray: ByteArray): Drawable? {
    val bitmap = byteArray2Bitmap(byteArray)
    return if (bitmap == null) null else BitmapDrawable(bitmap)
}

fun byteArray2Bitmap(byteArray: ByteArray): Bitmap? {
    return if (byteArray.isNotEmpty()) BitmapFactory.decodeByteArray(
        byteArray,
        0,
        byteArray.size
    ) else null
}

fun drawable2Bitmap(icon: Drawable): Bitmap {
    val bitmap =
        Bitmap.createBitmap(
            icon.intrinsicWidth,
            icon.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmap)
    icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    icon.draw(canvas)
    return bitmap.thumbnail()
}

fun Bitmap.thumbnail(): Bitmap {
    val scaleWidth = 72.0f / width
    val scaleHeight = 72.0f / height
    val matrix = Matrix()
    matrix.postScale(scaleWidth, scaleHeight)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun File.toBitmap(maxWidth: Int, maxHeight: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        BitmapFactory.decodeFile(absolutePath, options)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * 获取系统默认配置/framework/core/base/res/res/values/config.xml
 */
fun getDefaultConfig() {
    com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent
}