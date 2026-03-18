package com.pulse.app.ui.screens.play

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularVisualizer(
    fftData: FloatArray,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.minDimension * 0.25f
        val maxBarHeight = size.minDimension * 0.2f
        val barCount = 64
        val angleStep = 360f / barCount

        for (i in 0 until barCount) {
            val angle = i * angleStep
            val magnitude = if (fftData.isNotEmpty() && i < fftData.size) {
                (fftData[i] / 128f).coerceIn(0f, 1f)
            } else 0.1f

            val barHeight = maxBarHeight * magnitude + 4f
            val angleRad = Math.toRadians(angle.toDouble())
            val startX = cx + baseRadius * cos(angleRad).toFloat()
            val startY = cy + baseRadius * sin(angleRad).toFloat()
            val endX = cx + (baseRadius + barHeight) * cos(angleRad).toFloat()
            val endY = cy + (baseRadius + barHeight) * sin(angleRad).toFloat()

            drawLine(
                color = accentColor.copy(
                    alpha = 0.4f + 0.6f * magnitude
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f
            )
        }

        // Center circle
        drawCircle(
            color = accentColor.copy(alpha = 0.3f),
            radius = baseRadius,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.15f),
            radius = baseRadius * 0.7f,
            center = Offset(cx, cy)
        )
    }
}