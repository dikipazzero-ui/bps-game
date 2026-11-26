package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.AttributeItem
import com.example.model.AttributeStyle
import com.example.model.UniformItem
import com.example.model.WeaponItem
import com.example.model.WeaponStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Character3DPreviewStage(
    uniform: UniformItem,
    weapon: WeaponItem,
    attribute: AttributeItem,
    modifier: Modifier = Modifier
) {
    var manualYaw by remember { mutableFloatStateOf(0.35f) }
    val infinite = rememberInfiniteTransition(label = "turntable")
    val idleBob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idleBob"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    manualYaw += dragAmount * 0.012f
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f
            val baseY = h * 0.84f
            val scale = minOf(w, h) / 210f

            // Studio spotlight backdrop
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        uniform.primaryColor.copy(alpha = 0.28f),
                        Color(0xFF0A111F)
                    ),
                    center = Offset(cx, h * 0.45f),
                    radius = maxOf(w, h) * 0.72f
                )
            )

            // 3D Turntable pedestal ellipses
            val pedW = 130f * scale
            val pedH = 32f * scale
            drawOval(
                color = uniform.trimColor.copy(alpha = 0.22f),
                topLeft = Offset(cx - pedW / 2f, baseY - pedH / 2f),
                size = Size(pedW, pedH)
            )
            drawOval(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx - pedW * 0.44f, baseY - pedH * 0.44f),
                size = Size(pedW * 0.88f, pedH * 0.88f)
            )
            drawOval(
                color = uniform.trimColor.copy(alpha = 0.85f),
                topLeft = Offset(cx - pedW * 0.44f, baseY - pedH * 0.44f),
                size = Size(pedW * 0.88f, pedH * 0.88f),
                style = Stroke(width = 2.5f * scale)
            )

            // Rotating angle helpers
            val angle = manualYaw + sin(idleBob * 0.5f) * 0.12f
            val cosY = cos(angle)
            val sinY = sin(angle)
            val bobY = sin(idleBob) * 3f * scale

            fun px(x: Float, z: Float): Float = cx + (x * cosY - z * sinY) * scale
            fun py(y: Float, z: Float): Float = baseY - y * scale + z * 0.18f * scale + bobY

            // Cape / High Collar (behind torso for Armor Lv 4-5)
            if (uniform.hasCapeOrCollar && cosY > -0.6f) {
                val capePath = Path().apply {
                    moveTo(px(-18f, -6f), py(92f, -6f))
                    lineTo(px(18f, -6f), py(92f, -6f))
                    lineTo(px(26f, -14f), py(32f, -14f))
                    lineTo(px(-26f, -14f), py(32f, -14f))
                    close()
                }
                drawPath(capePath, uniform.primaryColor.copy(alpha = 0.72f))
                drawPath(capePath, uniform.trimColor.copy(alpha = 0.65f), style = Stroke(width = 1.8f * scale))
            }

            // Legs & Boots
            val bootColor = if (attribute.style == AttributeStyle.SPEED_BOOTS) attribute.primaryColor else Color(0xFF0F172A)
            val bootTrim = if (attribute.style == AttributeStyle.SPEED_BOOTS) attribute.accentColor else Color(0xFF475569)
            listOf(-10f, 10f).forEach { legX ->
                val hip = Offset(px(legX, 0f), py(52f, 0f))
                val foot = Offset(px(legX * 1.05f, 3f), py(6f, 3f))
                drawLine(Color(0xFF1E293B), hip, foot, strokeWidth = 11f * scale, cap = StrokeCap.Round)
                if (uniform.armorLevel >= 3) {
                    // Knee armor pads for Armor Level 3+
                    val knee = Offset((hip.x + foot.x) * 0.5f, (hip.y + foot.y) * 0.5f)
                    drawCircle(uniform.trimColor.copy(alpha = 0.85f), radius = 4.2f * scale, center = knee)
                }
                drawRoundRect(
                    color = bootColor,
                    topLeft = Offset(foot.x - 7f * scale, foot.y - 6f * scale),
                    size = Size(14f * scale, 10f * scale),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )
                drawLine(
                    bootTrim,
                    Offset(foot.x - 6f * scale, foot.y - 2f * scale),
                    Offset(foot.x + 6f * scale, foot.y - 2f * scale),
                    strokeWidth = 2.2f * scale
                )
            }

            // Torso (3D Jacket / Premium Armor)
            val torsoTopLeft = Offset(px(-19f, 0f), py(94f, 0f))
            val torsoW = kotlin.math.abs(px(19f, 0f) - px(-19f, 0f)).coerceAtLeast(18f * scale)
            val torsoH = 42f * scale
            drawRoundRect(
                color = uniform.primaryColor,
                topLeft = Offset(cx - torsoW / 2f, torsoTopLeft.y),
                size = Size(torsoW, torsoH),
                cornerRadius = CornerRadius(8f * scale, 8f * scale)
            )
            // Armor chest plates scaling with armorLevel (Lv 2+)
            if (uniform.armorLevel >= 2) {
                val plateW = torsoW * (0.68f + uniform.armorLevel * 0.04f)
                drawRoundRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.55f),
                    topLeft = Offset(cx - plateW / 2f, py(90f, 6f)),
                    size = Size(plateW, 24f * scale),
                    cornerRadius = CornerRadius(5f * scale, 5f * scale)
                )
                drawRoundRect(
                    color = uniform.trimColor.copy(alpha = 0.78f),
                    topLeft = Offset(cx - plateW / 2f, py(90f, 6f)),
                    size = Size(plateW, 24f * scale),
                    cornerRadius = CornerRadius(5f * scale, 5f * scale),
                    style = Stroke(width = (1.2f + uniform.armorLevel * 0.35f) * scale)
                )
            }

            // Uniform center zip & trim stripe
            val chestCenterX = px(0f, 9f)
            drawLine(
                color = uniform.trimColor,
                start = Offset(chestCenterX, py(91f, 9f)),
                end = Offset(chestCenterX, py(56f, 9f)),
                strokeWidth = 2.4f * scale
            )

            // Attribute: Kevlar Vest overlay
            if (attribute.style == AttributeStyle.KEVLAR_VEST) {
                val vestW = torsoW * 0.86f
                drawRoundRect(
                    color = attribute.primaryColor,
                    topLeft = Offset(cx - vestW / 2f, py(90f, 9f)),
                    size = Size(vestW, 28f * scale),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale)
                )
                drawRoundRect(
                    color = attribute.accentColor,
                    topLeft = Offset(cx - vestW / 2f, py(90f, 9f)),
                    size = Size(vestW, 28f * scale),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale),
                    style = Stroke(width = 1.8f * scale)
                )
            }

            // Utility Belt
            val beltColor = if (attribute.style == AttributeStyle.BELT) attribute.accentColor else uniform.trimColor
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - torsoW * 0.52f, py(57f, 6f)),
                size = Size(torsoW * 1.04f, 7f * scale),
                cornerRadius = CornerRadius(3f * scale, 3f * scale)
            )
            drawCircle(beltColor, radius = 4f * scale, center = Offset(px(0f, 10f), py(54f, 10f)))

            // Shoulder Pads for Armor Level 2+
            if (uniform.hasShoulderPads) {
                val padR = (6.5f + uniform.armorLevel * 0.7f) * scale
                listOf(-20f, 20f).forEach { sx ->
                    drawCircle(
                        color = uniform.trimColor,
                        radius = padR,
                        center = Offset(px(sx, 0f), py(91f, 0f))
                    )
                    drawCircle(
                        color = uniform.primaryColor,
                        radius = padR * 0.65f,
                        center = Offset(px(sx, 0f), py(91f, 0f))
                    )
                }
            }

            // Left Arm
            val lShoulder = Offset(px(-21f, 0f), py(89f, 0f))
            val lHand = Offset(px(-25f, 4f), py(61f, 4f))
            drawLine(uniform.primaryColor, lShoulder, lHand, strokeWidth = 9f * scale, cap = StrokeCap.Round)
            drawCircle(Color(0xFFF1C27D), radius = 4.5f * scale, center = lHand)

            // Head & Tactical Helmet / Cap
            val headCenter = Offset(px(0f, 0f), py(110f, 0f))
            drawCircle(Color(0xFFF1C27D), radius = 14f * scale, center = headCenter)
            drawArc(
                color = if (uniform.armorLevel >= 4) Color(0xFF1E293B) else Color(0xFF0F172A),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - 14.5f * scale, headCenter.y - 16f * scale),
                size = Size(29f * scale, 22f * scale)
            )
            if (uniform.armorLevel >= 4) {
                // Helmet trim crest
                drawLine(
                    uniform.trimColor,
                    Offset(headCenter.x - 12f * scale, headCenter.y - 7f * scale),
                    Offset(headCenter.x + 12f * scale, headCenter.y - 7f * scale),
                    strokeWidth = 2.5f * scale
                )
            }
            // Visor / NVG Goggles Attribute
            if (attribute.style == AttributeStyle.NVG_GOGGLES) {
                val eyeCenter = Offset(px(0f, 12f), headCenter.y - 1f * scale)
                drawRoundRect(
                    color = attribute.primaryColor,
                    topLeft = Offset(eyeCenter.x - 12f * scale, eyeCenter.y - 4.5f * scale),
                    size = Size(24f * scale, 9f * scale),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )
                drawCircle(attribute.accentColor, radius = 3.2f * scale, center = Offset(eyeCenter.x - 5f * scale, eyeCenter.y))
                drawCircle(attribute.accentColor, radius = 3.2f * scale, center = Offset(eyeCenter.x + 5f * scale, eyeCenter.y))
            } else if (cosY > -0.3f) {
                val eyeX = px(0f, 12f)
                drawCircle(Color(0xFF0F172A), radius = 2f * scale, center = Offset(eyeX - 4f * scale, headCenter.y))
                drawCircle(Color(0xFF0F172A), radius = 2f * scale, center = Offset(eyeX + 4f * scale, headCenter.y))
            }

            // Right Arm + HELD WEAPON in hand!
            val rShoulder = Offset(px(21f, 0f), py(89f, 0f))
            val rHand = Offset(px(26f, 14f), py(68f, 14f))
            drawLine(uniform.primaryColor, rShoulder, rHand, strokeWidth = 9f * scale, cap = StrokeCap.Round)
            drawCircle(Color(0xFFF1C27D), radius = 4.8f * scale, center = rHand)
            drawHeldWeaponPreview(weapon, rHand, scale, cosY)

            // Hovering Scanner Drone Attribute
            if (attribute.style == AttributeStyle.SCANNER_DRONE) {
                val dronePos = Offset(px(-30f, -8f), py(126f, -8f) + sin(idleBob * 1.8f) * 4f * scale)
                drawLine(
                    attribute.accentColor.copy(alpha = 0.35f),
                    dronePos,
                    Offset(dronePos.x, dronePos.y + 18f * scale),
                    strokeWidth = 2f * scale
                )
                drawRoundRect(
                    color = attribute.primaryColor,
                    topLeft = Offset(dronePos.x - 10f * scale, dronePos.y - 6f * scale),
                    size = Size(20f * scale, 11f * scale),
                    cornerRadius = CornerRadius(5f * scale, 5f * scale)
                )
                drawCircle(attribute.accentColor, radius = 3.5f * scale, center = dronePos)
                drawLine(
                    Color(0xFFE2E8F0),
                    Offset(dronePos.x - 14f * scale, dronePos.y - 7f * scale),
                    Offset(dronePos.x + 14f * scale, dronePos.y - 7f * scale),
                    strokeWidth = 2f * scale
                )
            }
        }
    }
}

