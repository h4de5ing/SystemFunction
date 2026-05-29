package com.android.systemlib

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 处理图片的工具类
 */
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
            if (icon.opacity == PixelFormat.OPAQUE) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmap)
    icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    icon.draw(canvas)
    return bitmap.thumbnail()
}
fun Bitmap.asMutableArgb8888(): Bitmap =
    if (isMutable && config == Bitmap.Config.ARGB_8888) this
    else copy(Bitmap.Config.ARGB_8888, true)
fun Bitmap.thumbnail(): Bitmap {
    val scaleWidth = 72.0f / width
    val scaleHeight = 72.0f / height
    val matrix = Matrix()
    matrix.postScale(scaleWidth, scaleHeight)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
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