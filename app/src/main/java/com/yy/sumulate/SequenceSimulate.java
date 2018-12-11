package com.yy.sumulate;

import android.util.Log;
import android.view.InputEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SequenceSimulate extends InputEventSimulate {
    private final static String TAG = SequenceSimulate.class.getSimpleName();
    private final List<InputEventSimulate> group = new ArrayList<>();
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
