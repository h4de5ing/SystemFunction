package com.android.systemlib;

import android.util.Log;

public class Logger {
    public static void v(String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(tag(), "[" + methodName() + "] " + msg);
        }
    }

    public static void d(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag(), "[" + methodName() + "] " + msg);
        }
    }

    public static void i() {
        Log.i(tag(), "[IN] [" + methodName() + "]");
    }

    public static void i(String msg) {
        Log.i(tag(), "[IN] [" + methodName() + "] " + msg);
    }

    public static void w(String msg) {
        Log.w(tag(), "[" + methodName() + "] " + msg);
    }

    public static void e(String msg) {
        Log.e(tag(), "[" + methodName() + "] " + msg);
    }

    public static void start() {
        if (BuildConfig.DEBUG) {
            Log.d(tag(), methodName() + " [IN]");
        }
    }

    public static void end() {
        if (BuildConfig.DEBUG) {
            Log.d(tag(), methodName() + " [END]");
        }
    }

    public static String tag() {
        int level = 4;
        StackTraceElement trace = Thread.currentThread().getStackTrace()[level];
        String fileName = trace.getFileName();
        String classPath = trace.getClassName();
        String className = classPath.substring(classPath.lastIndexOf(".") + 1);
        String methodName = trace.getMethodName();
        int lineNumber = trace.getLineNumber();
        return "GH0st_" + className + "(" + lineNumber + ")";
    }

    public static String methodName() {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
    }
}