private fun DrawScope.drawHeldWeaponPreview(
    weapon: WeaponItem,
    handPos: Offset,
    scale: Float,
    cosY: Float
) {
    val dir = if (cosY >= 0f) 1f else -1f
    when (weapon.style) {
        WeaponStyle.PISTOL -> {
            val barrelTip = Offset(handPos.x + 20f * dir * scale, handPos.y - 9f * scale)
            drawLine(weapon.primaryColor, Offset(handPos.x - 2f * dir * scale, handPos.y - 9f * scale), barrelTip, strokeWidth = 6f * scale, cap = StrokeCap.Round)
            drawLine(Color(0xFF0F172A), handPos, Offset(handPos.x + 2f * dir * scale, handPos.y - 8f * scale), strokeWidth = 5f * scale, cap = StrokeCap.Round)
            drawCircle(weapon.accentColor, radius = 2.6f * scale, center = barrelTip)
        }
        WeaponStyle.SHOTGUN -> {
            val stock = Offset(handPos.x - 14f * dir * scale, handPos.y - 4f * scale)
            val muzzle = Offset(handPos.x + 30f * dir * scale, handPos.y - 13f * scale)
            drawLine(weapon.primaryColor, stock, muzzle, strokeWidth = 7.5f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 6f * dir * scale, handPos.y - 7f * scale), Offset(handPos.x + 22f * dir * scale, handPos.y - 10f * scale), strokeWidth = 4.5f * scale, cap = StrokeCap.Round)
        }
        WeaponStyle.SMG -> {
            val back = Offset(handPos.x - 10f * dir * scale, handPos.y - 7f * scale)
            val muzzle = Offset(handPos.x + 24f * dir * scale, handPos.y - 11f * scale)
            drawLine(weapon.primaryColor, back, muzzle, strokeWidth = 7f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 6f * dir * scale, handPos.y - 7f * scale), Offset(handPos.x + 9f * dir * scale, handPos.y + 7f * scale), strokeWidth = 4f * scale, cap = StrokeCap.Round)
        }
        WeaponStyle.GRENADE -> {
            val bodyCenter = Offset(handPos.x + 3f * dir * scale, handPos.y - 5f * scale)
            drawCircle(weapon.primaryColor, radius = 8.5f * scale, center = bodyCenter)
            drawCircle(weapon.accentColor, radius = 8.5f * scale, center = bodyCenter, style = Stroke(width = 1.8f * scale))
            drawLine(Color(0xFFCBD5E1), Offset(bodyCenter.x, bodyCenter.y - 8f * scale), Offset(bodyCenter.x + 5f * dir * scale, bodyCenter.y - 12f * scale), strokeWidth = 2.5f * scale)
        }
        WeaponStyle.AK47 -> {
            val stock = Offset(handPos.x - 16f * dir * scale, handPos.y - 4f * scale)
            val muzzle = Offset(handPos.x + 34f * dir * scale, handPos.y - 14f * scale)
            drawLine(weapon.primaryColor, stock, Offset(handPos.x + 10f * dir * scale, handPos.y - 9f * scale), strokeWidth = 6.5f * scale, cap = StrokeCap.Round)
            drawLine(Color(0xFF334155), Offset(handPos.x - 4f * dir * scale, handPos.y - 7f * scale), muzzle, strokeWidth = 5f * scale, cap = StrokeCap.Round)
            val magTop = Offset(handPos.x + 8f * dir * scale, handPos.y - 5f * scale)
            val magBot = Offset(handPos.x + 14f * dir * scale, handPos.y + 8f * scale)
            drawLine(weapon.accentColor, magTop, magBot, strokeWidth = 4.5f * scale, cap = StrokeCap.Round)
        }
        WeaponStyle.SNIPER -> {
            val stock = Offset(handPos.x - 18f * dir * scale, handPos.y - 3f * scale)
            val muzzle = Offset(handPos.x + 42f * dir * scale, handPos.y - 16f * scale)
            drawLine(weapon.primaryColor, stock, Offset(handPos.x + 12f * dir * scale, handPos.y - 9f * scale), strokeWidth = 6.5f * scale, cap = StrokeCap.Round)
            drawLine(Color(0xFF1E293B), Offset(handPos.x - 2f * dir * scale, handPos.y - 7f * scale), muzzle, strokeWidth = 3.8f * scale, cap = StrokeCap.Round)
            // Scope on top
            drawLine(weapon.accentColor, Offset(handPos.x + 2f * dir * scale, handPos.y - 13f * scale), Offset(handPos.x + 18f * dir * scale, handPos.y - 16f * scale), strokeWidth = 4f * scale, cap = StrokeCap.Round)
        }
        WeaponStyle.BAZOOKA -> {
            val backEnd = Offset(handPos.x - 24f * dir * scale, handPos.y - 10f * scale)
            val frontEnd = Offset(handPos.x + 36f * dir * scale, handPos.y - 18f * scale)
            drawLine(weapon.primaryColor, backEnd, frontEnd, strokeWidth = 11.5f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 18f * dir * scale, handPos.y - 15.5f * scale), frontEnd, strokeWidth = 12f * scale)
            val nose = Offset(frontEnd.x + 8f * dir * scale, frontEnd.y - 1.5f * scale)
            drawLine(Color(0xFF38BDF8), frontEnd, nose, strokeWidth = 7f * scale, cap = StrokeCap.Round)
        }
        WeaponStyle.MINIGUN -> {
            val drum = Offset(handPos.x - 10f * dir * scale, handPos.y - 2f * scale)
            val barrelTip = Offset(handPos.x + 36f * dir * scale, handPos.y - 11f * scale)
            drawCircle(weapon.primaryColor, radius = 9.5f * scale, center = drum)
            drawLine(Color(0xFF334155), drum, barrelTip, strokeWidth = 11f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 2f * dir * scale, handPos.y - 5f * scale), barrelTip, strokeWidth = 3.2f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 2f * dir * scale, handPos.y - 1f * scale), Offset(barrelTip.x, barrelTip.y + 4f * scale), strokeWidth = 3.2f * scale, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun UniformVisualBadge(uniform: UniformItem, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(60.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF0F172A), cornerRadius = CornerRadius(16f, 16f))
        drawRoundRect(uniform.trimColor.copy(alpha = 0.45f), cornerRadius = CornerRadius(16f, 16f), style = Stroke(2.2f))

        val shirt = Path().apply {
            moveTo(w * 0.30f, h * 0.24f)
            lineTo(w * 0.14f, h * 0.40f)
            lineTo(w * 0.24f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.43f)
            lineTo(w * 0.30f, h * 0.80f)
            lineTo(w * 0.70f, h * 0.80f)
            lineTo(w * 0.70f, h * 0.43f)
            lineTo(w * 0.76f, h * 0.50f)
            lineTo(w * 0.86f, h * 0.40f)
            lineTo(w * 0.70f, h * 0.24f)
            lineTo(w * 0.57f, h * 0.20f)
            lineTo(w * 0.50f, h * 0.29f)
            lineTo(w * 0.43f, h * 0.20f)
            close()
        }
        drawPath(shirt, uniform.primaryColor)
        drawPath(shirt, uniform.trimColor, style = Stroke(width = 2.4f))
        drawLine(uniform.trimColor, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.78f), strokeWidth = 2.6f)
        // Armor Level pips at bottom edge
        for (i in 0 until uniform.armorLevel.coerceAtMost(5)) {
            drawCircle(
                color = uniform.trimColor,
                radius = 3.2f,
                center = Offset(w * 0.24f + i * (w * 0.13f), h * 0.89f)
            )
        }
    }
}

