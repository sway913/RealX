package com.yy.sumulate;

import android.app.Instrumentation;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class InputEventSimulate implements Runnable {
    private static final String TAG = InputEventSimulate.class.getSimpleName();
    private Instrumentation inst = new Instrumentation();
    private AtomicInteger seq = new AtomicInteger(0);

    @CallSuper
    @Override
    public void run() {
        Log.d(TAG, "InputEventSimulate.run()");
        InputEvent event = createInputEvent(seq.getAndIncrement());
        if (null != event) {
            if (event instanceof KeyEvent) {
                final KeyEvent keyEvent = (KeyEvent) event;
                inst.sendKeySync(keyEvent);
            } else if (event instanceof MotionEvent) {
                final MotionEvent motionEvent = (MotionEvent) event;
                inst.sendPointerSync(motionEvent);
            }
            //loop next
            handler.postDelayed(this, 30);
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
