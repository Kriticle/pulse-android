package com.pulse.app.ui.screens.levelselect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LevelGraphic(accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.6f
        val cy = size.height * 0.4f
        val hexRadius = size.minDimension * 0.38f

        val hexPath = Path()
        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i) - 30.0)
            val x = cx + hexRadius * cos(angle).toFloat()
            val y = cy + hexRadius * sin(angle).toFloat()
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        drawPath(hexPath, accentColor.copy(alpha = 0.12f))
        drawPath(hexPath, accentColor.copy(alpha = 0.3f), style = Stroke(width = 1.5f))

        val innerRadius = hexRadius * 0.55f
        val innerPath = Path()
        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i) - 30.0)
            val x = cx + innerRadius * cos(angle).toFloat()
            val y = cy + innerRadius * sin(angle).toFloat()
            if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
        }
        innerPath.close()
        drawPath(innerPath, accentColor.copy(alpha = 0.2f))
        drawPath(innerPath, accentColor.copy(alpha = 0.5f), style = Stroke(width = 1f))

        drawCircle(
            color = accentColor.copy(alpha = 0.6f),
            radius = hexRadius * 0.18f,
            center = Offset(cx, cy)
        )

        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i) - 30.0)
            val x = cx + hexRadius * cos(angle).toFloat()
            val y = cy + hexRadius * sin(angle).toFloat()
            drawCircle(
                color = accentColor.copy(alpha = 0.4f),
                radius = 4f,
                center = Offset(x, y)
            )
        }

        drawCircle(
            color = accentColor.copy(alpha = 0.15f),
            radius = size.minDimension * 0.2f,
            center = Offset(size.width * 0.1f, size.height * 0.85f)
        )
    }
}

@Composable
fun DifficultyBadge(difficulty: Int, accentColor: Color) {
    val label = when (difficulty) {
        1 -> "Easy"
        2 -> "Medium"
        3 -> "Hard"
        else -> "Level $difficulty"
    }
    Surface(
        color = accentColor.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}