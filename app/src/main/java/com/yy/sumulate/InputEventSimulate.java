package com.yy.sumulate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class InputEventSimulate implements Runnable {
    private static final String TAG = InputEventSimulate.class.getSimpleName();
    protected final Activity base;
    private Instrumentation inst;
    private AtomicInteger seq = new AtomicInteger(0);

    /**
     * 构造函数
     *
     * @param base
     */
    @SuppressLint("PrivateApi")
    public InputEventSimulate(Activity base) {
        if (null == base) {
            throw new IllegalArgumentException("Activity host cannot be null.");
        }
        this.base = base;
        //反射获取inst
        try {
            Class<?> clazz = Class.forName("android.app.ActivityThread");
            Method method = clazz.getDeclaredMethod("currentActivityThread");
            method.setAccessible(true);
            Object thread = method.invoke(null);
            Field field = clazz.getDeclaredField("mInstrumentation");
            field.setAccessible(true);
            inst = (Instrumentation) field.get(thread);
        } catch (Exception e) {
            e.printStackTrace();
            inst = null;
        }
    }

    @CallSuper
    @Override
    public void run() {
        Log.d(TAG, "InputEventSimulate.run()");
        InputEvent event = createInputEvent(seq.getAndIncrement());
        if (null != event) {
            if (event instanceof KeyEvent) {
                final KeyEvent keyEvent = (KeyEvent) event;
//                if (null != inst) {
//                    inst.sendKeySync(keyEvent);
//                    inst.waitForIdleSync();
//                } else {
                    base.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            base.dispatchKeyEvent(keyEvent);
                        }
                    });
//                }
            } else if (event instanceof MotionEvent) {
                final MotionEvent motionEvent = (MotionEvent) event;
//                if (null != inst) {
//                    inst.sendPointerSync(motionEvent);
//                    inst.waitForIdleSync();
//                } else {
                    base.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            base.dispatchTouchEvent(motionEvent);
                        }
                    });
//                }
            }
            //loop next
            handler.postDelayed(this, 10);
        }
    }

    /**
     * 静态创建方法
     *
     * @param action
     * @param x
     * @param y
     * @return
     */
    protected static MotionEvent obtain(int action, float x, float y) {
        long sync = SystemClock.uptimeMillis();
        long time = SystemClock.uptimeMillis();
        return MotionEvent.obtain(sync, time, action, x, y, 0);
    }

    /**
     * 创建事件，返回null时退出loop
     *
     * @param seq
     * @return
     */
    abstract InputEvent createInputEvent(int seq);

    private HandlerThread thread;
    private Handler handler;

    /**
     * 准备开始分发事件
     */
    public void simulate() {
        Log.d(TAG, "InputEventSimulate.simulate():" + seq.get());
        //创建线程
        if (null == handler) {
            thread = new HandlerThread(toString());
            thread.start();
            handler = new Handler(thread.getLooper());
        }
        handler.post(this);
    }
}
