package com.example.plantpall
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 60f
        strokeCap = Paint.Cap.BUTT
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 3f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private var currentValue = 30f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // Dark background

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) * 0.8f
        val arcRect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Draw colored arc segments
        val segmentSweep = 45f
        val colors = listOf(
            Color.parseColor("#2ECC71"), // Green
            Color.parseColor("#F1C40F"), // Yellow
            Color.parseColor("#F39C12"), // Orange
            Color.parseColor("#E74C3C")  // Red
        )

        for (i in colors.indices) {
            arcPaint.color = colors[i]
            canvas.drawArc(arcRect, 180f + i * segmentSweep, segmentSweep, false, arcPaint)
        }

        // Draw tick marks and labels
        val tickStart = radius * 0.88f
        val tickEndShort = radius * 0.94f
        val tickEndLong = radius * 0.97f
        val labelRadius = radius * 0.75f

        for (i in 0..100 step 5) {
            val angle = Math.toRadians(180.0 + (i / 100.0) * 180.0)
            val cosA = cos(angle)
            val sinA = sin(angle)

            val startX = centerX + cosA * tickStart
            val startY = centerY + sinA * tickStart
            val endX = centerX + cosA * if (i % 10 == 0) tickEndLong else tickEndShort
            val endY = centerY + sinA * if (i % 10 == 0) tickEndLong else tickEndShort

            canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), tickPaint)

            if (i % 10 == 0) {
                val labelX = centerX + cosA * labelRadius
                val labelY = centerY + sinA * labelRadius + 10f
                canvas.drawText(i.toString(), labelX.toFloat(), labelY.toFloat(), textPaint)
            }
        }

        // Draw triangle needle
        val needleAngleRad = Math.toRadians(mapValueToAngle(currentValue).toDouble())
        val needleLength = radius * 0.7f
        val tipX = centerX + cos(needleAngleRad) * needleLength
        val tipY = centerY + sin(needleAngleRad) * needleLength

        val baseAngle = 15f
        val leftX = centerX + cos(needleAngleRad + Math.toRadians(baseAngle.toDouble())) * 40f
        val leftY = centerY + sin(needleAngleRad + Math.toRadians(baseAngle.toDouble())) * 40f
        val rightX = centerX + cos(needleAngleRad - Math.toRadians(baseAngle.toDouble())) * 40f
        val rightY = centerY + sin(needleAngleRad - Math.toRadians(baseAngle.toDouble())) * 40f

        val needlePath = Path().apply {
            moveTo(tipX.toFloat(), tipY.toFloat())
            lineTo(leftX.toFloat(), leftY.toFloat())
            lineTo(rightX.toFloat(), rightY.toFloat())
            close()
        }

        canvas.drawPath(needlePath, needlePaint)

        // Draw center circle
        canvas.drawCircle(centerX, centerY, 16f, centerCirclePaint)
    }

    private fun mapValueToAngle(value: Float): Float {
        // Map 0–100 to 180–360 degrees
        return 180f + (value / 100f) * 180f
    }

    fun setSpeed(value: Float) {
        currentValue = value.coerceIn(0f, 100f)
        invalidate()
    }
}