package com.yy.sumulate;

import android.app.Activity;
import android.graphics.Point;
import android.util.Log;

public class ClickPointSimulate extends SequenceSimulate {
    private static final String TAG = ClickPointSimulate.class.getSimpleName();
    protected final Point point;

    /**
     * 构造函数
     *
     * @param base
     * @param point
     */
    public ClickPointSimulate(Activity base, Point point) {
        super(base);
        this.point = point;
        if (null == point) {
            throw new IllegalArgumentException("point cannot be null.");
        }
        addSimulate(CancelEventSimulate.obtain(base, point))
                .addSimulate(DownEventSimulate.obtain(base, point))
                .addSimulate(UpEventSimulate.obtain(base, point))
                .addSimulate(CancelEventSimulate.obtain(base, point));
    }

    /**
     * 静态创建方式
     *
     * @param base
     * @param x
     * @param y
     * @return
     */
    public static ClickPointSimulate obtain(Activity base, int x, int y) {
        Log.d(TAG, String.format("ClickPointSimulate.obtain():%d, %d", x, y));
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
    public static ClickPointSimulate obtain(Activity base, Point point) {
        ClickPointSimulate simulate = new ClickPointSimulate(base, point);
        return simulate;
    }
}
