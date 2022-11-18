package com.voidcom.videoproject.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.voidcom.v_base.utils.TimeUtils
import com.voidcom.v_base.utils.dp2px
import com.voidcom.v_base.utils.getColorValue
import com.voidcom.v_base.utils.parcelable
import com.voidcom.videoproject.R
import com.voidcom.videoproject.ui.VideoCutActivity
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 *
 * @Description: 视频帧画面预览
 * @Author: Void
 * @CreateDate: 2022/11/13 21:09
 * @UpdateDate: 2022/11/13 21:09
 */
class RangeSeekBarView : View {
    private val TAG = RangeSeekBarView::class.java.simpleName
    private var mActivePointerId: Int = INVALID_POINTER_ID
    private var mMinShootTime = 3L * 1000//最小剪辑3s，默认
    private val absoluteMinValuePrim: Double = 0.0
    private val absoluteMaxValuePrim: Double = VideoCutActivity.MAX_TIME * 1000.0
    private var normalizedMinValue = 0.0 //点坐标占总长度的比例值，范围从0-1
    private var normalizedMaxValue = 1.0 //点坐标占总长度的比例值，范围从0-1
    private var normalizedMinValueTime = 0.0
    private var normalizedMaxValueTime = 1.0 // normalized：规格化的--点坐标占总长度的比例值，范围从0-1
    private val mScaledTouchSlop = 0
    private var thumbImageLeft: Bitmap
    private var thumbImageRight: Bitmap
    private var thumbPressedImage: Bitmap
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mVideoTrimTimePaintL = Paint()
    private val mVideoTrimTimePaintR = Paint()
    private val mShadow: Paint = Paint()
    private var thumbWidth = 0f
    private var thumbHalfWidth = 0f
    private val padding = 0f
    private var mStartPosition: Long = 0
    private var mEndPosition: Long = 0
    private val thumbPaddingTop = 0f
    private var isTouchDown = false
    private var mDownMotionX = 0f
    private var mIsDragging = false
    private lateinit var pressedThumb: Thumb
    private var isMin = false
    private var minWidth = 1.0 //最小裁剪距离
    private val borderValue = dp2px(2f) //边框大小

