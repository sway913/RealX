package com.yy.sumulate;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.view.InputEvent;

import static android.os.Looper.getMainLooper;

public class SyncIdleSimulate extends InputEventSimulate {
    private static final String TAG = "InputEventSimulate";

    @Override
    InputEvent createInputEvent(int seq) {
        waitForIdleSync();
        return null;
    }

    /**
     * 同步等待设备闲置
     */
    private void waitForIdleSync() {
        Idler idler = new Idler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getMainLooper().getQueue().addIdleHandler(idler);
        } else {
            getMainLooper().myQueue().addIdleHandler(idler);
        }
        Empty empty = new Empty();
        getMainHandler().post(empty);
        idler.waitForIdle();
    }

    private Handler handler;

    /**
     * 把任务抛到主线程
     *
     * @return
     */
    private Handler getMainHandler() {
        if (null == handler) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    static final class Idler implements MessageQueue.IdleHandler {
        private final Runnable mCallback;
        private boolean mIdle;

        Idler() {
            this(null);
        }

        Idler(Runnable callback) {
            mCallback = callback;
            mIdle = false;
        }

        public final boolean queueIdle() {
            Log.d(TAG, "Idler.queueIdle()");
            if (mCallback != null) {
                mCallback.run();
            }
            synchronized (this) {
                mIdle = true;
                notifyAll();
            }
            return false;
        }

        public void waitForIdle() {
            Log.d(TAG, "Idler.waitForIdle()");
            synchronized (this) {
                while (!mIdle) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }
    }

    static final class Empty implements Runnable {

        @Override
        public void run() {

        }
    }

    /**
     * 静态创建方法
     *
     * @return
     */
    public static SyncIdleSimulate obtain() {
        SyncIdleSimulate simulate = new SyncIdleSimulate();
        return simulate;
    }
}
