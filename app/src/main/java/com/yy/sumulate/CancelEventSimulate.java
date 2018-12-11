package com.yy.sumulate;

import android.graphics.Point;
import android.view.InputEvent;
import android.view.MotionEvent;

public class CancelEventSimulate extends InputEventSimulate {
    private final Point point;

    /**
     * 构造函数
     */
    public CancelEventSimulate(Point point) {
        this.point = point;
        if (null == point) {
            throw new IllegalArgumentException("point cannot be null.");
        }
    }

    @Override
    InputEvent createInputEvent(int seq) {
        if (seq == 0) {
            return obtain(MotionEvent.ACTION_CANCEL, point.x, point.y);
        }
        return null;
    }

    /**
     * 静态创建方式
     *
     * @param x
     * @param y
     * @return
     */
    public static CancelEventSimulate obtain(int x, int y) {
        Point point = new Point(x, y);
        return obtain(point);
    }

    /**
     * 静态创建方法
     *
     * @param point
     * @return
     */
    public static final CancelEventSimulate obtain(Point point) {
        CancelEventSimulate simulate = new CancelEventSimulate(point);
        return simulate;
    }
}
