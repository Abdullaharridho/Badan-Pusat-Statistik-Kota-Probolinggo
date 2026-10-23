package com.example.bpskota

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.min

class BottomNavCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val glassColor = Color.parseColor("#35FFFFFF")
    private val glassStroke = Color.parseColor("#B3FFFFFF")
    private val orange = Color.parseColor("#F97316")
    private val glowColor = Color.parseColor("#44F97316")
    private val glitchCyan = Color.parseColor("#664DEBFF")

    private var activeCenterX = -1f
    private var targetCenterX = -1f

    private var glitchOffsetX = 0f
    private var glitchAlpha = 1f
    private var glitchLineAlpha = 0f

    private var moveAnimator: ValueAnimator? = null
    private var glitchAnimator: ValueAnimator? = null

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = glassColor
        setShadowLayer(
            dpToPx(4f),
            0f,
            dpToPx(1f),
            glowColor
        )
    }

    private val glassStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.2f)
        color = glassStroke
    }

    private val glitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setWillNotDraw(false)
    }

    fun setActiveCenterX(centerX: Float) {
        val maxX = width.toFloat().coerceAtLeast(0f)

        activeCenterX = centerX.coerceIn(0f, maxX)
        targetCenterX = activeCenterX

        invalidate()
    }

    fun animateCurveTo(newCenterX: Float) {
        if (width <= 0) {
            setActiveCenterX(newCenterX)
            return
        }

        val targetX = newCenterX.coerceIn(
            0f,
            width.toFloat()
        )

        val startX = if (activeCenterX >= 0f) {
            activeCenterX
        } else {
            targetX
        }

        targetCenterX = targetX

        moveAnimator?.cancel()
        glitchAnimator?.cancel()

        // Reset kondisi glitch sebelum animasi baru
        glitchOffsetX = 0f
        glitchAlpha = 1f
        glitchLineAlpha = 0f

        // Gerakan utama highlight
        moveAnimator = ValueAnimator.ofFloat(
            startX,
            targetX
        ).apply {
            duration = 380L
            interpolator = OvershootInterpolator(1.08f)

            addUpdateListener { animator ->
                activeCenterX = animator.animatedValue as Float
                invalidate()
            }

            start()
        }

        // Efek glitch singkat saat berpindah menu
        glitchAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float

                when {
                    progress < 0.18f -> {
                        glitchOffsetX = dpToPx(5f)
                        glitchAlpha = 0.55f
                        glitchLineAlpha = 1f
                    }

                    progress < 0.36f -> {
                        glitchOffsetX = -dpToPx(4f)
                        glitchAlpha = 0.8f
                        glitchLineAlpha = 0.25f
                    }

                    progress < 0.55f -> {
                        glitchOffsetX = dpToPx(3f)
                        glitchAlpha = 0.6f
                        glitchLineAlpha = 0.85f
                    }

                    else -> {
                        glitchOffsetX = 0f
                        glitchAlpha = 1f
                        glitchLineAlpha =
                            (1f - progress) * 0.45f
                    }
                }

                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    glitchOffsetX = 0f
                    glitchAlpha = 1f
                    glitchLineAlpha = 0f
                    invalidate()
                }

                override fun onAnimationCancel(animation: Animator) {
                    glitchOffsetX = 0f
                    glitchAlpha = 1f
                    glitchLineAlpha = 0f
                    invalidate()
                }
            })

            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val centerX = if (activeCenterX >= 0f) {
            activeCenterX
        } else {
            w / 2f
        }

        // Ukuran highlight agar bentuk kotak memanjang lebih jelas
        // Ukuran highlight navigasi yang lebih besar
        val highlightWidth = min(
            dpToPx(68f),
            w * 0.24f
        )

        val highlightHeight = min(
            dpToPx(56f),
            h - dpToPx(4f)

        )

        if (highlightHeight <= 0f) return

        val top = (h - highlightHeight) / 2f
        val bottom = top + highlightHeight

        val cornerRadius = dpToPx(14f)

        val normalRect = RectF(
            centerX - highlightWidth / 2f,
            top,
            centerX + highlightWidth / 2f,
            bottom
        )

        // Geser sementara untuk efek glitch
        val glitchRect = RectF(normalRect).apply {
            offset(glitchOffsetX, 0f)
        }

        val oldGlassAlpha = glassPaint.alpha
        val oldStrokeAlpha = glassStrokePaint.alpha

        glassPaint.alpha = (255 * glitchAlpha).toInt()
        glassStrokePaint.alpha = (255 * glitchAlpha).toInt()

        // Lapisan kaca utama
        canvas.drawRoundRect(
            glitchRect,
            cornerRadius,
            cornerRadius,
            glassPaint
        )

        // Outline putih agar bentuk navigasi lebih tegas
        canvas.drawRoundRect(
            glitchRect,
            cornerRadius,
            cornerRadius,
            glassStrokePaint
        )

        glassPaint.alpha = oldGlassAlpha
        glassStrokePaint.alpha = oldStrokeAlpha

        // Garis glitch cyan singkat
        if (glitchLineAlpha > 0f) {
            glitchPaint.color = glitchCyan
            glitchPaint.alpha = (255 * glitchLineAlpha).toInt()

            val lineRect = RectF(
                glitchRect.left + dpToPx(5f),
                glitchRect.top + dpToPx(3f),
                glitchRect.right - dpToPx(5f),
                glitchRect.top + dpToPx(5f)
            )

            canvas.drawRoundRect(
                lineRect,
                dpToPx(1f),
                dpToPx(1f),
                glitchPaint
            )

            // Aksen oranye di bagian bawah highlight
            accentPaint.color = orange
            accentPaint.alpha = (210 * glitchLineAlpha).toInt()

            canvas.drawLine(
                glitchRect.left + dpToPx(8f),
                glitchRect.bottom - dpToPx(3f),
                glitchRect.right - dpToPx(8f),
                glitchRect.bottom - dpToPx(3f),
                accentPaint
            )
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    override fun onDetachedFromWindow() {
        moveAnimator?.cancel()
        glitchAnimator?.cancel()

        moveAnimator = null
        glitchAnimator = null

        super.onDetachedFromWindow()
    }
}