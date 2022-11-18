package com.voidcom.videoproject.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.voidcom.v_base.utils.dp2px

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/18 9:12
 * @UpdateDate: 2022/11/18 9:12
 */
class PreviewSeekbar1 : View {
    private val seekbar: PreviewSeekbar
    private val leftImg: AppCompatImageView
    private val rightImg: AppCompatImageView
    private val defaultWidth = dp2px(100f)
    private val defaultHeight = dp2px(90f)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, def: Int) : super(context, attrs, def) {
        seekbar = PreviewSeekbar(context)
        leftImg = AppCompatImageView(context)
        rightImg = AppCompatImageView(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(defaultWidth.toInt(), defaultHeight.toInt())
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(defaultWidth.toInt(), heightSpecSize)
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, defaultHeight.toInt())
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
//        draw()
    }
}