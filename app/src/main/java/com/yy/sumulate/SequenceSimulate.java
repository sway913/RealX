package com.yy.sumulate;

import android.app.Activity;
import android.graphics.Point;
import android.util.Log;
import android.view.InputEvent;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SequenceSimulate extends InputEventSimulate {
    private final static String TAG = SequenceSimulate.class.getSimpleName();
    private final List<InputEventSimulate> group = new ArrayList<>();

    /**
     * 构造函数
     *
     * @param base
     */
    public SequenceSimulate(Activity base) {
        super(base);
    }

    private int index = 0;
    private AtomicInteger _seq = new AtomicInteger(0);

    /**
     * 添加一个模拟点击
     *
     * @param task
     */
    public synchronized SequenceSimulate addSimulate(InputEventSimulate task) {
        if (null == task) {
            return this;
        }
        group.add(task);
        return this;
    }

    /**
     * 产生平滑move事件
     *
     * @param from
     * @param to
     */
    public SequenceSimulate addSmoothMoveSimulate(Point from, Point to) {
        int slop = ViewConfiguration.get(base).getScaledTouchSlop();
        int limit = Math.max(Math.max(Math.abs(to.x - from.x) / slop, Math.abs(to.y - from.y) / slop), 1);
        int dx = (to.x - from.x) / limit;
        int dy = (to.y - from.y) / limit;
        MoveEventSimulate move = null;
        for (int i = 0; i <= limit; i++) {
            move = MoveEventSimulate.obtain(base, from.x + dx * i, from.y + dy * i);
            addSimulate(move);
        }
        if (null != move && !move.point.equals(to)) {
            addSimulate(MoveEventSimulate.obtain(base, to));
        }
        return this;
    }

    /**
     * 获取一个任务
     *
     * @return
     */
    private synchronized InputEvent retrieveInputEvent() {
        Log.d(TAG, String.format("retrieveInputEvent():%d, %d, %d", index, _seq.get(), hashCode()));
        if (index < 0 || index >= group.size()) {
            return null;
        }
        InputEvent event = group.get(index).createInputEvent(_seq.get());
        if (null == event) {
            index = index + 1;
            _seq.set(0);
            return retrieveInputEvent();
        } else {
            _seq.incrementAndGet();
            return event;
        }
    }

    @Override
    InputEvent createInputEvent(int seq) {
        if (group.isEmpty()) {
            return null;
        }
        return retrieveInputEvent();
    }
}
