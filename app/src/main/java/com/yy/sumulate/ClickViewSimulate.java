package com.yy.sumulate;

import android.util.Log;
import android.view.InputEvent;
import android.view.View;

public class ClickViewSimulate extends InputEventSimulate {
    private static final String TAG = ClickViewSimulate.class.getSimpleName();
    private final ClickPointSimulate real;

    /**
     * 构造函数
     */
    public ClickViewSimulate(View view) {
        if (null == view) {
            throw new IllegalArgumentException("View target cannot be null.");
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0] + view.getWidth() / 2;
        int y = location[1] + view.getHeight() / 2;
        real = ClickPointSimulate.obtain(x, y);
    }

    @Override
    InputEvent createInputEvent(int seq) {
        Log.d(TAG, String.format("createInputEvent():%d, %d, %d", seq, real.point.x, real.point.y));
        return real.createInputEvent(seq);
    }

    /**
     * 静态创建方式
     *
     * @param view
     * @return
     */
    public static final ClickViewSimulate obtain(View view) {
        ClickViewSimulate simulate = new ClickViewSimulate(view);
        return simulate;
    }
}
