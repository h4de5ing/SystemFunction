package com.android.appmonitor

import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.ICameraService
import android.hardware.ICameraServiceListener
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ServiceManager
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*

class ForegroundService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    var audioManager: AudioManager? = null
    var activityManager: ActivityManager? = null
    override fun onCreate() {
        super.onCreate()
        println("日志服务正在运行...")
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        monitorAudio()
        monitorCamera()
        monitorApp()
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
                    }
                    println("$value 正在录音")
                }
            }
        }
    }

    private fun monitorCamera() {
        val cameraServer: ICameraService =
            ICameraService.Stub.asInterface(ServiceManager.getService("media.camera"))
        cameraServer.addListener(object : ICameraServiceListener.Stub() {
            override fun onStatusChanged(status: Int, cameraId: String?) {
            }

            override fun onPhysicalCameraStatusChanged(
                status: Int,
                cameraId: String?,
                physicalCameraId: String?
            ) {
            }

            override fun onTorchStatusChanged(status: Int, cameraId: String?) {
            }

            override fun onCameraAccessPrioritiesChanged() {
            }

            override fun onCameraOpened(cameraId: String?, clientPackageId: String?) {
                println("${now()} $clientPackageId onCameraOpened $cameraId")
            }

            override fun onCameraClosed(cameraId: String?) {
                println("${now()} onCameraClosed $cameraId")
            }
        })
    }

    private fun monitorApp() {
        var activityManagerService: IActivityManager =
            IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))
        activityManagerService.registerProcessObserver(object : IProcessObserver.Stub() {
            override fun onForegroundActivitiesChanged(
                pid: Int, uid: Int, foregroundActivities: Boolean
            ) {
                println(
                    "onForegroundActivitiesChanged pid=$pid uid=$uid $foregroundActivities"
                )
            }

            override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) {
                println("onForegroundServicesChanged pid=$pid uid=$uid")
            }

            override fun onProcessDied(pid: Int, uid: Int) {
                println("onProcessDied pid=$pid uid=$uid")
            }
        })
//    activityManagerService.registerUserSwitchObserver()
        activityManagerService.registerTaskStackListener(object : TaskStackListener() {
            override fun onTaskCreated(taskId: Int, componentName: ComponentName?) {
                super.onTaskCreated(taskId, componentName)
                println("onTaskCreated $taskId $componentName")
            }

            override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo?) {
                super.onTaskMovedToFront(taskInfo)
                println("onTaskMovedToFront $taskInfo")
            }
        })
    }

    fun now(): String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss", Locale.CHINA
    ).format(Date(System.currentTimeMillis()))

    override fun onDestroy() {
        super.onDestroy()
        try {
            audioManager?.unregisterAudioRecordingCallback(audioRecordingCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}