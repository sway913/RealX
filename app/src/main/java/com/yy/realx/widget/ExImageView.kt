package com.yy.realx.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.util.concurrent.atomic.AtomicBoolean


class ExImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    RImageView(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = ExImageView::class.java.simpleName
        const val MIN_SCALE: Float = 0.1F
        const val MAX_SCALE: Float = 10F
    }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        //约束图片缩放模式
        scaleType = ScaleType.MATRIX
        //初始化变量
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.color = Color.RED
        paint.strokeWidth = 10f
    }

    private val values = mutableListOf<Float>()

    /**
     * 标记点坐标信息
     */
    fun setValues(values: List<Float>) {
        Log.d(TAG, "setValues():${values.size}")
        this.values.clear()
        this.values.addAll(values)
        post {
            parseCoordinates()
            invalidate()
        }
    }

    private var INIT_SCALE = MIN_SCALE

    /**
     * 设定最小缩放
     */
    fun setMinScale(min: Float) {
        if (min <= 0) {
            return
        }
        INIT_SCALE = min
        restrictScale(min, 0F, 0F)
    }

    /**
     * 返回修正后的点
     */
    fun getValues(): MutableList<Float> {
        return values
    }

    private val coordinates = mutableListOf<Float>()
    private val points = mutableListOf<PointF>()

    /**
     * 拆解坐标数据
     */
    private fun parseCoordinates() {
        Log.d(TAG, "parseCoordinates():${values.size}")
        if (values.isEmpty()) {
            return
        }
        Log.d(TAG, "parseCoordinates():$width, $height")
        if (width <= 0 || height <= 0) {
            return
        }
        if (coordinates.size != values.size) {
            coordinates.clear()
            coordinates.addAll(values)
        }
        points.clear()
        Log.d(TAG, "parseCoordinates():${coordinates.size}")
        val scale = getNowScale()
        val translate = getNowTranslate()
        var point = PointF()
        values.mapIndexed { index, value ->
            if (index % 2 == 0) {
                point = PointF()
                coordinates[index] = value * width * scale / INIT_SCALE + translate.x
                point.x = coordinates[index]
            } else {
                coordinates[index] = value * height * scale / INIT_SCALE + translate.y
                point.y = coordinates[index]
                points.add(point)
            }
        }
    }

    /**
     * 重新设定值
     */
    private fun remarkBackward() {
        Log.d(TAG, "remarkBackward()")
        val scale = getNowScale()
        val translate = getNowTranslate()
        var point: PointF
        coordinates.mapIndexed { index, value ->
            point = points[index / 2]
            if (index % 2 == 0) {
                values[index] = (value - translate.x) * INIT_SCALE / (scale * width)
                point.x = value
            } else {
                values[index] = (value - translate.y) * INIT_SCALE / (scale * height)
                point.y = value
            }
        }
        remark.set(false)
    }

    override fun setScaleType(scaleType: ScaleType?) {
        if (scaleType != ScaleType.MATRIX) {
            throw IllegalArgumentException("Only support scaleType ScaleType.MATRIX.")
        }
        super.setScaleType(scaleType)
    }

    private val _matrix = Matrix()

    /**
     * 返回当前缩放大小
     */
    private fun getNowScale(): Float {
        Log.d(TAG, "getNowScale()")
        val values = FloatArray(9)
        _matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    /**
     * 返回当前移动位置
     */
    private fun getNowTranslate(): PointF {
        Log.d(TAG, "getNowTranslate()")
        val values = FloatArray(9)
        _matrix.getValues(values)
        return PointF(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y])
    }

    private val single: GestureDetector = GestureDetector(context, object :
        GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent?): Boolean {
            Log.d(TAG, "onDoubleTap()")
            if (null != event) {
                restrictScale(getNowScale() * 1.5f, event.x, event.y)
            }
            return true
        }

        override fun onScroll(first: MotionEvent?, second: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            Log.d(TAG, "onScroll():$distanceX, $distanceY")
            if (first?.pointerCount ?: 0 > 1 || second?.pointerCount ?: 0 > 1) {
                return false
            }
            Log.d(TAG, "onScroll():action = ${first?.action}, ${second?.action}, ${remark.get()}")
            if (!translate.get() && markCoordinate(first, second, distanceX, distanceY)) {
                this@ExImageView.postInvalidate()
            } else if (!remark.get()) {
                restrictTranslate(-distanceX, -distanceY)
            }
            return true
        }
    })

    private val remark = AtomicBoolean(false)

    /**
     * 标记是否触摸点是检测点
     */
    private fun markCoordinate(
        first: MotionEvent?,
        second: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        Log.d(TAG, "markCoordinate():${first?.x}, ${first?.y}, ${second?.x}, ${second?.y}")
        if (null == first || null == second) {
            return false
        }
        if (coordinates.isNullOrEmpty()) {
            return false
        }
        val scale = getNowScale()
        Log.d(TAG, "markCoordinate():${coordinates[0]}, ${coordinates[1]}")
        val x = first.x
        val y = first.y
        var index = -1
        points.mapIndexed { idx, point ->
            if (Math.abs(point.x - x) < 10 * scale && Math.abs(point.y - y) < 10 * scale) {
                index = idx
                return@mapIndexed
            }
        }
        Log.d(TAG, "markCoordinate():${points.size}, $index")
        if (index < 0 || index > points.size) {
            return false
        }
        remark.set(true)
        coordinates[index * 2] -= distanceX
        coordinates[index * 2 + 1] -= distanceY
        return true
    }

    /**
     * 获取图片显示的rect
     */
    private fun getMatrixRect(): RectF {
        Log.d(TAG, "getMatrixRect()")
        val rect = RectF()
        val d = drawable
        if (d != null) {
            rect.set(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
            _matrix.mapRect(rect)
        }
        return rect
    }

    private val translate = AtomicBoolean(false)

    /**
     * 左右滑动
     */
    private fun restrictTranslate(x: Float, y: Float) {
        Log.d(TAG, "restrictTranslate()")
        _matrix.postTranslate(x, y)
        restrictMatrixBound()
        imageMatrix = _matrix
        //重新标记点
        translate.set(true)
        coordinates.clear()
    }

    private val multiple = ScaleGestureDetector(context, object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            Log.d(TAG, "onScale():${detector?.focusX}, ${detector?.focusY}")
            if (null != detector) {
                val translate = getNowTranslate()
                val scale = getNowScale()
                Log.d(TAG, "onScale():${translate.x}, ${translate.y}, $scale")
                val x = (detector.focusX - translate.x) / scale
                val y = (detector.focusY - translate.y) / scale
                Log.d(TAG, "onScale():$x, $y")
                restrictScale(scale * detector.scaleFactor, x, y)
            }
            return true
        }
    })

    /**
     * 约束缩放比例
     */
    private fun restrictScale(s: Float, x: Float, y: Float) {
        val scale = Math.max(Math.min(s, MAX_SCALE), INIT_SCALE)
        Log.d(TAG, "restrictScale():$s -> $scale")
        _matrix.setScale(scale, scale, x, y)
        restrictMatrixBound()
        imageMatrix = _matrix
        //重新标记点
        coordinates.clear()
    }

    /**
     * 约束试图边界
     */
    private fun restrictMatrixBound() {
        val rect = getMatrixRect()
        var deltaX = 0f
        var deltaY = 0f
        if (rect.top > 0) {
            deltaY = -rect.top
        }
        if (rect.bottom < height) {
            deltaY = height - rect.bottom
        }
        if (rect.left > 0) {
            deltaX = -rect.left
        }
        if (rect.right < width) {
            deltaX = width - rect.right
        }
        _matrix.postTranslate(deltaX, deltaY)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (null == event) {
            return false
        }
        Log.d(TAG, "onTouchEvent():action = ${event.action}")
        val action = event.action
        if (action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_CANCEL
        ) {
            Log.d(TAG, "onTouchEvent():${remark.get()}, ${translate.get()}")
            if (remark.get()) {
                remarkBackward()
            }
            if (translate.get()) {
                translate.set(false)
            }
        }
        Log.d(TAG, "onTouchEvent():${event.pointerCount}")
        if (single.onTouchEvent(event)) return true
        multiple.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw():${coordinates.isEmpty()}, ${values.isNotEmpty()}")
        if (coordinates.isNotEmpty()) {
            canvas.save()
            paint.strokeWidth = 10 * getNowScale()
            canvas.drawPoints(coordinates.toFloatArray(), paint)
            canvas.restore()
        } else if (values.isNotEmpty()) {
            post {
                parseCoordinates()
                invalidate()
            }
        }
    }
}
