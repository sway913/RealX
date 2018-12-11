package com.yy.sumulate;

import android.graphics.Point;
import android.util.Log;

public class ClickPointSimulate extends SequenceSimulate {
    private static final String TAG = ClickPointSimulate.class.getSimpleName();
    protected final Point point;

    /**
     * 构造函数
     *
     * @param point
     */
    public ClickPointSimulate(Point point) {
        this.point = point;
        if (null == point) {
            throw new IllegalArgumentException("point cannot be null.");
        }
        addSimulate(CancelEventSimulate.obtain(point))
                .addSimulate(DownEventSimulate.obtain(point))
                .addSimulate(UpEventSimulate.obtain(point))
                .addSimulate(CancelEventSimulate.obtain(point));
    }

    /**
     * 静态创建方式
     *
     * @param x
     * @param y
     * @return
     */
    public static ClickPointSimulate obtain(int x, int y) {
        Log.d(TAG, String.format("ClickPointSimulate.obtain():%d, %d", x, y));
        Point point = new Point(x, y);
        return obtain(point);
    }

    /**
     * 静态创建方法
     *
     * @param point
     * @return
     */
    public static ClickPointSimulate obtain(Point point) {
        ClickPointSimulate simulate = new ClickPointSimulate(point);
        return simulate;
    }
}
