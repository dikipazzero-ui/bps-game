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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.WeaponSoundEngine
import com.example.model.AttributeCatalog
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.MissionOutcome
import com.example.model.PatrolRoom
import com.example.model.RoomPlayer
import com.example.model.TacticalPing
import com.example.model.ThiefEntity
import com.example.model.UniformCatalog
import com.example.model.UserProfile
import com.example.model.VehicleType
import com.example.model.WeaponCatalog
import com.example.model.WeaponStyle
import com.example.model.WorldVehicle
import com.example.ui.theme.JetBrainsMonoFontFamily
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
    onSyncThiefEscape: (EstateSector, Float, Float) -> Unit,
    onRadarPingTap: (EstateSector, Float, Float) -> Unit,
    onWeaponHitThief: () -> Unit,
    onApprehend: () -> Unit,
    onMissionVictory: (Long, Int) -> Unit,
    onSwitchWeapon: (String) -> Unit,
    onOpenShop: () -> Unit,
    onToggleDrawer: () -> Unit,
    onExitRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    val soundEngine = remember { WeaponSoundEngine() }
    DisposableEffect(Unit) {
        onDispose { soundEngine.release() }
    }

    val me = players.firstOrNull { it.userId == currentUserId }

    val equippedUniformId = profile?.equippedUniformId ?: me?.uniformId ?: "rookie_cadet"
    val equippedWeaponId = profile?.equippedWeaponId ?: me?.weaponId ?: "pistol_9mm"
    val equippedAttributeId = profile?.equippedAttributeId ?: me?.attributeId ?: "utility_belt"
    val unlockedWeapons = profile?.unlockedWeaponIds?.ifEmpty { listOf(equippedWeaponId) } ?: listOf(equippedWeaponId)

    val uniform = UniformCatalog.byId(equippedUniformId)
    val weapon = WeaponCatalog.byId(equippedWeaponId)
    val attribute = AttributeCatalog.byId(equippedAttributeId)

    // Local Player 3D, Camera (Yaw + Up/Down Pitch) & Vehicle State
    var localX by remember(room.id) { mutableFloatStateOf(me?.posX?.toFloat() ?: 0.50f) }
    var localY by remember(room.id) { mutableFloatStateOf(me?.posY?.toFloat() ?: 0.60f) }
    var localFacingDeg by remember(room.id) { mutableFloatStateOf(me?.facingDeg?.toFloat() ?: 0f) }
    var walkPhase by remember { mutableFloatStateOf(0f) }
    var camYaw by remember { mutableFloatStateOf(0f) }
    var camPitch by remember { mutableFloatStateOf(0.18f) }
    var isNightMode by remember(room.id) { mutableStateOf(false) }

    var vehicles by remember(room.id) { mutableStateOf(EstateWorld.defaultVehicles) }
    var mountedVehicleId by remember(room.id) { mutableStateOf<String?>(null) }

    // Multi-Thief Wave State (banyak pencuri tersebar di berbagai blok & gang!)
    var missionWave by remember(room.id) { mutableIntStateOf(1) }
    var thieves by remember(room.id) { mutableStateOf(EstateWorld.spawnThiefWave(1, localX, localY)) }
    var thiefLastSyncMs by remember { mutableLongStateOf(0L) }
    var thiefSpottedHintUntilMs by remember { mutableLongStateOf(0L) }

    // GTA Mission Combat State
    var missionOutcome by remember(room.id) { mutableStateOf(MissionOutcome.ACTIVE) }
    var timeRemainingSec by remember(room.id) { mutableFloatStateOf(180f) }
    var playerHp by remember(room.id) { mutableFloatStateOf(100f) }
    val maxPlayerHp = 100f

    // Non-blocking Floating Banners
    var lastSectorId by remember(room.id) { mutableStateOf("") }
    var zoneBannerText by remember { mutableStateOf<String?>(null) }
    var zoneBannerUntilMs by remember { mutableLongStateOf(0L) }
    var rewardToastText by remember { mutableStateOf<String?>(null) }
    var rewardToastUntilMs by remember { mutableLongStateOf(0L) }

    // Controls & Stamina
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var sprintEnabled by remember { mutableStateOf(false) }
    var stamina by remember { mutableFloatStateOf(1f) }
    var animClock by remember { mutableFloatStateOf(0f) }

    // 4D VFX: Projectiles, Particles, Camera Shake
    var projectiles by remember { mutableStateOf<List<Projectile3D>>(emptyList()) }
    var particles by remember { mutableStateOf<List<Particle3D>>(emptyList()) }
    var camShakeStrength by remember { mutableFloatStateOf(0f) }
    var camShakeX by remember { mutableFloatStateOf(0f) }
    var camShakeY by remember { mutableFloatStateOf(0f) }
    var lastAttackTimeMs by remember { mutableLongStateOf(0L) }
    var lastDustMs by remember { mutableLongStateOf(0L) }
    var lastSyncTimeMs by remember { mutableLongStateOf(0L) }
    var lastSyncedPos by remember { mutableStateOf(localX to localY) }

    fun spawnBurstParticles(
        nx: Float,
        ny: Float,
        yWorld: Float,
        color: Color,
        count: Int,
        speed: Float,
        size: Float,
        withRing: Boolean = false
    ) {
        val wPos = normToWorld(nx, ny, yWorld)
        val newPts = buildList {
            if (withRing) {
                add(Particle3D(wPos.x, 0.35f, wPos.z, 0f, 0f, 0f, color, 0.55f, 0.55f, size * 2.5f, isRing = true))
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
        particles = (particles + newPts).takeLast(110)
    }

    fun sendThiefToPrison(targetThief: ThiefEntity, reasonLabel: String) {
        if (targetThief.goingToPrison) return
        soundEngine.playArrest()
        spawnBurstParticles(targetThief.x, targetThief.y, 2.0f, Color(0xFF38BDF8), 18, 10f, 2.5f, withRing = true)
        thieves = thieves.map {
            if (it.id == targetThief.id) it.copy(goingToPrison = true, hp = 0f) else it
        }
        val secsLeft = timeRemainingSec.roundToInt().coerceAtLeast(0)
        val bonusPoints = (secsLeft / 4L) + missionWave * 20L
        rewardToastText = "$reasonLabel: ${targetThief.label} -> PENJARA SELARAS (+${120L + bonusPoints} POIN)"
        rewardToastUntilMs = System.currentTimeMillis() + 2800L
        onMissionVictory(bonusPoints, missionWave)
    }

    fun resetAfterFailure() {
        playerHp = maxPlayerHp
        thieves = EstateWorld.spawnThiefWave(missionWave, localX, localY)
        timeRemainingSec = 180f
        missionOutcome = MissionOutcome.ACTIVE
        projectiles = emptyList()
        particles = emptyList()
    }

    LaunchedEffect(me?.posX, me?.posY) {
        if (me != null && joyX == 0f && joyY == 0f && mountedVehicleId == null) {
            val dx = me.posX.toFloat() - localX
            val dy = me.posY.toFloat() - localY
            if (hypot(dx, dy) > 0.25f) {
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
            zoneBannerUntilMs = System.currentTimeMillis() + 2500L
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

    // 60 FPS Combat, Vehicle Driving, Multi-Thief AI, Prison Escort, Particles & Timer Loop
    LaunchedEffect(room.id, attribute.id, uniform.id, missionOutcome) {
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
                if (rewardToastText != null && nowMs > rewardToastUntilMs) {
                    rewardToastText = null
                }

                // Periodic radar sweep pulse
                if (((animClock % 12f) < 0.04f) && nowMs > thiefSpottedHintUntilMs) {
                    thiefSpottedHintUntilMs = nowMs + 3000L
                }

                // Camera micro-shake decay
                if (camShakeStrength > 0.05f) {
                    camShakeX = (Random.nextFloat() - 0.5f) * camShakeStrength * 2f
                    camShakeY = (Random.nextFloat() - 0.5f) * camShakeStrength * 2f
                    camShakeStrength *= (1f - dt * 8f).coerceIn(0f, 1f)
                } else {
                    camShakeStrength = 0f
                    camShakeX = 0f
                    camShakeY = 0f
                }

                // Update Particle3D list
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

                val activeVehicle = vehicles.firstOrNull { it.id == mountedVehicleId }

                // Projectiles & Damage Resolution
                if (projectiles.isNotEmpty()) {
                    val remaining = mutableListOf<Projectile3D>()
                    projectiles.forEach { proj ->
                        val nextProg = proj.progress + dt * (if (proj.isArc) 2.2f else 4.5f)
                        if (nextProg >= 1f) {
                            if (proj.isEnemy) {
                                val distHit = EstateWorld.distance(proj.toX, proj.toY, localX, localY)
                                if (distHit < 0.065f && missionOutcome == MissionOutcome.ACTIVE) {
                                    val rawDmg = 12f + missionWave * 2.2f
                                    val totalArmor = (uniform.damageReduction + (activeVehicle?.type?.armorBonus ?: 0f)).coerceAtMost(0.88f)
                                    val mitigated = rawDmg * (1f - totalArmor)
                                    playerHp = (playerHp - mitigated).coerceAtLeast(0f)
                                    camShakeStrength = (camShakeStrength + 5f).coerceAtMost(14f)
                                    spawnBurstParticles(localX, localY, 1.5f, Color(0xFFEF4444), 6, 6f, 1.8f)
                                    if (playerHp <= 0f) {
                                        missionOutcome = MissionOutcome.WASTED
                                    }
                                }
                            } else {
                                val wpn = WeaponCatalog.byId(equippedWeaponId)
                                val isBlast = wpn.aoeRadius > 0f
                                spawnBurstParticles(
                                    proj.toX, proj.toY, 1.4f,
                                    if (isBlast) Color(0xFFF97316) else wpn.accentColor,
                                    count = if (isBlast) 22 else 8,
                                    speed = if (isBlast) 15f else 7f,
                                    size = if (isBlast) 3.2f else 1.6f,
                                    withRing = isBlast
                                )
                                if (isBlast) {
                                    spawnBurstParticles(proj.toX, proj.toY, 2.2f, Color(0xFF64748B), 8, 5f, 3.6f)
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

                // Multi-Thief AI + Straight-to-Prison Escort
                val thiefStunned = nowMs < thiefStunnedUntilMs
                val newEnemyProjectiles = mutableListOf<Projectile3D>()
                val updatedThieves = mutableListOf<ThiefEntity>()

                for (thief in thieves) {
                    if (thief.goingToPrison) {
                        // Diborgol & tidak bisa kabur atau menembak -> jalan lurus masuk Penjara Selaras
                        val dPrison = EstateWorld.distance(thief.x, thief.y, EstateWorld.prisonDoorX, EstateWorld.prisonDoorY)
                        if (dPrison <= 0.028f) {
                            spawnBurstParticles(EstateWorld.prisonDoorX, EstateWorld.prisonDoorY, 2.2f, Color(0xFFF59E0B), 16, 8f, 2.2f, withRing = true)
                            continue // Masuk sel penjara!
                        }
                        val prisonAngle = atan2(
                            (EstateWorld.prisonDoorY - thief.y).toDouble(),
                            (EstateWorld.prisonDoorX - thief.x).toDouble()
                        ).toFloat()
                        val escortSpeed = 0.22f
                        val tx = thief.x + cos(prisonAngle) * escortSpeed * dt
                        val ty = thief.y + sin(prisonAngle) * escortSpeed * dt
                        val (sx, sy) = EstateWorld.moveWithCollision(thief.x, thief.y, tx, ty)
                        val moved = hypot(sx - thief.x, sy - thief.y)
                        val finalPos = if (moved < escortSpeed * dt * 0.25f) {
                            // Terhalang bangunan -> geser langsung menuju pintu penjara
                            val lerpT = (dt * 1.4f).coerceAtMost(0.4f)
                            (thief.x + (EstateWorld.prisonDoorX - thief.x) * lerpT) to
                                (thief.y + (EstateWorld.prisonDoorY - thief.y) * lerpT)
                        } else sx to sy

                        updatedThieves.add(
                            thief.copy(
                                x = finalPos.first,
                                y = finalPos.second,
                                walkPhase = thief.walkPhase + dt * 11f,
                                wanderAngle = prisonAngle
                            )
                        )
                        continue
                    }

                    val dThief = EstateWorld.distance(localX, localY, thief.x, thief.y)
                    val recentlyShot = (nowMs - thief.lastHitMs) < 3500L
                    val hasLOS = EstateWorld.hasLineOfSight(localX, localY, thief.x, thief.y)

                    val thiefSpeed = when {
                        thiefStunned -> 0.032f
                        recentlyShot -> 0.145f
                        dThief < 0.28f -> 0.125f
                        else -> 0.070f
                    }
                    val shouldFlee = dThief < 0.28f || recentlyShot

                    var nextAngle = thief.wanderAngle
                    val chosenStep: Pair<Float, Float> = if (shouldFlee) {
                        val awayAngle = atan2((thief.y - localY).toDouble(), (thief.x - localX).toDouble()).toFloat()
                        val candidates = listOf(0f, 0.70f, -0.70f, 1.35f, -1.35f, 2.1f, -2.1f)
                        var bestX = thief.x
                        var bestY = thief.y
                        var bestScore = -999f

                        for (offset in candidates) {
                            val ang = awayAngle + offset
                            val tx = thief.x + cos(ang) * thiefSpeed * dt
                            val ty = thief.y + sin(ang) * thiefSpeed * dt
                            val (sx, sy) = EstateWorld.moveWithCollision(thief.x, thief.y, tx, ty)
                            val moved = hypot(sx - thief.x, sy - thief.y)
                            if (moved > thiefSpeed * dt * 0.35f) {
                                val newDist = EstateWorld.distance(localX, localY, sx, sy)
                                val hides = if (!EstateWorld.hasLineOfSight(localX, localY, sx, sy)) 0.18f else 0f
                                val score = newDist + hides - kotlin.math.abs(offset) * 0.03f
                                if (score > bestScore) {
                                    bestScore = score
                                    bestX = sx
                                    bestY = sy
                                    nextAngle = ang
                                }
                            }
                        }
                        bestX to bestY
                    } else {
                        if (Random.nextFloat() < dt * 0.65f) {
                            nextAngle += (Random.nextFloat() - 0.5f) * 1.4f
                        }
                        val tx = thief.x + cos(nextAngle) * thiefSpeed * dt
                        val ty = thief.y + sin(nextAngle) * thiefSpeed * dt
                        val (sx, sy) = EstateWorld.moveWithCollision(thief.x, thief.y, tx, ty)
                        if (hypot(sx - thief.x, sy - thief.y) < thiefSpeed * dt * 0.25f) {
                            nextAngle += PI.toFloat() * 0.65f
                        }
                        sx to sy
                    }

                    val movedThief = hypot(chosenStep.first - thief.x, chosenStep.second - thief.y)
                    var nextDust = thief.lastDustMs
                    if (movedThief > 0.0002f && shouldFlee && nowMs - thief.lastDustMs >= 300L) {
                        nextDust = nowMs
                        spawnBurstParticles(chosenStep.first, chosenStep.second, 0.25f, Color(0xFF94A3B8), 2, 2.2f, 1.3f)
                    }

                    var nextShotMs = thief.lastShotMs
                    if (hasLOS && dThief <= 0.22f && !thiefStunned && nowMs - thief.lastShotMs >= 1850L) {
                        nextShotMs = nowMs
                        newEnemyProjectiles.add(
                            Projectile3D(
                                fromX = chosenStep.first,
                                fromY = chosenStep.second,
                                toX = localX,
                                toY = localY,
                                progress = 0.05f,
                                color = Color(0xFFEF4444),
                                isEnemy = true
                            )
                        )
                        spawnBurstParticles(chosenStep.first, chosenStep.second, 1.8f, Color(0xFFFCA5A5), 4, 4f, 1.2f)
                    }

                    updatedThieves.add(
                        thief.copy(
                            x = chosenStep.first,
                            y = chosenStep.second,
                            walkPhase = if (movedThief > 0.0002f) thief.walkPhase + dt * (if (shouldFlee) 13f else 7.5f) else thief.walkPhase,
                            wanderAngle = nextAngle,
                            lastShotMs = nextShotMs,
                            lastDustMs = nextDust
                        )
                    )
                }

                if (newEnemyProjectiles.isNotEmpty()) {
                    projectiles = projectiles + newEnemyProjectiles
                }

                if (updatedThieves.isEmpty()) {
                    // Seluruh maling gelombang ini berhasil masuk penjara -> gelombang baru muncul otomatis!
                    val nextWave = missionWave + 1
                    missionWave = nextWave
                    timeRemainingSec = 180f
                    val spawned = EstateWorld.spawnThiefWave(nextWave, localX, localY)
                    thieves = spawned
                    rewardToastText = "GELOMBANG #$nextWave: ${spawned.size} PENCURI BARU MENYEBAR!"
                    rewardToastUntilMs = nowMs + 3200L
                    spawned.firstOrNull()?.let { firstT ->
                        onSyncThiefEscape(EstateWorld.nearestTo(firstT.x, firstT.y), firstT.x, firstT.y)
                    }
                } else {
                    thieves = updatedThieves
                    val nearestActive = updatedThieves.filter { !it.goingToPrison }
                        .minByOrNull { EstateWorld.distance(localX, localY, it.x, it.y) }
                    if (nearestActive != null && nowMs - thiefLastSyncMs >= 2600L) {
                        thiefLastSyncMs = nowMs
                        onSyncThiefEscape(EstateWorld.nearestTo(nearestActive.x, nearestActive.y), nearestActive.x, nearestActive.y)
                    }
                }

                // Player Movement (On Foot vs Mobil / Motor)
                val joyMag = hypot(joyX, joyY)
                if (joyMag > 0.08f) {
                    val isDriving = activeVehicle != null
                    val isSprinting = !isDriving && sprintEnabled && stamina > 0.05f
                    if (isSprinting) {
                        stamina = (stamina - dt * 0.25f * attribute.staminaDrainFactor).coerceAtLeast(0f)
                        if (stamina <= 0.01f) sprintEnabled = false
                    } else {
                        stamina = (stamina + dt * 0.22f).coerceAtMost(1f)
                    }

                    val speed = if (isDriving) {
                        0.125f * activeVehicle!!.type.speedMultiplier
                    } else {
                        0.115f * attribute.speedMultiplier * (if (isSprinting) 1.50f else 1.0f)
                    }

                    val forwardX = sin(camYaw)
                    val forwardY = -cos(camYaw)
                    val rightX = cos(camYaw)
                    val rightY = sin(camYaw)

                    val moveX = rightX * joyX + forwardX * (-joyY)
                    val moveY = rightY * joyX + forwardY * (-joyY)
                    val normLen = hypot(moveX, moveY).coerceAtLeast(0.001f)
                    val dirX = moveX / normLen
                    val dirY = moveY / normLen

                    val nextX = localX + dirX * speed * joyMag.coerceAtMost(1f) * dt
                    val nextY = localY + dirY * speed * joyMag.coerceAtMost(1f) * dt
                    val (safeX, safeY) = EstateWorld.moveWithCollision(localX, localY, nextX, nextY)
                    localX = safeX
                    localY = safeY
                    localFacingDeg = Math.toDegrees(atan2(dirX.toDouble(), (-dirY).toDouble())).toFloat()

                    if (isDriving) {
                        vehicles = vehicles.map { v ->
                            if (v.id == mountedVehicleId) v.copy(x = safeX, y = safeY, facingDeg = localFacingDeg) else v
                        }
                    } else {
                        walkPhase += dt * (if (isSprinting) 14f else 9.5f) * joyMag
                    }

                    if (nowMs - lastDustMs >= (if (isDriving || isSprinting) 130L else 250L)) {
                        lastDustMs = nowMs
                        spawnBurstParticles(localX, localY, 0.2f, Color(0xFF94A3B8), 2, if (isDriving) 3.2f else 1.8f, 1.3f)
                    }

                    if (nowMs - lastSyncTimeMs >= 450L && hypot(localX - lastSyncedPos.first, localY - lastSyncedPos.second) > 0.012f) {
                        lastSyncTimeMs = nowMs
                        lastSyncedPos = localX to localY
                        onMoveSync(EstateWorld.nearestTo(localX, localY), localX, localY, localFacingDeg)
                    }
                } else {
                    stamina = (stamina + dt * 0.24f).coerceAtMost(1f)
                }
            }
        }
    }

    val nowMs = System.currentTimeMillis()
    val activeThieves = thieves.filter { !it.goingToPrison }
    val nearestThief = activeThieves.minByOrNull { EstateWorld.distance(localX, localY, it.x, it.y) }
    val distToNearest = nearestThief?.let { EstateWorld.distance(localX, localY, it.x, it.y) } ?: 999f

    val visibleThiefIds = remember(thieves, localX, localY, attribute.visionBoost) {
        thieves.filter { t ->
            t.goingToPrison ||
                EstateWorld.hasLineOfSight(localX, localY, t.x, t.y) ||
                EstateWorld.distance(localX, localY, t.x, t.y) <= (0.16f + attribute.visionBoost * 0.14f)
        }.map { it.id }.toSet()
    }
    val radarVisibleThiefIds = remember(thieves, localX, localY, attribute.visionBoost, nowMs < thiefSpottedHintUntilMs) {
        thieves.filter { t ->
            t.goingToPrison ||
                nowMs < thiefSpottedHintUntilMs ||
                EstateWorld.hasLineOfSight(localX, localY, t.x, t.y) ||
                EstateWorld.distance(localX, localY, t.x, t.y) <= (0.20f + attribute.visionBoost * 0.18f)
        }.map { it.id }.toSet()
    }

    val inCatchRange = nearestThief != null && distToNearest <= (0.14f + attribute.catchRadiusBonus * 0.45f)
    val totalSecs = timeRemainingSec.roundToInt().coerceAtLeast(0)
    val timerText = "%02d:%02d".format(totalSecs / 60, totalSecs % 60)

    val mountedVehicle = vehicles.firstOrNull { it.id == mountedVehicleId }
    val nearbyVehicle = if (mountedVehicle == null) {
        vehicles.minByOrNull { EstateWorld.distance(localX, localY, it.x, it.y) }
            ?.takeIf { EstateWorld.distance(localX, localY, it.x, it.y) <= 0.06f }
    } else null

    val effectiveArmorPct = ((uniform.damageReduction + (mountedVehicle?.type?.armorBonus ?: 0f)).coerceAtMost(0.88f) * 100).toInt()
    val gtaCashDisplay = "$" + ((profile?.points ?: 0L).coerceAtLeast(0L)).toString().padStart(8, '0')

    Box(modifier = modifier) {
        // 1. Fullscreen Solid 3D World Canvas (Drag horizontal = Yaw, vertical = Look Up / Down Pitch)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("estate_3d_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        camYaw += dragAmount.x * 0.006f
                        camPitch = (camPitch - dragAmount.y * 0.0045f).coerceIn(-0.22f, 0.85f)
                    }
                }
        ) {
            renderEstate3DScene(
                room = room,
                players = players,
                pings = pings,
                vehicles = vehicles,
                mountedVehicleId = mountedVehicleId,
                currentUserId = currentUserId,
                localPlayerX = localX,
                localPlayerY = localY,
                localFacingDeg = localFacingDeg,
                localWalkPhase = walkPhase,
                localWeaponId = equippedWeaponId,
                localAttributeId = equippedAttributeId,
                localUniformId = equippedUniformId,
                thieves = thieves,
                visibleThiefIds = visibleThiefIds,
                projectiles = projectiles,
                particles = particles,
                camYaw = camYaw,
                camPitch = camPitch,
                isNightMode = isNightMode,
                camShakeX = camShakeX,
                camShakeY = camShakeY,
                pulseScale = pulseScale,
                animClock = animClock,
                textMeasurer = textMeasurer
            )
        }

        // 2. POJOK KIRI ATAS (GTA San Andreas style): Peta / Mini-Radar dipindah ke ATAS!
        MiniRadarOverlay(
            players = players,
            vehicles = vehicles,
            thieves = thieves,
            radarVisibleThiefIds = radarVisibleThiefIds,
            localX = localX,
            localY = localY,
            localFacingDeg = localFacingDeg,
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
                .align(Alignment.TopStart)
                .padding(top = 10.dp, start = 10.dp)
                .size(104.dp)
        )

        // 3. ATAS TENGAH: HP Maling Terdekat (saat terlihat) + Notif Zona / Penangkapan Non-Blocking
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (nearestThief != null && nearestThief.id in visibleThiefIds && missionOutcome == MissionOutcome.ACTIVE) {
                Surface(
                    color = Color(0xD81A0B0E),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${nearestThief.label} (${(distToNearest * WORLD_SPAN).toInt()}m) • ${nearestThief.hp.roundToInt()}/${nearestThief.maxHp.toInt()} HP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        LinearProgressIndicator(
                            progress = { (nearestThief.hp / nearestThief.maxHp).coerceIn(0f, 1f) },
                            color = Color(0xFFEF4444),
                            trackColor = Color(0xFF450A0A),
                            modifier = Modifier
                                .width(134.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = rewardToastText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xDD064E3B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF34D399))
                ) {
                    Text(
                        text = rewardToastText ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6EE7B7),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = zoneBannerText != null,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xD8090E1A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF22D3EE))
                ) {
                    Text(
                        text = zoneBannerText ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 4. POJOK KANAN ATAS (GTA San Andreas HUD Cluster): Waktu/Siang-Malam, $Poin, HP/Armor + Kotak Senjata, Toko/Regu/Keluar
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kolom Jam Misi, Mode Siang/Malam, Uang/$Poin, HP & Armor
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { isNightMode = !isNightMode },
                            color = if (isNightMode) Color(0xD81E1B4B) else Color(0xD8422006),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isNightMode) Color(0xFF818CF8) else Color(0xFFFBBF24)),
                            modifier = Modifier.testTag("day_night_toggle_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNightMode) Icons.Filled.DarkMode else Icons.Filled.WbSunny,
                                    contentDescription = "Mode Siang atau Malam",
                                    tint = if (isNightMode) Color(0xFFA5B4FC) else Color(0xFFFDE047),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isNightMode) "MALAM" else "SIANG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = if (totalSecs <= 20) Color(0xFFEF4444) else Color(0xFFFDE047),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = gtaCashDisplay,
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )

                    // Bar HP Merah ala GTA SA
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${activeThieves.size} MALING • ${currentSector.shortCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2E8F0),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { (playerHp / maxPlayerHp).coerceIn(0f, 1f) },
                            color = Color(0xFFDC2626),
                            trackColor = Color(0xFF450A0A),
                            modifier = Modifier
                                .width(84.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
                        )
                    }

                    // Bar Armor + Stamina tipis
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Shield, null, tint = uniform.trimColor, modifier = Modifier.size(10.dp))
                        Text(
                            text = if (mountedVehicle != null) "LV.${uniform.armorLevel}+${mountedVehicle.type.label.uppercase()}" else "ARMOR LV.${uniform.armorLevel} (-$effectiveArmorPct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = uniform.trimColor,
                            fontSize = 8.sp
                        )
                        LinearProgressIndicator(
                            progress = { stamina },
                            color = if (sprintEnabled) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            trackColor = Color(0xFF0F172A),
                            modifier = Modifier
                                .width(42.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Kotak Ikon Senjata GTA SA di pojok kanan atas (ketuk untuk ganti senjata)
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
                    color = Color(0xD8090E1A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, weapon.accentColor),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("hud_switch_weapon_btn")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val weaponIcon = when (weapon.style) {
                            WeaponStyle.PISTOL -> Icons.Filled.GppGood
                            WeaponStyle.GRENADE -> Icons.Filled.LocalFireDepartment
                            WeaponStyle.AK47 -> Icons.Filled.Bolt
                            WeaponStyle.BAZOOKA -> Icons.Filled.RocketLaunch
                        }
                        Icon(
                            imageVector = weaponIcon,
                            contentDescription = "Ganti Senjata",
                            tint = weapon.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = weapon.name.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // Tombol cepat TOKO, REGU, KELUAR tepat di bawah kotak HUD
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onOpenShop,
                    color = Color(0xE6F59E0B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("hud_open_shop_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingBag, null, tint = Color(0xFF090E1A), modifier = Modifier.size(12.dp))
                        Text("TOKO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF090E1A), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }
                }

                Surface(
                    onClick = onToggleDrawer,
                    color = Color(0xD80F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("hud_toggle_drawer_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.Forum, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                        Text("REGU", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Surface(
                    onClick = onExitRoom,
                    color = Color(0xCC1E293B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    modifier = Modifier.testTag("exit_room_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Keluar ke Markas",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(14.dp)
                    )
                }
            }
        }

        // 5. POJOK KIRI BAWAH: HANYA Analog Joystick saja (Peta sudah dipindah ke atas!)
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
                .padding(start = 14.dp, bottom = 12.dp)
                .size(96.dp)
        )

        // 6. POJOK KANAN BAWAH: Tangkap (bekukan maling & arahkan ke Penjara!), Naik/Turun Kendaraan, Sprint, Tembak
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (inCatchRange && nearestThief != null && missionOutcome == MissionOutcome.ACTIVE) {
                Button(
                    onClick = {
                        sendThiefToPrison(nearestThief, "TERTANGKAP")
                        onApprehend()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF090E1A)),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("hud_3d_catch_btn")
                ) {
                    Icon(Icons.Filled.GppGood, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("TANGKAP!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }

            if (mountedVehicle != null) {
                Button(
                    onClick = {
                        val (dx, dy) = EstateWorld.moveWithCollision(localX, localY, localX + 0.025f, localY + 0.025f)
                        localX = dx
                        localY = dy
                        mountedVehicleId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("dismount_vehicle_btn")
                ) {
                    Icon(
                        if (mountedVehicle.type == VehicleType.CAR) Icons.Filled.DirectionsCar else Icons.Filled.TwoWheeler,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("TURUN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            } else if (nearbyVehicle != null) {
                Button(
                    onClick = {
                        mountedVehicleId = nearbyVehicle.id
                        localX = nearbyVehicle.x
                        localY = nearbyVehicle.y
                        localFacingDeg = nearbyVehicle.facingDeg
                        sprintEnabled = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("mount_vehicle_btn")
                ) {
                    Icon(
                        if (nearbyVehicle.type == VehicleType.CAR) Icons.Filled.DirectionsCar else Icons.Filled.TwoWheeler,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (nearbyVehicle.type == VehicleType.CAR) "NAIK MOBIL" else "NAIK MOTOR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (mountedVehicle == null) {
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
                }

                // Tombol Tembak / Lempar (Suara Pistol, AK-47, Granat, Bazoka)
                Surface(
                    onClick = {
                        if (missionOutcome != MissionOutcome.ACTIVE) return@Surface
                        val now = System.currentTimeMillis()
                        if (now - lastAttackTimeMs >= weapon.cooldownMs) {
                            lastAttackTimeMs = now
                            soundEngine.playWeaponFire(weapon.style)

                            val aimRad = Math.toRadians(localFacingDeg.toDouble()).toFloat()
                            val hitRange = weapon.rangeNorm * 0.65f + weapon.aoeRadius * 0.65f

                            // Cari target maling aktif terdekat dalam jangkauan tembak
                            val hitTarget = activeThieves
                                .filter { t ->
                                    val d = EstateWorld.distance(localX, localY, t.x, t.y)
                                    val canReach = weapon.aoeRadius > 0f || EstateWorld.hasLineOfSight(localX, localY, t.x, t.y)
                                    d <= hitRange && canReach
                                }
                                .minByOrNull { EstateWorld.distance(localX, localY, it.x, it.y) }

                            val targetPt = if (hitTarget != null) {
                                hitTarget.x to hitTarget.y
                            } else {
                                (localX + sin(aimRad) * weapon.rangeNorm * 0.6f).coerceIn(0.04f, 0.96f) to
                                    (localY - cos(aimRad) * weapon.rangeNorm * 0.6f).coerceIn(0.04f, 0.96f)
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

                            if (hitTarget != null) {
                                thiefSpottedHintUntilMs = now + 4500L
                                val affectedIds = if (weapon.aoeRadius > 0f) {
                                    activeThieves.filter {
                                        EstateWorld.distance(hitTarget.x, hitTarget.y, it.x, it.y) <= weapon.aoeRadius * 0.6f
                                    }.map { it.id }.toSet()
                                } else {
                                    setOf(hitTarget.id)
                                }

                                val defeated = mutableListOf<ThiefEntity>()
                                thieves = thieves.map { t ->
                                    if (t.id in affectedIds && !t.goingToPrison) {
                                        val nextHp = (t.hp - weapon.damage).coerceAtLeast(0f)
                                        val updated = t.copy(hp = nextHp, lastHitMs = now)
                                        if (nextHp <= 0f) defeated.add(updated)
                                        updated
                                    } else t
                                }
                                onWeaponHitThief()
                                defeated.forEach { d ->
                                    sendThiefToPrison(d, "LUMPUI")
                                }
                            }
                        }
                    },
                    color = weapon.primaryColor.copy(alpha = 0.94f),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, weapon.accentColor),
                    modifier = Modifier
                        .size(64.dp)
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

        // 7. OVERLAY KEKALAHAN SAJA (WASTED / TIME_UP)
        if (missionOutcome == MissionOutcome.WASTED || missionOutcome == MissionOutcome.TIME_UP) {
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
                        if (missionOutcome == MissionOutcome.WASTED) Color(0xFFEF4444) else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier
                        .padding(20.dp)
                        .width(340.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (missionOutcome == MissionOutcome.WASTED) "WASTED // TUMBANG" else "WAKTU HABIS (TIME UP)",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (missionOutcome == MissionOutcome.WASTED) Color(0xFFF87171) else Color(0xFFFBBF24),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (missionOutcome == MissionOutcome.WASTED) {
                                "Petugas kehabisan HP! Gunakan Mobil/Motor atau tingkatkan Baju Armor Premium di Toko."
                            } else {
                                "Timer habis sebelum semua pencuri dimasukkan ke Penjara Selaras! Naik kendaraan untuk mengejar."
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
                                onClick = { resetAfterFailure() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color(0xFF090E1A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .testTag("mission_next_retry_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ULANGI", fontWeight = FontWeight.ExtraBold)
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
                    if (maxR <= 1f) return@detectDragGestures
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
            if (outerR <= 1f) return@Canvas
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
    players: List<RoomPlayer>,
    vehicles: List<WorldVehicle>,
    thieves: List<ThiefEntity>,
    radarVisibleThiefIds: Set<String>,
    localX: Float,
    localY: Float,
    localFacingDeg: Float,
    isPingMode: Boolean,
    onRadarTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 16)
    val prisonTagLayout = remember(textMeasurer) {
        textMeasurer.measure("P", TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold))
    }
    val radarZoomRadius = 0.28f

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xDC080D1A))
            .border(
                width = 2.5.dp,
                color = if (isPingMode) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                shape = CircleShape
            )
            .pointerInput(isPingMode, localX, localY) {
                detectTapGestures { offset ->
                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                    val dx = (offset.x / size.width.toFloat() - 0.5f) * 2f * radarZoomRadius
                    val dy = (offset.y / size.height.toFloat() - 0.5f) * 2f * radarZoomRadius
                    val nx = (localX + dx).coerceIn(0.05f, 0.95f)
                    val ny = (localY + dy).coerceIn(0.05f, 0.95f)
                    onRadarTap(nx, ny)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas
            val cx = w * 0.5f
            val cy = h * 0.5f
            val radiusPx = minOf(w, h) * 0.5f

            fun mapX(nx: Float): Float = cx + ((nx - localX) / radarZoomRadius) * radiusPx
            fun mapY(ny: Float): Float = cy + ((ny - localY) / radarZoomRadius) * radiusPx

            val circleClip = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
            }
            clipPath(circleClip) {
                // Streets cross
                drawLine(Color(0xFF334155), Offset(mapX(0.5f), mapY(0.02f)), Offset(mapX(0.5f), mapY(0.98f)), strokeWidth = 5f)
                drawLine(Color(0xFF334155), Offset(mapX(0.02f), mapY(0.295f)), Offset(mapX(0.98f), mapY(0.295f)), strokeWidth = 4f)
                drawLine(Color(0xFF334155), Offset(mapX(0.02f), mapY(0.705f)), Offset(mapX(0.98f), mapY(0.705f)), strokeWidth = 4f)
                drawLine(Color(0xFF334155), Offset(mapX(0.337f), mapY(0.04f)), Offset(mapX(0.337f), mapY(0.96f)), strokeWidth = 3f)
                drawLine(Color(0xFF334155), Offset(mapX(0.662f), mapY(0.04f)), Offset(mapX(0.662f), mapY(0.96f)), strokeWidth = 3f)

                // All Buildings, Masjid & Penjara Selaras
                EstateWorld.allBuildings.forEach { b ->
                    val left = mapX(b.minX)
                    val top = mapY(b.minY)
                    val right = mapX(b.maxX)
                    val bottom = mapY(b.maxY)
                    drawRoundRect(
                        color = when {
                            b.isMosque -> Color(0xFF10B981)
                            b.isPrison -> Color(0xFFD97706)
                            else -> Color(0xFF334155)
                        },
                        topLeft = Offset(left, top),
                        size = Size((right - left).coerceAtLeast(3f), (bottom - top).coerceAtLeast(3f)),
                        cornerRadius = CornerRadius(if (b.isMosque) 7f else 2.5f, if (b.isMosque) 7f else 2.5f)
                    )
                }

                // Ikon P untuk Penjara Selaras
                val px = mapX(EstateWorld.prisonDoorX)
                val py = mapY(EstateWorld.prisonDoorY)
                if (px in -20f..(w + 20f) && py in -20f..(h + 20f)) {
                    drawCircle(Color(0xFFF59E0B), radius = 6.5f, center = Offset(px, py))
                    drawText(
                        prisonTagLayout,
                        topLeft = Offset(px - prisonTagLayout.size.width / 2f, py - prisonTagLayout.size.height / 2f)
                    )
                }

                // Vehicles
                vehicles.forEach { v ->
                    val vx = mapX(v.x)
                    val vy = mapY(v.y)
                    if (v.type == VehicleType.CAR) {
                        drawRect(Color(0xFF38BDF8), Offset(vx - 3f, vy - 2f), Size(6f, 4f))
                    } else {
                        drawCircle(Color(0xFFF59E0B), radius = 2.6f, center = Offset(vx, vy))
                    }
                }

                // Squadmates
                players.forEach { p ->
                    drawCircle(Color(0xFF60A5FA), radius = 3.2f, center = Offset(mapX(p.posX.toFloat()), mapY(p.posY.toFloat())))
                }

                // Multiple Thieves on Radar
                thieves.forEach { t ->
                    if (t.id in radarVisibleThiefIds) {
                        val tx = mapX(t.x)
                        val ty = mapY(t.y)
                        val dotColor = if (t.goingToPrison) Color(0xFF38BDF8) else Color(0xFFEF4444)
                        drawCircle(dotColor, radius = 4.2f, center = Offset(tx, ty))
                        drawCircle(Color.White, radius = 4.2f, center = Offset(tx, ty), style = Stroke(width = 1.1f))
                    }
                }

                // Player directional arrow in center
                val rad = Math.toRadians(localFacingDeg.toDouble()).toFloat()
                val tip = Offset(cx + sin(rad) * 8.5f, cy - cos(rad) * 8.5f)
                val leftW = Offset(cx + sin(rad - 2.3f) * 5.5f, cy - cos(rad - 2.3f) * 5.5f)
                val rightW = Offset(cx + sin(rad + 2.3f) * 5.5f, cy - cos(rad + 2.3f) * 5.5f)
                drawPath(
                    Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(leftW.x, leftW.y)
                        lineTo(rightW.x, rightW.y)
                        close()
                    },
                    Color(0xFFFDE047)
                )
            }

            // Edge compass blip on radar rim pointing to nearest active thief
            val nearestActive = thieves.filter { !it.goingToPrison && it.id in radarVisibleThiefIds }
                .minByOrNull { EstateWorld.distance(localX, localY, it.x, it.y) }
            if (nearestActive != null) {
                val dirRad = atan2((nearestActive.y - localY).toDouble(), (nearestActive.x - localX).toDouble()).toFloat()
                val rimR = radiusPx * 0.88f
                val rimPt = Offset(cx + cos(dirRad) * rimR, cy + sin(dirRad) * rimR)
                drawCircle(Color(0xFFEF4444), radius = 4.4f, center = rimPt)
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
