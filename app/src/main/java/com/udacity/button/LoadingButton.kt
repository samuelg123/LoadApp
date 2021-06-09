package com.udacity.button

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.udacity.R
import kotlin.properties.Delegates


private const val TAG: String = "LoadingButton"

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var canvasColor: Int = 0

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    private val progressPaint = Paint().apply {
        isAntiAlias = true
    }

    private val arcPaint = Paint().apply {
        isAntiAlias = true
    }


    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            textPaint.apply {
                color = getColor(
                    R.styleable.LoadingButton_textColor,
                    Color.WHITE
                )
                textSize = getDimension(
                    R.styleable.LoadingButton_textSize,
                    resources.getDimension(R.dimen.default_text_size)
                )
            }
            canvasColor = getColor(
                R.styleable.LoadingButton_buttonBgColor,
                ContextCompat.getColor(context, R.color.colorPrimary)
            )
            progressPaint.color = getColor(
                R.styleable.LoadingButton_loadingBgColor,
                ContextCompat.getColor(context, R.color.colorPrimaryDark)
            )
            arcPaint.color = getColor(
                R.styleable.LoadingButton_arcColor,
                ContextCompat.getColor(context, R.color.colorAccent)
            )
        }
    }

    private var widthSize = 0
    private var heightSize = 0

    private val r = Rect()

    private val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            invalidate()
        }
    }

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { prop, old, new ->
        when (new) {
            ButtonState.Clicked -> {
                isEnabled = false
                if (!valueAnimator.isStarted) {
                    valueAnimator.start()
                }
                invalidate()
            }
            ButtonState.Completed -> {
                isEnabled = true
                if (valueAnimator.isStarted) {
                    valueAnimator.cancel()
                }
                invalidate()
            }
            is ButtonState.Loading -> {
                if (new.percent < 0f) {
                    if (!valueAnimator.isStarted) {
                        valueAnimator.start()
                    }
                } else {
                    invalidate()
                }
            }
        }
    }

    fun setProgress(percent: Float) {
        if (buttonState == ButtonState.Completed) return
        buttonState = ButtonState.Loading(percent)
    }

    fun setIndeterminateProgress() {
        if (buttonState == ButtonState.Completed) return
        buttonState = ButtonState.Loading(-1f)
    }

    fun done() {
        buttonState = ButtonState.Completed
    }

    override fun performClick(): Boolean {
        buttonState = ButtonState.Clicked
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawButtonBackground(canvas)

        when (val localButtonState = buttonState) {
            ButtonState.Clicked -> {
                Log.d(TAG, "onDraw: ButtonState.Clicked")
                drawComplete()
                drawTextCenter(canvas, textPaint, "Please wait...")
            }
            ButtonState.Completed -> {
                Log.d(TAG, "onDraw: ButtonState.Completed")
                drawComplete()
                drawTextCenter(canvas, textPaint, "Download")
            }
            is ButtonState.Loading -> {
                Log.d(TAG, "onDraw: ButtonState.Loading: ${localButtonState.percent}")
                if (localButtonState.percent < 0f) {
                    drawLoading(canvas, valueAnimator.animatedValue as Float)
                    drawTextCenter(
                        canvas,
                        textPaint,
                        "Downloading..."
                    )
                } else {
                    drawLoading(canvas, localButtonState.percent)
                    drawTextCenter(
                        canvas,
                        textPaint,
                        "Downloading ${(localButtonState.percent * 100).toInt()}%"
                    )
                }
            }
        }
    }

    private fun drawButtonBackground(canvas: Canvas) {
        canvas.drawColor(canvasColor)
    }

    private fun drawTextCenter(canvas: Canvas, paint: Paint, text: String) {
        canvas.getClipBounds(r)
        val cHeight: Int = r.height()
        val cWidth: Int = r.width()
        paint.apply {
            getTextBounds(text, 0, text.length, r)
        }
        val x: Float = cWidth / 2f - r.width() / 2f - r.left
        val y: Float = cHeight / 2f + r.height() / 2f - r.bottom
        canvas.drawText(text, x, y, paint)
    }

    private fun drawComplete() {
        //No op
    }

    private fun drawLoading(canvas: Canvas, value: Float) {
        canvas.apply {
            //Draw Loading Rect
            drawRect(0f, 0f, widthSize * value, heightSize.toFloat(), progressPaint)

            //Draw Loading Arc
            var xPosition = widthSize * 0.95f
            var yPosition = heightSize / 2f
            val size = heightSize * 0.5f
            xPosition -= size / 2
            yPosition -= size / 2

            xPosition -= size / 2 //To make sure the arc is drawn inside the view
            val rectF = RectF(
                xPosition, yPosition,
                (xPosition + size), (yPosition + size)
            )
            canvas.drawArc(rectF, 0 + 90f, value * 360f, true, arcPaint)
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
}