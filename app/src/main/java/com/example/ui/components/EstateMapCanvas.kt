package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.GateStatus
import com.example.model.PatrolRoom
import com.example.model.PingType
import com.example.model.PlayerRole
import com.example.model.RoomPlayer
import com.example.model.SectorKind
import com.example.model.TacticalPing
import com.example.model.UniformCatalog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun EstateMapCanvas(
    room: PatrolRoom,
    players: List<RoomPlayer>,
    pings: List<TacticalPing>,
    currentUserId: String,
    isPingMode: Boolean,
    onMapTap: (EstateSector, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPhase"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("estate_map_canvas")
                .pointerInput(isPingMode) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && size.height > 0) {
                            val nx = (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                            val ny = (offset.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                            val nearest = EstateWorld.nearestTo(nx, ny)
                            onMapTap(nearest, nx, ny)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas

            // 1. Estate Base Ground
            drawRoundRect(
                color = Color(0xFF0A111F),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Subtle tactical coordinate grid
            val gridColor = Color(0xFF162238)
            for (i in 1..7) {
                val gx = w * (i / 8f)
                val gy = h * (i / 8f)
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            }

            // 2. Perimeter Wall
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(w * 0.03f, h * 0.04f),
                size = Size(w * 0.94f, h * 0.92f),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 4f)
            )

            // 3. Paved Residential Streets
            drawStreets(w, h, textMeasurer)

            // 4. Open Plaza & Mosque Courtyard (Area Masjid Agung Selaras)
            drawParks(w, h, textMeasurer)

            // 5. Residential Complexes (Krisan, Cempaka, Cemara, Akasia, Camelia, Flamboyan)
            drawBlocks(w, h, textMeasurer)

            // 6. North Main Gate & South Service Portal
            drawGates(
                w = w,
                h = h,
                northStatus = GateStatus.safeValueOf(room.northGateStatus),
                southStatus = GateStatus.safeValueOf(room.southGateStatus),
                textMeasurer = textMeasurer
            )

            // 7. Radar Sweep Pulse from Central Crosswalk
            val radarCenter = Offset(w * 0.50f, h * 0.50f)
            val maxRadius = minOf(w, h) * 0.48f
            drawCircle(
                color = Color(0xFF06B6D4).copy(alpha = (1f - radarPhase) * 0.22f),
                radius = maxRadius * radarPhase,
                center = radarCenter,
                style = Stroke(width = 2.5f)
            )

            // 8. Sector Hotspots (subtle node markers)
            EstateWorld.sectors.forEach { sector ->
                val cx = w * sector.normX
                val cy = h * sector.normY
                drawCircle(
                    color = Color(0xFF64748B).copy(alpha = 0.45f),
                    radius = 6f,
                    center = Offset(cx, cy)
                )
            }

            // 9. Tactical Location Pings
            pings.take(6).forEach { ping ->
                val px = w * ping.posX.toFloat()
                val py = h * ping.posY.toFloat()
                val pingColor = when (PingType.safeValueOf(ping.pingType)) {
                    PingType.SUSPECT -> Color(0xFFEF4444)
                    PingType.BACKUP -> Color(0xFF3B82F6)
                    PingType.BLOCK_GATE -> Color(0xFF06B6D4)
                    PingType.CLEAR -> Color(0xFF10B981)
                }
                drawCircle(
                    color = pingColor.copy(alpha = 0.26f),
                    radius = 22f * pulseScale,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = pingColor,
                    radius = 8f,
                    center = Offset(px, py),
                    style = Stroke(width = 3f)
                )
            }

            // 10. Active Intruder / Thief Marker
            if (room.thiefSpotted) {
                val tx = w * room.activeThiefX.toFloat()
                val ty = h * room.activeThiefY.toFloat()
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.28f),
                    radius = 28f * pulseScale,
                    center = Offset(tx, ty)
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 13f,
                    center = Offset(tx, ty)
                )
                drawCircle(
                    color = Color.White,
                    radius = 13f,
                    center = Offset(tx, ty),
                    style = Stroke(width = 2.5f)
                )
                val labelResult = textMeasurer.measure(
                    text = "PENYUSUP",
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color(0xFFFCA5A5)
                    )
                )
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(
                        (tx - labelResult.size.width / 2f).coerceIn(8f, (w - labelResult.size.width - 8f).coerceAtLeast(8f)),
                        (ty - 32f).coerceAtLeast(8f)
                    )
                )
            }

            // 11. Live Multiplayer Guard Tokens
            players.forEach { player ->
                val gx = w * player.posX.toFloat()
                val gy = h * player.posY.toFloat()
                val uniform = UniformCatalog.byId(player.uniformId)
                val isMe = player.userId == currentUserId
                val isThiefRole = player.role == PlayerRole.THIEF.name
                val tokenColor = if (isThiefRole) Color(0xFFEF4444) else uniform.primaryColor
                val ringColor = if (player.pttActive) Color(0xFF22D3EE) else uniform.trimColor

                if (player.pttActive) {
                    drawCircle(
                        color = Color(0xFF22D3EE).copy(alpha = 0.35f),
                        radius = 26f * pulseScale,
                        center = Offset(gx, gy)
                    )
                }
                if (isMe) {
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.40f),
                        radius = 22f,
                        center = Offset(gx, gy),
                        style = Stroke(width = 2.5f)
                    )
                }
                drawCircle(
                    color = tokenColor,
                    radius = if (isMe) 14f else 11f,
                    center = Offset(gx, gy)
                )
                drawCircle(
                    color = ringColor,
                    radius = if (isMe) 14f else 11f,
                    center = Offset(gx, gy),
                    style = Stroke(width = 2.8f)
                )

                val tag = if (isMe) "ANDA" else player.displayName.take(8)
                val tagLayout = textMeasurer.measure(
                    text = tag,
                    style = TextStyle(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isMe) Color(0xFF60A5FA) else Color(0xFFE2E8F0)
                    )
                )
                drawText(
                    textLayoutResult = tagLayout,
                    topLeft = Offset(
                        (gx - tagLayout.size.width / 2f).coerceIn(6f, (w - tagLayout.size.width - 6f).coerceAtLeast(6f)),
                        (gy + 16f).coerceIn(6f, (h - tagLayout.size.height - 6f).coerceAtLeast(6f))
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawStreets(w: Float, h: Float, textMeasurer: TextMeasurer) {
    val roadColor = Color(0xFF1E293B)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
    val stripeColor = Color(0xFF475569)

    drawRect(roadColor, Offset(w * 0.06f, h * 0.265f), Size(w * 0.88f, h * 0.06f))
    drawRect(roadColor, Offset(w * 0.06f, h * 0.675f), Size(w * 0.88f, h * 0.06f))
    drawRect(roadColor, Offset(w * 0.06f, h * 0.07f), Size(w * 0.045f, h * 0.86f))
    drawRect(roadColor, Offset(w * 0.895f, h * 0.07f), Size(w * 0.045f, h * 0.86f))
    drawRect(roadColor, Offset(w * 0.31f, h * 0.07f), Size(w * 0.06f, h * 0.86f))
    drawRect(roadColor, Offset(w * 0.63f, h * 0.07f), Size(w * 0.06f, h * 0.86f))
    drawRect(roadColor, Offset(w * 0.46f, h * 0.06f), Size(w * 0.08f, h * 0.88f))
    drawRect(roadColor, Offset(w * 0.26f, h * 0.47f), Size(w * 0.48f, h * 0.06f))

    drawLine(stripeColor, Offset(w * 0.08f, h * 0.295f), Offset(w * 0.92f, h * 0.295f), 2f, pathEffect = dashEffect)
    drawLine(stripeColor, Offset(w * 0.08f, h * 0.705f), Offset(w * 0.92f, h * 0.705f), 2f, pathEffect = dashEffect)
    drawLine(stripeColor, Offset(w * 0.50f, h * 0.08f), Offset(w * 0.50f, h * 0.92f), 2f, pathEffect = dashEffect)

    val streetStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 8.sp,
        color = Color(0xFF94A3B8)
    )
    drawText(textMeasurer.measure("BUMI PAPAN SELARAS", streetStyle), topLeft = Offset(w * 0.56f, h * 0.282f))
}

private fun DrawScope.drawParks(w: Float, h: Float, textMeasurer: TextMeasurer) {
    drawRoundRect(
        color = Color(0xFF065F46).copy(alpha = 0.68f),
        topLeft = Offset(w * 0.40f, h * 0.40f),
        size = Size(w * 0.20f, h * 0.20f),
        cornerRadius = CornerRadius(18f, 18f)
    )
    drawRoundRect(
        color = Color(0xFF38BDF8).copy(alpha = 0.65f),
        topLeft = Offset(w * 0.44f, h * 0.44f),
        size = Size(w * 0.12f, h * 0.12f),
        cornerRadius = CornerRadius(24f, 24f),
        style = Stroke(width = 2.5f)
    )
    val label = textMeasurer.measure(
        "AREA MASJID",
        TextStyle(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color(0xFF60A5FA))
    )
    drawText(label, topLeft = Offset(w * 0.50f - label.size.width / 2f, h * 0.575f))
}

private fun DrawScope.drawBlocks(w: Float, h: Float, textMeasurer: TextMeasurer) {
    val style = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 8.sp,
        color = Color(0xFFE2E8F0)
    )

    EstateWorld.allBuildings.filter { !it.isMosque }.forEach { b ->
        val topLeft = Offset(b.minX * w, b.minY * h)
        val bSize = Size((b.maxX - b.minX) * w, (b.maxY - b.minY) * h)
        drawRoundRect(
            color = if (b.isPrison) Color(0xFF1E3A8A) else b.roofColor,
            topLeft = topLeft,
            size = bSize,
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = b.accentColor.copy(alpha = if (b.isPrison) 0.9f else 0.55f),
            topLeft = topLeft,
            size = bSize,
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = if (b.isPrison) 2.5f else 2f)
        )
        val measured = textMeasurer.measure(
            if (b.isPrison) "PENJARA" else b.label.removePrefix("BLOK "),
            style
        )
        drawText(
            measured,
            topLeft = Offset(
                topLeft.x + (bSize.width - measured.size.width) / 2f,
                topLeft.y + (bSize.height - measured.size.height) / 2f
            )
        )
    }
    EstateWorld.defaultVehicles.forEach { v ->
        drawCircle(v.accentColor.copy(alpha = 0.85f), radius = 5.5f, center = Offset(v.x * w, v.y * h))
    }
}

