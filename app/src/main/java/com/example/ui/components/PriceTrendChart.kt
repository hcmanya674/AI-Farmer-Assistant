package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyFormatter
import com.example.domain.model.PricePoint
import com.example.domain.model.SellVerdict
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun PriceTrendChart(
    points: List<PricePoint>,
    verdict: SellVerdict,
    pastLegend: String,
    forecastLegend: String,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    var selectedIndex by remember(points) {
        mutableIntStateOf(points.lastIndex)
    }

    val selectedPoint = points.getOrElse(selectedIndex) { points.last() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val forecastColor = if (verdict == SellVerdict.HOLD_WAIT) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val allValues = points.flatMap { listOf(it.lowBound, it.highBound, it.pricePerQuintal) }
    val minPrice = (allValues.minOrNull() ?: 0.0) * 0.97
    val maxPrice = (allValues.maxOrNull() ?: 1.0) * 1.03
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected Point Pill & Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedPoint.dayOffset == 0) "Today" else selectedPoint.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CurrencyFormatter.formatInr(selectedPoint.pricePerQuintal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (selectedPoint.isForecast) forecastColor else primaryColor
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = primaryColor, label = pastLegend)
                LegendChip(color = forecastColor, label = forecastLegend)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val density = LocalDensity.current
        val horizontalPadPx = with(density) { 12.dp.toPx() }
        val verticalPadPx = with(density) { 14.dp.toPx() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Y-axis labels
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .height(190.dp)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatInr(maxPrice),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatInr((maxPrice + minPrice) / 2.0),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatInr(minPrice),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chart Canvas
            Box(modifier = Modifier.weight(1f).height(190.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val chartW = (size.width - horizontalPadPx * 2).coerceAtLeast(1f)
                                val relX = ((offset.x - horizontalPadPx) / chartW).coerceIn(0f, 1f)
                                val idx = (relX * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                                selectedIndex = idx
                            }
                        }
                ) {
                    val chartW = size.width - horizontalPadPx * 2
                    val chartH = size.height - verticalPadPx * 2

                    fun xFor(index: Int): Float =
                        horizontalPadPx + (index.toFloat() / (points.size - 1)) * chartW

                    fun yFor(price: Double): Float {
                        val norm = ((price - minPrice) / priceRange).toFloat().coerceIn(0f, 1f)
                        return verticalPadPx + chartH * (1f - norm)
                    }

                    // Horizontal gridlines
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = verticalPadPx + chartH * frac
                        drawLine(
                            color = gridColor,
                            start = Offset(horizontalPadPx, y),
                            end = Offset(size.width - horizontalPadPx, y),
                            strokeWidth = 2f
                        )
                    }

                    val todayIndex = points.indexOfFirst { it.dayOffset == 0 }.coerceAtLeast(0)
                    val todayX = xFor(todayIndex)

                    // Vertical "Today" marker
                    drawLine(
                        color = gridColor.copy(alpha = 0.6f),
                        start = Offset(todayX, verticalPadPx),
                        end = Offset(todayX, size.height - verticalPadPx),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // Confidence Band Polygon for Forecast segment
                    val forecastStartIdx = todayIndex
                    if (forecastStartIdx < points.lastIndex) {
                        val bandPath = Path().apply {
                            moveTo(xFor(forecastStartIdx), yFor(points[forecastStartIdx].highBound))
                            for (i in (forecastStartIdx + 1)..points.lastIndex) {
                                lineTo(xFor(i), yFor(points[i].highBound))
                            }
                            for (i in points.lastIndex downTo forecastStartIdx) {
                                lineTo(xFor(i), yFor(points[i].lowBound))
                            }
                            close()
                        }
                        drawPath(
                            path = bandPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    forecastColor.copy(alpha = 0.20f),
                                    forecastColor.copy(alpha = 0.04f)
                                )
                            )
                        )
                    }

                    // Historical Solid Line
                    val historyPath = Path().apply {
                        moveTo(xFor(0), yFor(points[0].pricePerQuintal))
                        for (i in 1..todayIndex) {
                            lineTo(xFor(i), yFor(points[i].pricePerQuintal))
                        }
                    }
                    drawPath(
                        path = historyPath,
                        color = primaryColor,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Forecast Dashed Line
                    if (todayIndex < points.lastIndex) {
                        val forecastPath = Path().apply {
                            moveTo(xFor(todayIndex), yFor(points[todayIndex].pricePerQuintal))
                            for (i in (todayIndex + 1)..points.lastIndex) {
                                lineTo(xFor(i), yFor(points[i].pricePerQuintal))
                            }
                        }
                        drawPath(
                            path = forecastPath,
                            color = forecastColor,
                            style = Stroke(
                                width = 7f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                            )
                        )
                    }

                    // Today Anchor Dot
                    val todayY = yFor(points[todayIndex].pricePerQuintal)
                    drawCircle(color = surfaceColor, radius = 11f, center = Offset(todayX, todayY))
                    drawCircle(color = primaryColor, radius = 7.5f, center = Offset(todayX, todayY))

                    // Selected Scrub Dot
                    val selX = xFor(selectedIndex)
                    val selY = yFor(selectedPoint.pricePerQuintal)
                    val selColor = if (selectedPoint.isForecast) forecastColor else primaryColor
                    drawCircle(color = selColor.copy(alpha = 0.25f), radius = 18f, center = Offset(selX, selY))
                    drawCircle(color = surfaceColor, radius = 10f, center = Offset(selX, selY))
                    drawCircle(color = selColor, radius = 7f, center = Offset(selX, selY))
                }
            }
        }

        // Bottom X-axis markers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp, top = 4.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = primaryColor
            )
            Text(
                text = points.last().label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = forecastColor
            )
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
