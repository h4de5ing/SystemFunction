package com.android.systemfunction.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.systemfunction.adapter.ListAppAdapter
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.databinding.ActivityAppmanagerBinding
import com.android.systemfunction.utils.getInstallApp
import com.github.h4de5ing.baseui.base.BaseReturnActivity

class APPManagerActivity : BaseReturnActivity() {
    private val adapter = ListAppAdapter()
    private val appList = mutableListOf<AppBean>()
    private lateinit var binding: ActivityAppmanagerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppmanagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.setNewInstance(appList)
    }

    override fun onResume() {
        super.onResume()
        appList.clear()
        appList.addAll(getInstallApp())
        adapter.notifyDataSetChanged()
    }
}