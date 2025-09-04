package com.android.android14;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 *
 * Turn off the display without locking the device on Android 14
 * 只息屏不锁屏
 */
public class TurnOffScreen {
    public static void log(String value) {
        System.out.println(value);
        Log.d("Test", value);
    }

    @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi", "BlockedPrivateApi"})
    public static void main(String[] args) {
        try {
            log("Start");
            var mode = 0; // OFF
            if (args.length == 1) {
                mode = Integer.parseInt(args[0]);
            }
            var surfaceComposer = SurfaceComposer.getInstance();
            long[] displayIds = surfaceComposer.getPhysicalDisplayIds();
            for (long displayId : displayIds) {
                log("displayId: " + displayId);
                surfaceComposer.setPowerMode(displayId, mode);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/native/libs/gui/aidl/android/gui/ISurfaceComposer.aidl
    public static class SurfaceComposer {
        private static SurfaceComposer instance;
        private final IBinder surfaceComposer;

        @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi", "BlockedPrivateApi"})
        private SurfaceComposer() {
            try {
                var serviceManagerClass = Class.forName("android.os.ServiceManager");
                var getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
                surfaceComposer = (IBinder) getServiceMethod.invoke(null, "SurfaceFlingerAIDL");
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        public static SurfaceComposer getInstance() {
            if (instance == null) {
                instance = new SurfaceComposer();
            }
            return instance;
        }

        public long[] getPhysicalDisplayIds() {
            var data = Parcel.obtain();
            var reply = Parcel.obtain();
            try {
                data.writeInterfaceToken("android.gui.ISurfaceComposer");
                surfaceComposer.transact(IBinder.FIRST_CALL_TRANSACTION + 5, data, reply, 0);
                reply.readException();
                return reply.createLongArray();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        public IBinder getPhysicalDisplayToken(long displayId) {
            var data = Parcel.obtain();
            var reply = Parcel.obtain();
            try {
                data.writeInterfaceToken("android.gui.ISurfaceComposer");
                data.writeLong(displayId);
                surfaceComposer.transact(IBinder.FIRST_CALL_TRANSACTION + 6, data, reply, 0);
                reply.readException();
                return reply.readStrongBinder();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        public void setPowerMode(IBinder display, int mode) {
            var data = Parcel.obtain();
            var reply = Parcel.obtain();
            try {
                data.writeInterfaceToken("android.gui.ISurfaceComposer");
                data.writeStrongBinder(display);
                data.writeInt(mode);
                surfaceComposer.transact(IBinder.FIRST_CALL_TRANSACTION + 8, data, reply, 0);
                reply.readException();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        public void setPowerMode(long displayId, int mode) {
            var token = getPhysicalDisplayToken(displayId);
            setPowerMode(token, mode);
        }
    }
}
