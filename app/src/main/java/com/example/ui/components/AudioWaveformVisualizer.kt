package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    soundRms: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val rmsAnim = remember { Animatable(0f) }
    LaunchedEffect(soundRms) {
        rmsAnim.animateTo(soundRms, tween(50, easing = LinearEasing))
    }

    val primaryColor = when {
        isListening -> JarvisCyan
        isSpeaking -> JarvisGold
        else -> JarvisCyan.copy(alpha = 0.3f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = (width / barCount) * 0.65f
        val gap = (width - (barCount * barWidth)) / (barCount - 1)
        val centerY = height / 2f
        val baseRmsMultiplier = if (isListening) (rmsAnim.value / 15f).coerceIn(0.1f, 1f) else if (isSpeaking) 0.65f else 0.12f

        for (i in 0 until barCount) {
            val normalizedIndex = i.toDouble() / barCount
            val sinVal = sin(phase.toDouble() + (normalizedIndex * 4.0 * Math.PI))
            val wave = ((sinVal + 1.0) / 2.0).toFloat()
            val envelope = sin(normalizedIndex * Math.PI).toFloat()

            val dynamicHeight: Float = (height * 0.85f * baseRmsMultiplier * (0.3f + 0.7f * wave) * envelope)
                .coerceAtLeast(4.dp.toPx())

            val left: Float = i * (barWidth + gap)
            val top: Float = centerY - (dynamicHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.6f)
                    ),
                    startY = top,
                    endY = top + dynamicHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, dynamicHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
