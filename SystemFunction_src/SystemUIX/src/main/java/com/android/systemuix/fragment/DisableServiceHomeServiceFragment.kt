package com.android.systemuix.fragment

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.systemuix.R
import com.android.systemuix.databinding.FragmentHomeBinding

class DisableServiceHomeServiceFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun addFragment(fragment: Fragment) {
        fragmentManager.beginTransaction().replace(R.id.fragmentLayout, fragment).commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button1.setOnClickListener { addFragment(NormalServiceFragment()) }
        binding.button2.setOnClickListener { addFragment(DangersServiceFragment()) }
        binding.button1.performClick()
    }
}