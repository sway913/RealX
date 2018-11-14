package com.yy.sumulate;

import android.app.Activity;
import android.graphics.Point;

public class MoveDistanceSimulate extends SequenceSimulate {
    private final Point from;
    private final Point to;

    /**
     * 构造函数
     *
     * @param base
     */
    public MoveDistanceSimulate(Activity base, Point from, Point to) {
        super(base);
        this.from = from;
        this.to = to;
        if (null == from || null == to) {
            throw new IllegalArgumentException("form or to cannot be null.");
        }
        addSimulate(DownEventSimulate.obtain(base, from))
                .addSmoothMoveSimulate(from, to)
                .addSimulate(UpEventSimulate.obtain(base, to));
    }

    /**
     * 静态创建方法
     *
     * @param base
     * @param x
     * @param y
     * @param dx
     * @param dy
     * @return
     */
    public static MoveDistanceSimulate obtain(Activity base, int x, int y, int dx, int dy) {
        Point from = new Point(x, y);
        Point to = new Point(dx, dx);
        return obtain(base, from, to);
    }

    /**
     * 静态创建方法
     *
     * @param base
     * @param from
     * @param to
     * @return
     */
    public static MoveDistanceSimulate obtain(Activity base, Point from, Point to) {
        MoveDistanceSimulate simulate = new MoveDistanceSimulate(base, from, to);
        return simulate;
    }
}
