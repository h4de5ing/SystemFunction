package com.android.systemfunction.utils;

import android.os.Handler;
import android.view.WindowManager;
import android.widget.PopupWindow;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

//系统接口hook
public class PopNoRecordProxy implements InvocationHandler {
    private Object mWindowManager;//PopupWindow类的mWindowManager对象

    public static PopNoRecordProxy instance() {
        return new PopNoRecordProxy();
    }

    public void noScreenRecord(PopupWindow popupWindow) {
        if (popupWindow == null) {
            return;
        }
        try {
            //通过反射获得PopupWindow类的私有对象：mWindowManager
            Field windowManagerField = PopupWindow.class.getDeclaredField("mWindowManager");
            windowManagerField.setAccessible(true);
            mWindowManager = windowManagerField.get(popupWindow);
            if (mWindowManager == null) {
                return;
            }
            //创建WindowManager的动态代理对象proxy
            Object proxy = Proxy.newProxyInstance(Handler.class.getClassLoader(), new Class[]{WindowManager.class}, this);

            //注入动态代理对象proxy（即：mWindowManager对象由proxy对象来代理）
            windowManagerField.set(popupWindow, proxy);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //拦截方法mWindowManager.addView(View view, ViewGroup.LayoutParams params);
            if (method != null && method.getName() != null && method.getName().equals("addView")
                    && args != null && args.length == 2) {
                //获取WindowManager.LayoutParams，即：ViewGroup.LayoutParams
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) args[1];
                //禁止录屏
                setNoScreenRecord(params);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return method.invoke(mWindowManager, args);
    }

    /**
     * 禁止录屏
     */
    private void setNoScreenRecord(WindowManager.LayoutParams params) {
        setFlags(params, WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * 允许录屏
     */
    private void setAllowScreenRecord(WindowManager.LayoutParams params) {
        setFlags(params, 0, WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * 设置WindowManager.LayoutParams flag属性（参考系统类Window.setFlags(int flags, int mask)）
     *
     * @param params WindowManager.LayoutParams
     * @param flags  The new window flags (see WindowManager.LayoutParams).
     * @param mask   Which of the window flag bits to modify.
     */
    private void setFlags(WindowManager.LayoutParams params, int flags, int mask) {
        try {
            if (params == null) {
                return;
            }
            params.flags = (params.flags & ~mask) | (flags & mask);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
