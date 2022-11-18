package com.voidcom.videoproject.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.voidcom.v_base.utils.dp2px
import com.voidcom.v_base.utils.getColorValue
import com.voidcom.v_base.utils.getDrawableObj
import com.voidcom.videoproject.R
import kotlin.math.roundToInt


/**
 *
 * @Description: 双滑块Seekbar
 * @Author: Void
 * @CreateDate: 2022/11/17 15:18
 * @UpdateDate: 2022/11/17 15:18
 */
class PreviewSeekbar : View {
    private val TAG = PreviewSeekbar::class.java.simpleName
    private val filePathArrays = ArrayList<String>()
    private var leftIcon: Bitmap = getBitmap(R.drawable.ic_seekbar_icon)
    private var rightIcon: Bitmap = getBitmap(R.drawable.ic_seekbar_icon)
    private val clickIcon: Bitmap = getBitmap(R.drawable.ic_seekbar_click_icon)
    private var leftImg: Bitmap? = null
    private var rightImg: Bitmap? = null
    private var maxValue: Int = 100
    private var minValue: Int = 0
    private val bgHeight: Float = dp2px(2f) //设置背景下线条的高度
    private var leftValue: Float = minValue.toFloat()
    private var rightValue: Float = maxValue.toFloat()
    private var defaultWidth: Float = dp2px(100f)
    private var defaultHeight: Float = dp2px(100f)
    private val paddingLeft: Float = dp2px(16f)
    private val paddingRight: Float = dp2px(16f)
    private var defaultImgWidth = dp2px(50f)
    private var defaultImgHeight = dp2px(50f)
    private var viewWidth = 0
    private var viewHeight = 0
    private var clickedType: ClickIconType = ClickIconType.UNKNOWN
    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBg2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var listener: SeekbarChangeListener? = null

    //左按钮的坐标值
    private val indexLeft = FloatArray(2) { 0f }

    //右按钮的坐标值
    private val indexRight = FloatArray(2) { 0f }

    //左右预览图的坐标
    private val indexImage = FloatArray(4) { 0f }

