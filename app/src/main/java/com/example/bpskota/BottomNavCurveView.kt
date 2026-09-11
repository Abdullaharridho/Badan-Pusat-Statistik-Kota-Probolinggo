package com.example.bpskota

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.min

class BottomNavCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        // Penting: Mematikan hardware acceleration khusus untuk View ini
        // agar efek setShadowLayer (Glow) bisa dirender dengan sempurna oleh Android.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F97316")
        style = Paint.Style.STROKE
        strokeWidth = 8.5f // Sedikit lebih tebal agar lebih tegas
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND

        // Menambahkan efek Glow/Shadow berwarna oranye (opacity 30%) di bawah garis
        setShadowLayer(12f, 0f, 6f, Color.parseColor("#4DF97316"))
    }

    private val path = Path()
    private var activeCenterX = -1f

    fun setActiveCenterX(centerX: Float) {
        activeCenterX = centerX
        invalidate()
    }

    fun animateCurveTo(newCenterX: Float) {
        val startX = if (activeCenterX > 0f) activeCenterX else width / 2f

        val animator = ValueAnimator.ofFloat(startX, newCenterX)
        animator.duration = 450 // Durasi sedikit dipanjangkan untuk menikmati efek membalnya

        // Menggunakan OvershootInterpolator agar lengkungan bergerak seperti cairan/jelly
        animator.interpolator = OvershootInterpolator(1.1f)

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            setActiveCenterX(animatedValue)
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0f || height <= 0f) return

        val baselineY = height * 0.97f
        val curveTopY = height * 0.05f

        val centerX = if (activeCenterX > 0f) activeCenterX else width / 2f

        // Melebarkan sedikit ruang lengkungan agar terkesan lebih lega (fluid)
        val curveHalfWidth = min(dpToPx(48f), width * 0.14f)

        val leftCurve = centerX - curveHalfWidth
        val rightCurve = centerX + curveHalfWidth

        path.reset()

        path.moveTo(0f, baselineY)
        path.lineTo(leftCurve, baselineY)

        // Penyesuaian titik kubik (Bézier points) agar lengkungan terasa lebih mulus
        path.cubicTo(
            leftCurve + curveHalfWidth * 0.40f, baselineY,
            centerX - curveHalfWidth * 0.60f, curveTopY,
            centerX, curveTopY
        )

        path.cubicTo(
            centerX + curveHalfWidth * 0.60f, curveTopY,
            rightCurve - curveHalfWidth * 0.40f, baselineY,
            rightCurve, baselineY
        )

        path.lineTo(width, baselineY)

        canvas.drawPath(path, paint)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}