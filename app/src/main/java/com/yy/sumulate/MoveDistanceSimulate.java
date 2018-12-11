package com.yy.sumulate;

import android.graphics.Point;

public class MoveDistanceSimulate extends SequenceSimulate {
    private final Point from;
    private final Point to;

    /**
     * 构造函数
     */
    public MoveDistanceSimulate(Point from, Point to) {
        this.from = from;
        this.to = to;
        if (null == from || null == to) {
            throw new IllegalArgumentException("form or to cannot be null.");
        }
        addSimulate(DownEventSimulate.obtain(from))
                .addSmoothMoveSimulate(from, to)
                .addSimulate(UpEventSimulate.obtain(to));
    }

    /**
     * 静态创建方法
     *
     * @param x
     * @param y
     * @param dx
     * @param dy
     * @return
     */
    public static MoveDistanceSimulate obtain(int x, int y, int dx, int dy) {
        Point from = new Point(x, y);
        Point to = new Point(dx, dx);
        return obtain(from, to);
    }

    /**
     * 静态创建方法
     *
     * @param from
     * @param to
     * @return
     */
    public static MoveDistanceSimulate obtain(Point from, Point to) {
        MoveDistanceSimulate simulate = new MoveDistanceSimulate(from, to);
        return simulate;
    }
}
