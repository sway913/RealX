package com.yy.realx.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.yy.realx.R

class SegmentBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = SegmentBar::class.java.simpleName
        private const val SEP_COLOR = Color.RED
        private const val SEG_COLOR = Color.GREEN
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val SEP_WIDTH: Int

    init {
        if (null != attrs) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.SegmentBar, defStyleAttr, 0)
            SEP_WIDTH = array.getDimensionPixelOffset(R.styleable.SegmentBar_sep_width, 8)
            array.recycle()
        } else {
            SEP_WIDTH = 8
        }
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = SEP_WIDTH.toFloat()
    }

    private val durations = mutableListOf<Int>()

    /**
     * 分段时长设置进来
     */
    fun setSegments(list: List<Int>) {
        durations.clear()
        durations.addAll(list)
        post {
            segments.clear()
            prepareSegments()
            invalidate()
        }
    }

    private val segments = mutableListOf<Pair<Int, Int>>()

    /**
     * 计算分段数据
     */
    private fun prepareSegments() {
        Log.d(TAG, "prepareSegments():${durations.size}")
        if (durations.isNullOrEmpty()) {
            return
        }
        Log.d(TAG, "prepareSegments():$width, $height")
        if (width <= 0 || height <= 0) {
            return
        }
        paint.strokeWidth = height.toFloat()
        //计算分段长度
        val total = durations.sum()
        Log.d(TAG, "prepareSegments():$total")
        val sep = durations.size - 1
        val rest = width - sep * SEP_WIDTH
        Log.d(TAG, "prepareSegments():$rest")
        durations.forEach {
            val w = rest * it / total
            Log.d(TAG, "prepareSegments():$w")
            segments.add(Pair(w, SEG_COLOR))
            segments.add(Pair(SEP_WIDTH, SEP_COLOR))
        }
        Log.d(TAG, "prepareSegments():${segments.size}")
        segments.removeAt(segments.size - 1)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segments.isNotEmpty()) {
            canvas.save()
            var x = 0f
            segments.forEach {
                Log.d(TAG, "onDraw():$x")
                paint.color = it.second
                canvas.drawLine(x, 0f, x + it.first, 0f, paint)
                x += it.first
            }
            canvas.restore()
        } else if (segments.isNotEmpty()) {
            post {
                segments.clear()
                prepareSegments()
                invalidate()
            }
        }
    }
}