    /**
     * 供外部activity调用，控制是都在拖动的时候打印log信息，默认是false不打印
     */
    var isNotifyWhileDragging = false
    private var mRangeSeekBarChangeListener: OnRangeSeekBarChangeListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, def: Int) : super(context, attrs, def){
        isFocusable = true
        isFocusableInTouchMode = true
//        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        thumbImageLeft = BitmapFactory.decodeResource(resources, R.mipmap.ic_video_thumb_handle)
        val width: Int = thumbImageLeft.width
        val height: Int = thumbImageLeft.height
        val newWidth = dp2px(12.5f)
        val newHeight = dp2px(50f)
        val scaleWidth = newWidth * 1.0f / width
        val scaleHeight = newHeight * 1.0f / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, width, height, matrix, true)
        thumbImageRight = thumbImageLeft
        thumbPressedImage = thumbImageLeft
        thumbWidth = newWidth
        thumbHalfWidth = thumbWidth / 2f
        mShadow.isAntiAlias = true
        mShadow.color = resources.getColorValue(R.color.shadow_color)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = resources.getColorValue(R.color.white)
        mVideoTrimTimePaintL.strokeWidth = 3f
        mVideoTrimTimePaintL.setARGB(255, 51, 51, 51)
        mVideoTrimTimePaintL.textSize = 28f
        mVideoTrimTimePaintL.isAntiAlias = true
        mVideoTrimTimePaintL.color = resources.getColorValue(R.color.white)
        mVideoTrimTimePaintL.textAlign = Paint.Align.LEFT
        mVideoTrimTimePaintR.strokeWidth = 3f
        mVideoTrimTimePaintR.setARGB(255, 51, 51, 51)
        mVideoTrimTimePaintR.textSize = 28f
        mVideoTrimTimePaintR.isAntiAlias = true
        mVideoTrimTimePaintR.color = resources.getColorValue(R.color.white)
        mVideoTrimTimePaintR.textAlign = Paint.Align.RIGHT
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 300
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec)
        }
        var height = 120
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rangeL = normalizedToScreen(normalizedMinValue)
        val rangeR = normalizedToScreen(normalizedMaxValue)
        val leftRect = Rect(0, height, rangeL.toInt(), 0)
        val rightRect = Rect(rangeR.toInt(), height, width - paddingRight, 0)
        canvas.drawRect(leftRect, mShadow)
        canvas.drawRect(rightRect, mShadow)

        //上边框
        canvas.drawRect(
            rangeL + thumbHalfWidth,
            thumbPaddingTop + mPaddingTop,
            rangeR - thumbHalfWidth,
            thumbPaddingTop + borderValue + mPaddingTop,
            rectPaint
        )

        //下边框
        canvas.drawRect(
            rangeL + thumbHalfWidth, height - borderValue, rangeR - thumbHalfWidth,
            height.toFloat(), rectPaint
        )

        //画左边thumb
        drawThumb(normalizedToScreen(normalizedMinValue), false, canvas, true)

        //画右thumb
        drawThumb(normalizedToScreen(normalizedMaxValue), false, canvas, false)

        //绘制文字
        drawVideoTrimTimeText(canvas)
    }

    private fun drawThumb(screenCoord: Float, pressed: Boolean, canvas: Canvas, isLeft: Boolean) {
        canvas.drawBitmap(
            if (pressed) thumbPressedImage else if (isLeft) thumbImageLeft else thumbImageRight,
            screenCoord - if (isLeft) 0f else thumbWidth,
            mPaddingTop,
            paint
        )
    }

    private fun drawVideoTrimTimeText(canvas: Canvas) {
        val leftThumbsTime: String = TimeUtils.formatTimeS(mStartPosition)
        val rightThumbsTime: String = TimeUtils.formatTimeS(mEndPosition)
        canvas.drawText(
            leftThumbsTime,
            normalizedToScreen(normalizedMinValue),
            TextPositionY,
            mVideoTrimTimePaintL
        )
        canvas.drawText(
            rightThumbsTime,
            normalizedToScreen(normalizedMaxValue),
            TextPositionY,
            mVideoTrimTimePaintR
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTouchDown || event.pointerCount > 1) return super.onTouchEvent(event)
        if (!isEnabled) return false
        if (absoluteMaxValuePrim <= mMinShootTime) return super.onTouchEvent(event)
        val pointerIndex: Int // 记录点击点的index
        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
                mActivePointerId = event.getPointerId(event.pointerCount - 1)
                pointerIndex = event.findPointerIndex(mActivePointerId)
                mDownMotionX = event.getX(pointerIndex)
                // 判断touch到的是最大值thumb还是最小值thumb
                pressedThumb = evalPressedThumb(mDownMotionX)
                if (pressedThumb == Thumb.UNKNOWN) return super.onTouchEvent(event)
                isPressed = true // 设置该控件被按下了
                onStartTrackingStatus(true) // 置mIsDragging为true，开始追踪touch事件
                trackTouchEvent(event)
                attemptClaimDrag()
                mRangeSeekBarChangeListener?.onRangeSeekBarValuesChanged(
                    this,
                    selectedMinValue,
                    selectedMaxValue, MotionEvent.ACTION_DOWN, isMin, pressedThumb
                )
            }
            MotionEvent.ACTION_MOVE -> if (pressedThumb != Thumb.UNKNOWN) {
                if (mIsDragging) {
                    trackTouchEvent(event)
                } else {
                    // Scroll to follow the motion event
                    pointerIndex = event.findPointerIndex(mActivePointerId)
                    val x = event.getX(pointerIndex) // 手指在控件上点的X坐标
                    // 手指没有点在最大最小值上，并且在控件上有滑动事件
                    if (abs(x - mDownMotionX) > mScaledTouchSlop) {
                        isPressed = true
                        Log.e(TAG, "没有拖住最大最小值") // 一直不会执行？
                        invalidate()
                        onStartTrackingStatus(true)
                        trackTouchEvent(event)
                        attemptClaimDrag()
                    }
                }
                if (isNotifyWhileDragging) {
                    mRangeSeekBarChangeListener?.onRangeSeekBarValuesChanged(
                        this,
                        selectedMinValue,
                        selectedMaxValue, MotionEvent.ACTION_MOVE, isMin, pressedThumb
                    )
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsDragging) {
                    trackTouchEvent(event)
                    onStartTrackingStatus(false)
                    isPressed = false
                } else {
                    onStartTrackingStatus(true)
                    trackTouchEvent(event)
                    onStartTrackingStatus(false)
                }
                invalidate()
                mRangeSeekBarChangeListener?.onRangeSeekBarValuesChanged(
                    this,
                    selectedMinValue,
                    selectedMaxValue, MotionEvent.ACTION_UP, isMin,
                    pressedThumb
                )
                pressedThumb = Thumb.UNKNOWN // 手指抬起，则置被touch到的thumb为UNKNOWN
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.pointerCount - 1
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index)
                mActivePointerId = event.getPointerId(index)
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mIsDragging) {
                    onStartTrackingStatus(false)
                    isPressed = false
                }
                invalidate() // see above explanation
            }
            else -> return true
        }
        return true
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex =
            ev.action and ACTION_POINTER_INDEX_MASK shr ACTION_POINTER_INDEX_SHIFT
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mDownMotionX = ev.getX(newPointerIndex)
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    private fun trackTouchEvent(event: MotionEvent) {
        if (event.pointerCount > 1) return
        Log.e(TAG, "trackTouchEvent: " + event.action + " x: " + event.x)
        val pointerIndex = event.findPointerIndex(mActivePointerId) // 得到按下点的index
        val x = try {
            event.getX(pointerIndex)
        } catch (e: Exception) {
            return
        }
        if (Thumb.MIN == pressedThumb) {
            // screenToNormalized(x)-->得到规格化的0-1的值
            setNormalizedMinValue(screenToNormalized(x, 0))
        } else if (Thumb.MAX == pressedThumb) {
            setNormalizedMaxValue(screenToNormalized(x, 1))
        }
    }

    private fun screenToNormalized(screenCoord: Float, position: Int): Double {
        val width = width
        return if (width <= 2 * padding) {  // prevent division by zero, simply return 0.
            0.0
        } else {
            isMin = false
            var currentWidth = screenCoord.toDouble()
            val rangeL = normalizedToScreen(normalizedMinValue)
            val rangeR = normalizedToScreen(normalizedMaxValue)
            val min =
                mMinShootTime / (absoluteMaxValuePrim - absoluteMinValuePrim) * (width - thumbWidth * 2)
            minWidth = if (absoluteMaxValuePrim > 5 * 60 * 1000) { //大于5分钟的精确小数四位
                val df = DecimalFormat("0.0000")
                df.format(min).toDouble()
            } else {
                (min + 0.5).roundToInt().toDouble()
            }
            if (position == 0) {
                if (isInThumbRangeLeft(screenCoord, normalizedMinValue, 0.5)) {
                    return normalizedMinValue
                }
                val rightPosition: Float = if (getWidth() - rangeR >= 0) getWidth() - rangeR else 0f
                val leftLength = getValueLength() - (rightPosition + minWidth)
                if (currentWidth > rangeL) {
                    currentWidth = rangeL + (currentWidth - rangeL)
                } else if (currentWidth <= rangeL) {
                    currentWidth = rangeL - (rangeL - currentWidth)
                }
                if (currentWidth > leftLength) {
                    isMin = true
                    currentWidth = leftLength
                }
                if (currentWidth < thumbWidth * 2 / 3) {
                    currentWidth = 0.0
                }
                val resultTime = (currentWidth - padding) / (width - 2 * thumbWidth)
                normalizedMinValueTime = min(1.0, max(0.0, resultTime))
                val result = (currentWidth - padding) / (width - 2 * padding)
                min(1.0, max(0.0, result)) // 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            } else {
                if (isInThumbRange(screenCoord, normalizedMaxValue, 0.5)) {
                    return normalizedMaxValue
                }
                val rightLength = getValueLength() - (rangeL + minWidth)
                if (currentWidth > rangeR) {
                    currentWidth = rangeR + (currentWidth - rangeR)
                } else if (currentWidth <= rangeR) {
                    currentWidth = rangeR - (rangeR - currentWidth)
                }
                var paddingRight = getWidth() - currentWidth
                if (paddingRight > rightLength) {
                    isMin = true
                    currentWidth = getWidth() - rightLength
                    paddingRight = rightLength
                }
                if (paddingRight < thumbWidth * 2 / 3) {
                    currentWidth = getWidth().toDouble()
                    paddingRight = 0.0
                }
                var resultTime = (paddingRight - padding) / (width - 2 * thumbWidth)
                resultTime = 1 - resultTime
                normalizedMaxValueTime = min(1.0, max(0.0, resultTime))
                val result = (currentWidth - padding) / (width - 2 * padding)
                min(1.0, max(0.0, result)) // 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            }
        }
    }

    private fun getValueLength(): Float = width - 2 * thumbWidth

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private fun evalPressedThumb(touchX: Float): Thumb {
        val minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 2.0) // 触摸点是否在最小值图片范围内
        val maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 2.0)
        return if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            if (touchX / width > 0.5f) Thumb.MIN else Thumb.MAX
        } else if (minThumbPressed) {
            Thumb.MIN
        } else if (maxThumbPressed) {
            Thumb.MAX
        } else {
            Thumb.UNKNOWN
        }
    }

    /**
     *  当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
     *  即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
     */
    private fun isInThumbRange(
        touchX: Float,
        normalizedThumbValue: Double,
        scale: Double
    ): Boolean = abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth * scale

    /**
     * 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
     * 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
     */
    private fun isInThumbRangeLeft(
        touchX: Float,
        normalizedThumbValue: Double,
        scale: Double
    ): Boolean =
        abs(touchX - normalizedToScreen(normalizedThumbValue) - thumbWidth) <= thumbHalfWidth * scale

    /**
     * 试图告诉父view不要拦截子控件的drag
     */
    private fun attemptClaimDrag() {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
    }

    fun onStartTrackingStatus(status: Boolean) {
        mIsDragging = status
    }

    fun setMinShootTime(min_cut_time: Long) {
        mMinShootTime = min_cut_time
    }

    private fun normalizedToScreen(normalizedCoord: Double): Float {
        return (paddingLeft + normalizedCoord * (width - paddingLeft - paddingRight)).toFloat()
    }

    private fun valueToNormalized(value: Long): Double {
        return if (0.0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            0.0
        } else (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim)
    }

    fun setStartEndTime(start: Long, end: Long) {
        mStartPosition = start / 1000
        mEndPosition = end / 1000
    }

    fun setNormalizedMinValue(value: Double) {
        normalizedMinValue = max(0.0, min(1.0, min(value, normalizedMaxValue)))
        invalidate() // 重新绘制此view
    }

    fun setNormalizedMaxValue(value: Double) {
        normalizedMaxValue = max(0.0, min(1.0, max(value, normalizedMinValue)))
        invalidate() // 重新绘制此view
    }

    var selectedMinValue: Long
        get() = normalizedToValue(normalizedMinValueTime)
        set(value) {
            if (0.0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
                setNormalizedMinValue(0.0)
            } else {
                setNormalizedMinValue(valueToNormalized(value))
            }
        }
    var selectedMaxValue: Long
        get() = normalizedToValue(normalizedMaxValueTime)
        set(value) {
            if (0.0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
                setNormalizedMaxValue(1.0)
            } else {
                setNormalizedMaxValue(valueToNormalized(value))
            }
        }

    private fun normalizedToValue(normalized: Double): Long {
        return (absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim)).toLong()
    }

    fun setTouchDown(touchDown: Boolean) {
        isTouchDown = touchDown
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable("SUPER", super.onSaveInstanceState())
        putDouble("MIN", normalizedMinValue)
        putDouble("MAX", normalizedMaxValue)
        putDouble("MIN_TIME", normalizedMinValueTime)
        putDouble("MAX_TIME", normalizedMaxValueTime)
    }

    override fun onRestoreInstanceState(parcel: Parcelable) {
        if (parcel !is Bundle) return
        parcel.apply {
            super.onRestoreInstanceState(parcelable("SUPER"))
            normalizedMinValue = getDouble("MIN")
            normalizedMaxValue = getDouble("MAX")
            normalizedMinValueTime = getDouble("MIN_TIME")
            normalizedMaxValueTime = getDouble("MAX_TIME")
        }
    }

    interface OnRangeSeekBarChangeListener {
        fun onRangeSeekBarValuesChanged(
            bar: RangeSeekBarView,
            minValue: Long,
            maxValue: Long,
            action: Int,
            isMin: Boolean,
            pressedThumb: Thumb
        )
    }

    fun setOnRangeSeekBarChangeListener(listener: OnRangeSeekBarChangeListener?) {
        mRangeSeekBarChangeListener = listener
    }

    enum class Thumb {
        UNKNOWN, MIN, MAX
    }

    companion object {
        const val INVALID_POINTER_ID = 255
        const val ACTION_POINTER_INDEX_MASK = 0x0000ff00
        const val ACTION_POINTER_INDEX_SHIFT = 8
        val TextPositionY: Float = dp2px(7f)
        val mPaddingTop: Float = dp2px(10f)
    }
}