package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.RankCatalog
import com.example.model.RankTier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bingkai foto profil 10 tingkat ala Mobile Legends yang semakin mewah, bersayap,
 * bermahkota, dan bercahaya animatif seiring naiknya Level Status Akun (Lv.1 - Lv.10).
 */
@Composable
fun RankAvatarFrame(
    tier: RankTier,
    avatarBgColor: Color,
    size: Dp = 68.dp,
    showLevelBadge: Boolean = true,
    useCommanderPhoto: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rankFrameAnim")
    val animAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (tier.level >= 9) 3400 else 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rankOrbit"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rankPulse"
    )

    val boxModifier = modifier
        .size(size)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.5f
            val cy = h * 0.5f
            val baseR = minOf(w, h) * 0.34f
            val lv = tier.level

            // 1. Halo / Aura cahaya belakang untuk Level 3+
            if (lv >= 3) {
                val auraRadius = baseR * (1.28f + lv * 0.03f) * (if (lv >= 7) pulseGlow else 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tier.frameGlow.copy(alpha = (0.20f + lv * 0.045f).coerceAtMost(0.65f)),
                            tier.frameSecondary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = auraRadius
                    ),
                    radius = auraRadius,
                    center = Offset(cx, cy)
                )
            }

            // 2. Sinar bintang berputar di belakang (Level 8–10)
            if (lv >= 8) {
                val rayCount = if (lv == 10) 12 else if (lv == 9) 8 else 6
                val rayOuter = baseR * (1.36f + (lv - 7) * 0.05f)
                val rayInner = baseR * 0.96f
                for (i in 0 until rayCount) {
                    val deg = animAngle + (360f / rayCount) * i
                    val rad = (deg * PI / 180.0).toFloat()
                    val sx = cx + cos(rad) * rayInner
                    val sy = cy + sin(rad) * rayInner
                    val ex = cx + cos(rad) * rayOuter
                    val ey = cy + sin(rad) * rayOuter
                    drawLine(
                        color = tier.gemColor.copy(alpha = if (lv == 10) 0.58f else 0.36f),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = if (lv == 10) 2.6f else 1.8f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Sayap Taktis & Kerajaan di kiri-kanan (Level 4–10)
            if (lv >= 4) {
                val wingSpan = baseR * (1.24f + (lv - 3) * 0.04f)
                val featherLayers = when {
                    lv >= 9 -> 3
                    lv >= 6 -> 2
                    else -> 1
                }
                for (layer in 0 until featherLayers) {
                    val yOff = (layer - 1) * baseR * 0.18f
                    val reach = wingSpan - layer * baseR * 0.10f
                    val wingBrush = Brush.linearGradient(
                        colors = listOf(tier.frameSecondary, tier.framePrimary)
                    )
                    // Sayap kiri
                    val leftWing = Path().apply {
                        moveTo(cx - baseR * 0.88f, cy + yOff)
                        lineTo(cx - reach, cy - baseR * 0.34f + yOff)
                        lineTo(cx - reach * 0.84f, cy + baseR * 0.14f + yOff)
                        close()
                    }
                    drawPath(leftWing, brush = wingBrush)
                    drawPath(leftWing, color = tier.gemColor.copy(alpha = 0.65f), style = Stroke(width = 1.1f))

                    // Sayap kanan
                    val rightWing = Path().apply {
                        moveTo(cx + baseR * 0.88f, cy + yOff)
                        lineTo(cx + reach, cy - baseR * 0.34f + yOff)
                        lineTo(cx + reach * 0.84f, cy + baseR * 0.14f + yOff)
                        close()
                    }
                    drawPath(rightWing, brush = wingBrush)
                    drawPath(rightWing, color = tier.gemColor.copy(alpha = 0.65f), style = Stroke(width = 1.1f))
                }
            }

            // 4. Cincin Bingkai Metalik Utama (semakin tebal & ganda di level tinggi)
            val ringR = baseR * 1.03f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        tier.framePrimary,
                        tier.frameSecondary,
                        tier.gemColor,
                        tier.framePrimary,
                        tier.frameSecondary,
                        tier.framePrimary
                    ),
                    center = Offset(cx, cy)
                ),
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = (3.4f + lv * 0.45f))
            )

            // Cincin luar berlian/ornamen (Level 2+)
            if (lv >= 2) {
                drawCircle(
                    color = tier.frameSecondary.copy(alpha = 0.82f),
                    radius = ringR + baseR * 0.11f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.6f)
                )
            }

            // 5. Segi-segien / Studs sudut (Level 3+)
            if (lv >= 3) {
                val studCount = if (lv >= 7) 8 else 4
                val studR = ringR + baseR * 0.11f
                for (i in 0 until studCount) {
                    val deg = (i * 360f / studCount) + (if (lv >= 7) animAngle * 0.35f else 45f)
                    val rad = (deg * PI / 180.0).toFloat()
                    val sx = cx + cos(rad) * studR
                    val sy = cy + sin(rad) * studR
                    drawCircle(
                        color = tier.gemColor,
                        radius = if (lv >= 9) 3.2f else 2.2f,
                        center = Offset(sx, sy)
                    )
                }
            }

            // 6. Mahkota Puncak / Crest (Level 5–10)
            if (lv >= 5) {
                val crownY = cy - ringR - baseR * 0.12f
                val crownW = baseR * (0.36f + (lv - 5) * 0.05f)
                val crownH = baseR * (0.26f + (lv - 5) * 0.04f)
                val crownPath = Path().apply {
                    moveTo(cx - crownW, crownY + crownH * 0.45f)
                    lineTo(cx - crownW * 0.65f, crownY - crownH * 0.45f)
                    lineTo(cx - crownW * 0.25f, crownY + crownH * 0.05f)
                    lineTo(cx, crownY - crownH * 0.85f)
                    lineTo(cx + crownW * 0.25f, crownY + crownH * 0.05f)
                    lineTo(cx + crownW * 0.65f, crownY - crownH * 0.45f)
                    lineTo(cx + crownW, crownY + crownH * 0.45f)
                    close()
                }
                drawPath(
                    crownPath,
                    brush = Brush.verticalGradient(listOf(tier.gemColor, tier.frameSecondary, tier.framePrimary))
                )
                drawPath(crownPath, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.0f))
                drawCircle(
                    color = if (lv >= 9) Color(0xFFFDE047) else Color(0xFF38BDF8),
                    radius = if (lv >= 9) 3.5f else 2.5f,
                    center = Offset(cx, crownY - crownH * 0.15f)
                )
            }

            // 7. Partikel orbit bercahaya untuk Mythic+ (Level 7–10)
            if (lv >= 7) {
                val orbCount = lv - 5
                val orbR = ringR + baseR * 0.22f
                for (i in 0 until orbCount) {
                    val deg = animAngle + i * (360f / orbCount)
                    val rad = (deg * PI / 180.0).toFloat()
                    val ox = cx + cos(rad) * orbR
                    val oy = cy + sin(rad) * orbR
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 2.4f, center = Offset(ox, oy))
                    drawCircle(color = tier.frameGlow.copy(alpha = 0.45f), radius = 5.0f, center = Offset(ox, oy))
                }
            }
        }

        // Foto Profil / Emblem Petugas di tengah bingkai
        val innerSize = size * 0.64f
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (useCommanderPhoto) {
                Image(
                    painter = painterResource(id = R.drawable.img_gandi_avatar),
                    contentDescription = "Foto Profil ${tier.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = tier.title,
                    tint = Color.White,
                    modifier = Modifier.size(innerSize * 0.56f)
                )
            }
        }

        // Lencana Level di bagian bawah bingkai
        if (showLevelBadge) {
            Surface(
                color = tier.framePrimary,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, tier.gemColor),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
            ) {
                Text(
                    text = "LV.${tier.level}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Dialog Roadmap 10 Tingkat Status Akun & Preview Bingkai Profil Mewah.
 */
@Composable
fun RankRoadmapDialog(
    currentStars: Long,
    onDismiss: () -> Unit
) {
    val currentTier = RankCatalog.tierForStars(currentStars)
    val progress = RankCatalog.progressInTier(currentStars)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF50B1325),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, currentTier.frameSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .testTag("rank_roadmap_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = currentTier.frameSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "10 TINGKAT STATUS AKUN & BINGKAI",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF93C5FD),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Level ${currentTier.level} • ${currentTier.title} ($currentStars ★)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                // Bar Progress Bintang saat ini
                Surface(
                    color = Color(0xCC111C33),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Push Mode RANK untuk +1 ★ tiap kemenangan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = currentTier.nextLevelStars?.let { "$currentStars / $it ★" } ?: "MAX LEVEL ($currentStars ★)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF60A5FA),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color(0xFF3B82F6),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RankCatalog.tiers, key = { it.level }) { tier ->
                        val unlocked = currentStars >= tier.minStars
                        val isCurrent = tier.level == currentTier.level
                        Surface(
                            color = when {
                                isCurrent -> Color(0xFF172554)
                                unlocked -> Color(0xCC131E34)
                                else -> Color(0x880B1120)
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isCurrent) 1.6.dp else 1.dp,
                                color = if (isCurrent) tier.frameSecondary else if (unlocked) Color(0xFF334155) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RankAvatarFrame(
                                    tier = tier,
                                    avatarBgColor = tier.framePrimary,
                                    size = 56.dp,
                                    showLevelBadge = true
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Lv.${tier.level} • ${tier.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (unlocked) Color.White else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (isCurrent) {
                                            Surface(
                                                color = Color(0xFF2563EB),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "AKTIF",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${tier.divisionTag} • ${tier.frameStyleName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tier.frameSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (!unlocked) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Terkunci",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Terbuka",
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "${tier.minStars}★",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (unlocked) Color(0xFF93C5FD) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
