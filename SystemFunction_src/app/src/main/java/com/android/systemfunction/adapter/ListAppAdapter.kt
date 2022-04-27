package com.android.kiosk.adapter

import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.android.kiosk.R
import com.android.kiosk.db.AppBean
import com.android.kiosk.utils.byteArray2Drawable

class ListAppAdapter(layoutRes: Int = R.layout.item_app_list) :
    BaseQuickAdapter<AppBean, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: AppBean) {
        holder.setText(R.id.app_name, item.name)
        holder.setText(R.id.app_package, item.packageName)
        holder.setImageDrawable(R.id.icon, byteArray2Drawable(item.icon))
        val edit = holder.getView<ImageView>(R.id.edit)
        if (item.add == 0) {
            edit.setImageResource(R.drawable.plus_128)
        } else {
            edit.setImageResource(R.drawable.minus_128)
        }
        edit.setOnClickListener {
            item.add = if (item.add == 0) 1 else 0
            if (item.add == 0) {
                edit.setImageResource(R.drawable.plus_128)
            } else {
                edit.setImageResource(R.drawable.minus_128)
            }
            println("改变了:${item}")
            onCheckBoxChangeListener?.onchange(item)
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