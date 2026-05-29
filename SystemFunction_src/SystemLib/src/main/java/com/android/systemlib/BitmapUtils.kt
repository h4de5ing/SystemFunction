package com.android.systemlib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import java.io.ByteArrayOutputStream
import java.io.File

/** Launcher 图标的目标像素尺寸 */
private const val ICON_SIZE_PX = 72

/**
 * 解码/绘制时允许的最大单边像素数。
 * 超过此尺寸的图片会被降采样，防止 3MB+ 大图 OOM。
 */
private const val MAX_DECODE_SIZE = 512

// ─────────────────────────────────────────────
// 兜底默认图标
// ─────────────────────────────────────────────

/** 返回系统默认 App 图标，用于解码失败时的兜底 */
fun defaultAppDrawable(context: Context): Drawable =
    ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
        ?: context.packageManager.defaultActivityIcon

// ─────────────────────────────────────────────
// 序列化：Drawable → Bitmap → ByteArray
// ─────────────────────────────────────────────

/**
 * Drawable → Bitmap，缩放到 ICON_SIZE_PX。
 *
 * 安全保障：
 * - intrinsicWidth/Height 为负（如 ColorDrawable）时回退到 ICON_SIZE_PX，避免 crash。
 * - 尺寸超过 MAX_DECODE_SIZE 时先裁剪再绘制，防止超大 Drawable（如 3MB 图片）导致 OOM。
 */
fun drawable2Bitmap(icon: Drawable): Bitmap {
    val rawW = icon.intrinsicWidth.takeIf { it > 0 } ?: ICON_SIZE_PX
    val rawH = icon.intrinsicHeight.takeIf { it > 0 } ?: ICON_SIZE_PX
    val w = rawW.coerceAtMost(MAX_DECODE_SIZE)
    val h = rawH.coerceAtMost(MAX_DECODE_SIZE)
    val config =
        if (icon.opacity == PixelFormat.OPAQUE) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
    val bitmap = Bitmap.createBitmap(w, h, config)
    val canvas = Canvas(bitmap)
    icon.setBounds(0, 0, w, h)
    icon.draw(canvas)
    return bitmap.thumbnail()
}

/** Drawable → PNG ByteArray，内部经过 drawable2Bitmap 缩放 */
fun drawable2ByteArray(icon: Drawable): ByteArray = bitmap2ByteArray(drawable2Bitmap(icon))

/** Bitmap → PNG ByteArray（quality=100 无损压缩） */
fun bitmap2ByteArray(bitmap: Bitmap): ByteArray {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return baos.toByteArray()
}

// ─────────────────────────────────────────────
// 反序列化：ByteArray → Bitmap → Drawable
// ─────────────────────────────────────────────

/**
 * ByteArray → Drawable，解码失败或输入为空时返回默认图标。
 *
 * 安全保障：先用 inJustDecodeBounds 读取尺寸，超过 MAX_DECODE_SIZE 时
 * 自动计算 inSampleSize 降采样，避免大字节数组 OOM。
 */
fun byteArrayToDrawable(context: Context, byteArray: ByteArray?): Drawable {
    if (byteArray == null || byteArray.isEmpty()) return defaultAppDrawable(context)
    return byteArray2Bitmap(byteArray)?.toDrawable(context.resources) ?: defaultAppDrawable(context)
}

/**
 * ByteArray → Drawable（无 Context 版本）。
 * 注意：BitmapDrawable 使用 null Resources，屏幕密度可能不准确；
 * 有 Context 时优先使用 byteArrayToDrawable。
 */
fun byteArray2Drawable(byteArray: ByteArray): Drawable? =
    byteArray2Bitmap(byteArray)?.let { BitmapDrawable(null, it) }

/**
 * ByteArray → Bitmap，带安全降采样。
 *
 * 安全保障：先用 inJustDecodeBounds 扫描尺寸，超过 MAX_DECODE_SIZE 时
 * 计算 inSampleSize，防止 3MB+ 大图在解码时 OOM。
 */
fun byteArray2Bitmap(byteArray: ByteArray): Bitmap? {
    if (byteArray.isEmpty()) return null
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, opts)
    opts.inSampleSize = calculateInSampleSize(opts, MAX_DECODE_SIZE, MAX_DECODE_SIZE)
    opts.inJustDecodeBounds = false
    opts.inPreferredConfig = Bitmap.Config.ARGB_8888
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, opts)
}

// ─────────────────────────────────────────────
// Bitmap 扩展工具
// ─────────────────────────────────────────────

/** 确保 Bitmap 是可变的 ARGB_8888 格式，供 Canvas 叠加绘制使用 */
fun Bitmap.asMutableArgb8888(): Bitmap = if (isMutable && config == Bitmap.Config.ARGB_8888) this
else copy(Bitmap.Config.ARGB_8888, true)

/**
 * 等比缩放 Bitmap 到目标尺寸。
 * 宽或高为 0 时直接返回原图，避免除零 crash。
 */
fun Bitmap.thumbnail(size: Int = ICON_SIZE_PX): Bitmap {
    if (width <= 0 || height <= 0) return this
    val matrix = Matrix().apply { postScale(size.toFloat() / width, size.toFloat() / height) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

// ─────────────────────────────────────────────
// 文件工具
// ─────────────────────────────────────────────

/**
 * 计算 inSampleSize，使解码后尺寸不超过 reqWidth × reqHeight。
 * 始终返回 2 的幂次，以最大化硬件解码器兼容性。
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
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

/** 从文件安全解码 Bitmap，自动降采样到 maxWidth × maxHeight */
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
