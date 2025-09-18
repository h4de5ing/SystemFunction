package com.android.android14

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log

/**
 *
 * Turn off the display without locking the device on Android 14
 * 只息屏不锁屏
 */
object TurnOffScreen {
    fun log(value: String) {
        println(value)
        Log.d("Test", value)
    }

    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/native/libs/gui/aidl/android/gui/ISurfaceComposer.aidl
    class SurfaceComposer @SuppressLint(
        "PrivateApi",
        "SoonBlockedPrivateApi",
        "DiscouragedPrivateApi",
        "BlockedPrivateApi"
    ) private constructor() {
        companion object {
            var instance: SurfaceComposer? = null
                get() {
                    if (field == null) field = SurfaceComposer()
                    return field
                }
                private set
        }

        private val surfaceComposer: IBinder?

        init {
            try {
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod =
                    serviceManagerClass.getDeclaredMethod("getService", String::class.java)
                surfaceComposer = getServiceMethod.invoke(null, "SurfaceFlingerAIDL") as IBinder?
            } catch (e: Exception) {
                throw AssertionError(e)
            }
        }

        val physicalDisplayIds: LongArray
            get() {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken("android.gui.ISurfaceComposer")
                    surfaceComposer!!.transact(
                        IBinder.FIRST_CALL_TRANSACTION + 5,
                        data,
                        reply,
                        0
                    )
                    reply.readException()
                    return reply.createLongArray()!!
                } catch (e: RemoteException) {
                    throw RuntimeException(e)
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }

        fun getPhysicalDisplayToken(displayId: Long): IBinder? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("android.gui.ISurfaceComposer")
                data.writeLong(displayId)
                surfaceComposer!!.transact(IBinder.FIRST_CALL_TRANSACTION + 6, data, reply, 0)
                reply.readException()
                return reply.readStrongBinder()
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

        fun setPowerMode(display: IBinder?, mode: Int) {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("android.gui.ISurfaceComposer")
                data.writeStrongBinder(display)
                data.writeInt(mode)
                surfaceComposer!!.transact(IBinder.FIRST_CALL_TRANSACTION + 8, data, reply, 0)
                reply.readException()
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

        fun setPowerMode(displayId: Long, mode: Int) {
            val token = getPhysicalDisplayToken(displayId)
            setPowerMode(token, mode)
        }


    }
}
