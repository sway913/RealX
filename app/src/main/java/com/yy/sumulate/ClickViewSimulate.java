package com.yy.sumulate;

import android.app.Activity;
import android.util.Log;
import android.view.InputEvent;
import android.view.View;

public class ClickViewSimulate extends InputEventSimulate {
    private static final String TAG = ClickViewSimulate.class.getSimpleName();
    private final View target;
    private final ClickPointSimulate real;

    /**
     * 构造函数
     *
     * @param base
     */
    public ClickViewSimulate(Activity base, View view) {
        super(base);
        this.target = view;
        if (null == target) {
            throw new IllegalArgumentException("View target cannot be null.");
        }
        int x = (int) (target.getX() + target.getWidth() / 2);
        int y = (int) (target.getY() + target.getHeight() / 2);
        real = ClickPointSimulate.obtain(base, x, y);
    }

    @Override
    InputEvent createInputEvent(int seq) {
        Log.d(TAG, "createInputEvent():" + seq);
        return real.createInputEvent(seq);
    }

    /**
     * 静态创建方式
     *
     * @param base
     * @param view
     * @return
     */
    public static final ClickViewSimulate obtain(Activity base, View view) {
        ClickViewSimulate simulate = new ClickViewSimulate(base, view);
        return simulate;
    }
}
