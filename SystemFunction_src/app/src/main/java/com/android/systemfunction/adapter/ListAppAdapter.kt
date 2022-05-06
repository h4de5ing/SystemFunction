package com.android.systemfunction.adapter

import android.os.Build
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatCheckBox
import com.android.mdmsdk.change
import com.android.systemfunction.R
import com.android.systemfunction.app.App
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.utils.*
import com.android.systemlib.canUninstall
import com.android.systemlib.isSystemAPP
import com.android.systemlib.uninstall
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.github.h4de5ing.baseui.alertConfirm

@RequiresApi(Build.VERSION_CODES.N)
class ListAppAdapter(layoutRes: Int = R.layout.item_app_list) :
    BaseQuickAdapter<AppBean, BaseViewHolder>(layoutRes) {
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
            disUninstallAPP(
                context,
                App.componentName2,
                item.packageName,
                it
            )
            disableUninstall.isChecked =
                isDisUninstallAPP(context, App.componentName2, item.packageName)
        }
        val suspended = holder.getView<AppCompatCheckBox>(R.id.suspended)
        suspended.change {
            suspendedAPP(
                context,
                App.componentName2,
                item.packageName,
                it
            )
            suspended.isChecked =
                isSuspendedAPP(context, App.componentName2, item.packageName)
        }
        val hidden = holder.getView<AppCompatCheckBox>(R.id.hidden)
        hidden.change {
            hiddenAPP(
                context,
                App.componentName2,
                item.packageName,
                it
            )
            hidden.isChecked =
                isHiddenAPP(context, App.componentName2, item.packageName)
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
                disableUninstall.isChecked =
                    isDisUninstallAPP(context, App.componentName2, item.packageName)
            }
            suspended.isChecked =
                isSuspendedAPP(context, App.componentName2, item.packageName)
            hidden.isChecked =
                isHiddenAPP(context, App.componentName2, item.packageName)
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