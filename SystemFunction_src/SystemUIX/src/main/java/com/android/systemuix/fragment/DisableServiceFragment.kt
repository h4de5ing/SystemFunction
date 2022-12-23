package com.android.systemuix.fragment

import android.annotation.SuppressLint
import android.app.Fragment
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.systemuix.R
import com.android.systemuix.adapter.DisableServiceAdapter
import com.android.systemuix.utils.EditTextChangeListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
class DisableServiceFragment : Fragment() {
    private val adapter = DisableServiceAdapter()
    private val appList = mutableListOf<PackageInfo>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
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
            etKeyword?.addTextChangedListener(EditTextChangeListener { update(it) })
            val recyclerView = view.findViewById<RecyclerView>(R.id.app)
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = adapter
            adapter.setNewInstance(appList)
            loadData()
            recyclerView.setOnTouchListener { _, _ -> refreshing }
        }
    }


    private fun update(keyword: String) {
        val newList = appList.filter { it.toString().contains(keyword) }
        val disabled = newList.filter { !it.applicationInfo.enabled }
        val enable = newList.filter { it.applicationInfo.enabled }
        tvResult?.text =
            "search result: [${newList.size}] application contains $keyword ,enable:${enable.size},disable:${disabled.size}"
        adapter.setNewInstance(newList.toMutableList())
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged", "QueryPermissionsNeeded")
    private fun loadData() {
        GlobalScope.launch {
            activity?.runOnUiThread {
                val pm = activity.packageManager
                appList.clear()
                appList.addAll(pm.getInstalledPackages(0))
                val disabled = appList.filter { !it.applicationInfo.enabled }
                val enable = appList.filter { it.applicationInfo.enabled }
                tvResult?.text =
                    "search result: [${appList.size}] application contains - ,enable:${enable.size},disable:${disabled.size}"
                adapter.notifyDataSetChanged()
                refreshing = false
            }
        }
    }
}