private fun DrawScope.drawGates(
    w: Float,
    h: Float,
    northStatus: GateStatus,
    southStatus: GateStatus,
    textMeasurer: TextMeasurer
) {
    fun gateColor(status: GateStatus): Color = when (status) {
        GateStatus.OPEN -> Color(0xFF10B981)
        GateStatus.ALERT -> Color(0xFF3B82F6)
        GateStatus.LOCKED -> Color(0xFFEF4444)
    }

    val gateSize = Size(w * 0.26f, h * 0.055f)
    val nTopLeft = Offset(w * 0.37f, h * 0.05f)
    val sTopLeft = Offset(w * 0.37f, h * 0.895f)

    listOf(
        Triple("U: ${northStatus.labelId}", nTopLeft, gateColor(northStatus)),
        Triple("S: ${southStatus.labelId}", sTopLeft, gateColor(southStatus))
    ).forEach { (label, topLeft, color) ->
        drawRoundRect(
            color = color.copy(alpha = 0.24f),
            topLeft = topLeft,
            size = gateSize,
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = gateSize,
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2.5f)
        )
        val measured = textMeasurer.measure(
            label,
            TextStyle(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
        )
        drawText(
            measured,
            topLeft = Offset(
                topLeft.x + (gateSize.width - measured.size.width) / 2f,
                topLeft.y + (gateSize.height - measured.size.height) / 2f
            )
        )
    }
}
