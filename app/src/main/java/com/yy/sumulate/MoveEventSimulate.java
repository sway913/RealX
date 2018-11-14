package com.yy.sumulate;

import android.app.Activity;
import android.graphics.Point;
import android.view.InputEvent;
import android.view.MotionEvent;

public class MoveEventSimulate extends InputEventSimulate {
    protected final Point point;

    /**
     * 构造函数
     *
     * @param base
     */
    public MoveEventSimulate(Activity base, Point point) {
        super(base);
        this.point = point;
        if (null == point) {
            throw new IllegalArgumentException("point cannot be null.");
        }
    }

    @Override
    InputEvent createInputEvent(int seq) {
        if (seq == 0) {
            return obtain(MotionEvent.ACTION_DOWN, point.x, point.y);
        }
        return null;
    }

    /**
     * 静态创建方式
     *
     * @param base
     * @param x
     * @param y
     * @return
     */
    public static MoveEventSimulate obtain(Activity base, int x, int y) {
        Point point = new Point(x, y);
        return obtain(base, point);
    }

    /**
     * 静态创建方法
     *
     * @param base
     * @param point
     * @return
     */
    public static final MoveEventSimulate obtain(Activity base, Point point) {
        MoveEventSimulate simulate = new MoveEventSimulate(base, point);
        return simulate;
    }
}
