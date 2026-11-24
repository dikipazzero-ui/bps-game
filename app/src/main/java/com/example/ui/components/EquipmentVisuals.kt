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
                        uniform.primaryColor.copy(alpha = 0.25f),
                        Color(0xFF0A111F)
                    ),
                    center = Offset(cx, h * 0.45f),
                    radius = maxOf(w, h) * 0.7f
                )
            )

            // 3D Turntable pedestal ellipses
            val pedW = 128f * scale
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

            // Cape / High Collar (behind torso when facing front)
            if (uniform.hasCapeOrCollar && cosY > -0.6f) {
                val capePath = Path().apply {
                    moveTo(px(-18f, -6f), py(92f, -6f))
                    lineTo(px(18f, -6f), py(92f, -6f))
                    lineTo(px(26f, -14f), py(32f, -14f))
                    lineTo(px(-26f, -14f), py(32f, -14f))
                    close()
                }
                drawPath(capePath, uniform.primaryColor.copy(alpha = 0.72f))
                drawPath(capePath, uniform.trimColor.copy(alpha = 0.6f), style = Stroke(width = 1.6f * scale))
            }

            // Legs & Boots
            val bootColor = if (attribute.style == AttributeStyle.SPEED_BOOTS) attribute.primaryColor else Color(0xFF0F172A)
            val bootTrim = if (attribute.style == AttributeStyle.SPEED_BOOTS) attribute.accentColor else Color(0xFF475569)
            listOf(-10f, 10f).forEach { legX ->
                val hip = Offset(px(legX, 0f), py(52f, 0f))
                val foot = Offset(px(legX * 1.05f, 3f), py(6f, 3f))
                drawLine(Color(0xFF1E293B), hip, foot, strokeWidth = 11f * scale, cap = StrokeCap.Round)
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

            // Torso (3D Jacket / Uniform)
            val torsoTopLeft = Offset(px(-19f, 0f), py(94f, 0f))
            val torsoW = kotlin.math.abs(px(19f, 0f) - px(-19f, 0f)).coerceAtLeast(18f * scale)
            val torsoH = 42f * scale
            drawRoundRect(
                color = uniform.primaryColor,
                topLeft = Offset(cx + sinY * 0f - torsoW / 2f, torsoTopLeft.y),
                size = Size(torsoW, torsoH),
                cornerRadius = CornerRadius(8f * scale, 8f * scale)
            )
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

            // Attribute: Utility Belt (or belt base on all guards)
            val beltColor = if (attribute.style == AttributeStyle.BELT) attribute.accentColor else uniform.trimColor
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - torsoW * 0.52f, py(57f, 6f)),
                size = Size(torsoW * 1.04f, 7f * scale),
                cornerRadius = CornerRadius(3f * scale, 3f * scale)
            )
            drawCircle(beltColor, radius = 4f * scale, center = Offset(px(0f, 10f), py(54f, 10f)))

            // Shoulder Pads for upgraded uniforms
            if (uniform.hasShoulderPads) {
                listOf(-20f, 20f).forEach { sx ->
                    drawCircle(
                        color = uniform.trimColor,
                        radius = 7.5f * scale,
                        center = Offset(px(sx, 0f), py(91f, 0f))
                    )
                }
            }

            // Left Arm
            val lShoulder = Offset(px(-21f, 0f), py(89f, 0f))
            val lHand = Offset(px(-25f, 4f), py(61f, 4f))
            drawLine(uniform.primaryColor, lShoulder, lHand, strokeWidth = 9f * scale, cap = StrokeCap.Round)
            drawCircle(Color(0xFFF1C27D), radius = 4.5f * scale, center = lHand)

            // Head & Hair / Cap
            val headCenter = Offset(px(0f, 0f), py(110f, 0f))
            drawCircle(Color(0xFFF1C27D), radius = 14f * scale, center = headCenter)
            // Patrol cap / hair
            drawArc(
                color = Color(0xFF0F172A),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - 14.5f * scale, headCenter.y - 16f * scale),
                size = Size(29f * scale, 22f * scale)
            )
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
        WeaponStyle.BATON -> {
            val tip = Offset(handPos.x + 14f * dir * scale, handPos.y - 26f * scale)
            drawLine(weapon.primaryColor, handPos, tip, strokeWidth = 5f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 8f * dir * scale, handPos.y - 15f * scale), tip, strokeWidth = 5.5f * scale, cap = StrokeCap.Round)
        }
        WeaponStyle.FLASHLIGHT -> {
            val head = Offset(handPos.x + 18f * dir * scale, handPos.y - 8f * scale)
            drawLine(weapon.primaryColor, handPos, head, strokeWidth = 7f * scale, cap = StrokeCap.Round)
            drawCircle(weapon.accentColor, radius = 5f * scale, center = head)
            val cone = Path().apply {
                moveTo(head.x, head.y)
                lineTo(head.x + 36f * dir * scale, head.y - 18f * scale)
                lineTo(head.x + 36f * dir * scale, head.y + 10f * scale)
                close()
            }
            drawPath(cone, weapon.accentColor.copy(alpha = 0.22f))
        }
        WeaponStyle.TASER_PISTOL -> {
            val barrel = Offset(handPos.x + 22f * dir * scale, handPos.y - 7f * scale)
            drawLine(weapon.primaryColor, Offset(handPos.x, handPos.y - 6f * scale), barrel, strokeWidth = 6.5f * scale, cap = StrokeCap.Round)
            drawLine(weapon.primaryColor, handPos, Offset(handPos.x + 2f * dir * scale, handPos.y - 7f * scale), strokeWidth = 5.5f * scale)
            drawCircle(weapon.accentColor, radius = 3.5f * scale, center = barrel)
        }
        WeaponStyle.NET_LAUNCHER -> {
            val muzzle = Offset(handPos.x + 28f * dir * scale, handPos.y - 9f * scale)
            drawLine(weapon.primaryColor, Offset(handPos.x - 4f * dir * scale, handPos.y - 6f * scale), muzzle, strokeWidth = 9f * scale, cap = StrokeCap.Round)
            drawCircle(weapon.accentColor, radius = 6f * scale, center = muzzle, style = Stroke(width = 2.4f * scale))
        }
        WeaponStyle.CYBER_CARBINE -> {
            val stock = Offset(handPos.x - 10f * dir * scale, handPos.y - 4f * scale)
            val muzzle = Offset(handPos.x + 34f * dir * scale, handPos.y - 11f * scale)
            drawLine(weapon.primaryColor, stock, muzzle, strokeWidth = 8f * scale, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, Offset(handPos.x + 6f * dir * scale, handPos.y - 8f * scale), muzzle, strokeWidth = 3f * scale)
            drawCircle(weapon.accentColor, radius = 4f * scale, center = muzzle)
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

        // Stylized 3D Shirt / Jacket silhouette
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
        drawCircle(uniform.trimColor, radius = 4f, center = Offset(w * 0.39f, h * 0.44f))
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
            WeaponStyle.BATON -> {
                drawLine(weapon.primaryColor, Offset(w * 0.24f, h * 0.76f), Offset(w * 0.76f, h * 0.24f), 9f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.46f, h * 0.54f), Offset(w * 0.76f, h * 0.24f), 9.5f, cap = StrokeCap.Round)
            }
            WeaponStyle.FLASHLIGHT -> {
                drawLine(weapon.primaryColor, Offset(w * 0.20f, h * 0.65f), Offset(w * 0.62f, h * 0.38f), 11f, cap = StrokeCap.Round)
                drawCircle(weapon.accentColor, radius = 8f, center = Offset(w * 0.64f, h * 0.36f))
                drawPath(
                    Path().apply {
                        moveTo(w * 0.64f, h * 0.36f)
                        lineTo(w * 0.90f, h * 0.16f)
                        lineTo(w * 0.90f, h * 0.54f)
                        close()
                    },
                    weapon.accentColor.copy(alpha = 0.32f)
                )
            }
            WeaponStyle.TASER_PISTOL -> {
                drawLine(weapon.primaryColor, Offset(w * 0.24f, h * 0.44f), Offset(w * 0.78f, h * 0.44f), 10f, cap = StrokeCap.Round)
                drawLine(weapon.primaryColor, Offset(w * 0.30f, h * 0.44f), Offset(w * 0.24f, h * 0.74f), 9f, cap = StrokeCap.Round)
                drawCircle(weapon.accentColor, radius = 5f, center = Offset(w * 0.80f, h * 0.44f))
            }
            WeaponStyle.NET_LAUNCHER -> {
                drawLine(weapon.primaryColor, Offset(w * 0.18f, h * 0.56f), Offset(w * 0.72f, h * 0.36f), 13f, cap = StrokeCap.Round)
                drawCircle(weapon.accentColor, radius = 9f, center = Offset(w * 0.75f, h * 0.34f), style = Stroke(3f))
            }
            WeaponStyle.CYBER_CARBINE -> {
                drawLine(weapon.primaryColor, Offset(w * 0.14f, h * 0.62f), Offset(w * 0.84f, h * 0.34f), 11f, cap = StrokeCap.Round)
                drawLine(weapon.accentColor, Offset(w * 0.32f, h * 0.55f), Offset(w * 0.86f, h * 0.33f), 4f)
                drawCircle(weapon.accentColor, radius = 5.5f, center = Offset(w * 0.85f, h * 0.33f))
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
