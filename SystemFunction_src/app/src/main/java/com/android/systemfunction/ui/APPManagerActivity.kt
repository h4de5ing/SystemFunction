package com.android.systemfunction.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.systemfunction.R
import com.android.systemfunction.adapter.ListAppAdapter
import com.android.systemfunction.app.App
import com.android.systemfunction.bean.AppBean
import kotlinx.android.synthetic.main.activity_appmanager.*

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
//        adapter.setChange(ApplicationClass.dao::updateApp)
        loadData()
    }

    private fun loadData(){
        appList.clear()
        appList.addAll(App.list)
        adapter.notifyDataSetChanged()
    }
}