@Composable
fun WeaponVisualBadge(weapon: WeaponItem, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(60.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF0F172A), cornerRadius = CornerRadius(16f, 16f))
        drawRoundRect(weapon.accentColor.copy(alpha = 0.5f), cornerRadius = CornerRadius(16f, 16f), style = Stroke(2.2f))

        when (weapon.style) {
            WeaponStyle.PISTOL -> {
                drawLine(weapon.primaryColor, Offset(w * 0.24f, h * 0.42f), Offset(w * 0.78f, h * 0.42f), 10f, cap = StrokeCap.Round)
                drawLine(Color(0xFF334155), Offset(w * 0.30f, h * 0.42f), Offset(w * 0.24f, h * 0.74f), 9f, cap = StrokeCap.Round)
                drawCircle(weapon.accentColor, radius = 4.5f, center = Offset(w * 0.80f, h * 0.42f))
            }
            WeaponStyle.SHOTGUN -> {
                drawLine(weapon.primaryColor, Offset(w * 0.14f, h * 0.62f), Offset(w * 0.84f, h * 0.32f), 11f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.45f, h * 0.54f), Offset(w * 0.70f, h * 0.43f), 7f, cap = StrokeCap.Round)
            }
            WeaponStyle.SMG -> {
                drawLine(weapon.primaryColor, Offset(w * 0.20f, h * 0.46f), Offset(w * 0.78f, h * 0.46f), 11f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.50f, h * 0.48f), Offset(w * 0.56f, h * 0.76f), 6.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFF334155), Offset(w * 0.30f, h * 0.48f), Offset(w * 0.26f, h * 0.72f), 7f, cap = StrokeCap.Round)
            }
            WeaponStyle.GRENADE -> {
                drawCircle(weapon.primaryColor, radius = w * 0.23f, center = Offset(w * 0.50f, h * 0.56f))
                drawCircle(weapon.accentColor, radius = w * 0.23f, center = Offset(w * 0.50f, h * 0.56f), style = Stroke(width = 3f))
                drawLine(Color(0xFFCBD5E1), Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.22f), 6f)
                drawCircle(weapon.accentColor, radius = 6f, center = Offset(w * 0.64f, h * 0.26f), style = Stroke(2.5f))
            }
            WeaponStyle.AK47 -> {
                drawLine(weapon.primaryColor, Offset(w * 0.14f, h * 0.64f), Offset(w * 0.52f, h * 0.46f), 10f, cap = StrokeCap.Round)
                drawLine(Color(0xFF475569), Offset(w * 0.32f, h * 0.55f), Offset(w * 0.86f, h * 0.30f), 7.5f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.52f, h * 0.48f), Offset(w * 0.60f, h * 0.74f), 6f, cap = StrokeCap.Round)
            }
            WeaponStyle.SNIPER -> {
                drawLine(weapon.primaryColor, Offset(w * 0.12f, h * 0.66f), Offset(w * 0.52f, h * 0.48f), 9f, cap = StrokeCap.Round)
                drawLine(Color(0xFF1E293B), Offset(w * 0.35f, h * 0.55f), Offset(w * 0.90f, h * 0.28f), 5.5f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.40f, h * 0.40f), Offset(w * 0.66f, h * 0.28f), 6f, cap = StrokeCap.Round)
            }
            WeaponStyle.BAZOOKA -> {
                drawLine(weapon.primaryColor, Offset(w * 0.14f, h * 0.64f), Offset(w * 0.80f, h * 0.32f), 16f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.64f, h * 0.40f), Offset(w * 0.82f, h * 0.31f), 16f)
                drawCircle(Color(0xFF38BDF8), radius = 7f, center = Offset(w * 0.86f, h * 0.29f))
            }
            WeaponStyle.MINIGUN -> {
                drawCircle(weapon.primaryColor, radius = w * 0.16f, center = Offset(w * 0.28f, h * 0.58f))
                drawLine(Color(0xFF334155), Offset(w * 0.30f, h * 0.56f), Offset(w * 0.86f, h * 0.34f), 14f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.42f, h * 0.49f), Offset(w * 0.86f, h * 0.32f), 4f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.45f, h * 0.55f), Offset(w * 0.88f, h * 0.38f), 4f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun AttributeVisualBadge(attribute: AttributeItem, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(60.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF0F172A), cornerRadius = CornerRadius(16f, 16f))
        drawRoundRect(attribute.accentColor.copy(alpha = 0.5f), cornerRadius = CornerRadius(16f, 16f), style = Stroke(2.2f))

        when (attribute.style) {
            AttributeStyle.BELT -> {
                drawRoundRect(attribute.primaryColor, Offset(w * 0.16f, h * 0.42f), Size(w * 0.68f, h * 0.18f), CornerRadius(6f, 6f))
                drawRoundRect(attribute.accentColor, Offset(w * 0.41f, h * 0.38f), Size(w * 0.18f, h * 0.26f), CornerRadius(4f, 4f))
            }
            AttributeStyle.SPEED_BOOTS -> {
                val boot = Path().apply {
                    moveTo(w * 0.30f, h * 0.26f)
                    lineTo(w * 0.52f, h * 0.26f)
                    lineTo(w * 0.52f, h * 0.58f)
                    lineTo(w * 0.78f, h * 0.62f)
                    lineTo(w * 0.78f, h * 0.76f)
                    lineTo(w * 0.26f, h * 0.76f)
                    close()
                }
                drawPath(boot, attribute.primaryColor)
                drawPath(boot, attribute.accentColor, style = Stroke(2.4f))
                drawLine(attribute.accentColor, Offset(w * 0.16f, h * 0.44f), Offset(w * 0.28f, h * 0.44f), 3f)
            }
            AttributeStyle.KEVLAR_VEST -> {
                val vest = Path().apply {
                    moveTo(w * 0.30f, h * 0.22f)
                    lineTo(w * 0.42f, h * 0.22f)
                    lineTo(w * 0.50f, h * 0.32f)
                    lineTo(w * 0.58f, h * 0.22f)
                    lineTo(w * 0.70f, h * 0.22f)
                    lineTo(w * 0.74f, h * 0.78f)
                    lineTo(w * 0.26f, h * 0.78f)
                    close()
                }
                drawPath(vest, attribute.primaryColor)
                drawPath(vest, attribute.accentColor, style = Stroke(2.6f))
            }
            AttributeStyle.NVG_GOGGLES -> {
                drawRoundRect(attribute.primaryColor, Offset(w * 0.18f, h * 0.34f), Size(w * 0.64f, h * 0.32f), CornerRadius(12f, 12f))
                drawCircle(attribute.accentColor, radius = 9f, center = Offset(w * 0.36f, h * 0.50f))
                drawCircle(attribute.accentColor, radius = 9f, center = Offset(w * 0.64f, h * 0.50f))
            }
            AttributeStyle.SCANNER_DRONE -> {
                drawLine(Color(0xFFE2E8F0), Offset(w * 0.16f, h * 0.34f), Offset(w * 0.84f, h * 0.34f), 4f, cap = StrokeCap.Round)
                drawRoundRect(attribute.primaryColor, Offset(w * 0.28f, h * 0.42f), Size(w * 0.44f, h * 0.24f), CornerRadius(10f, 10f))
                drawCircle(attribute.accentColor, radius = 6f, center = Offset(w * 0.50f, h * 0.54f))
            }
        }
    }
}
