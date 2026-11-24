package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttributeCatalog
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.MissionOutcome
import com.example.model.PatrolRoom
import com.example.model.RoomPlayer
import com.example.model.TacticalPing
import com.example.model.UniformCatalog
import com.example.model.UserProfile
import com.example.model.WeaponCatalog
import com.example.model.WeaponStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
    onMissionVictory: (Long, Int) -> Unit,
    onSwitchWeapon: (String) -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val me = players.firstOrNull { it.userId == currentUserId }

    val equippedUniformId = profile?.equippedUniformId ?: me?.uniformId ?: "rookie_cadet"
    val equippedWeaponId = profile?.equippedWeaponId ?: me?.weaponId ?: "pistol_9mm"
    val equippedAttributeId = profile?.equippedAttributeId ?: me?.attributeId ?: "utility_belt"
    val unlockedWeapons = profile?.unlockedWeaponIds?.ifEmpty { listOf(equippedWeaponId) } ?: listOf(equippedWeaponId)

    val uniform = UniformCatalog.byId(equippedUniformId)
    val weapon = WeaponCatalog.byId(equippedWeaponId)
    val attribute = AttributeCatalog.byId(equippedAttributeId)

    // Smooth local 3D state
    var localX by remember(room.id) { mutableFloatStateOf(me?.posX?.toFloat() ?: 0.50f) }
    var localY by remember(room.id) { mutableFloatStateOf(me?.posY?.toFloat() ?: 0.60f) }
    var localFacingDeg by remember(room.id) { mutableFloatStateOf(me?.facingDeg?.toFloat() ?: 0f) }
    var walkPhase by remember { mutableFloatStateOf(0f) }
    var camYaw by remember { mutableFloatStateOf(0f) }

    // Smooth interpolated thief position
    var thiefVisX by remember(room.id) { mutableFloatStateOf(room.activeThiefX.toFloat()) }
    var thiefVisY by remember(room.id) { mutableFloatStateOf(room.activeThiefY.toFloat()) }

    // GTA Mission Combat State: HP, Armor, Timer, Wave, Outcome
    var missionWave by remember(room.id) { mutableIntStateOf(1) }
    var missionOutcome by remember(room.id) { mutableStateOf(MissionOutcome.ACTIVE) }
    var timeRemainingSec by remember(room.id) { mutableFloatStateOf(120f) }
    var playerHp by remember(room.id) { mutableFloatStateOf(100f) }
    val maxPlayerHp = 100f
    val maxThiefHp = remember(missionWave) { 100f + (missionWave - 1) * 35f }
    var thiefHp by remember(room.id, missionWave) { mutableFloatStateOf(maxThiefHp) }

    // Zone Entry Banner Notification ("MEMASUKI BLOK CEMARA")
    var lastSectorId by remember(room.id) { mutableStateOf("") }
    var zoneBannerText by remember { mutableStateOf<String?>(null) }
    var zoneBannerUntilMs by remember { mutableLongStateOf(0L) }

    // Controls & Stamina
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var sprintEnabled by remember { mutableStateOf(false) }
    var stamina by remember { mutableFloatStateOf(1f) }
    var animClock by remember { mutableFloatStateOf(0f) }

    // 4D VFX: Projectiles, High-Particle FX, Camera Micro-Shake
    var projectiles by remember { mutableStateOf<List<Projectile3D>>(emptyList()) }
    var particles by remember { mutableStateOf<List<Particle3D>>(emptyList()) }
    var camShakeStrength by remember { mutableFloatStateOf(0f) }
    var camShakeX by remember { mutableFloatStateOf(0f) }
    var camShakeY by remember { mutableFloatStateOf(0f) }
    var lastAttackTimeMs by remember { mutableLongStateOf(0L) }
    var lastThiefShotMs by remember { mutableLongStateOf(0L) }
    var lastDustMs by remember { mutableLongStateOf(0L) }
    var lastSyncTimeMs by remember { mutableLongStateOf(0L) }
    var lastSyncedPos by remember { mutableStateOf(localX to localY) }

    fun spawnBurstParticles(nx: Float, ny: Float, yWorld: Float, color: Color, count: Int, speed: Float, size: Float, withRing: Boolean = false) {
        val wPos = normToWorld(nx, ny, yWorld)
        val newPts = buildList {
            if (withRing) {
                add(Particle3D(wPos.x, 0.35f, wPos.z, 0f, 0f, 0f, color, 0.55f, 0.55f, size * 2.2f, isRing = true))
            }
            repeat(count) {
                val ang = Random.nextFloat() * 2f * PI.toFloat()
                val spd = speed * (0.4f + Random.nextFloat() * 0.8f)
                add(
                    Particle3D(
                        x = wPos.x,
                        y = wPos.y,
                        z = wPos.z,
                        vx = cos(ang) * spd,
                        vy = 2f + Random.nextFloat() * speed * 0.9f,
                        vz = sin(ang) * spd,
                        color = color,
                        life = 0.45f + Random.nextFloat() * 0.45f,
                        maxLife = 0.85f,
                        size = size * (0.6f + Random.nextFloat() * 0.7f)
                    )
                )
            }
        }
        particles = (particles + newPts).takeLast(90)
    }

    fun resetMission(nextWave: Boolean) {
        if (nextWave) missionWave += 1
        playerHp = maxPlayerHp
        thiefHp = 100f + (missionWave - 1) * 35f
        timeRemainingSec = 120f
        missionOutcome = MissionOutcome.ACTIVE
        projectiles = emptyList()
        particles = emptyList()
    }

    LaunchedEffect(me?.posX, me?.posY) {
        if (me != null && joyX == 0f && joyY == 0f) {
            val dx = me.posX.toFloat() - localX
            val dy = me.posY.toFloat() - localY
            if (hypot(dx, dy) > 0.14f) {
                localX = me.posX.toFloat()
                localY = me.posY.toFloat()
            }
        }
    }

    val currentSector = EstateWorld.nearestTo(localX, localY)
    LaunchedEffect(currentSector.id) {
        if (currentSector.id != lastSectorId) {
            lastSectorId = currentSector.id
            zoneBannerText = "MEMASUKI ${currentSector.name.uppercase()}"
            zoneBannerUntilMs = System.currentTimeMillis() + 3000L
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

    // 60 FPS Combat, Physics, Particle & Timer Loop
    LaunchedEffect(room.id, attribute.id, uniform.id, missionWave, missionOutcome) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos
                animClock += dt
                val nowMs = System.currentTimeMillis()

                if (zoneBannerText != null && nowMs > zoneBannerUntilMs) {
                    zoneBannerText = null
                }

                // Update camera micro-shake decay
                if (camShakeStrength > 0.05f) {
                    camShakeX = (Random.nextFloat() - 0.5f) * camShakeStrength * 2f
                    camShakeY = (Random.nextFloat() - 0.5f) * camShakeStrength * 2f
                    camShakeStrength *= (1f - dt * 8f).coerceIn(0f, 1f)
                } else {
                    camShakeStrength = 0f
                    camShakeX = 0f
                    camShakeY = 0f
                }

                // Update Particle3D list (gravity + fade)
                if (particles.isNotEmpty()) {
                    particles = particles.mapNotNull { pt ->
                        val rem = pt.life - dt
                        if (rem <= 0f) null
                        else pt.copy(
                            x = pt.x + pt.vx * dt,
                            y = (pt.y + pt.vy * dt).coerceAtLeast(0.1f),
                            z = pt.z + pt.vz * dt,
                            vy = if (pt.isRing) 0f else pt.vy - 9.5f * dt,
                            life = rem
                        )
                    }
                }

                // Update Projectiles & Check Hit Resolution
                if (projectiles.isNotEmpty()) {
                    val remaining = mutableListOf<Projectile3D>()
                    projectiles.forEach { proj ->
                        val nextProg = proj.progress + dt * (if (proj.isArc) 2.2f else 4.2f)
                        if (nextProg >= 1f) {
                            if (proj.isEnemy) {
                                val distHit = EstateWorld.distance(proj.toX, proj.toY, localX, localY)
                                if (distHit < 0.08f && missionOutcome == MissionOutcome.ACTIVE) {
                                    val rawDmg = 13f + missionWave * 2f
                                    val mitigated = rawDmg * (1f - uniform.damageReduction)
                                    playerHp = (playerHp - mitigated).coerceAtLeast(0f)
                                    camShakeStrength = (camShakeStrength + 5f).coerceAtMost(14f)
                                    spawnBurstParticles(localX, localY, 1.5f, Color(0xFFEF4444), 6, 4.5f, 1.4f)
                                    if (playerHp <= 0f) {
                                        missionOutcome = MissionOutcome.WASTED
                                    }
                                }
                            } else {
                                // Player projectile explosion / impact particles
                                val wpn = WeaponCatalog.byId(equippedWeaponId)
                                val isBlast = wpn.aoeRadius > 0f
                                spawnBurstParticles(
                                    proj.toX, proj.toY, 1.4f,
                                    if (isBlast) Color(0xFFF97316) else wpn.accentColor,
                                    count = if (isBlast) 18 else 8,
                                    speed = if (isBlast) 11f else 5f,
                                    size = if (isBlast) 2.6f else 1.3f,
                                    withRing = isBlast
                                )
                                if (isBlast) {
                                    spawnBurstParticles(proj.toX, proj.toY, 2.2f, Color(0xFF64748B), 8, 4f, 3.0f)
                                    camShakeStrength = (camShakeStrength + 10f).coerceAtMost(18f)
                                }
                            }
                        } else {
                            remaining.add(proj.copy(progress = nextProg))
                        }
                    }
                    projectiles = remaining
                }

                if (missionOutcome != MissionOutcome.ACTIVE) return@withFrameNanos

                // Countdown Timer
                timeRemainingSec = (timeRemainingSec - dt).coerceAtLeast(0f)
                if (timeRemainingSec <= 0f) {
                    missionOutcome = MissionOutcome.TIME_UP
                    return@withFrameNanos
                }

                // Thief interpolation
                val targetTX = room.activeThiefX.toFloat()
                val targetTY = room.activeThiefY.toFloat()
                thiefVisX += (targetTX - thiefVisX) * (dt * 4.5f).coerceAtMost(1f)
                thiefVisY += (targetTY - thiefVisY) * (dt * 4.5f).coerceAtMost(1f)

                // Thief Return Fire when within engagement range and not stunned
                val dThief = EstateWorld.distance(localX, localY, thiefVisX, thiefVisY)
                if (room.thiefSpotted && dThief <= 0.36f && nowMs >= thiefStunnedUntilMs && nowMs - lastThiefShotMs >= 1850L) {
                    lastThiefShotMs = nowMs
                    projectiles = projectiles + Projectile3D(
                        fromX = thiefVisX,
                        fromY = thiefVisY,
                        toX = localX,
                        toY = localY,
                        progress = 0.05f,
                        color = Color(0xFFEF4444),
                        isEnemy = true
                    )
                    spawnBurstParticles(thiefVisX, thiefVisY, 1.8f, Color(0xFFFCA5A5), 5, 3f, 1.1f)
                }

                // Player Movement, Footstep Dust & Stamina
                val joyMag = hypot(joyX, joyY)
                if (joyMag > 0.08f) {
                    val isSprinting = sprintEnabled && stamina > 0.05f
                    if (isSprinting) {
                        stamina = (stamina - dt * 0.28f * attribute.staminaDrainFactor).coerceAtLeast(0f)
                        if (stamina <= 0.01f) sprintEnabled = false
                    } else {
                        stamina = (stamina + dt * 0.14f).coerceAtMost(1f)
                    }

                    val baseSpeed = 0.165f * attribute.speedMultiplier * (if (isSprinting) 1.55f else 1.0f)
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

                    if (nowMs - lastDustMs >= (if (isSprinting) 130L else 240L)) {
                        lastDustMs = nowMs
                        spawnBurstParticles(localX, localY, 0.2f, Color(0xFF94A3B8), 2, 1.8f, 1.1f)
                    }

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
    val totalSecs = timeRemainingSec.roundToInt().coerceAtLeast(0)
    val timerText = "%02d:%02d".format(totalSecs / 60, totalSecs % 60)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        // 1. 4D Perspective Game Canvas (Drag horizontally to orbit camera)
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
                projectiles = projectiles,
                particles = particles,
                camYaw = camYaw,
                camShakeX = camShakeX,
                camShakeY = camShakeY,
                pulseScale = pulseScale,
                animClock = animClock,
                textMeasurer = textMeasurer
            )
        }

        // 2. POJOK KIRI ATAS (GTA HUD): HP Pemain, Armor Level & Stamina
        Surface(
            color = Color(0xD8090E1A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("HP", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (playerHp / maxPlayerHp).coerceIn(0f, 1f) },
                        color = if (playerHp <= 30f) Color(0xFFEF4444) else Color(0xFF10B981),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .width(68.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text("${playerHp.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = uniform.trimColor, modifier = Modifier.size(11.dp))
                    Text(
                        "ARMOR LV.${uniform.armorLevel} (-${(uniform.damageReduction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = uniform.trimColor,
                        fontSize = 10.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("STA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                    LinearProgressIndicator(
                        progress = { stamina },
                        color = if (sprintEnabled) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .width(64.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // 3. ATAS TENGAH: Bar Nyawa Pencuri (saat kontak / terlihat) + Notifikasi Blok
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (room.thiefSpotted && missionOutcome == MissionOutcome.ACTIVE) {
                Surface(
                    color = Color(0xD81A0B0E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PENCURI ${(distToThief * 100).toInt()}m • ${thiefHp.roundToInt()}/${maxThiefHp.toInt()} HP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        LinearProgressIndicator(
                            progress = { (thiefHp / maxThiefHp).coerceIn(0f, 1f) },
                            color = Color(0xFFEF4444),
                            trackColor = Color(0xFF450A0A),
                            modifier = Modifier
                                .width(105.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = zoneBannerText != null,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xE6090E1A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF22D3EE))
                ) {
                    Text(
                        text = zoneBannerText ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 4. POJOK KANAN ATAS (GTA HUD): Countdown Timer, Poin, Lokasi, Senjata Aktif & Tombol TOKO
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                color = Color(0xD8090E1A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (totalSecs <= 20) Color(0xFFEF4444) else Color(0xFFF59E0B))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "MISI #$missionWave",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (totalSecs <= 20) Color(0xFFEF4444) else Color(0xFFFDE047),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "${profile?.points ?: 0} POIN • ${currentSector.shortCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Senjata Aktif + Quick Weapon Switch + Shortcut Toko
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = {
                        if (unlockedWeapons.size > 1) {
                            val idx = unlockedWeapons.indexOf(equippedWeaponId)
                            val nextId = unlockedWeapons[(idx + 1) % unlockedWeapons.size]
                            onSwitchWeapon(nextId)
                        } else {
                            onOpenShop()
                        }
                    },
                    color = Color(0xD80F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, weapon.accentColor),
                    modifier = Modifier.testTag("hud_switch_weapon_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.SwapHoriz, null, tint = weapon.accentColor, modifier = Modifier.size(13.dp))
                        Text(
                            text = weapon.name.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    onClick = onOpenShop,
                    color = Color(0xE6F59E0B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("hud_open_shop_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingBag, null, tint = Color(0xFF090E1A), modifier = Modifier.size(13.dp))
                        Text(
                            text = "TOKO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF090E1A),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 5. Analog Virtual Joystick (di atas Mini-Map kiri bawah agar tidak tumpang tindih)
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
                .padding(start = 10.dp, bottom = 120.dp)
                .size(94.dp)
        )

        // 6. POJOK KIRI BAWAH (GTA HUD): Mini-Map / Radar Bumi Papan Selaras + Panah Arah Misi
        MiniRadarOverlay(
            room = room,
            players = players,
            localX = localX,
            localY = localY,
            localFacingDeg = localFacingDeg,
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
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .size(104.dp)
        )

        // 7. POJOK KANAN BAWAH: Sprint, Serang Senjata & Tangkap
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (inCatchRange && (isThiefStunned || thiefHp <= maxThiefHp * 0.45f) && missionOutcome == MissionOutcome.ACTIVE) {
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
                Surface(
                    onClick = { sprintEnabled = !sprintEnabled },
                    color = if (sprintEnabled) Color(0xFF0284C7) else Color(0xCC0F172A),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, if (sprintEnabled) Color(0xFF38BDF8) else Color(0xFF475569)),
                    modifier = Modifier
                        .size(48.dp)
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

                // Tombol Tembak / Lempar Granat / Bazoka
                Surface(
                    onClick = {
                        if (missionOutcome != MissionOutcome.ACTIVE) return@Surface
                        val now = System.currentTimeMillis()
                        if (now - lastAttackTimeMs >= weapon.cooldownMs) {
                            lastAttackTimeMs = now
                            val aimRad = Math.toRadians(localFacingDeg.toDouble()).toFloat()
                            val hitRange = weapon.rangeNorm + weapon.aoeRadius
                            val isHit = distToThief <= hitRange
                            val targetPt = if (isHit) {
                                thiefVisX to thiefVisY
                            } else {
                                (localX + sin(aimRad) * weapon.rangeNorm).coerceIn(0.05f, 0.95f) to
                                    (localY - cos(aimRad) * weapon.rangeNorm).coerceIn(0.05f, 0.95f)
                            }
                            projectiles = projectiles + Projectile3D(
                                fromX = localX,
                                fromY = localY,
                                toX = targetPt.first,
                                toY = targetPt.second,
                                progress = 0.05f,
                                color = weapon.accentColor,
                                isEnemy = false,
                                isArc = weapon.style == WeaponStyle.GRENADE
                            )
                            spawnBurstParticles(localX, localY, 2.0f, weapon.accentColor, 4, 3f, 1.0f)
                            camShakeStrength = (camShakeStrength + if (weapon.aoeRadius > 0f) 4f else 1.8f).coerceAtMost(14f)

                            if (isHit) {
                                val nextHp = (thiefHp - weapon.damage).coerceAtLeast(0f)
                                thiefHp = nextHp
                                onWeaponHitThief()
                                if (nextHp <= 0f) {
                                    missionOutcome = MissionOutcome.VICTORY
                                    val timeBonus = (totalSecs / 3L) + missionWave * 25L
                                    onMissionVictory(timeBonus, missionWave)
                                }
                            }
                        }
                    },
                    color = weapon.primaryColor.copy(alpha = 0.94f),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, weapon.accentColor),
                    modifier = Modifier
                        .size(62.dp)
                        .testTag("fire_weapon_3d_btn")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = "Serang dengan ${weapon.name}",
                            tint = weapon.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (weapon.style == WeaponStyle.GRENADE) "LEMPAR" else "TEMBAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // 8. OVERLAY HASIL MISI GTA (VICTORY / WASTED / TIME_UP)
        if (missionOutcome != MissionOutcome.ACTIVE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC040812)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xF00F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        2.dp,
                        when (missionOutcome) {
                            MissionOutcome.VICTORY -> Color(0xFF10B981)
                            MissionOutcome.WASTED -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B)
                        }
                    ),
                    modifier = Modifier
                        .padding(20.dp)
                        .width(300.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = when (missionOutcome) {
                                MissionOutcome.VICTORY -> "MISI BERHASIL!"
                                MissionOutcome.WASTED -> "WASTED // TUMBANG"
                                MissionOutcome.TIME_UP -> "WAKTU HABIS (TIME UP)"
                                MissionOutcome.ACTIVE -> ""
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = when (missionOutcome) {
                                MissionOutcome.VICTORY -> Color(0xFF34D399)
                                MissionOutcome.WASTED -> Color(0xFFF87171)
                                else -> Color(0xFFFBBF24)
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when (missionOutcome) {
                                MissionOutcome.VICTORY -> "Pencuri berhasil dilumpuhkan sebelum waktu habis! Poin misi masuk ke akun Anda."
                                MissionOutcome.WASTED -> "Petugas kehabisan HP! Tingkatkan Baju Armor Premium di Toko untuk meredam serangan."
                                MissionOutcome.TIME_UP -> "Timer habis sebelum pencuri tumbang! Gunakan AK-47 atau Bazoka agar lebih cepat."
                                MissionOutcome.ACTIVE -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenShop,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Checkroom, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("TOKO", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    resetMission(nextWave = (missionOutcome == MissionOutcome.VICTORY))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (missionOutcome == MissionOutcome.VICTORY) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    contentColor = Color(0xFF090E1A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .testTag("mission_next_retry_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (missionOutcome == MissionOutcome.VICTORY) "MISI BARU" else "ULANGI",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
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

            drawCircle(Color(0x78090E1A), outerR, Offset(cx, cy))
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
    localFacingDeg: Float,
    thiefX: Float,
    thiefY: Float,
    isPingMode: Boolean,
    onRadarTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xDC080D1A))
            .border(
                width = 2.5.dp,
                color = if (isPingMode) Color(0xFFF59E0B) else Color(0xFF22D3EE),
                shape = CircleShape
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

            // Streets cross on GTA mini-map
            drawLine(Color(0xFF334155), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 3f)
            drawLine(Color(0xFF334155), Offset(0f, h * 0.30f), Offset(w, h * 0.30f), strokeWidth = 2.5f)
            drawLine(Color(0xFF334155), Offset(0f, h * 0.70f), Offset(w, h * 0.70f), strokeWidth = 2.5f)

            // 6 Housing Blocks + Iconic Mosque Landmark
            EstateWorld.buildings.forEach { b ->
                drawRoundRect(
                    color = if (b.isMosque) Color(0xFF10B981) else Color(0xFF1E3A5F),
                    topLeft = Offset(b.minX * w, b.minY * h),
                    size = Size((b.maxX - b.minX) * w, (b.maxY - b.minY) * h),
                    cornerRadius = CornerRadius(if (b.isMosque) 8f else 3f, if (b.isMosque) 8f else 3f)
                )
            }

            // Squadmates
            players.forEach { p ->
                drawCircle(Color(0xFF38BDF8), radius = 3.2f, center = Offset(p.posX.toFloat() * w, p.posY.toFloat() * h))
            }

            // Player directional facing triangle
            val pCenter = Offset(localX * w, localY * h)
            val rad = Math.toRadians(localFacingDeg.toDouble()).toFloat()
            val tip = Offset(pCenter.x + sin(rad) * 8f, pCenter.y - cos(rad) * 8f)
            val leftW = Offset(pCenter.x + sin(rad - 2.3f) * 5.5f, pCenter.y - cos(rad - 2.3f) * 5.5f)
            val rightW = Offset(pCenter.x + sin(rad + 2.3f) * 5.5f, pCenter.y - cos(rad + 2.3f) * 5.5f)
            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(leftW.x, leftW.y)
                    lineTo(rightW.x, rightW.y)
                    close()
                },
                Color(0xFFFDE047)
            )

            // Thief blip + GTA directional compass pointer line
            if (room.thiefSpotted) {
                val tCenter = Offset(thiefX * w, thiefY * h)
                drawLine(Color(0xFFEF4444).copy(alpha = 0.45f), pCenter, tCenter, strokeWidth = 1.5f)
                drawCircle(Color(0xFFEF4444), radius = 4.5f, center = tCenter)
                drawCircle(Color.White, radius = 4.5f, center = tCenter, style = Stroke(width = 1.2f))

                // Edge directional beacon on circular radar rim
                val dirRad = atan2((tCenter.y - h / 2f).toDouble(), (tCenter.x - w / 2f).toDouble()).toFloat()
                val rimR = (minOf(w, h) * 0.44f)
                val rimPt = Offset(w / 2f + cos(dirRad) * rimR, h / 2f + sin(dirRad) * rimR)
                drawCircle(Color(0xFFF97316), radius = 4f, center = rimPt)
            }
        }
        Surface(
            color = Color(0xBB090E1A),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
        ) {
            Text(
                text = "U",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF22D3EE),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
    }
}
