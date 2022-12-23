package com.android.systemfunction.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.systemfunction.R
import com.android.systemfunction.databinding.ActivityBackupSettingsBinding
import com.android.systemfunction.utils.*
import com.android.systemlib.*
import com.github.h4de5ing.filepicker.DialogUtils
import java.io.File
import kotlin.io.buffered
import kotlin.io.readLines

class BackupSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backup.setOnClickListener {
            DialogUtils.selectDir(this, "select dir", true) { files ->
                try {
                    val backFile = File("${files[0]}${File.separator}${files[1]}")
                    if (backFile.exists()) backFile.delete()
                    write2File(
                        backFile.absolutePath, getAllSettingsForSettingsBean(this).toJson(), false
                    )
                    toast("backup settings finish")
                } catch (e: Exception) {
                    toast("${e.message}")
                    e.printStackTrace()
                }
            }
        }
        binding.restore.setOnClickListener {
            DialogUtils.selectFile(this, "select file") { files ->
                try {
                    val json = files[0].stream().buffered().reader("utf-8").readString()
                    val bean = parserJson(json)
                    putSettings(this, bean)
                    toast("restore settings finish")
                } catch (e: Exception) {
                    toast("${e.message}")
                    e.printStackTrace()
                }
            }
        }
        binding.backupApn.setOnClickListener {
            DialogUtils.selectDir(this, "select dir", true) { files ->
                val apnFile = File("${files[0]}${File.separator}${files[1]}")
                if (apnFile.exists()) apnFile.delete()
                Thread {
                    try {
                        write2File(apnFile.absolutePath, getAPN(this), true)
                        runOnUiThread { toast("backup apn finish") }
                    } catch (e: Exception) {
                        runOnUiThread { toast("${e.message}") }
                    }
                }.start()
            }
        }
        binding.cleanApn.setOnClickListener {
            try {
                cleanAPN(this)
            } catch (e: Exception) {
                toast("${e.message}")
                e.printStackTrace()
            }
        }
        binding.restoreApn.setOnClickListener {
            DialogUtils.selectFile(this, "select file") { files ->
                Thread {
                    try {
                        runOnUiThread { showLoading("restore apn progress...") }
                        val list = files[0].stream().buffered().reader("utf-8").readLines()
                        println("apn：条数;${list.size}")
                        setAPN(this, list) { message, done ->
                            runOnUiThread { if (done) dismissLoading() else update(message) }
                        }
                    } catch (e: Exception) {
                        toast("${e.message}")
                        e.printStackTrace()
                    }
                }.start()
            }
        }
    }

    private fun update(message: String) {
        tipsView?.text = message
    }

    private var tipsView: TextView? = null
    private var dialog: AlertDialog? = null
    private fun showLoading(message: String) {
        try {
            if (dialog == null) {
                val build = AlertDialog.Builder(this)
                build.setTitle(message)
                val view = View.inflate(
                    this, R.layout.layout_custom_progress_dialog_view, null
                )
                dialog = build.setCancelable(true).setView(view).create()
                tipsView = view.findViewById(R.id.loading_tips)
                tipsView?.text = message
            }
            dialog?.apply {
                try {
                    if (this.isShowing) this.dismiss()
                    this.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissLoading() {
        dialog?.dismiss()
    }
}