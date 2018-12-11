package com.yy.sumulate;

import android.graphics.Point;
import android.view.ViewConfiguration;

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
        addSimulate(DownEventSimulate.obtain(from));
        addSmoothMoveSimulate(from, to);
        addSimulate(UpEventSimulate.obtain(to));
    }

    /**
     * 产生平滑move事件
     *
     * @param from
     * @param to
     */
    public SequenceSimulate addSmoothMoveSimulate(Point from, Point to) {
        int slop = Math.max(ViewConfiguration.getTouchSlop(), 8);
        int limit = Math.max(Math.max(Math.abs(to.x - from.x) / slop, Math.abs(to.y - from.y) / slop), 1);
        int dx = (to.x - from.x) / limit;
        int dy = (to.y - from.y) / limit;
        MoveEventSimulate move = null;
        for (int i = 0; i <= limit; i++) {
            move = MoveEventSimulate.obtain(from.x + dx * i, from.y + dy * i);
            addSimulate(move);
        }
        if (null != move && !move.point.equals(to)) {
            addSimulate(MoveEventSimulate.obtain(to));
        }
        return this;
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
