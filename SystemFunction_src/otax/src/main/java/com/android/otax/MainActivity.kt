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
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        copy.setOnClickListener {
            DialogUtils.selectFile(this, "请选择一个ZIP") {
                val zipFile = File(it[0])
                if (zipFile.exists()) {
                    try {
                        val otaFile = File("/data/ota_package/update.zip")
                        zipFile.copyTo(otaFile, true)
                        result.text = if (otaFile.exists()) "复制成功" else "复制失败"
                        val commandResult = runCommand("chmod 666 ${otaFile.absoluteFile}")
                        result.append(commandResult)
                    } catch (e: Exception) {
                        result.text = e.message
                        e.printStackTrace()
                    }
                }
            }
        }
        upload.setOnClickListener {
            val otaFile = File("/data/ota_package/update.zip")
            abupdate(otaFile)
//            DialogUtils.selectFile(this, "请选择一个ZIP") {
//                val zipFile = File(it[0])
//                if (zipFile.exists()) {
////                    otaFile(zipFile)
//                    val sp = getSystemPropertyString("ro.boot.slot_suffix")
//                    if (TextUtils.isEmpty(sp)) {//整包升级
//                        //RecoverySystem.verifyPackage(zipFile, null, null)
//                        RecoverySystem.installPackage(this, zipFile)
//                    } else { //A/B系统升级
//                        abupdate(zipFile)
//                    }
//                } else {
//                    result.text = "file not found"
//                }
//            }
        }
    }

    private fun otaFile(zipFile: File) {
        Thread {
            try {
                RecoverySystem.verifyPackage(
                    zipFile,
                    { progress -> println("文件校验进度：${progress}") },
                    File("/system/etc/security/otacerts.zip")
                )
                //val result = RecoverySystem.verifyPackageCompatibility(zipFile)
                //println("文件校验结果:${result}")
                //val otaFile = File("/data/ota_package/update.zip")
                //zipFile.copyTo(otaFile, true)
                //RecoverySystem.installPackage(this, otaFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun abupdate(zipFile: File) {
        Thread {
            try {
                val updateEngine = UpdateEngine()
                updateEngine.bind(object : UpdateEngineCallback() {
                    override fun onStatusUpdate(status: Int, percent: Float) {
                        when (status) {
                            UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT -> reboot()
                            UpdateEngine.UpdateStatusConstants.DOWNLOADING -> result.text =
                                "${percent.toInt()}"
                        }
                    }

                    override fun onPayloadApplicationComplete(errorCode: Int) {
                        setError(errorCode)
                    }
                })
//                val otaFile = File("/data/ota_package/update.zip")
//                zipFile.copyTo(otaFile, true)
//                result.text = if (otaFile.exists()) "复制成功" else "复制失败"

                val specs = PayloadSpecs().forNonStreaming(zipFile)
                updateEngine.applyPayload(
                    specs.url,
                    specs.offset,
                    specs.size,
                    specs.properties.toTypedArray()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    @SuppressLint("SetTextI18n")
    private fun setError(errorCode: Int) {
        runOnUiThread {
            var message = ""
            when (errorCode) {
                UpdateEngine.ErrorCodeConstants.SUCCESS -> message = "升级成功"
                UpdateEngine.ErrorCodeConstants.ERROR -> message = "升级失败"
                UpdateEngine.ErrorCodeConstants.FILESYSTEM_COPIER_ERROR -> message = "暂时未使用的错误码"
                UpdateEngine.ErrorCodeConstants.POST_INSTALL_RUNNER_ERROR -> message =
                    "升级安装结束，设置启动分区失败"
                UpdateEngine.ErrorCodeConstants.PAYLOAD_MISMATCHED_TYPE_ERROR -> message = ""
                UpdateEngine.ErrorCodeConstants.INSTALL_DEVICE_OPEN_ERROR -> message =
                    "无法启动升级。可能是原因：分区错误，设备支持升级的分区和升级包内的不匹配；设备处于disable-verity状态"
                UpdateEngine.ErrorCodeConstants.KERNEL_DEVICE_OPEN_ERROR -> message = "暂时未使用的错误码"
                UpdateEngine.ErrorCodeConstants.DOWNLOAD_TRANSFER_ERROR -> message = "找不到升级包"
                UpdateEngine.ErrorCodeConstants.PAYLOAD_HASH_MISMATCH_ERROR -> message =
                    "文件hash值不匹配"
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
                        errMessage = "$name [$value] (${message})"
                    }
                }
                result.text = errMessage
            } catch (e: Exception) {
                result.text = "${e.message} [" + errorCode + "]"
                e.printStackTrace()
            }
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

    private fun runCommand(command: String): String? {
        var process: Process? = null
        var result = ""
        var os: DataOutputStream? = null
        var read: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec("${command}\n")
            Thread.sleep(500)
            os = DataOutputStream(process.outputStream)
            read = BufferedReader(InputStreamReader(process.inputStream))
//            os.writeBytes("${command}\n")
            os.writeBytes("exit\n")
            os.flush()
            var line: String?
            while (read.readLine().also { line = it } != null) {
                result += line
                result += "\n"
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            os?.close()
            read?.close()
            process?.destroy()
        }
        return result
    }
}