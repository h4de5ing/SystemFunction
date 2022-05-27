package com.android.otax

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.RecoverySystem
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.android.systemlib.getSystemPropertyString
import com.github.h4de5ing.filepicker.DialogUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        upload.setOnClickListener {
            DialogUtils.selectFile(this, "请选择一个ZIP") {
                val zipFile = File(it[0])
                if (zipFile.exists()) {
                    val sp = getSystemPropertyString("ro.boot.slot_suffix")
                    if (TextUtils.isEmpty(sp)) {//整包升级
                        RecoverySystem.installPackage(this, zipFile)
                    } else { //A/B系统升级
                        try {
                            val updateEngine = UpdateEngine()
                            updateEngine.bind(object : UpdateEngineCallback() {
                                override fun onStatusUpdate(status: Int, percent: Float) {
                                    when (status) {
                                        UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT ->
                                            reboot()
                                        UpdateEngine.UpdateStatusConstants.DOWNLOADING ->
                                            result.text = "${percent.toInt()}"
                                    }
                                }

                                override fun onPayloadApplicationComplete(errorCode: Int) {
                                    setError(errorCode)
                                }
                            })
                            val pu: UpdateParser.ParsedUpdate = UpdateParser.parse(zipFile)!!
                            updateEngine.applyPayload(pu.mUrl, pu.mOffset, pu.mSize, pu.mProps)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {

                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setError(errorCode: Int) {
        var message = ""
        when (errorCode) {
            UpdateEngine.ErrorCodeConstants.SUCCESS -> message = "升级成功"
            UpdateEngine.ErrorCodeConstants.ERROR -> message = "升级失败"
            UpdateEngine.ErrorCodeConstants.FILESYSTEM_COPIER_ERROR -> message = "暂时未使用的错误码"
            UpdateEngine.ErrorCodeConstants.POST_INSTALL_RUNNER_ERROR -> message = "升级安装结束，设置启动分区失败"
            UpdateEngine.ErrorCodeConstants.PAYLOAD_MISMATCHED_TYPE_ERROR -> message = ""
            UpdateEngine.ErrorCodeConstants.INSTALL_DEVICE_OPEN_ERROR -> message =
                "无法启动升级。可能是原因：分区错误，设备支持升级的分区和升级包内的不匹配；设备处于disable-verity状态"
            UpdateEngine.ErrorCodeConstants.KERNEL_DEVICE_OPEN_ERROR -> message = "暂时未使用的错误码"
            UpdateEngine.ErrorCodeConstants.DOWNLOAD_TRANSFER_ERROR -> message = "找不到升级包"
            UpdateEngine.ErrorCodeConstants.PAYLOAD_HASH_MISMATCH_ERROR -> message = "文件hash值不匹配"
            UpdateEngine.ErrorCodeConstants.PAYLOAD_SIZE_MISMATCH_ERROR -> message = "文件大小值不匹配"
            UpdateEngine.ErrorCodeConstants.DOWNLOAD_PAYLOAD_VERIFICATION_ERROR -> message =
                "签名验证失败"
            UpdateEngine.ErrorCodeConstants.PAYLOAD_TIMESTAMP_ERROR -> message = "不能降级安装"
        }
        try {
            val constants = UpdateEngine.ErrorCodeConstants()
            var errMessage = ""
            val declaredFields = constants.javaClass.declaredFields
            for (field in declaredFields) {
                field.isAccessible = true
                val name = field.name
                val value = field[constants] as Int
                if (value == errorCode) {
                    errMessage = "$name [$value]"
                }
            }
            result.text = errMessage
        } catch (e: Exception) {
            result.text = "${e.message} [" + errorCode + "]"
            e.printStackTrace()
        }
    }

    private fun reboot() {
        val intent = Intent("android.intent.action.REBOOT")
        intent.putExtra("nowait", 1)
        intent.putExtra("interval", 1)
        intent.putExtra("window", 0)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}