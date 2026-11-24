package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.model.AttributeCatalog
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.PatrolRoom
import com.example.model.RoomPlayer
import com.example.model.TacticalPing
import com.example.model.UserProfile
import com.example.model.WeaponCatalog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun Estate3DViewport(
    room: PatrolRoom,
    players: List<RoomPlayer>,
    pings: List<TacticalPing>,
    profile: UserProfile?,
    currentUserId: String,
    isPingMode: Boolean,
    thiefStunnedUntilMs: Long,
    onMoveSync: (EstateSector, Float, Float, Float) -> Unit,
    onRadarPingTap: (EstateSector, Float, Float) -> Unit,
    onWeaponHitThief: () -> Unit,
    onApprehend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val me = players.firstOrNull { it.userId == currentUserId }

    val equippedUniformId = profile?.equippedUniformId ?: me?.uniformId ?: "rookie_cadet"
    val equippedWeaponId = profile?.equippedWeaponId ?: me?.weaponId ?: "baton_standard"
    val equippedAttributeId = profile?.equippedAttributeId ?: me?.attributeId ?: "utility_belt"
    val weapon = WeaponCatalog.byId(equippedWeaponId)
    val attribute = AttributeCatalog.byId(equippedAttributeId)

    // Smooth local 3D state for 60fps gameplay
    var localX by remember(room.id) { mutableFloatStateOf(me?.posX?.toFloat() ?: 0.50f) }
    var localY by remember(room.id) { mutableFloatStateOf(me?.posY?.toFloat() ?: 0.12f) }
    var localFacingDeg by remember(room.id) { mutableFloatStateOf(me?.facingDeg?.toFloat() ?: 180f) }
    var walkPhase by remember { mutableFloatStateOf(0f) }
    var camYaw by remember { mutableFloatStateOf(0f) }

    // Smooth interpolated thief position in 3D
    var thiefVisX by remember(room.id) { mutableFloatStateOf(room.activeThiefX.toFloat()) }
    var thiefVisY by remember(room.id) { mutableFloatStateOf(room.activeThiefY.toFloat()) }

    // Joystick vector (-1..1) & Sprint/Stamina state
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var sprintEnabled by remember { mutableStateOf(false) }
    var stamina by remember { mutableFloatStateOf(1f) }
    var animClock by remember { mutableFloatStateOf(0f) }

    // Weapon fire VFX state
    var attackProgress by remember { mutableFloatStateOf(0f) }
    var attackOrigin by remember { mutableStateOf(0.5f to 0.5f) }
    var attackTarget by remember { mutableStateOf(0.5f to 0.5f) }
    var lastAttackTimeMs by remember { mutableLongStateOf(0L) }
    var lastSyncTimeMs by remember { mutableLongStateOf(0L) }
    var lastSyncedPos by remember { mutableStateOf(localX to localY) }

    // Sync if external teleport happens (e.g. radar tap when not dragging joystick)
    LaunchedEffect(me?.posX, me?.posY) {
        if (me != null && joyX == 0f && joyY == 0f) {
            val dx = me.posX.toFloat() - localX
            val dy = me.posY.toFloat() - localY
            if (hypot(dx, dy) > 0.12f) {
                localX = me.posX.toFloat()
                localY = me.posY.toFloat()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse3d")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse3dVal"
    )

    // 60 FPS Game Loop
    LaunchedEffect(room.id, attribute.id) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos
                animClock += dt

                // Interpolate thief smoothly toward Firestore coordinate
                val targetTX = room.activeThiefX.toFloat()
                val targetTY = room.activeThiefY.toFloat()
                thiefVisX += (targetTX - thiefVisX) * (dt * 4.5f).coerceAtMost(1f)
                thiefVisY += (targetTY - thiefVisY) * (dt * 4.5f).coerceAtMost(1f)

                // Update weapon projectile VFX
                if (attackProgress > 0f) {
                    attackProgress += dt * 4.5f
                    if (attackProgress >= 1f) attackProgress = 0f
                }

                // Movement & Stamina logic
                val joyMag = hypot(joyX, joyY)
                if (joyMag > 0.08f) {
                    val isSprinting = sprintEnabled && stamina > 0.05f
                    if (isSprinting) {
                        stamina = (stamina - dt * 0.28f * attribute.staminaDrainFactor).coerceAtLeast(0f)
                        if (stamina <= 0.01f) sprintEnabled = false
                    } else {
                        stamina = (stamina + dt * 0.14f).coerceAtMost(1f)
                    }

                    val baseSpeed = 0.155f * attribute.speedMultiplier * (if (isSprinting) 1.55f else 1.0f)
                    val forwardX = sin(camYaw)
                    val forwardY = -cos(camYaw)
                    val rightX = cos(camYaw)
                    val rightY = sin(camYaw)

                    val moveX = rightX * joyX + forwardX * (-joyY)
                    val moveY = rightY * joyX + forwardY * (-joyY)
                    val normLen = hypot(moveX, moveY).coerceAtLeast(0.001f)
                    val dirX = moveX / normLen
                    val dirY = moveY / normLen

                    val nextX = localX + dirX * baseSpeed * joyMag.coerceAtMost(1f) * dt
                    val nextY = localY + dirY * baseSpeed * joyMag.coerceAtMost(1f) * dt
                    val (safeX, safeY) = EstateWorld.moveWithCollision(localX, localY, nextX, nextY)
                    localX = safeX
                    localY = safeY
                    localFacingDeg = Math.toDegrees(atan2(dirX.toDouble(), (-dirY).toDouble())).toFloat()
                    walkPhase += dt * (if (isSprinting) 14f else 9.5f) * joyMag

                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastSyncTimeMs >= 450L && hypot(localX - lastSyncedPos.first, localY - lastSyncedPos.second) > 0.015f) {
                        lastSyncTimeMs = nowMs
                        lastSyncedPos = localX to localY
                        onMoveSync(EstateWorld.nearestTo(localX, localY), localX, localY, localFacingDeg)
                    }
                } else {
                    stamina = (stamina + dt * 0.22f).coerceAtMost(1f)
                }
            }
        }
    }

    val distToThief = EstateWorld.distance(localX, localY, thiefVisX, thiefVisY)
    val inCatchRange = distToThief <= (0.24f + attribute.catchRadiusBonus)
    val isThiefStunned = System.currentTimeMillis() < thiefStunnedUntilMs
    val currentSector = EstateWorld.nearestTo(localX, localY)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        // 1. 3D Perspective Game Canvas (Drag to Orbit Camera)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("estate_3d_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        camYaw += dragAmount.x * 0.0065f
                    }
                }
        ) {
            renderEstate3DScene(
                room = room,
                players = players,
                pings = pings,
                currentUserId = currentUserId,
                localPlayerX = localX,
                localPlayerY = localY,
                localFacingDeg = localFacingDeg,
                localWalkPhase = walkPhase,
                localWeaponId = equippedWeaponId,
                localAttributeId = equippedAttributeId,
                localUniformId = equippedUniformId,
                thiefPos = thiefVisX to thiefVisY,
                thiefStunned = isThiefStunned,
                attackProgress = attackProgress,
                attackOrigin = attackOrigin,
                attackTarget = attackTarget,
                camYaw = camYaw,
                pulseScale = pulseScale,
                animClock = animClock,
                textMeasurer = textMeasurer
            )
        }

        // 2. Top-Left HUD: Location, Stamina & Active Equipment Needs
        Surface(
            color = Color(0xCC090E1A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${currentSector.shortCode} • ${currentSector.streetName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF22D3EE),
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("STAMINA ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    LinearProgressIndicator(
                        progress = { stamina },
                        color = if (sprintEnabled) Color(0xFFF59E0B) else Color(0xFF10B981),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .width(68.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
                Text(
                    text = "Target: ${(distToThief * 100).toInt()}m • ${weapon.name.take(14)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inCatchRange) Color(0xFFFDE047) else Color(0xFFCBD5E1)
                )
            }
        }

        // 3. Top-Right Compact Tactical Mini-Radar (Tap to Ping or Jump-Check)
        MiniRadarOverlay(
            room = room,
            players = players,
            localX = localX,
            localY = localY,
            thiefX = thiefVisX,
            thiefY = thiefVisY,
            isPingMode = isPingMode,
            onRadarTap = { nx, ny ->
                val sec = EstateWorld.nearestTo(nx, ny)
                if (isPingMode) {
                    onRadarPingTap(sec, nx, ny)
                } else {
                    val (safeX, safeY) = EstateWorld.moveWithCollision(localX, localY, nx, ny)
                    localX = safeX
                    localY = safeY
                    onMoveSync(sec, safeX, safeY, localFacingDeg)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(92.dp)
        )

        // 4. Bottom-Left Analog Virtual Joystick
        VirtualJoystick3D(
            joyX = joyX,
            joyY = joyY,
            onStickMove = { nx, ny ->
                joyX = nx
                joyY = ny
            },
            onStickRelease = {
                joyX = 0f
                joyY = 0f
                lastSyncedPos = localX to localY
                onMoveSync(EstateWorld.nearestTo(localX, localY), localX, localY, localFacingDeg)
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .size(104.dp)
        )

        // 5. Bottom-Right 3D Game Action Buttons (Sprint, Weapon Fire, Apprehend)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (inCatchRange) {
                Button(
                    onClick = onApprehend,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF090E1A)),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("hud_3d_catch_btn")
                ) {
                    Icon(Icons.Filled.GppGood, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TANGKAP!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Sprint Toggle
                Surface(
                    onClick = { sprintEnabled = !sprintEnabled },
                    color = if (sprintEnabled) Color(0xFF0284C7) else Color(0xCC0F172A),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, if (sprintEnabled) Color(0xFF38BDF8) else Color(0xFF475569)),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("sprint_3d_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = "Lari Cepat",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Weapon Fire / Swing Button
                Surface(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastAttackTimeMs >= 650L) {
                            lastAttackTimeMs = now
                            attackOrigin = localX to localY
                            val aimRad = Math.toRadians(localFacingDeg.toDouble()).toFloat()
                            val hit = distToThief <= weapon.rangeNorm
                            attackTarget = if (hit) {
                                thiefVisX to thiefVisY
                            } else {
                                (localX + sin(aimRad) * weapon.rangeNorm).coerceIn(0.05f, 0.95f) to
                                    (localY - cos(aimRad) * weapon.rangeNorm).coerceIn(0.05f, 0.95f)
                            }
                            attackProgress = 0.05f
                            if (hit) {
                                onWeaponHitThief()
                            }
                        }
                    },
                    color = weapon.primaryColor.copy(alpha = 0.92f),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, weapon.accentColor),
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("fire_weapon_3d_btn")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = "Gunakan ${weapon.name}",
                            tint = weapon.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "AKSI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualJoystick3D(
    joyX: Float,
    joyY: Float,
    onStickMove: (Float, Float) -> Unit,
    onStickRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onStickRelease() },
                    onDragCancel = { onStickRelease() }
                ) { change, _ ->
                    change.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = minOf(cx, cy) * 0.8f
                    val dx = change.position.x - cx
                    val dy = change.position.y - cy
                    val dist = hypot(dx, dy).coerceAtLeast(0.001f)
                    val clamped = dist.coerceAtMost(maxR)
                    onStickMove((dx / dist) * (clamped / maxR), (dy / dist) * (clamped / maxR))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = minOf(cx, cy) * 0.92f
            val knobR = outerR * 0.40f
            val travel = outerR * 0.56f

            drawCircle(Color(0x88090E1A), outerR, Offset(cx, cy))
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.5f), outerR, Offset(cx, cy), style = Stroke(width = 2.5f))
            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.88f),
                radius = knobR,
                center = Offset(cx + joyX * travel, cy + joyY * travel)
            )
        }
    }
}

