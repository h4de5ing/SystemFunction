package com.android.systemfunction.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import com.android.systemfunction.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// TODO 封装到lib里面去
public class RealtimeToast {
    private static final int BK_COLOR = -10197916;
    private static final int HANDLE_CANCEL = 0;
    private static final int LONG_DELAY = 3600;
    private static final int SHORT_DELAY = 2100;
    private static Method mHide;
    private static Field mNextView;
    private static Field mParams;
    private static RealtimeToast mRealtimeToast;
    private static Method mShow;
    private static Field mTn;
    private static boolean mVerN_MR1 = false;
    private boolean isShow = false;
    private Animation mAnimation;
    private Context mContext;
    private int mDuration = 0;
    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            RealtimeToast.this.hide();
        }
    };
    private Object mTN;
    private Toast mToast;
    private View mView;

    private RealtimeToast(Context context) {
        this.mContext = context;
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private void initToast() {
        try {
            if (mTn == null) {
                mTn = this.mToast.getClass().getDeclaredField("mTN");
                mTn.setAccessible(true);
            }
            if (this.mTN == null) {
                this.mTN = mTn.get(this.mToast);
            }
            if (mShow == null) {
                if (Build.VERSION.SDK_INT >= 25) {
                    try {
                        mShow = this.mTN.getClass().getMethod("show", IBinder.class);
                        mVerN_MR1 = true;
                    } catch (Exception e) {
                    }
                }
                if (mShow == null) {
                    mShow = this.mTN.getClass().getMethod("show", new Class[0]);
                }
            }
            if (mHide == null) {
                mHide = this.mTN.getClass().getMethod("hide", new Class[0]);
            }
            if (mNextView == null) {
                mNextView = this.mTN.getClass().getDeclaredField("mNextView");
                mNextView.setAccessible(true);
            }
            if (mParams == null) {
                mParams = this.mTN.getClass().getDeclaredField("mParams");
                mParams.setAccessible(true);
            }
        } catch (Exception e2) {
        }
    }

    private void setWindowType(int type) {
        try {
            ((WindowManager.LayoutParams) mParams.get(this.mTN)).type = type;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setAlwaysOnTop() {
        setWindowType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

    public void setDrawOverStatusBar() {
        setWindowType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
    }

    private void handleCnacel() {
        this.mHandler.removeMessages(0);
        this.mHandler.sendEmptyMessageDelayed(0, (long) (this.mDuration == 0 ? SHORT_DELAY : LONG_DELAY));
    }

    public void disableStartAnimation() {
        if (this.mAnimation == null) {
            this.mAnimation = this.mView.getAnimation();
        }
        this.mView.clearAnimation();
        this.mView.setAnimation(null);
    }

    public void setText(String newTxt) {
        this.mToast.setText(newTxt);
    }

    public void setTextAppearance(int resId) {
        ((TextView) ((ViewGroup) this.mView).getChildAt(0)).setTextAppearance(resId);
    }

    public void setGravity(int gravity, int xOffset, int yOffset) {
        this.mToast.setGravity(gravity, xOffset, yOffset);
    }

    public void setBackgroundColor(int color) {
        this.mView.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    public void show() {
        try {
            if (!this.isShow) {
                mNextView.set(this.mTN, this.mView);
                if (mVerN_MR1) {
                    mShow.invoke(this.mTN, this.mView.getWindowToken());
                } else {
                    mShow.invoke(this.mTN, new Object[0]);
                }
                this.isShow = true;
                handleCnacel();
                if (this.mAnimation != null) {
                    this.mView.setAnimation(this.mAnimation);
                    return;
                }
                return;
            }
            handleCnacel();
        } catch (Exception e) {
        }
    }

    public void hide() {
        if (this.isShow) {
            mRealtimeToast = null;
            this.mHandler.removeMessages(0);
            try {
                mHide.invoke(this.mTN, new Object[0]);
                this.isShow = false;
            } catch (Exception e) {
            }
        }
    }

    public static RealtimeToast makeText(Context context, int resId, int duration) {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    public static RealtimeToast makeText(Context context, CharSequence text, int duration) {
        if (mRealtimeToast != null) {
            if (mRealtimeToast.mContext == context) {
                mRealtimeToast.mToast.setText(text);
                mRealtimeToast.mDuration = duration;
            } else {
                mRealtimeToast.hide();
            }
        }
        if (mRealtimeToast == null) {
            try {
                mRealtimeToast = new RealtimeToast(context);
                mRealtimeToast.mToast = Toast.makeText(context, text, duration);
                //mRealtimeToast.mView = mRealtimeToast.mToast.getView();
                View view = View.inflate(context, R.layout.toast, null);
                TextView textView = view.findViewById(R.id.tv);
                textView.setText(text);
                mRealtimeToast.mView = view;
                mRealtimeToast.mDuration = duration;
                mRealtimeToast.setBackgroundColor(BK_COLOR);
                mRealtimeToast.initToast();
                //mRealtimeToast.setAlwaysOnTop();
                mRealtimeToast.setDrawOverStatusBar();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mRealtimeToast;
    }
}
