package com.android.systemuix.adapter

import android.app.AlertDialog
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.android.systemuix.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.github.h4de5ing.base.exec
import java.io.ByteArrayOutputStream

class DisableServiceAdapter(layoutRes: Int = R.layout.item_app_list, val change: (() -> Unit)) :
    BaseQuickAdapter<PackageInfo, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: PackageInfo) {
        val pm = context.packageManager
        val appName = pm.getApplicationLabel(item.applicationInfo)
        val icon = item.applicationInfo.loadIcon(pm)
        holder.setText(R.id.app_name, "$appName")
        holder.setText(R.id.app_package, item.packageName)
        holder.setImageDrawable(R.id.icon, byteArray2Drawable(drawable2ByteArray(icon)))
        val edit = holder.getView<ImageView>(R.id.edit)
        if (getAppIsEnabled(item.packageName)) edit.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
        else edit.setImageResource(R.drawable.ic_baseline_block_24)
        edit.setOnClickListener {
            confirmDialog(
                "disable/enable service?",
                "confirm ${if (getAppIsEnabled(item.packageName)) "disable" else "enable"} service ${item.packageName} ?"
            ) {
                exec("pm ${if (getAppIsEnabled(item.packageName)) "disable-user" else "enable"} ${item.packageName}")
                if (getAppIsEnabled(item.packageName)) edit.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                else edit.setImageResource(R.drawable.ic_baseline_block_24)
                change()
            }
        }
    }

    private fun getAppIsEnabled(packageName: String): Boolean =
        context.packageManager.getPackageInfo(packageName, 0).applicationInfo.enabled

    private fun confirmDialog(title: String, message: String, block: () -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.setPositiveButton(android.R.string.ok) { _, _ -> block() }
        builder.show()
    }

    //序列化 Drawable->Bitmap->ByteArray
    private fun drawable2ByteArray(icon: Drawable): ByteArray =
        bitmap2ByteArray(drawable2Bitmap(icon))

    private fun bitmap2ByteArray(bitmap: Bitmap): ByteArray {
        val bao = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bao)
        return bao.toByteArray()
    }

    //反序列化 ByteArray->Bitmap->Drawable
    private fun byteArray2Drawable(byteArray: ByteArray): Drawable? {
        val bitmap = byteArray2Bitmap(byteArray)
        return if (bitmap == null) null else BitmapDrawable(bitmap)
    }

    private fun byteArray2Bitmap(byteArray: ByteArray): Bitmap? {
        return if (byteArray.isNotEmpty()) BitmapFactory.decodeByteArray(
            byteArray,
            0,
            byteArray.size
        ) else null
    }

    private fun drawable2Bitmap(icon: Drawable): Bitmap {
        val bitmap =
            Bitmap.createBitmap(
                icon.intrinsicWidth,
                icon.intrinsicHeight,
                if (icon.opacity == PixelFormat.OPAQUE) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            )
        val canvas = Canvas(bitmap)
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        return bitmap
    }
}