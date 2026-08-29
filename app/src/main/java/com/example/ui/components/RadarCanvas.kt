package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CivicNotice
import com.example.model.NoticeCategory
import com.example.model.NoticeStatus
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RadarVisualizer(
    pincode: String,
    locality: String,
    notices: List<CivicNotice>,
    onNoticeClick: (CivicNotice) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("radar_visualizer_card"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE IMPACT RADAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Pincode $pincode • 3km Radius",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Canvas Radar
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A1128)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = min(size.width, size.height) / 2 - 16.dp.toPx()

                    // Concentric Range Rings (500m, 1.5km, 3km)
                    val ringFractions = listOf(0.33f, 0.66f, 1.0f)
                    val ringLabels = listOf("500m", "1.5km", "3.0km")

                    ringFractions.forEachIndexed { index, fraction ->
                        val r = maxRadius * fraction
                        drawCircle(
                            color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            radius = r,
                            center = center,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }

                    // Crosshair grid lines
                    drawLine(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Expanding Pulse Wave
                    drawCircle(
                        color = Color(0xFF0D9488).copy(alpha = (1f - pulseRadius) * 0.4f),
                        radius = maxRadius * pulseRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Rotating Radar Sweep line
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        (center.x + maxRadius * cos(sweepRad)).toFloat(),
                        (center.y + maxRadius * sin(sweepRad)).toFloat()
                    )
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2DD4BF), Color(0xFF0D9488).copy(alpha = 0.1f)),
                            start = center,
                            end = sweepEnd
                        ),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2.5.dp.toPx()
                    )

                    // Plot Civic Notice Blips
                    notices.take(12).forEach { notice ->
                        // Calculate position based on distance and angle
                        val distRatio = (notice.distanceKmFromCenter / 3.0).coerceIn(0.15, 0.95).toFloat()
                        val blipR = maxRadius * distRatio
                        val blipAngleRad = Math.toRadians(notice.angleDegree.toDouble())
                        val blipX = (center.x + blipR * cos(blipAngleRad)).toFloat()
                        val blipY = (center.y + blipR * sin(blipAngleRad)).toFloat()
                        val blipPos = Offset(blipX, blipY)

                        val blipColor = when {
                            notice.status == NoticeStatus.OBJECTION_OPEN -> Color(0xFFF43F5E) // Red urgent
                            notice.category == NoticeCategory.ENVIRONMENT -> Color(0xFF10B981) // Green
                            notice.category == NoticeCategory.TRANSPORT_METRO -> Color(0xFF0284C7) // Sky
                            notice.category == NoticeCategory.RERA_REALESTATE -> Color(0xFF8B5CF6) // Purple
                            else -> Color(0xFFF59E0B) // Amber
                        }

                        // Outer halo
                        drawCircle(
                            color = blipColor.copy(alpha = 0.35f),
                            radius = 10.dp.toPx(),
                            center = blipPos
                        )
                        // Inner dot
                        drawCircle(
                            color = blipColor,
                            radius = 4.5.dp.toPx(),
                            center = blipPos
                        )
                    }

                    // Center User Locality Pin
                    drawCircle(
                        color = Color(0xFF2DD4BF),
                        radius = 6.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = center
                    )
                }

                // Overlay interactive chips for top 3 closest notices around radar
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "My Locality Center",
                        tint = Color(0xFF2DD4BF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFFF43F5E), text = "Objection Open")
                LegendItem(color = Color(0xFF10B981), text = "Parivesh/EIA")
                LegendItem(color = Color(0xFF0284C7), text = "Metro/PWD")
                LegendItem(color = Color(0xFF8B5CF6), text = "RERA Tower")
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${notices.size} projects tracked in $locality perimeter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
