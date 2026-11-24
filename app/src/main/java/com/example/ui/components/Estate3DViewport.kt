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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.example.model.VehicleType
import com.example.model.WeaponCatalog
import com.example.model.WeaponStyle
import com.example.model.WorldVehicle
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
    val textMeasurer = rememberTextMeasurer()
    val me = players.firstOrNull { it.userId == currentUserId }

    val equippedUniformId = profile?.equippedUniformId ?: me?.uniformId ?: "rookie_cadet"
    val equippedWeaponId = profile?.equippedWeaponId ?: me?.weaponId ?: "pistol_9mm"
    val equippedAttributeId = profile?.equippedAttributeId ?: me?.attributeId ?: "utility_belt"
    val unlockedWeapons = profile?.unlockedWeaponIds?.ifEmpty { listOf(equippedWeaponId) } ?: listOf(equippedWeaponId)

    val uniform = UniformCatalog.byId(equippedUniformId)
    val weapon = WeaponCatalog.byId(equippedWeaponId)
    val attribute = AttributeCatalog.byId(equippedAttributeId)

    // Local Player 3D & Vehicle State
    var localX by remember(room.id) { mutableFloatStateOf(me?.posX?.toFloat() ?: 0.50f) }
    var localY by remember(room.id) { mutableFloatStateOf(me?.posY?.toFloat() ?: 0.60f) }
    var localFacingDeg by remember(room.id) { mutableFloatStateOf(me?.facingDeg?.toFloat() ?: 0f) }
    var walkPhase by remember { mutableFloatStateOf(0f) }
    var camYaw by remember { mutableFloatStateOf(0f) }

    var vehicles by remember(room.id) { mutableStateOf(EstateWorld.defaultVehicles) }
    var mountedVehicleId by remember(room.id) { mutableStateOf<String?>(null) }

    // Active Fleeing & Hiding Thief AI State (HP jauh lebih tebal!)
    var thiefVisX by remember(room.id) { mutableFloatStateOf(room.activeThiefX.toFloat()) }
    var thiefVisY by remember(room.id) { mutableFloatStateOf(room.activeThiefY.toFloat()) }
    var thiefWalkPhase by remember { mutableFloatStateOf(0f) }
    var thiefWanderAngle by remember(room.id) { mutableFloatStateOf(Random.nextFloat() * 2f * PI.toFloat()) }
    var thiefLastHitMs by remember { mutableLongStateOf(0L) }
    var thiefLastSyncMs by remember { mutableLongStateOf(0L) }
    var thiefSpottedHintUntilMs by remember { mutableLongStateOf(0L) }

    // GTA Mission Combat State
    var missionWave by remember(room.id) { mutableIntStateOf(1) }
    var missionOutcome by remember(room.id) { mutableStateOf(MissionOutcome.ACTIVE) }
    var timeRemainingSec by remember(room.id) { mutableFloatStateOf(150f) }
    var playerHp by remember(room.id) { mutableFloatStateOf(100f) }
    val maxPlayerHp = 100f
    val maxThiefHp = 350f + (missionWave - 1) * 100f
    var thiefHp by remember(room.id) { mutableFloatStateOf(350f) }

    // Non-blocking Floating Banners (Zone Entry & Seamless Score Gain)
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
    var lastThiefShotMs by remember { mutableLongStateOf(0L) }
    var lastDustMs by remember { mutableLongStateOf(0L) }
    var lastThiefDustMs by remember { mutableLongStateOf(0L) }
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
        particles = (particles + newPts).takeLast(96)
    }

    fun spawnNextThiefSeamless(completedWave: Int, bonusPoints: Long) {
        val nextWave = completedWave + 1
        missionWave = nextWave
        thiefHp = 350f + (nextWave - 1) * 100f
        timeRemainingSec = 150f

        // Pick distant sector so thief hides across expanded Bumi Papan Selaras
        val distantCandidates = EstateWorld.sectors.filter { sec ->
            EstateWorld.distance(localX, localY, sec.normX, sec.normY) >= 0.38f &&
                EstateWorld.isWalkable(sec.normX, sec.normY)
        }
        val spawnSec = distantCandidates.randomOrNull() ?: EstateWorld.sectors.first()
        val (sx, sy) = EstateWorld.moveWithCollision(
            spawnSec.normX,
            spawnSec.normY,
            spawnSec.normX + (Random.nextFloat() - 0.5f) * 0.06f,
            spawnSec.normY + (Random.nextFloat() - 0.5f) * 0.06f
        )
        thiefVisX = sx
        thiefVisY = sy
        thiefWanderAngle = Random.nextFloat() * 2f * PI.toFloat()
        thiefSpottedHintUntilMs = System.currentTimeMillis() + 3500L

        // Floating non-blocking reward text only (no popup!)
        rewardToastText = "+${120L + bonusPoints} POIN • PENCURI BARU DI ${spawnSec.shortCode}"
        rewardToastUntilMs = System.currentTimeMillis() + 2500L
        onMissionVictory(bonusPoints, completedWave)
        onSyncThiefEscape(spawnSec, sx, sy)
    }

    fun resetAfterFailure() {
        playerHp = maxPlayerHp
        thiefHp = 350f + (missionWave - 1) * 100f
        timeRemainingSec = 150f
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

    // For non-host clients, smoothly follow Firestore thief coordinate
    val isHost = room.hostUid.isEmpty() || room.hostUid == currentUserId
    LaunchedEffect(room.activeThiefX, room.activeThiefY, isHost) {
        if (!isHost) {
            thiefVisX = room.activeThiefX.toFloat()
            thiefVisY = room.activeThiefY.toFloat()
        }
    }

    val currentSector = EstateWorld.nearestTo(localX, localY)
    LaunchedEffect(currentSector.id) {
        if (currentSector.id != lastSectorId) {
            lastSectorId = currentSector.id
            zoneBannerText = "MEMASUKI ${currentSector.name.uppercase()}"
            zoneBannerUntilMs = System.currentTimeMillis() + 2600L
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

    // 60 FPS Combat, Vehicle Driving, Thief Run-and-Hide AI, Particles & Timer Loop
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

                // Periodic brief radar ping pulse every 14 seconds so thief isn't impossible across 360m
                if (((animClock % 14f) < 0.04f) && nowMs > thiefSpottedHintUntilMs) {
                    thiefSpottedHintUntilMs = nowMs + 2800L
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
                                    val rawDmg = 14f + missionWave * 2.5f
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
                                    count = if (isBlast) 20 else 8,
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

                // Active Run & Hide Thief AI (Host authoritative)
                val dThief = EstateWorld.distance(localX, localY, thiefVisX, thiefVisY)
                val thiefStunned = nowMs < thiefStunnedUntilMs
                val recentlyShot = (nowMs - thiefLastHitMs) < 3500L
                val thiefHasLOS = EstateWorld.hasLineOfSight(localX, localY, thiefVisX, thiefVisY)

                if (isHost) {
                    val thiefSpeed = when {
                        thiefStunned -> 0.032f
                        recentlyShot -> 0.145f
                        dThief < 0.30f -> 0.125f
                        else -> 0.072f
                    }
                    val shouldFlee = dThief < 0.30f || recentlyShot

                    val chosenStep: Pair<Float, Float> = if (shouldFlee) {
                        val awayAngle = atan2((thiefVisY - localY).toDouble(), (thiefVisX - localX).toDouble()).toFloat()
                        val candidates = listOf(0f, 0.70f, -0.70f, 1.35f, -1.35f, 2.1f, -2.1f)
                        var bestX = thiefVisX
                        var bestY = thiefVisY
                        var bestScore = -999f

                        for (offset in candidates) {
                            val ang = awayAngle + offset
                            val tx = thiefVisX + cos(ang) * thiefSpeed * dt
                            val ty = thiefVisY + sin(ang) * thiefSpeed * dt
                            val (sx, sy) = EstateWorld.moveWithCollision(thiefVisX, thiefVisY, tx, ty)
                            val moved = hypot(sx - thiefVisX, sy - thiefVisY)
                            if (moved > thiefSpeed * dt * 0.35f) {
                                val newDist = EstateWorld.distance(localX, localY, sx, sy)
                                val hidesFromPlayer = if (!EstateWorld.hasLineOfSight(localX, localY, sx, sy)) 0.18f else 0f
                                val score = newDist + hidesFromPlayer - kotlin.math.abs(offset) * 0.03f
                                if (score > bestScore) {
                                    bestScore = score
                                    bestX = sx
                                    bestY = sy
                                    thiefWanderAngle = ang
                                }
                            }
                        }
                        bestX to bestY
                    } else {
                        // Prowl between blocks & duck behind cover
                        if (Random.nextFloat() < dt * 0.65f) {
                            thiefWanderAngle += (Random.nextFloat() - 0.5f) * 1.4f
                        }
                        val tx = thiefVisX + cos(thiefWanderAngle) * thiefSpeed * dt
                        val ty = thiefVisY + sin(thiefWanderAngle) * thiefSpeed * dt
                        val (sx, sy) = EstateWorld.moveWithCollision(thiefVisX, thiefVisY, tx, ty)
                        if (hypot(sx - thiefVisX, sy - thiefVisY) < thiefSpeed * dt * 0.25f) {
                            thiefWanderAngle += (PI.toFloat() * 0.65f)
                        }
                        sx to sy
                    }

                    val movedThief = hypot(chosenStep.first - thiefVisX, chosenStep.second - thiefVisY)
                    thiefVisX = chosenStep.first
                    thiefVisY = chosenStep.second
                    if (movedThief > 0.0002f) {
                        thiefWalkPhase += dt * (if (shouldFlee) 13f else 7.5f)
                        if (shouldFlee && nowMs - lastThiefDustMs >= 260L) {
                            lastThiefDustMs = nowMs
                            spawnBurstParticles(thiefVisX, thiefVisY, 0.25f, Color(0xFF94A3B8), 2, 2.2f, 1.4f)
                        }
                    }

                    if (nowMs - thiefLastSyncMs >= 2600L) {
                        thiefLastSyncMs = nowMs
                        onSyncThiefEscape(EstateWorld.nearestTo(thiefVisX, thiefVisY), thiefVisX, thiefVisY)
                    }
                }

                // Thief Return Fire when he has Line-of-Sight within combat range
                if (room.thiefSpotted && thiefHasLOS && dThief <= 0.24f && !thiefStunned && nowMs - lastThiefShotMs >= 1650L) {
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
                    spawnBurstParticles(thiefVisX, thiefVisY, 1.8f, Color(0xFFFCA5A5), 5, 4f, 1.3f)
                }

                // Player Movement (On Foot vs Driving Mobil / Motor)
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
    val distToThief = EstateWorld.distance(localX, localY, thiefVisX, thiefVisY)
    val hasLOS = EstateWorld.hasLineOfSight(localX, localY, thiefVisX, thiefVisY)
    val thiefVisible3D = hasLOS || distToThief <= (0.14f + attribute.visionBoost * 0.14f)
    val thiefDetectedHud = hasLOS || distToThief <= (0.20f + attribute.visionBoost * 0.20f) || nowMs < thiefSpottedHintUntilMs
    val inCatchRange = distToThief <= (0.09f + attribute.catchRadiusBonus * 0.4f)
    val isThiefStunned = nowMs < thiefStunnedUntilMs
    val totalSecs = timeRemainingSec.roundToInt().coerceAtLeast(0)
    val timerText = "%02d:%02d".format(totalSecs / 60, totalSecs % 60)

    val mountedVehicle = vehicles.firstOrNull { it.id == mountedVehicleId }
    val nearbyVehicle = if (mountedVehicle == null) {
        vehicles.minByOrNull { EstateWorld.distance(localX, localY, it.x, it.y) }
            ?.takeIf { EstateWorld.distance(localX, localY, it.x, it.y) <= 0.06f }
    } else null

    val effectiveArmorPct = ((uniform.damageReduction + (mountedVehicle?.type?.armorBonus ?: 0f)).coerceAtMost(0.88f) * 100).toInt()

    Box(modifier = modifier) {
        // 1. Fullscreen 4D Perspective Game Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("estate_3d_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        camYaw += dragAmount.x * 0.006f
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
                thiefPos = thiefVisX to thiefVisY,
                thiefVisible = thiefVisible3D,
                thiefStunned = isThiefStunned,
                thiefWalkPhase = thiefWalkPhase,
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

        // 2. POJOK KIRI ATAS (dibawah marquee transparan): HP, Armor Lv, Stamina & Status Kendaraan
        Surface(
            color = Color(0xC8090E1A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 34.dp, start = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("HP", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (playerHp / maxPlayerHp).coerceIn(0f, 1f) },
                        color = if (playerHp <= 30f) Color(0xFFEF4444) else Color(0xFF10B981),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .width(74.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text("${playerHp.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = uniform.trimColor, modifier = Modifier.size(11.dp))
                    Text(
                        text = if (mountedVehicle != null) {
                            "ARMOR LV.${uniform.armorLevel} + ${mountedVehicle.type.label.uppercase()} (-$effectiveArmorPct%)"
                        } else {
                            "ARMOR LV.${uniform.armorLevel} (-$effectiveArmorPct%)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = uniform.trimColor,
                        fontSize = 9.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("STA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 9.sp)
                    LinearProgressIndicator(
                        progress = { stamina },
                        color = if (sprintEnabled) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .width(70.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // 3. ATAS TENGAH (dibawah marquee transparan): HP Pencuri (saat terdeteksi) + Notif Zona / Poin non-blocking
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (room.thiefSpotted && thiefDetectedHud && missionOutcome == MissionOutcome.ACTIVE) {
                Surface(
                    color = Color(0xD01A0B0E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PENCURI ${(distToThief * WORLD_SPAN).toInt()}m • ${thiefHp.roundToInt()}/${maxThiefHp.toInt()} HP",
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
                                .width(126.dp)
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

        // 4. POJOK KANAN ATAS: Timer, Poin, Lokasi, Shortcut Senjata, TOKO, REGU & KELUAR
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 34.dp, end = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = Color(0xC8090E1A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (totalSecs <= 20) Color(0xFFEF4444) else Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MISI #$missionWave • ${currentSector.shortCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "${profile?.points ?: 0} POIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF22D3EE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (totalSecs <= 20) Color(0xFFEF4444) else Color(0xFFFDE047),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

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
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, weapon.accentColor),
                    modifier = Modifier.testTag("hud_switch_weapon_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.testTag("hud_open_shop_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingBag, null, tint = Color(0xFF090E1A), modifier = Modifier.size(13.dp))
                        Text("TOKO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF090E1A), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }
                }

                Surface(
                    onClick = onToggleDrawer,
                    color = Color(0xD80F172A),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("hud_toggle_drawer_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.Forum, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                        Text("REGU", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Surface(
                    onClick = onExitRoom,
                    color = Color(0xCC1E293B),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    modifier = Modifier.testTag("exit_room_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Keluar ke Markas",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(15.dp)
                    )
                }
            }
        }

        // 5. POJOK KIRI BAWAH (Landscape): Mini-Map Zoomed GTA + Joystick di kanannya (tidak bertumpuk)
        MiniRadarOverlay(
            room = room,
            players = players,
            vehicles = vehicles,
            localX = localX,
            localY = localY,
            localFacingDeg = localFacingDeg,
            thiefX = thiefVisX,
            thiefY = thiefVisY,
            showThiefOnRadar = thiefDetectedHud,
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
                .padding(start = 10.dp, bottom = 10.dp)
                .size(102.dp)
        )

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
                .padding(start = 124.dp, bottom = 10.dp)
                .size(92.dp)
        )

        // 6. POJOK KANAN BAWAH: Naik/Turun Mobil & Motor, Sprint, Tangkap & Tembak/Lempar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 10.dp),
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
                        .height(44.dp)
                        .testTag("hud_3d_catch_btn")
                ) {
                    Icon(Icons.Filled.GppGood, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TANGKAP!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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

                // Tombol Tembak / Lempar Granat / Bazoka
                Surface(
                    onClick = {
                        if (missionOutcome != MissionOutcome.ACTIVE) return@Surface
                        val now = System.currentTimeMillis()
                        if (now - lastAttackTimeMs >= weapon.cooldownMs) {
                            lastAttackTimeMs = now
                            val aimRad = Math.toRadians(localFacingDeg.toDouble()).toFloat()
                            val hitRange = weapon.rangeNorm * 0.65f + weapon.aoeRadius * 0.65f
                            val canHitThrough = weapon.aoeRadius > 0f || EstateWorld.hasLineOfSight(localX, localY, thiefVisX, thiefVisY)
                            val isHit = distToThief <= hitRange && canHitThrough
                            val targetPt = if (isHit) {
                                thiefVisX to thiefVisY
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

                            if (isHit) {
                                thiefLastHitMs = now
                                thiefSpottedHintUntilMs = now + 4500L
                                val nextHp = (thiefHp - weapon.damage).coerceAtLeast(0f)
                                thiefHp = nextHp
                                onWeaponHitThief()
                                if (nextHp <= 0f) {
                                    spawnBurstParticles(thiefVisX, thiefVisY, 2.0f, Color(0xFFFDE047), 24, 14f, 3.0f, withRing = true)
                                    val timeBonus = (totalSecs / 3L) + missionWave * 25L
                                    spawnNextThiefSeamless(completedWave = missionWave, bonusPoints = timeBonus)
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

        // 7. OVERLAY KEKALAHAN SAJA (WASTED / TIME_UP) — TANPA OVERLAY VICTORY!
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
                                "Timer habis sebelum pencuri tumbang! Naik kendaraan untuk mengejar & upgrade senjata."
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
    vehicles: List<WorldVehicle>,
    localX: Float,
    localY: Float,
    localFacingDeg: Float,
    thiefX: Float,
    thiefY: Float,
    showThiefOnRadar: Boolean,
    isPingMode: Boolean,
    onRadarTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local zoomed radius around player so expanded Bumi Papan Selaras blocks & alleys stay crisp
    val radarZoomRadius = 0.28f

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xDC080D1A))
            .border(
                width = 2.5.dp,
                color = if (isPingMode) Color(0xFFF59E0B) else Color(0xFF22D3EE),
                shape = CircleShape
            )
            .pointerInput(isPingMode, localX, localY) {
                detectTapGestures { offset ->
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

                // All 15 Estate Buildings & Masjid Landmark
                EstateWorld.allBuildings.forEach { b ->
                    val left = mapX(b.minX)
                    val top = mapY(b.minY)
                    val right = mapX(b.maxX)
                    val bottom = mapY(b.maxY)
                    drawRoundRect(
                        color = if (b.isMosque) Color(0xFF10B981) else Color(0xFF1E3A5F),
                        topLeft = Offset(left, top),
                        size = Size((right - left).coerceAtLeast(3f), (bottom - top).coerceAtLeast(3f)),
                        cornerRadius = CornerRadius(if (b.isMosque) 7f else 2.5f, if (b.isMosque) 7f else 2.5f)
                    )
                }

                // Vehicles (Mobil = Cyan square, Motor = Amber dot)
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

                // Thief blip only when detected / in line-of-sight / radar pinged
                if (room.thiefSpotted && showThiefOnRadar) {
                    val tx = mapX(thiefX)
                    val ty = mapY(thiefY)
                    drawCircle(Color(0xFFEF4444), radius = 4.6f, center = Offset(tx, ty))
                    drawCircle(Color.White, radius = 4.6f, center = Offset(tx, ty), style = Stroke(width = 1.2f))
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

            // Edge compass blip on radar rim during detection/ping windows
            if (room.thiefSpotted && showThiefOnRadar) {
                val dirRad = atan2((thiefY - localY).toDouble(), (thiefX - localX).toDouble()).toFloat()
                val rimR = radiusPx * 0.88f
                val rimPt = Offset(cx + cos(dirRad) * rimR, cy + sin(dirRad) * rimR)
                drawCircle(Color(0xFFF97316), radius = 4.2f, center = rimPt)
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
