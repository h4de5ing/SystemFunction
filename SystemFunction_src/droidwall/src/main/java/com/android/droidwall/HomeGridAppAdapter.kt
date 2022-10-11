package com.android.droidwall

import android.content.pm.ApplicationInfo
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class HomeGridAppAdapter(layoutRes: Int = R.layout.item_app_grid_list) :
    BaseQuickAdapter<ApplicationInfo, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: ApplicationInfo) {
        holder.setText(
            R.id.app_name,
            "${item.uid}:${context.packageManager.getApplicationLabel(item)}"
        )
        holder.setImageDrawable(R.id.icon, item.loadIcon(context.packageManager))
        holder.getView<CheckBox>(R.id.checkbox).setOnCheckedChangeListener { _, isChecked ->
            onCheckedChangeListener?.onchange(item.uid, isChecked)
        }
    }

    interface OnCheckedChangeListener {
        fun onchange(uid: Int, isChecked: Boolean)
    }

    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    fun setOnCheckedChangeListener(change: (Int, Boolean) -> Unit) {
        onCheckedChangeListener = object : OnCheckedChangeListener {
            override fun onchange(uid: Int, isChecked: Boolean) {
                change(uid, isChecked)
            }
        }
    }
}