    //左右按钮的开始x轴坐标值
    private var startLeftX = 0f
    private var startRightX = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, def: Int) : super(context, attrs, def) {
        paintBg.color = resources.getColorValue(R.color.red)
        paintBg2.color = resources.getColorValue(R.color.black)
//        leftImg = AppCompatImageView(context)
//        rightImg = AppCompatImageView(context)
//        leftImg.setBackgroundColor(Color.YELLOW)
//        rightImg.setBackgroundColor(Color.YELLOW)
//        val layoutParams = FrameLayout.LayoutParams(context, attrs).apply {
//            width = defaultImgWidth.toInt()
//            height = defaultImgHeight.toInt()
//            gravity = Gravity.CENTER
//        }
//        leftImg.layoutParams = layoutParams
//        rightImg.layoutParams = layoutParams
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    /**
     * 重写onMeasure方法，设置wrap_content 时需要默认大小
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(defaultWidth.toInt(), defaultHeight.toInt())
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(defaultWidth.toInt(), heightSpecSize)
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, defaultHeight.toInt())
        }
    }

    /**
     * 注意，在LinearLayout布局中，设置weight = 1时，onlayout 会多次回调，导致indexL(/R)X值一直为默认值，导致无法滑动
     * 可以将布局改成releativeLayout。
     *
     * @param changed
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewWidth = width
        viewHeight = height
        //左右按钮的初始值位置计算
        startLeftX = 0 + paddingLeft - leftIcon.width / 2
        startRightX = viewWidth - paddingRight - rightIcon.width / 2
        indexLeft[0] = startLeftX
        indexLeft[1] = viewHeight / 2 - bgHeight / 2 - leftIcon.height / 2
        indexRight[0] = startRightX
        indexRight[1] = viewHeight / 2 - bgHeight / 2 - rightIcon.height / 2
        leftValue = (maxValue - minValue) * (indexLeft[0] - startLeftX) / (startRightX - startLeftX)
        rightValue =
            (maxValue - minValue) * (indexRight[0] - startLeftX) / (startRightX - startLeftX)
        indexImage[0] = indexLeft[0] - defaultImgWidth / 2
        indexImage[1] = indexLeft[1] - defaultImgHeight
        indexImage[2] = indexRight[0] - defaultImgWidth / 2
        indexImage[3] = indexRight[1] - defaultImgHeight
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBg(canvas)
        drawClickIcon(canvas)
        drawClickIcon(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> if (isClickedIcon(event)) {
                postInvalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                clickedType = ClickIconType.UNKNOWN
                postInvalidate()
            }
            MotionEvent.ACTION_MOVE -> if (handleMoveEvent(event)) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getBitmap(drawableRes: Int): Bitmap {
        val drawable = resources.getDrawableObj(drawableRes)
        return drawable.toBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    }

    /**
     * 判断是否点击到按钮了
     *
     * @param event
     * @return
     */
    private fun isClickedIcon(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (x < indexLeft[0] + leftIcon.width && x > indexLeft[0] && y > indexLeft[1] && y < indexLeft[1] + leftIcon.height) {
            clickedType = ClickIconType.LEFT_ICON
            return true
        }
        if (x < indexRight[0] + rightIcon.width && x > indexRight[0] && y > indexRight[1] && y < indexRight[1] + rightIcon.height) {
            clickedType = ClickIconType.RIGHT_ICON
            return true
        }
        clickedType = ClickIconType.UNKNOWN
        return false
    }

    /**
     * 滑动事件处理
     *
     * @param motionEvent
     */
    private fun handleMoveEvent(motionEvent: MotionEvent): Boolean {
        val x = motionEvent.x
        if (clickedType == ClickIconType.LEFT_ICON) {
            if (x < indexRight[0] - leftIcon.width && x > startLeftX) { //左按钮的范围小于右按钮的位置，大于初始值位置
                indexLeft[0] = x
                (x - defaultImgWidth / 2).let {
                    if (it < 0f)
                        indexImage[0] = 0f
                    else
                        indexImage[0] = it
                }
            }
        } else if (clickedType == ClickIconType.RIGHT_ICON) {
            if (x > indexLeft[0] + rightIcon.width && x < startRightX) { //右按钮的范围大于左按钮的位置，小于初始值位置
                indexRight[0] = x
                (indexRight[0] - defaultImgWidth / 2).let { v1 ->
                    (startRightX - defaultImgWidth / 2).let { v2 ->
                        if (x > v2)
                            indexImage[2] = v2
                        else
                            indexImage[2] = v1
                    }
                }
                (startRightX - defaultImgWidth).let {
                    if (indexImage[2] > it) {
                        indexImage[2] = it
                    }
                }
            }
        }
        leftValue = (maxValue - minValue) * (indexLeft[0] - startLeftX) / (startRightX - startLeftX)
        rightValue =
            (maxValue - minValue) * (indexRight[0] - startLeftX) / (startRightX - startLeftX)
        listener?.onChange(clickedType, leftValue, rightValue)
        postInvalidate()
        return true
    }

    /**
     * 绘制背景
     *
     * @param canvas
     */
    private fun drawBg(canvas: Canvas) {
        paintBg.isAntiAlias = true
        //画两端的半圆
        val circleLeftCenterX = 0 + paddingLeft
        val circleRightCenterX = viewWidth - paddingRight
        canvas.drawCircle(circleLeftCenterX, viewHeight / 2f, bgHeight / 2, paintBg)
        canvas.drawCircle(circleRightCenterX, viewHeight / 2f, bgHeight / 2, paintBg)
        canvas.drawRect(
            RectF(
                circleLeftCenterX,
                viewHeight / 2 - bgHeight / 2,
                circleRightCenterX,
                viewHeight / 2 + bgHeight / 2
            ), paintBg
        )

        //左右按钮滑动区域之外绘制其他颜色
        val centerLeftIconX = indexLeft[0] + leftIcon.width / 2 //左按钮图标的中心点
        val centerRightIconX = indexRight[0] + rightIcon.width / 2 //右按钮图标的中心点
        if (centerLeftIconX > circleLeftCenterX) {
            canvas.drawCircle(circleLeftCenterX, viewHeight / 2f, bgHeight / 2, paintBg2)
            canvas.drawRect(
                RectF(
                    circleLeftCenterX,
                    viewHeight / 2 - bgHeight / 2,
                    centerLeftIconX,
                    viewHeight / 2 + bgHeight / 2
                ), paintBg2
            )
        }
        if (centerRightIconX < circleRightCenterX) {
            canvas.drawCircle(
                circleRightCenterX,
                viewHeight / 2f,
                bgHeight / 2,
                paintBg2
            )
            canvas.drawRect(
                RectF(
                    centerRightIconX,
                    viewHeight / 2 - bgHeight / 2,
                    circleRightCenterX,
                    viewHeight / 2 + bgHeight / 2
                ), paintBg2
            )
        }
    }

    /**
     * 绘制按钮
     */
    private fun drawClickIcon(canvas: Canvas) {
        when (clickedType) {
            ClickIconType.LEFT_ICON -> {
                iconPaint.maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.INNER)
                canvas.drawBitmap(
                    clickIcon,
                    indexLeft[0] - dp2px(2.5f),
                    indexLeft[1] - dp2px(2.5f),
                    iconPaint
                )
                getImgBitmap(leftValue.toInt())?.let {
                    canvas.drawBitmap(it, indexImage[0], indexImage[1], imgPaint)
                }
            }
            ClickIconType.RIGHT_ICON -> {
                iconPaint.maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.INNER)
                canvas.drawBitmap(
                    clickIcon,
                    indexRight[0] - dp2px(2.5f),
                    indexRight[1] - dp2px(2.5f),
                    iconPaint
                )
                getImgBitmap(rightValue.toInt())?.let {
                    canvas.drawBitmap(it, indexImage[2], indexImage[3], imgPaint)
                }
            }
            else -> {
                iconPaint.maskFilter = null
                canvas.drawBitmap(leftIcon, indexLeft[0], indexLeft[1], iconPaint)
                canvas.drawBitmap(rightIcon, indexRight[0], indexRight[1], iconPaint)
            }
        }

    }

    private fun getImgBitmap(index: Int): Bitmap? {
        if (filePathArrays.isEmpty()) return null
        if (index < 0 || index >= filePathArrays.size) return null
        val option = BitmapFactory.Options()
        option.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePathArrays[index], option)
        option.inSampleSize = computeScale(option, defaultImgWidth, defaultImgHeight)
        option.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePathArrays[index], option)
    }

    private fun computeScale(
        options: BitmapFactory.Options,
        viewWidth: Float,
        viewHeight: Float
    ): Int {
        var inSampleSize = 1
        if (viewWidth == 0f || viewHeight == 0f) return inSampleSize
        val bitmapWidth = options.outWidth
        val bitmapHeight = options.outHeight

        //假如Bitmap的宽度或高度大于我们设定图片的View的宽高，则计算缩放比例
        if (bitmapWidth > viewWidth || bitmapHeight > viewWidth) {
            val widthScale = (bitmapWidth.toFloat() / viewWidth).roundToInt()
            val heightScale = (bitmapHeight.toFloat() / viewWidth).roundToInt()

            //为了保证图片不缩放变形，我们取宽高比例最小的那个
            inSampleSize = if (widthScale < heightScale) widthScale else heightScale
        }
        return inSampleSize
    }

    fun setMaxValue(value: Int) {
        this.maxValue = if (value < 0)
            0
        else if (value < minValue)
            minValue
        else
            value
    }

    fun setMinValue(value: Int) {
        this.minValue = if (value < 0)
            0
        else if (value > maxValue)
            maxValue
        else
            value
    }

    fun setLeftIconValue(value: Float) {
        if (value == indexLeft[0] || value < minValue || value > maxValue || value >= indexRight[0]) return
        indexLeft[0] = value
    }

    fun setRightIconValue(value: Float) {
        if (value == indexRight[0] || value < minValue || value > maxValue || value <= indexLeft[0]) return
        indexRight[0] = value
    }

    fun setChangeListener(callback: SeekbarChangeListener?) {
        this.listener = callback
    }

    fun setFilePathArray(list: List<String>) {
        filePathArrays.clear()
        filePathArrays.addAll(list)
        getImgBitmap(0)
        getImgBitmap(filePathArrays.size - 1)
    }

    fun setPreviewImgWidth(width: Float) {
        defaultImgWidth = dp2px(width)
        defaultWidth = dp2px(100f + width)
        postInvalidate()
    }

    fun setPreviewImgHeight(height: Float) {
        defaultImgHeight = dp2px(height)
        defaultHeight = dp2px(30f + height)
        postInvalidate()
    }

    /**
     * 回调接口，回调左右值
     */
    interface SeekbarChangeListener {
        fun onChange(type: ClickIconType, leftValue: Float, rightValue: Float)
        fun onChangeComplete(leftValue: Int, rightValue: Int)
    }

    enum class ClickIconType {
        UNKNOWN, LEFT_ICON, RIGHT_ICON
    }
}