@Composable
private fun MiniRadarOverlay(
    room: PatrolRoom,
    players: List<RoomPlayer>,
    localX: Float,
    localY: Float,
    thiefX: Float,
    thiefY: Float,
    isPingMode: Boolean,
    onRadarTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC090E1A))
            .border(
                width = 1.5.dp,
                color = if (isPingMode) Color(0xFFF59E0B) else Color(0xFF334155),
                shape = RoundedCornerShape(14.dp)
            )
            .pointerInput(isPingMode) {
                detectTapGestures { offset ->
                    val nx = (offset.x / size.width.toFloat()).coerceIn(0.06f, 0.94f)
                    val ny = (offset.y / size.height.toFloat()).coerceIn(0.06f, 0.94f)
                    onRadarTap(nx, ny)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Buildings on mini-radar
            EstateWorld.buildings.forEach { b ->
                drawRoundRect(
                    color = Color(0xFF1E3A5F),
                    topLeft = Offset(b.minX * w, b.minY * h),
                    size = Size((b.maxX - b.minX) * w, (b.maxY - b.minY) * h),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            // Squadmates
            players.forEach { p ->
                drawCircle(Color(0xFF38BDF8), radius = 3.5f, center = Offset(p.posX.toFloat() * w, p.posY.toFloat() * h))
            }
            // Local Guard (gold)
            drawCircle(Color(0xFFFDE047), radius = 4.5f, center = Offset(localX * w, localY * h))
            // Thief (red)
            if (room.thiefSpotted) {
                drawCircle(Color(0xFFEF4444), radius = 5f, center = Offset(thiefX * w, thiefY * h))
            }
        }
        Icon(
            Icons.Filled.Radar,
            contentDescription = "Mini Radar",
            tint = Color(0xFF22D3EE).copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(12.dp)
        )
    }
}
