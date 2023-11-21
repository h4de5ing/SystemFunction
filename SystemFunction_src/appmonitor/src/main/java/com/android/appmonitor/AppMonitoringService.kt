package com.android.appmonitor

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.ICameraService
import android.hardware.ICameraServiceListener
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.*
import java.io2.J2
import java.io2.JavaUtils
import java.lang.reflect.Field
import java.util.*

class AppMonitoringService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    private var audioManager: AudioManager? = null
    private var activityManager: ActivityManager? = null
    override fun onCreate() {
        super.onCreate()
        println("gh0st in shadow...")
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        monitorAudio()
        monitorCamera()
        monitorApp()
        writePackageList(this)?.apply {
            try {
                val builder = StringBuilder()
                this.forEach { builder.append(it).append("\n") }
                JavaUtils.write(J2.packagePath, builder.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val intentFile = IntentFilter("com.android.permissioncontroller.ops")
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                try {
                    val packageName = p1?.getStringExtra("packageName")
                    val perminsss = p1?.getStringExtra("perminsss")
                    val status = p1?.getIntExtra("status", -1) ?: -1
                    println("接收到权限广播:${packageName}->${perminsss}->${status}")

                    listener.invoke("$packageName", "$perminsss", status)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, intentFile)
    }


    private fun monitorAudio() {
        audioManager?.registerAudioRecordingCallback(
            audioRecordingCallback, Handler(Looper.myLooper()!!)
        )
    }

    private val audioRecordingCallback = object : AudioManager.AudioRecordingCallback() {
        @SuppressLint("SoonBlockedPrivateApi")
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            super.onRecordingConfigChanged(configs)
            if ((configs?.size ?: 0) > 0) {
//                Log.d("gh0st", "有人在录音:${configs?.size}")
                configs?.forEach {
                    var value = ""
                    try {
                        val field: Field = it.javaClass.getDeclaredField("mClientPackageName")
                        field.isAccessible = true
                        value = field.get(it)?.toString() ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    println("$value 正在录音")
                }
            }
        }
    }
    private var clientPackageId: String = ""
    private fun monitorCamera() {
        val cameraServer: ICameraService =
            ICameraService.Stub.asInterface(ServiceManager.getService("media.camera"))
        cameraServer.addListener(object : ICameraServiceListener.Stub() {
            override fun onStatusChanged(status: Int, cameraId: String?) {
            }

            override fun onPhysicalCameraStatusChanged(
                status: Int, cameraId: String?, physicalCameraId: String?
            ) {
            }

            override fun onTorchStatusChanged(status: Int, cameraId: String?) {
            }

            override fun onCameraAccessPrioritiesChanged() {
            }

            override fun onCameraOpened(cameraId: String?, clientPackageId: String?) {
                println("${now()} $clientPackageId 打开 id=$cameraId")
            }

            override fun onCameraClosed(cameraId: String?) {
                println("${now()} $clientPackageId 关闭 id=$cameraId")
            }
        })
    }

    private fun monitorApp() {
        val activityManagerService: IActivityManager =
            IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))
        activityManagerService.registerProcessObserver(object : IProcessObserver.Stub() {
            override fun onForegroundActivitiesChanged(
                pid: Int, uid: Int, foregroundActivities: Boolean
            ) {
                val packageName = getPackageFromPid2(this@AppMonitoringService, pid)
                println("${now()} $packageName 页面->${if (foregroundActivities) "打开" else "关闭"}")
            }

            override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) {
                //val packageName = getPackageFromPid2(this@AppMonitoringService, pid)
                //println("${now()} $packageName 服务结束 $serviceTypes ")
            }

            override fun onProcessDied(pid: Int, uid: Int) {
                //val packageName = getPackageFromPid2(this@AppMonitoringService, pid)
                //println("${now()} $packageName 进程结束 pid=$pid uid=$uid ")
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            audioManager?.unregisterAudioRecordingCallback(audioRecordingCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}