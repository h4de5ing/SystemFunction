package com.android.systemuix.fragment

import android.annotation.SuppressLint
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.systemuix.R
import com.android.systemuix.adapter.DisableServiceAdapter
import com.android.systemuix.whiteList
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
class DisableServiceFragment : Fragment() {
    private val adapter = DisableServiceAdapter { updateTips() }
    private val appList = mutableListOf<PackageInfo>()
    private var textKeyword = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_disable_service, container, false)
    }

    private var refreshing = false
    private var etKeyword: EditText? = null
    private var tvResult: TextView? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view?.apply {
            etKeyword = view.findViewById(R.id.keyword)
            tvResult = view.findViewById(R.id.result)
            etKeyword?.change {
                update(activity, it, false)
                textKeyword = it
            }
            val recyclerView = view.findViewById<RecyclerView>(R.id.app)
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = adapter
            adapter.setNewInstance(appList)
            loadData()
            recyclerView.setOnTouchListener { _, _ -> refreshing }
        }
    }

    private fun TextView.change(change: ((String) -> Unit)) =
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                change(s.toString())
            }
        })


    @SuppressLint("QueryPermissionsNeeded")
    private fun update(context: Context, keyword: String, updateAppList: Boolean) {
        if(updateAppList) {
            appList.clear()
            appList.addAll(activity.packageManager.getInstalledPackages(0).filter { it.packageName !in whiteList })
        }
        val newList = appList.filter {
            it.packageName.contains(keyword) ||
//                    context.packageManager.getApplicationLabel(it.applicationInfo).contains(keyword)
                    it.applicationInfo.loadLabel(context.packageManager).contains(keyword)
        }
        val disabled = newList.filter { !it.applicationInfo.enabled }
        val enable = newList.filter { it.applicationInfo.enabled }
        tvResult?.text = getString(R.string.search_result, newList.size, disabled.size, enable.size)
        adapter.setNewInstance(newList.toMutableList())
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged", "QueryPermissionsNeeded")
    private fun loadData() {
        GlobalScope.launch {
            activity?.runOnUiThread {
                val pm = activity.packageManager
                appList.clear()
                appList.addAll(pm.getInstalledPackages(0).filter { it.packageName !in whiteList })
                val disabled = appList.filter { !it.applicationInfo.enabled }
                val enable = appList.filter { it.applicationInfo.enabled }
                tvResult?.text =
                    getString(R.string.search_result, appList.size, disabled.size, enable.size)
                adapter.notifyDataSetChanged()
                refreshing = false
            }
        }
    }

    private fun updateTips() = update(activity, textKeyword, true)
}