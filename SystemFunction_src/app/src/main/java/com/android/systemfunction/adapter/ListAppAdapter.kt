package com.android.systemfunction.adapter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatCheckBox
import com.android.mdmsdk.change
import com.android.systemfunction.R
import com.android.systemfunction.app.App
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.utils.*
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

@RequiresApi(Build.VERSION_CODES.N)
class ListAppAdapter(layoutRes: Int = R.layout.item_app_list) :
    BaseQuickAdapter<AppBean, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: AppBean) {
        holder.setText(R.id.app_name, item.name)
        holder.setText(R.id.app_package, item.packageName)
        holder.setImageDrawable(R.id.icon, byteArray2Drawable(item.icon))
        val disableUninstall = holder.getView<AppCompatCheckBox>(R.id.disable_uninstall)
        disableUninstall.isChecked =
            isUninstallAPP(context, App.componentName2, item.packageName)
        disableUninstall.change {
            disUninstallAPP(
                context,
                App.componentName2,
                item.packageName,
                it
            )
            disableUninstall.isChecked =
                isUninstallAPP(context, App.componentName2, item.packageName)
        }
        val suspended = holder.getView<AppCompatCheckBox>(R.id.suspended)
        suspended.isChecked =
            isSuspendedAPP(context, App.componentName2, item.packageName)
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
        hidden.isChecked =
            isHiddenAPP(context, App.componentName2, item.packageName)
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
    }

    private fun updateUI() {
        allDBPackages.clear()
        allDBPackages.addAll(App.systemDao.selectAllPackagesList(0))
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