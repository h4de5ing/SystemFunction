package com.android.droidwall

import android.widget.CheckBox
import com.android.droidwall.db.FirewallData
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class HomeGridAppAdapter(layoutRes: Int = R.layout.item_app_grid_list) :
    BaseQuickAdapter<FirewallData, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: FirewallData) {
        holder.setText(R.id.app_name, "${item.uid}:${item.appName}")
        holder.setImageDrawable(R.id.icon, item.icon)
        val checkbox = holder.getView<CheckBox>(R.id.checkbox)
        checkbox.isChecked = item.isWhite
        checkbox.setOnCheckedChangeListener { view, isChecked ->
//            updateKT(item.uid, isChecked)
//            iNetD?.setFirewallUidRule(1, item.uid, if (isChecked) 1 else 2)
//            fwDao.selectAllConfig().forEach {
//                try {
//                    iNetD?.setFirewallUidRule(1, it.uid, if (it.isWhite) 1 else 2)
//                } catch (e: Exception) {
//                }
//            }
            listener?.onCheckedChanged(item.uid, isChecked)
        }
    }

    private var listener: OnCheckedChangeListener? = null
    fun setOnClickListener(listener: OnCheckedChangeListener) {
        this.listener = listener
    }
    open interface OnCheckedChangeListener {
        fun onCheckedChanged(uid: Int, isChecked: Boolean)
    }
}

