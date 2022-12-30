package com.android.systemuix.adapter

import android.app.AlertDialog
import android.content.pm.PackageInfo
import android.widget.ImageView
import com.android.systemlib.byteArray2Drawable
import com.android.systemlib.drawable2ByteArray
import com.android.systemuix.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.github.h4de5ing.base.exec

class DisableServiceAdapter(layoutRes: Int = R.layout.item_app_list) :
    BaseQuickAdapter<PackageInfo, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: PackageInfo) {
        val pm = context.packageManager
        val appName = pm.getApplicationLabel(item.applicationInfo)
        val icon = item.applicationInfo.loadIcon(pm)
        holder.setText(R.id.time, "$appName")
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
            }
        }
    }

    private fun getAppIsEnabled(packageName: String): Boolean {
        return context.packageManager.getPackageInfo(packageName, 0).applicationInfo.enabled
    }

    private fun confirmDialog(title: String, message: String, block: () -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.setPositiveButton(android.R.string.ok) { _, _ -> block() }
        builder.show()
    }
}