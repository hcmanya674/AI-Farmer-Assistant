package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CropArtworkBadge(
    cropId: String,
    accentColorHex: Long,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val accent = Color(accentColorHex)
    Box(
        modifier = modifier
            .size(size)
            .background(accent.copy(alpha = 0.14f), CircleShape)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            when (cropId) {
                "sugarcane" -> {
                    // Segmented green & golden-lime sugarcane stalks with leaves
                    val caneGreen = Color(0xFF16A34A)
                    val nodeColor = Color(0xFF86EFAC)
                    val leafColor = Color(0xFF15803D)
                    drawLine(
                        color = leafColor,
                        start = Offset(w * 0.34f, h * 0.28f),
                        end = Offset(w * 0.14f, h * 0.14f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = leafColor,
                        start = Offset(w * 0.66f, h * 0.24f),
                        end = Offset(w * 0.86f, h * 0.12f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    // Left stalk
                    drawRoundRect(
                        color = caneGreen,
                        topLeft = Offset(w * 0.28f, h * 0.20f),
                        size = Size(w * 0.16f, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                    // Right stalk
                    drawRoundRect(
                        color = Color(0xFF22C55E),
                        topLeft = Offset(w * 0.54f, h * 0.16f),
                        size = Size(w * 0.16f, h * 0.76f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                    // Cane joint nodes
                    listOf(0.38f, 0.56f, 0.74f).forEach { ny ->
                        drawLine(
                            color = nodeColor,
                            start = Offset(w * 0.26f, h * ny),
                            end = Offset(w * 0.46f, h * ny),
                            strokeWidth = h * 0.045f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = nodeColor,
                            start = Offset(w * 0.52f, h * (ny - 0.04f)),
                            end = Offset(w * 0.72f, h * (ny - 0.04f)),
                            strokeWidth = h * 0.045f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                "wheat" -> {
                    // Golden wheat ear with braided kernels
                    drawLine(
                        color = Color(0xFFB45309),
                        start = Offset(cx, h * 0.92f),
                        end = Offset(cx, h * 0.16f),
                        strokeWidth = w * 0.07f,
                        cap = StrokeCap.Round
                    )
                    listOf(0.24f, 0.38f, 0.52f, 0.66f).forEach { ry ->
                        drawOval(
                            color = Color(0xFFD97706),
                            topLeft = Offset(w * 0.24f, h * ry),
                            size = Size(w * 0.22f, h * 0.12f)
                        )
                        drawOval(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(w * 0.54f, h * (ry - 0.04f)),
                            size = Size(w * 0.22f, h * 0.12f)
                        )
                    }
                }

                "maize" -> {
                    // Bright yellow corn cob wrapped in green husk
                    drawOval(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(w * 0.30f, h * 0.14f),
                        size = Size(w * 0.40f, h * 0.70f)
                    )
                    listOf(0.30f, 0.44f, 0.58f).forEach { ry ->
                        drawLine(
                            color = Color(0xFFCA8A04),
                            start = Offset(w * 0.34f, h * ry),
                            end = Offset(w * 0.66f, h * ry),
                            strokeWidth = h * 0.03f
                        )
                    }
                    // Green husk leaves
                    val leftHusk = Path().apply {
                        moveTo(w * 0.50f, h * 0.88f)
                        quadraticTo(w * 0.14f, h * 0.74f, w * 0.22f, h * 0.40f)
                        quadraticTo(w * 0.34f, h * 0.66f, w * 0.50f, h * 0.84f)
                        close()
                    }
                    val rightHusk = Path().apply {
                        moveTo(w * 0.50f, h * 0.88f)
                        quadraticTo(w * 0.86f, h * 0.74f, w * 0.78f, h * 0.40f)
                        quadraticTo(w * 0.66f, h * 0.66f, w * 0.50f, h * 0.84f)
                        close()
                    }
                    drawPath(leftHusk, Color(0xFF16A34A))
                    drawPath(rightHusk, Color(0xFF15803D))
                }

                "soybean" -> {
                    // Curved green pod with 3 golden-cream beans inside
                    val podPath = Path().apply {
                        moveTo(w * 0.16f, h * 0.72f)
                        quadraticTo(w * 0.48f, h * 0.14f, w * 0.84f, h * 0.30f)
                        quadraticTo(w * 0.64f, h * 0.82f, w * 0.16f, h * 0.72f)
                        close()
                    }
                    drawPath(podPath, Color(0xFF65A30D))
                    listOf(
                        Offset(w * 0.34f, h * 0.60f),
                        Offset(w * 0.51f, h * 0.47f),
                        Offset(w * 0.68f, h * 0.37f)
                    ).forEach { pt ->
                        drawCircle(color = Color(0xFFFEF08A), radius = w * 0.09f, center = pt)
                    }
                }

                "groundnut" -> {
                    // Classic double-bulge peanut shell with texture lines
                    drawCircle(color = Color(0xFFB45309), radius = w * 0.23f, center = Offset(w * 0.35f, h * 0.58f))
                    drawCircle(color = Color(0xFFD97706), radius = w * 0.21f, center = Offset(w * 0.62f, h * 0.42f))
                    drawCircle(color = Color(0xFFFDE68A).copy(alpha = 0.35f), radius = w * 0.08f, center = Offset(w * 0.32f, h * 0.54f))
                    drawCircle(color = Color(0xFFFDE68A).copy(alpha = 0.35f), radius = w * 0.07f, center = Offset(w * 0.60f, h * 0.38f))
                }

                "arecanut" -> {
                    drawLine(
                        color = Color(0xFF1B5E20),
                        start = Offset(cx, h * 0.1f),
                        end = Offset(cx, h * 0.34f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawOval(
                        color = Color(0xFF8D4E2A),
                        topLeft = Offset(w * 0.18f, h * 0.36f),
                        size = Size(w * 0.32f, h * 0.42f)
                    )
                    drawOval(
                        color = Color(0xFFA16207),
                        topLeft = Offset(w * 0.50f, h * 0.34f),
                        size = Size(w * 0.32f, h * 0.42f)
                    )
                    drawOval(
                        color = Color(0xFF78350F),
                        topLeft = Offset(w * 0.34f, h * 0.50f),
                        size = Size(w * 0.32f, h * 0.40f)
                    )
                }

                "paddy" -> {
                    val stalkColor = Color(0xFFD97706)
                    val leafColor = Color(0xFF2E7D32)
                    val path = Path().apply {
                        moveTo(cx, h * 0.9f)
                        quadraticTo(cx - w * 0.05f, h * 0.45f, w * 0.72f, h * 0.18f)
                    }
                    drawPath(path, color = stalkColor, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                    listOf(
                        Offset(w * 0.52f, h * 0.36f),
                        Offset(w * 0.60f, h * 0.27f),
                        Offset(w * 0.69f, h * 0.19f),
                        Offset(w * 0.44f, h * 0.47f)
                    ).forEach { pt ->
                        drawOval(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(pt.x - w * 0.09f, pt.y - h * 0.05f),
                            size = Size(w * 0.18f, h * 0.11f)
                        )
                    }
                    drawLine(
                        color = leafColor,
                        start = Offset(cx, h * 0.88f),
                        end = Offset(w * 0.24f, h * 0.55f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                }

                "tomato" -> {
                    drawCircle(
                        color = Color(0xFFDC2626),
                        radius = w * 0.35f,
                        center = Offset(cx, cy + h * 0.06f)
                    )
                    drawCircle(
                        color = Color(0xFFFCA5A5).copy(alpha = 0.5f),
                        radius = w * 0.09f,
                        center = Offset(cx - w * 0.12f, cy - h * 0.04f)
                    )
                    drawLine(
                        color = Color(0xFF15803D),
                        start = Offset(cx, h * 0.12f),
                        end = Offset(cx, h * 0.30f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF15803D),
                        start = Offset(cx - w * 0.18f, h * 0.26f),
                        end = Offset(cx + w * 0.18f, h * 0.26f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                }

                "onion" -> {
                    drawLine(
                        color = Color(0xFF16A34A),
                        start = Offset(cx, h * 0.08f),
                        end = Offset(cx, h * 0.28f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawOval(
                        color = Color(0xFF7E22CE),
                        topLeft = Offset(w * 0.16f, h * 0.24f),
                        size = Size(w * 0.68f, h * 0.66f)
                    )
                    drawOval(
                        color = Color(0xFFA855F7).copy(alpha = 0.45f),
                        topLeft = Offset(w * 0.29f, h * 0.28f),
                        size = Size(w * 0.42f, h * 0.58f)
                    )
                }

                "black_pepper" -> {
                    drawLine(
                        color = Color(0xFF166534),
                        start = Offset(w * 0.28f, h * 0.15f),
                        end = Offset(w * 0.72f, h * 0.85f),
                        strokeWidth = w * 0.07f,
                        cap = StrokeCap.Round
                    )
                    listOf(
                        Offset(0.32f, 0.28f), Offset(0.48f, 0.26f),
                        Offset(0.38f, 0.46f), Offset(0.58f, 0.44f),
                        Offset(0.48f, 0.65f), Offset(0.66f, 0.64f),
                        Offset(0.64f, 0.82f)
                    ).forEach { (rx, ry) ->
                        drawCircle(
                            color = Color(0xFF1F2937),
                            radius = w * 0.11f,
                            center = Offset(w * rx, h * ry)
                        )
                        drawCircle(
                            color = Color(0xFF9CA3AF).copy(alpha = 0.4f),
                            radius = w * 0.03f,
                            center = Offset(w * rx - w * 0.03f, h * ry - h * 0.03f)
                        )
                    }
                }

                "coconut" -> {
                    drawCircle(
                        color = Color(0xFF15803D),
                        radius = w * 0.36f,
                        center = Offset(cx - w * 0.06f, cy)
                    )
                    drawCircle(
                        color = Color(0xFF78350F),
                        radius = w * 0.24f,
                        center = Offset(cx + w * 0.14f, cy + h * 0.12f)
                    )
                    drawCircle(
                        color = Color(0xFFFFFBEB),
                        radius = w * 0.18f,
                        center = Offset(cx + w * 0.14f, cy + h * 0.12f)
                    )
                }

                "turmeric" -> {
                    drawRoundRect(
                        color = Color(0xFFEAB308),
                        topLeft = Offset(w * 0.14f, h * 0.40f),
                        size = Size(w * 0.72f, h * 0.26f),
                        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
                    )
                    drawRoundRect(
                        color = Color(0xFFCA8A04),
                        topLeft = Offset(w * 0.32f, h * 0.22f),
                        size = Size(w * 0.34f, h * 0.56f),
                        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
                    )
                }

                else -> {
                    val bract = Color(0xFF78350F)
                    drawLine(bract, Offset(cx, h * 0.88f), Offset(cx, h * 0.58f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                    drawCircle(Color(0xFFFFFFFF), radius = w * 0.21f, center = Offset(cx - w * 0.16f, cy))
                    drawCircle(Color(0xFFF3F4F6), radius = w * 0.21f, center = Offset(cx + w * 0.16f, cy))
                    drawCircle(Color(0xFFFFFFFF), radius = w * 0.22f, center = Offset(cx, cy - h * 0.16f))
                    drawCircle(Color(0xFFE5E7EB), radius = w * 0.22f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
                }
            }
        }
    }
}
