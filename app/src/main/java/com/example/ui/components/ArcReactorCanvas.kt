package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisGold
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCanvas(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isProcessing: Boolean = false,
    soundRms: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_rotations")

    // Slow continuous ambient rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Reverse middle rotation
    val middleRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mid_rot"
    )

    // Breathing pulse for core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening || isSpeaking) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Interactive rms audio expansion
    val rmsAnim = remember { Animatable(0f) }
    LaunchedEffect(soundRms) {
        rmsAnim.animateTo(soundRms, tween(60, easing = LinearEasing))
    }

    val primaryGlowColor = when {
        isListening -> JarvisCyan
        isSpeaking -> JarvisGold
        isProcessing -> Color(0xFF00E676)
        else -> JarvisCyan
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.88f
            val audioBoost = (rmsAnim.value / 15f) * 16f

            // 1. Ambient outer glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = if (isListening || isSpeaking) 0.35f else 0.15f),
                        JarvisCyanGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.25f + audioBoost
                ),
                radius = baseRadius * 1.25f + audioBoost,
                center = center
            )

            // 2. Outer segmented tech ring
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primaryGlowColor.copy(alpha = 0.3f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Outer tick marks (16 radial ticks)
                val tickCount = 16
                val tickInner = baseRadius - 6.dp.toPx()
                val tickOuter = baseRadius + 4.dp.toPx() + (if (isListening) audioBoost * 0.5f else 0f)
                for (i in 0 until tickCount) {
                    val angleRad = Math.toRadians((i * (360.0 / tickCount))).toFloat()
                    val startX = center.x + tickInner * cos(angleRad)
                    val startY = center.y + tickInner * sin(angleRad)
                    val endX = center.x + tickOuter * cos(angleRad)
                    val endY = center.y + tickOuter * sin(angleRad)

                    drawLine(
                        color = if (i % 4 == 0) primaryGlowColor else primaryGlowColor.copy(alpha = 0.4f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 4 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Middle segment arcs
            val midRadius = baseRadius * 0.72f
            rotate(middleRotation, pivot = center) {
                val segments = 8
                val arcSweep = 360f / segments
                for (i in 0 until segments) {
                    val startAngle = i * arcSweep
                    drawArc(
                        color = if (i % 2 == 0) primaryGlowColor.copy(alpha = 0.85f) else JarvisGold.copy(alpha = 0.5f),
                        startAngle = startAngle + 6f,
                        sweepAngle = arcSweep - 12f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = Size(midRadius * 2, midRadius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Inner rotating triangular power core brackets
            val innerRingRadius = baseRadius * 0.46f * pulseScale
            rotate(-outerRotation * 1.5f, pivot = center) {
                drawCircle(
                    color = primaryGlowColor.copy(alpha = 0.7f),
                    radius = innerRingRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Inscribed triangle path
                val trianglePath = Path()
                for (i in 0..2) {
                    val angle = Math.toRadians(i * 120.0 - 90.0).toFloat()
                    val px = center.x + innerRingRadius * cos(angle)
                    val py = center.y + innerRingRadius * sin(angle)
                    if (i == 0) trianglePath.moveTo(px, py) else trianglePath.lineTo(px, py)
                }
                trianglePath.close()

                drawPath(
                    path = trianglePath,
                    color = primaryGlowColor.copy(alpha = 0.5f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 5. Center Arc Reactor glowing nucleus
            val nucleusRadius = baseRadius * 0.28f * pulseScale + (audioBoost * 0.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlowColor,
                        primaryGlowColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = nucleusRadius
                ),
                radius = nucleusRadius,
                center = center
            )

            // Center high-intensity core dot
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = center
            )
        }
    }
}
