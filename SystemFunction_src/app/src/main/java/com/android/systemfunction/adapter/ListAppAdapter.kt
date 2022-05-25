package com.android.systemfunction.adapter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import com.android.mdmsdk.change
import com.android.systemfunction.R
import com.android.systemfunction.bean.AppBean
import com.android.systemlib.*
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.github.h4de5ing.baseui.alertConfirm

@RequiresApi(Build.VERSION_CODES.N)
class ListAppAdapter(layoutRes: Int = R.layout.item_app_list) :
    BaseQuickAdapter<AppBean, BaseViewHolder>(layoutRes) {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun convert(holder: BaseViewHolder, item: AppBean) {
        holder.setText(R.id.app_name, item.name)
        holder.setText(R.id.app_package, item.packageName)
        holder.setImageDrawable(R.id.icon, byteArray2Drawable(item.icon))
        val uninstall = holder.getView<Button>(R.id.uninstall)
        uninstall.setOnClickListener {
            alertConfirm(context, "卸载${item.name}?") {
                if (it) uninstall(context, item.packageName)
            }
        }
        val disableUninstall = holder.getView<AppCompatCheckBox>(R.id.disable_uninstall)
        disableUninstall.change {
            disUninstallAPP(item.packageName, it)
            disableUninstall.isChecked = isDisUninstallAPP(item.packageName)
        }
        val suspended = holder.getView<AppCompatCheckBox>(R.id.suspended)
        suspended.change {
            suspendedAPP(item.packageName, it)
            suspended.isChecked = isSuspendedAPP(item.packageName)
        }
        val hidden = holder.getView<AppCompatCheckBox>(R.id.hidden)
        hidden.change {
            hiddenAPP(item.packageName, it)
            hidden.isChecked = isHiddenAPP(item.packageName)
        }
        val superWhite = holder.getView<AppCompatCheckBox>(R.id.super_white)
        superWhite.change {
            grantAllPermission(item.packageName)
        }
        holder.getView<AppCompatButton>(R.id.settings).setOnClickListener {
            try {
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                intent.data = Uri.fromParts("package", item.packageName, null)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        try {
            println(
                "${item.name} ${
                    canUninstall(
                        context,
                        item.packageName
                    )
                }"
            )
            disableUninstall.visibility =
                if (isSystemAPP(context, item.packageName)) View.GONE else View.VISIBLE
            uninstall.visibility =
                if (isSystemAPP(context, item.packageName)) View.GONE else View.VISIBLE
            if (isSystemAPP(context, item.packageName)) {
                disableUninstall.isChecked = isDisUninstallAPP(item.packageName)
            }
            suspended.isChecked = isSuspendedAPP(item.packageName)
            hidden.isChecked = isHiddenAPP(item.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setChange(change: (AppBean) -> Unit) {
        onCheckBoxChangeListener = object : OnCheckBoxChangeListener {
            override fun onchange(item: AppBean) = change(item)
        }
    }

    private var onCheckBoxChangeListener: OnCheckBoxChangeListener? = null

    interface OnCheckBoxChangeListener {
        fun onchange(item: AppBean)
    }
}