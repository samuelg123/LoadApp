package com.udacity.button

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0

    private val valueAnimator = ValueAnimator()

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { prop, old, new ->
        when (new) {
            ButtonState.Clicked -> {

            }
            ButtonState.Completed -> {

            }
            is ButtonState.Loading -> {


            }
        }
    }

    // From O.0 to 1.0
    fun setProgress(percent: Float) {
        buttonState = ButtonState.Loading(percent)
    }

    fun done() {
        buttonState = ButtonState.Completed
    }

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    var percent: Float = 0f

    init {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val startAngle = 0f
        val drawTo = startAngle + (percent * 360)

        val mArea = RectF()

        // Rotate the canvas around the center of the pie by 90 degrees
        // counter clockwise so the pie stars at 12 o'clock.
        canvas?.apply {
            rotate(-90f, mArea.centerX(), mArea.centerY())
            drawArc(mArea, startAngle, drawTo, true, mPaint)
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


//    private fun scaler() {
//        //Parallel animation by using property values holder
//        val scaleX =
//            PropertyValuesHolder.ofFloat(View.SCALE_X, 4f) // scaling X by 4, default value: 1f
//        val scaleY =
//            PropertyValuesHolder.ofFloat(View.SCALE_Y, 4f) // scaling Y by 4, default value: 1f
//        val animator = ObjectAnimator.ofPropertyValuesHolder(star, scaleX, scaleY)
//        animator.repeatCount = 1
//        animator.repeatMode = ObjectAnimator.REVERSE
//        animator.disableViewDuringAnimation(scaleButton)
//
//        animator.start()
//    }

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