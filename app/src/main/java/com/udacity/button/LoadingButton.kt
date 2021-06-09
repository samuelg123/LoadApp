package com.udacity.button

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.udacity.R
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0

    private val valueAnimator = ValueAnimator()

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { prop, old, new ->
        when (new) {
            ButtonState.Clicked -> {

            }
            ButtonState.Completed -> {

            }
            is ButtonState.Loading -> {


            }
        }
        invalidate()
    }

    // From O.0 to 1.0
    fun setProgress(percent: Float) {
        buttonState = ButtonState.Loading(percent)
    }

    fun done() {
        buttonState = ButtonState.Completed
    }

//    var percent: Float = 0f

    init {

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawButtonBackground(canvas)

        when(val localButtonState = buttonState){
            ButtonState.Clicked -> {

            }
            ButtonState.Completed -> {
                drawComplete()
                drawTextCenter(canvas,paint,"Download")
            }
            is ButtonState.Loading -> {
                drawLoading(canvas)
                drawTextCenter(canvas,paint,"Downloading ${(localButtonState.percent * 100).toInt()}%")
            }
        }


//        val startAngle = 0f
//        val drawTo = startAngle + (percent * 360)

//        val mArea = RectF()
//
//        // Rotate the canvas around the center of the pie by 90 degrees
//        // counter clockwise so the pie stars at 12 o'clock.
//        canvas?.apply {
//            rotate(-90f, mArea.centerX(), mArea.centerY())
//            drawArc(mArea, startAngle, drawTo, true, mPaint)
//        }
    }

    private fun drawButtonBackground(canvas: Canvas) {
        canvas.drawColor(ContextCompat.getColor(context, R.color.colorPrimary))
    }

    private val r = Rect()
    private fun drawTextCenter(canvas: Canvas, paint: Paint, text: String) {
        canvas.getClipBounds(r)
        val cHeight: Int = r.height()
        val cWidth: Int = r.width()
        paint.apply {
            textAlign = Paint.Align.LEFT
            color = Color.WHITE
            textSize = resources.getDimension(R.dimen.default_text_size)
            getTextBounds(text, 0, text.length, r)
        }
        val x: Float = cWidth / 2f - r.width() / 2f - r.left
        val y: Float = cHeight / 2f + r.height() / 2f - r.bottom
        canvas.drawText(text, x, y, paint)
    }

    private fun drawComplete() {

    }

    private fun drawLoading(canvas: Canvas) {
        buttonState.apply {
            if (this is ButtonState.Loading) {
                canvas.drawRect(0f, 0f, widthSize * percent, 0f, paint)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }


    private fun scaler() {
        //Parallel animation by using property values holder
        val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0.0f)
        animator.disableViewDuringAnimation(this)

        animator.start()
    }

    private fun ObjectAnimator.disableViewDuringAnimation(view: View) {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                view.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator?) {
                view.isEnabled = true
            }
        })
    }
}