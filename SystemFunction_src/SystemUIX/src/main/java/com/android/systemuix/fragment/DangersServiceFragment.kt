package com.android.systemuix.fragment

import android.annotation.SuppressLint
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.systemuix.R
import com.android.systemuix.adapter.DisableServiceAdapter
import com.android.systemuix.change
import com.android.systemuix.dangerous
import com.android.systemuix.databinding.FragmentNormalServiceBinding
import com.android.systemuix.whiteList
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DangersServiceFragment : Fragment() {
    private lateinit var binding: FragmentNormalServiceBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentNormalServiceBinding.inflate(layoutInflater)
        return binding.root
    }

    private val adapter = DisableServiceAdapter { updateTips() }
    private val appList = mutableListOf<PackageInfo>()
    private var textKeyword = ""
    private val scope = MainScope()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.keyword.change {
            update(activity, it, false)
            textKeyword = it
        }
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter
        update(activity, textKeyword, true)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun update(context: Context, keyword: String, updateAppList: Boolean) {
        scope.launch {
            activity?.runOnUiThread {
                if (updateAppList) {
                    appList.clear()
                    appList.addAll(dangerous.filter { it.packageName !in whiteList })
                }
                val newList = appList.filter {
                    it.packageName.contains(keyword) || it.applicationInfo.loadLabel(context.packageManager)
                        .contains(keyword)
                }
                val disabled = newList.filter { !it.applicationInfo.enabled }
                val enable = newList.filter { it.applicationInfo.enabled }
                binding.result.setTextColor(Color.RED)
                binding.result.text =
                    getString(R.string.danger_search_result, newList.size, disabled.size, enable.size)
                adapter.setNewInstance(newList.toMutableList())
            }
        }
    }

    private fun updateTips() = update(activity, textKeyword, true)
}