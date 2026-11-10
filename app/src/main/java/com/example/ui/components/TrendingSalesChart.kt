package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.util.FormatUtils

data class ChartPoint(
    val label: String,
    val value: Double
)

@Composable
fun TrendingSalesChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    trendPercentage: String = "+18.5%",
    peakLabel: String = "Puncak: 13:00"
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val defaultSlots = listOf("08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00", "22:00")
    val displayPoints = if (points.isEmpty()) {
        defaultSlots.map { ChartPoint(it, 0.0) }
    } else {
        points
    }

    val hasSales = displayPoints.any { it.value > 0.0 }
    val maxValue = if (hasSales) {
        (displayPoints.maxOfOrNull { it.value } ?: 10000.0).coerceAtLeast(1000.0)
    } else {
        10000.0
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Tren Penjualan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasSales) peakLabel else "Belum ada transaksi hari ini (Rp 0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (hasSales) AccentEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (hasSales) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasSales) trendPercentage else "0.0%",
                            color = if (hasSales) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height - 30.dp.toPx()
                    val count = displayPoints.size
                    if (count < 2) return@Canvas

                    val stepX = w / (count - 1)
                    val progress = animatedProgress.value

                    // Draw subtle grid lines
                    val gridPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.LTGRAY
                        strokeWidth = 1f
                        isAntiAlias = true
                    }

                    for (i in 0..3) {
                        val y = h * (i / 3f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val path = Path()
                    val fillPath = Path()

                    val offsets = displayPoints.mapIndexed { index, pt ->
                        val x = index * stepX
                        val ratio = (pt.value / maxValue).toFloat()
                        val y = h - (ratio * h * progress)
                        Offset(x, y)
                    }

                    offsets.forEachIndexed { i, pt ->
                        if (i == 0) {
                            path.moveTo(pt.x, pt.y)
                            fillPath.moveTo(pt.x, h)
                            fillPath.lineTo(pt.x, pt.y)
                        } else {
                            val prev = offsets[i - 1]
                            val cx1 = (prev.x + pt.x) / 2
                            val cy1 = prev.y
                            val cx2 = (prev.x + pt.x) / 2
                            val cy2 = pt.y
                            path.cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                            fillPath.cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                        }
                    }

                    fillPath.lineTo(offsets.last().x, h)
                    fillPath.close()

                    // Draw Area Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NavyLight.copy(alpha = 0.35f),
                                NavyPrimary.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw Line Path
                    drawPath(
                        path = path,
                        brush = if (hasSales) {
                            Brush.horizontalGradient(listOf(NavyLight, AccentEmerald))
                        } else {
                            Brush.horizontalGradient(listOf(Color.LightGray, Color.Gray))
                        },
                        style = Stroke(width = if (hasSales) 3.5.dp.toPx() else 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw point dots and peak marker
                    val maxIndex = if (hasSales) displayPoints.indexOfMaxBy { it.value } else -1

                    offsets.forEachIndexed { i, pt ->
                        if (hasSales && i == maxIndex && progress > 0.8f) {
                            // Glowing peak indicator
                            drawCircle(
                                color = AccentEmerald.copy(alpha = 0.3f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = AccentEmerald,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = pt
                            )
                        } else {
                            drawCircle(
                                color = if (hasSales) NavyPrimary else Color.LightGray,
                                radius = if (hasSales) 3.5.dp.toPx() else 2.5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    if (!hasSales) {
                        val emptyPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 12.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            alpha = 150
                        }
                        val subPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            alpha = 130
                        }
                        drawContext.canvas.nativeCanvas.drawText("Grafik flat di Rp 0 (Belum ada transaksi)", w / 2f, h / 2f - 6.dp.toPx(), emptyPaint)
                        drawContext.canvas.nativeCanvas.drawText("Akan otomatis naik saat transaksi kasir diproses", w / 2f, h / 2f + 14.dp.toPx(), subPaint)
                    }

                    // Labels below
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    displayPoints.forEachIndexed { index, pt ->
                        val x = index * stepX
                        val y = size.height - 4.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(pt.label, x, y, textPaint)
                    }
                }
            }
        }
    }
}

private inline fun <T> List<T>.indexOfMaxBy(selector: (T) -> Double): Int {
    if (isEmpty()) return -1
    var maxIndex = 0
    var maxValue = selector(this[0])
    for (i in 1 until size) {
        val v = selector(this[i])
        if (v > maxValue) {
            maxValue = v
            maxIndex = i
        }
    }
    return maxIndex
}
