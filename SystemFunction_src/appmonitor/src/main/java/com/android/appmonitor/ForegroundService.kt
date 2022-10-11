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
import android.os.*
import androidx.annotation.RequiresApi
import com.jiangc.receiver.FileObserverJni
import java.io.File
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
        monitorOps()
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
                status: Int, cameraId: String?, physicalCameraId: String?
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

    private fun monitorOps() {
        val opsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            opsManager.startWatchingActive(
                arrayOf(
                    AppOpsManager.OPSTR_CAMERA,
                    AppOpsManager.OPSTR_FINE_LOCATION,
                    AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE,
                    AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE,
                ), this.mainExecutor
            ) { op, uid, packageName, active ->
                println("op=$op uid=$uid packageName=$packageName active=$active")
            }
//            opsManager.startWatchingMode(
//                AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE, "com.coolapk.market", 0
//            ) { op, packageName ->
//                println("op=$op packageName=$packageName")
//            }

//            opsManager.setOnOpNotedCallback(mainExecutor,object:AppOpsManager.OnOpNotedCallback(){
//                override fun onNoted(op: SyncNotedAppOp) {
//                    println("onNoted ${op}")
//                }
//
//                override fun onSelfNoted(op: SyncNotedAppOp) {
//                    println("onSelfNoted ${op}")
//                }
//
//                override fun onAsyncNoted(op: AsyncNotedAppOp) {
//                    println("onAsyncNoted ${op}")
//                }
//            })

            FileObserverJni("/sdcard/",
                FileObserverJni.ALL_EVENTS,
                object : FileObserverJni.ILifecycle {
                    override fun onInit(errno: Int) {
                        if (0 == errno) {
                            println("onInit: 初始化成功")
                        } else {
                            println("onInit: 初始化失败: " + FileObserverJni.error2String(errno))
                        }
                    }

                    override fun onExit(errno: Int) {
                        if (0 == errno) {
                            println("onExit: 正常退出")
                        } else {
                            println("onExit: 异常退出: $errno")
                        }
                    }
                }).setmCallback { path, mask -> println("$path $mask") }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    class SDCardListener(path: File) : FileObserver(path, ACCESS) {
        override fun onEvent(event: Int, path: String?) {
            println("有人动了文件:${path} $event")
        }
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