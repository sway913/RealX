package com.yy.sumulate;

import android.view.InputEvent;

public class SyncIdleSimulate extends InputEventSimulate {

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
     * @return
     */
    public static SyncIdleSimulate obtain() {
        SyncIdleSimulate simulate = new SyncIdleSimulate();
        return simulate;
    }
}
