package com.yy.sumulate;

import android.app.Activity;
import android.view.InputEvent;

public class SyncIdleSimulate extends InputEventSimulate {
    /**
     * 构造函数
     *
     * @param base
     */
    public SyncIdleSimulate(Activity base) {
        super(base);
    }

    @Override
    InputEvent createInputEvent(int seq) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 静态创建方法
     *
     * @param base
     * @return
     */
    public static SyncIdleSimulate obtain(Activity base) {
        SyncIdleSimulate simulate = new SyncIdleSimulate(base);
        return simulate;
    }
}
