package com.android.systemfunction.ui

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.systemfunction.R
import com.android.systemfunction.adapter.ListAppAdapter
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.utils.getInstallApp
import kotlinx.android.synthetic.main.activity_appmanager.*

@RequiresApi(Build.VERSION_CODES.N)
class APPManagerActivity : AppCompatActivity() {
    private val adapter = ListAppAdapter()
    private val appList = mutableListOf<AppBean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appmanager)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        adapter.setNewInstance(appList)
    }

    override fun onResume() {
        super.onResume()
        appList.clear()
        appList.addAll(getInstallApp())
        adapter.notifyDataSetChanged()
    }
}