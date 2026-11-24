package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.AttributeCatalog
import com.example.model.AttributeStyle
import com.example.model.BuildingFootprint
import com.example.model.EstateWorld
import com.example.model.GateStatus
import com.example.model.PatrolRoom
import com.example.model.PingType
import com.example.model.PlayerRole
import com.example.model.RoomPlayer
import com.example.model.TacticalPing
import com.example.model.UniformCatalog
import com.example.model.WeaponCatalog
import com.example.model.WeaponStyle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float)

data class Particle3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float,
    val size: Float,
    val isRing: Boolean = false
)

data class Projectile3D(
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val progress: Float,
    val color: Color,
    val isEnemy: Boolean = false,
    val isArc: Boolean = false
)

data class Camera3D(
    val pos: Vec3,
    val yaw: Float,
    val pitch: Float,
    val focal: Float,
    val cx: Float,
    val cy: Float
) {
    private val sinY = sin(yaw)
    private val cosY = cos(yaw)
    private val sinP = sin(pitch)
    private val cosP = cos(pitch)

    fun project(w: Vec3): Pair<Offset, Float>? {
        val dx = w.x - pos.x
        val dy = w.y - pos.y
        val dz = w.z - pos.z
        val right = dx * cosY + dz * sinY
        val fwdHoriz = dx * sinY - dz * cosY
        val depth = fwdHoriz * cosP - dy * sinP
        if (depth < 1.2f) return null
        val upCam = fwdHoriz * sinP + dy * cosP
        val sx = cx + (right / depth) * focal
        val sy = cy - (upCam / depth) * focal
        return Offset(sx, sy) to depth
    }
}

fun normToWorld(nx: Float, ny: Float, y: Float = 0f): Vec3 =
    Vec3((nx - 0.5f) * 100f, y, (ny - 0.5f) * 100f)

private data class DepthObject(
    val depth: Float,
    val draw: DrawScope.() -> Unit
)

private data class ParkedCar(
    val nx: Float,
    val ny: Float,
    val horizontal: Boolean,
    val bodyColor: Color,
    val isPatrolCar: Boolean
)

private val estateVehicles = listOf(
    ParkedCar(0.36f, 0.295f, horizontal = true, bodyColor = Color(0xFF0F172A), isPatrolCar = true),
    ParkedCar(0.64f, 0.295f, horizontal = true, bodyColor = Color(0xFF991B1B), isPatrolCar = false),
    ParkedCar(0.36f, 0.705f, horizontal = true, bodyColor = Color(0xFF1D4ED8), isPatrolCar = false),
    ParkedCar(0.64f, 0.705f, horizontal = true, bodyColor = Color(0xFFE2E8F0), isPatrolCar = true)
)

fun DrawScope.renderEstate3DScene(
    room: PatrolRoom,
    players: List<RoomPlayer>,
    pings: List<TacticalPing>,
    currentUserId: String,
    localPlayerX: Float,
    localPlayerY: Float,
    localFacingDeg: Float,
    localWalkPhase: Float,
    localWeaponId: String,
    localAttributeId: String,
    localUniformId: String,
    thiefPos: Pair<Float, Float>,
    thiefStunned: Boolean,
    projectiles: List<Projectile3D>,
    particles: List<Particle3D>,
    camYaw: Float,
    camShakeX: Float,
    camShakeY: Float,
    pulseScale: Float,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val focal = minOf(w, h) * 0.92f
    val attr = AttributeCatalog.byId(localAttributeId)
    val nvgActive = attr.style == AttributeStyle.NVG_GOGGLES

    // Dynamic "4D" Time-of-Day atmosphere blend (Dusk -> Night -> Dawn -> Dusk)
    val atmoCycle = (sin(animClock * 0.14f) * 0.5f + 0.5f).coerceIn(0f, 1f)
    val shadowShiftX = cos(animClock * 0.14f) * 0.032f
    val shadowShiftY = sin(animClock * 0.14f) * 0.022f + 0.028f

    val playerWorld = normToWorld(localPlayerX, localPlayerY, 0f)
    val camDist = 20f
    val camPos = Vec3(
        x = playerWorld.x - sin(camYaw) * camDist,
        y = 10.8f,
        z = playerWorld.z + cos(camYaw) * camDist
    )
    val cam = Camera3D(
        pos = camPos,
        yaw = camYaw,
        pitch = 0.40f,
        focal = focal,
        cx = w * 0.50f + camShakeX,
        cy = h * 0.44f + camShakeY
    )

    // 1. Dynamic 4D Sky & Atmospheric Horizon
    val skyTop = if (nvgActive) Color(0xFF031A12)
    else lerp(Color(0xFF040814), Color(0xFF172554), atmoCycle)
    val skyHorizon = if (nvgActive) Color(0xFF093826)
    else lerp(Color(0xFF0F172A), Color(0xFF7C2D12).copy(alpha = 0.72f), atmoCycle * 0.75f)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyHorizon, Color(0xFF0A111F)),
            startY = 0f,
            endY = h
        )
    )

    // Atmospheric Sun / Moon Glow & Horizon Stars
    val celestialX = (w * 0.5f + sin(camYaw + atmoCycle * 1.4f) * w * 0.35f)
    val celestialY = h * (0.14f - atmoCycle * 0.05f)
    val celestialColor = if (nvgActive) Color(0xFF34D399) else lerp(Color(0xFFE2E8F0), Color(0xFFFB923C), atmoCycle)
    drawCircle(celestialColor.copy(alpha = 0.16f), radius = 46f, center = Offset(celestialX, celestialY))
    drawCircle(celestialColor.copy(alpha = 0.75f), radius = 12f, center = Offset(celestialX, celestialY))

    for (i in 0..18) {
        val sx = ((i * 97 + (camYaw * 120f).toInt()) % w.toInt().coerceAtLeast(100)).toFloat().let {
            if (it < 0f) it + w else it
        }
        val sy = h * (0.03f + (i % 5) * 0.032f)
        drawCircle(
            color = celestialColor.copy(alpha = (0.48f - atmoCycle * 0.25f).coerceAtLeast(0.12f)),
            radius = if (i % 3 == 0) 2.2f else 1.3f,
            center = Offset(sx, sy)
        )
    }

    // 2. Ground Plane (Bumi Papan Selaras Housing Grid)
    val grassColor = if (nvgActive) Color(0xFF0B291D) else lerp(Color(0xFF0B1322), Color(0xFF10202E), atmoCycle * 0.6f)
    drawGroundQuad3D(cam, 0.03f, 0.03f, 0.97f, 0.97f, grassColor)

    // Tactical grid lines on grass
    val gridColor = if (nvgActive) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFF1E293B).copy(alpha = 0.45f)
    for (i in 1..9) {
        val t = i / 10f
        drawGroundLine3D(cam, t, 0.05f, t, 0.95f, gridColor, 1.3f)
        drawGroundLine3D(cam, 0.05f, t, 0.95f, t, gridColor, 1.3f)
    }

    // Paved Asphalt Streets connecting Krisan, Cempaka, Cemara, Akasia, Camelia, Flamboyan & Masjid
    val roadColor = if (nvgActive) Color(0xFF133628) else Color(0xFF1C2434)
    drawGroundQuad3D(cam, 0.06f, 0.265f, 0.94f, 0.325f, roadColor) // Jl. Krisan - Cempaka
    drawGroundQuad3D(cam, 0.06f, 0.675f, 0.94f, 0.735f, roadColor) // Jl. Camelia - Flamboyan
    drawGroundQuad3D(cam, 0.06f, 0.07f, 0.105f, 0.93f, roadColor)  // Gang Barat
    drawGroundQuad3D(cam, 0.895f, 0.07f, 0.94f, 0.93f, roadColor)  // Gang Timur
    drawGroundQuad3D(cam, 0.31f, 0.07f, 0.37f, 0.93f, roadColor)   // Poros Barat Masjid
    drawGroundQuad3D(cam, 0.63f, 0.07f, 0.69f, 0.93f, roadColor)   // Poros Timur Masjid
    drawGroundQuad3D(cam, 0.46f, 0.06f, 0.54f, 0.94f, roadColor)   // Poros Utama Masjid (U-S)
    drawGroundQuad3D(cam, 0.26f, 0.47f, 0.74f, 0.53f, roadColor)   // Jalan Tengah Masjid

    // Wet Asphalt Reflections / Glossy Rain Puddles (4D Realism)
    val puddleGlow = if (nvgActive) Color(0xFF34D399).copy(alpha = 0.20f) else lerp(Color(0xFF38BDF8), Color(0xFFF59E0B), atmoCycle).copy(alpha = 0.18f)
    listOf(
        Triple(0.34f, 0.30f, 0.03f),
        Triple(0.66f, 0.30f, 0.028f),
        Triple(0.50f, 0.63f, 0.035f),
        Triple(0.34f, 0.70f, 0.032f),
        Triple(0.66f, 0.70f, 0.026f)
    ).forEach { (rx, ry, rad) ->
        drawGroundQuad3D(cam, rx - rad, ry - rad * 0.6f, rx + rad, ry + rad * 0.6f, puddleGlow, y = 0.05f)
    }

    // Road center stripes
    val stripe = Color(0xFF94A3B8).copy(alpha = 0.65f)
    drawGroundLine3D(cam, 0.08f, 0.295f, 0.92f, 0.295f, stripe, 2.2f)
    drawGroundLine3D(cam, 0.08f, 0.705f, 0.92f, 0.705f, stripe, 2.2f)
    drawGroundLine3D(cam, 0.50f, 0.08f, 0.50f, 0.92f, stripe, 2.2f)
    drawGroundLine3D(cam, 0.34f, 0.09f, 0.34f, 0.91f, stripe, 1.8f)
    drawGroundLine3D(cam, 0.66f, 0.09f, 0.66f, 0.91f, stripe, 1.8f)

    // Mosque Courtyard Plaza (Area Masjid Agung Selaras)
    drawGroundQuad3D(cam, 0.40f, 0.40f, 0.60f, 0.60f, if (nvgActive) Color(0xFF065F46) else Color(0xFF0F766E).copy(alpha = 0.55f), y = 0.02f)

    // 2b. Real-Time Sharp Directional Ground Shadows under Buildings & Mosque
    val shadowColor = Color(0xFF020617).copy(alpha = 0.55f)
    EstateWorld.buildings.forEach { b ->
        drawGroundQuad3D(
            cam,
            (b.minX + shadowShiftX).coerceIn(0.03f, 0.96f),
            (b.minY + shadowShiftY).coerceIn(0.03f, 0.96f),
            (b.maxX + shadowShiftX).coerceIn(0.04f, 0.97f),
            (b.maxY + shadowShiftY).coerceIn(0.04f, 0.97f),
            shadowColor,
            y = 0.03f
        )
    }

    // 3. Collect Depth-Sorted 3D Objects
    val depthObjects = ArrayList<DepthObject>(72)

    // 3a. 3D Buildings (6 Residential Blocks + Iconic Masjid Agung)
    EstateWorld.buildings.forEach { b ->
        val center = normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, b.height3D * 0.45f)
        val proj = cam.project(center)
        val dist = proj?.second ?: distance3D(cam.pos, center)
        depthObjects.add(DepthObject(dist) {
            if (b.isMosque) {
                drawMosque3D(cam, b, nvgActive, animClock, textMeasurer)
            } else {
                drawBuilding3D(cam, b, nvgActive, textMeasurer)
            }
        })
    }

    // 3b. Parked Resident & Patrol Vehicles (with Headlight / Taillight Reflections)
    estateVehicles.forEach { car ->
        val cw = normToWorld(car.nx, car.ny, 1f)
        depthObjects.add(DepthObject(distance3D(cam.pos, cw)) {
            drawCar3D(cam, car, nvgActive, animClock)
        })
    }

    // 3c. North & South 3D Gates
    listOf(
        Triple("GERBANG UTARA", 0.50f to 0.08f, GateStatus.safeValueOf(room.northGateStatus)),
        Triple("GERBANG SELATAN", 0.50f to 0.92f, GateStatus.safeValueOf(room.southGateStatus))
    ).forEach { (label, pos, status) ->
        val gw = normToWorld(pos.first, pos.second, 2f)
        depthObjects.add(DepthObject(distance3D(cam.pos, gw)) {
            drawGate3D(cam, pos.first, pos.second, status, label, textMeasurer)
        })
    }

    // 3d. Street Lamps & Shade Trees around Bumi Papan Selaras
    val lampCoords = listOf(
        0.38f to 0.26f, 0.62f to 0.26f,
        0.38f to 0.74f, 0.62f to 0.74f,
        0.27f to 0.42f, 0.73f to 0.42f,
        0.27f to 0.58f, 0.73f to 0.58f
    )
    lampCoords.forEach { (lx, ly) ->
        val lw = normToWorld(lx, ly, 2.5f)
        depthObjects.add(DepthObject(distance3D(cam.pos, lw)) {
            drawStreetLamp3D(cam, lx, ly, nvgActive)
        })
    }

    val treeCoords = listOf(
        0.41f to 0.39f, 0.59f to 0.39f,
        0.41f to 0.61f, 0.59f to 0.61f,
        0.27f to 0.37f, 0.73f to 0.37f,
        0.27f to 0.63f, 0.73f to 0.63f,
        0.13f to 0.36f, 0.87f to 0.36f
    )
    treeCoords.forEach { (tx, ty) ->
        val tw = normToWorld(tx, ty, 2f)
        depthObjects.add(DepthObject(distance3D(cam.pos, tw)) {
            drawTree3D(cam, tx, ty, shadowShiftX, shadowShiftY)
        })
    }

    // 3e. Tactical Ping 3D Light Pillars
    pings.take(6).forEach { ping ->
        val px = ping.posX.toFloat()
        val py = ping.posY.toFloat()
        val pw = normToWorld(px, py, 2f)
        val pingColor = when (PingType.safeValueOf(ping.pingType)) {
            PingType.SUSPECT -> Color(0xFFEF4444)
            PingType.BACKUP -> Color(0xFFF59E0B)
            PingType.BLOCK_GATE -> Color(0xFF06B6D4)
            PingType.CLEAR -> Color(0xFF10B981)
        }
        depthObjects.add(DepthObject(distance3D(cam.pos, pw)) {
            val baseProj = cam.project(normToWorld(px, py, 0f))
            val topProj = cam.project(normToWorld(px, py, 9f))
            if (baseProj != null && topProj != null) {
                val (bPt, bDepth) = baseProj
                val (tPt, _) = topProj
                val r = (180f / bDepth).coerceIn(8f, 36f) * pulseScale
                drawCircle(pingColor.copy(alpha = 0.25f), r, bPt)
                drawLine(pingColor.copy(alpha = 0.85f), bPt, tPt, strokeWidth = (75f / bDepth).coerceIn(3f, 12f), cap = StrokeCap.Round)
                drawCircle(pingColor, (60f / bDepth).coerceIn(4f, 12f), tPt)
            }
        })
    }

    // 3f. Thief 3D Character
    if (room.thiefSpotted) {
        val (tx, ty) = thiefPos
        val tw = normToWorld(tx, ty, 1.5f)
        depthObjects.add(DepthObject(distance3D(cam.pos, tw)) {
            drawThief3D(cam, tx, ty, thiefStunned, animClock, pulseScale, nvgActive, textMeasurer)
        })
    }

    // 3g. Multiplayer Guard Characters
    players.forEach { player ->
        val isMe = player.userId == currentUserId
        val px = if (isMe) localPlayerX else player.posX.toFloat()
        val py = if (isMe) localPlayerY else player.posY.toFloat()
        val facing = if (isMe) localFacingDeg else player.facingDeg.toFloat()
        val walkPhase = if (isMe) localWalkPhase else (animClock * 3f)
        val uId = if (isMe) localUniformId else player.uniformId
        val wId = if (isMe) localWeaponId else player.weaponId
        val aId = if (isMe) localAttributeId else player.attributeId
        val pw = normToWorld(px, py, 1.5f)
        depthObjects.add(DepthObject(distance3D(cam.pos, pw)) {
            drawGuard3D(
                cam = cam,
                nx = px,
                ny = py,
                facingDeg = facing,
                walkPhase = walkPhase,
                uniformId = uId,
                weaponId = wId,
                attributeId = aId,
                isMe = isMe,
                isThiefRole = player.role == PlayerRole.THIEF.name,
                pttActive = player.pttActive,
                label = if (isMe) "ANDA" else player.displayName.take(9),
                pulseScale = pulseScale,
                animClock = animClock,
                textMeasurer = textMeasurer
            )
        })
    }

    if (players.none { it.userId == currentUserId }) {
        val pw = normToWorld(localPlayerX, localPlayerY, 1.5f)
        depthObjects.add(DepthObject(distance3D(cam.pos, pw)) {
            drawGuard3D(
                cam = cam,
                nx = localPlayerX,
                ny = localPlayerY,
                facingDeg = localFacingDeg,
                walkPhase = localWalkPhase,
                uniformId = localUniformId,
                weaponId = localWeaponId,
                attributeId = localAttributeId,
                isMe = true,
                isThiefRole = false,
                pttActive = false,
                label = "ANDA",
                pulseScale = pulseScale,
                animClock = animClock,
                textMeasurer = textMeasurer
            )
        })
    }

    // Painter's Algorithm Sort
    depthObjects.sortByDescending { it.depth }
    depthObjects.forEach { it.draw(this) }

    // 4. Active 3D Projectiles (Player Bullets, Grenades, Rockets & Thief Return Shots)
    projectiles.forEach { proj ->
        val p = proj.progress.coerceIn(0f, 1f)
        val s3 = normToWorld(proj.fromX, proj.fromY, 2.2f)
        val e3 = normToWorld(proj.toX, proj.toY, if (proj.isArc) 0.5f else 2.0f)
        val arcLift = if (proj.isArc) sin(p * PI.toFloat()) * 6.5f else 0f
        val cur3 = Vec3(
            s3.x + (e3.x - s3.x) * p,
            s3.y + (e3.y - s3.y) * p + arcLift,
            s3.z + (e3.z - s3.z) * p
        )
        val tailP = (p - 0.14f).coerceAtLeast(0f)
        val tailLift = if (proj.isArc) sin(tailP * PI.toFloat()) * 6.5f else 0f
        val tail3 = Vec3(
            s3.x + (e3.x - s3.x) * tailP,
            s3.y + (e3.y - s3.y) * tailP + tailLift,
            s3.z + (e3.z - s3.z) * tailP
        )
        val tProj = cam.project(tail3)
        val cProj = cam.project(cur3)
        if (tProj != null && cProj != null) {
            drawLine(
                color = proj.color.copy(alpha = 0.92f),
                start = tProj.first,
                end = cProj.first,
                strokeWidth = if (proj.isArc) 7.5f else 5.5f,
                cap = StrokeCap.Round
            )
            drawCircle(Color.White, radius = if (proj.isEnemy) 5f else 6.5f, center = cProj.first)
            drawCircle(proj.color.copy(alpha = 0.45f), radius = 14f, center = cProj.first)
        }
    }

    // 5. High-Particle 4D VFX (Dust, Sparks, Smoke Plumes, Fireballs & Blast Rings)
    particles.forEach { pt ->
        val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
        val screenProj = cam.project(Vec3(pt.x, pt.y, pt.z)) ?: return@forEach
        val (pos2D, depth) = screenProj
        val scaledR = ((pt.size * 22f) / depth).coerceIn(2f, 90f)
        if (pt.isRing) {
            val ringRadius = scaledR * (1.8f - alpha * 0.9f)
            drawCircle(
                color = pt.color.copy(alpha = alpha * 0.8f),
                radius = ringRadius,
                center = pos2D,
                style = Stroke(width = 4.5f * alpha + 1f)
            )
        } else {
            drawCircle(
                color = pt.color.copy(alpha = alpha * 0.85f),
                radius = scaledR,
                center = pos2D
            )
        }
    }
}

private fun distance3D(a: Vec3, b: Vec3): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}

private fun DrawScope.drawGroundQuad3D(
    cam: Camera3D,
    minX: Float, minY: Float, maxX: Float, maxY: Float,
    color: Color,
    y: Float = 0f
) {
    val p1 = cam.project(normToWorld(minX, minY, y))?.first ?: return
    val p2 = cam.project(normToWorld(maxX, minY, y))?.first ?: return
    val p3 = cam.project(normToWorld(maxX, maxY, y))?.first ?: return
    val p4 = cam.project(normToWorld(minX, maxY, y))?.first ?: return
    drawPath(
        Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            close()
        },
        color
    )
}

private fun DrawScope.drawGroundLine3D(
    cam: Camera3D,
    x1: Float, y1: Float, x2: Float, y2: Float,
    color: Color,
    width: Float
) {
    val a = cam.project(normToWorld(x1, y1, 0.04f))?.first ?: return
    val b = cam.project(normToWorld(x2, y2, 0.04f))?.first ?: return
    drawLine(color, a, b, strokeWidth = width)
}

private fun DrawScope.drawBuilding3D(
    cam: Camera3D,
    b: BuildingFootprint,
    nvgActive: Boolean,
    textMeasurer: TextMeasurer
) {
    val cornersBase = listOf(
        normToWorld(b.minX, b.minY, 0f),
        normToWorld(b.maxX, b.minY, 0f),
        normToWorld(b.maxX, b.maxY, 0f),
        normToWorld(b.minX, b.maxY, 0f)
    )
    val cornersTop = listOf(
        normToWorld(b.minX, b.minY, b.height3D),
        normToWorld(b.maxX, b.minY, b.height3D),
        normToWorld(b.maxX, b.maxY, b.height3D),
        normToWorld(b.minX, b.maxY, b.height3D)
    )

    val pBase = cornersBase.map { cam.project(it)?.first }
    val pTop = cornersTop.map { cam.project(it)?.first }

    val faces = (0..3).map { i ->
        val j = (i + 1) % 4
        val mid = Vec3(
            (cornersBase[i].x + cornersBase[j].x) * 0.5f,
            b.height3D * 0.5f,
            (cornersBase[i].z + cornersBase[j].z) * 0.5f
        )
        Triple(i, j, distance3D(cam.pos, mid))
    }.sortedByDescending { it.third }

    val wallTint = if (nvgActive) Color(0xFF123B2B) else b.wallColor
    val roofTint = if (nvgActive) Color(0xFF19523B) else b.roofColor
    val trimTint = if (nvgActive) Color(0xFF34D399) else b.accentColor

    faces.forEach { (i, j, _) ->
        val b1 = pBase[i] ?: return@forEach
        val b2 = pBase[j] ?: return@forEach
        val t2 = pTop[j] ?: return@forEach
        val t1 = pTop[i] ?: return@forEach
        val facePath = Path().apply {
            moveTo(b1.x, b1.y)
            lineTo(b2.x, b2.y)
            lineTo(t2.x, t2.y)
            lineTo(t1.x, t1.y)
            close()
        }
        drawPath(facePath, wallTint)
        drawPath(facePath, trimTint.copy(alpha = 0.48f), style = Stroke(width = 1.6f))

        for (row in 1..3) {
            val fy = row * 0.24f
            for (col in 1..3) {
                val fx = col * 0.24f
                val w1 = Offset(b1.x + (b2.x - b1.x) * fx, b1.y + (t1.y - b1.y) * fy)
                val w2 = Offset(b1.x + (b2.x - b1.x) * (fx + 0.08f), b1.y + (t1.y - b1.y) * fy)
                val winColor = if ((row + col + i) % 2 == 0) Color(0xFFFDE047).copy(alpha = 0.60f) else trimTint.copy(alpha = 0.35f)
                drawLine(winColor, w1, w2, strokeWidth = 3.2f)
            }
        }
    }

    if (pTop.all { it != null }) {
        val r0 = pTop[0]!!
        val r1 = pTop[1]!!
        val r2 = pTop[2]!!
        val r3 = pTop[3]!!
        val roofPath = Path().apply {
            moveTo(r0.x, r0.y)
            lineTo(r1.x, r1.y)
            lineTo(r2.x, r2.y)
            lineTo(r3.x, r3.y)
            close()
        }
        drawPath(roofPath, roofTint)
        drawPath(roofPath, trimTint.copy(alpha = 0.85f), style = Stroke(width = 2.2f))
    }

    val labelProj = cam.project(normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, b.height3D + 2.2f))
    if (labelProj != null && labelProj.second < 78f) {
        val measured = textMeasurer.measure(
            b.label,
            TextStyle(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
        )
        drawText(measured, topLeft = Offset(labelProj.first.x - measured.size.width / 2f, labelProj.first.y))
    }
}

private fun DrawScope.drawMosque3D(
    cam: Camera3D,
    b: BuildingFootprint,
    nvgActive: Boolean,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val cornersBase = listOf(
        normToWorld(b.minX, b.minY, 0f),
        normToWorld(b.maxX, b.minY, 0f),
        normToWorld(b.maxX, b.maxY, 0f),
        normToWorld(b.minX, b.maxY, 0f)
    )
    val cornersTop = listOf(
        normToWorld(b.minX, b.minY, b.height3D),
        normToWorld(b.maxX, b.minY, b.height3D),
        normToWorld(b.maxX, b.maxY, b.height3D),
        normToWorld(b.minX, b.maxY, b.height3D)
    )
    val pBase = cornersBase.map { cam.project(it)?.first }
    val pTop = cornersTop.map { cam.project(it)?.first }

    val wallTint = if (nvgActive) Color(0xFF6EE7B7) else Color(0xFFF1F5F9)
    val domeColor = if (nvgActive) Color(0xFF10B981) else Color(0xFF047857)
    val goldAccent = Color(0xFFFDE047)

    val faces = (0..3).map { i ->
        val j = (i + 1) % 4
        val mid = Vec3((cornersBase[i].x + cornersBase[j].x) * 0.5f, b.height3D * 0.5f, (cornersBase[i].z + cornersBase[j].z) * 0.5f)
        Triple(i, j, distance3D(cam.pos, mid))
    }.sortedByDescending { it.third }

    faces.forEach { (i, j, _) ->
        val b1 = pBase[i] ?: return@forEach
        val b2 = pBase[j] ?: return@forEach
        val t2 = pTop[j] ?: return@forEach
        val t1 = pTop[i] ?: return@forEach
        val facePath = Path().apply {
            moveTo(b1.x, b1.y)
            lineTo(b2.x, b2.y)
            lineTo(t2.x, t2.y)
            lineTo(t1.x, t1.y)
            close()
        }
        drawPath(facePath, wallTint.copy(alpha = 0.92f))
        drawPath(facePath, domeColor, style = Stroke(width = 1.8f))
        // Arched glowing mosque doorway/windows
        val midBot = Offset((b1.x + b2.x) * 0.5f, (b1.y + b2.y) * 0.5f)
        val archTop = Offset((t1.x + t2.x) * 0.5f, ((t1.y + t2.y) * 0.5f + midBot.y) * 0.5f)
        drawLine(Color(0xFFFDE047).copy(alpha = 0.72f), midBot, archTop, strokeWidth = 7f, cap = StrokeCap.Round)
    }

    if (pTop.all { it != null }) {
        val roofPath = Path().apply {
            moveTo(pTop[0]!!.x, pTop[0]!!.y)
            lineTo(pTop[1]!!.x, pTop[1]!!.y)
            lineTo(pTop[2]!!.x, pTop[2]!!.y)
            lineTo(pTop[3]!!.x, pTop[3]!!.y)
            close()
        }
        drawPath(roofPath, Color(0xFFCBD5E1))
        drawPath(roofPath, goldAccent, style = Stroke(width = 2f))
    }

    // Main Emerald-Gold Dome
    val cx = (b.minX + b.maxX) * 0.5f
    val cy = (b.minY + b.maxY) * 0.5f
    val domeProj = cam.project(normToWorld(cx, cy, b.height3D + 2.6f))
    if (domeProj != null) {
        val (dPt, depth) = domeProj
        val domeR = (210f / depth).coerceIn(10f, 44f)
        drawCircle(domeColor, radius = domeR, center = dPt)
        drawCircle(goldAccent.copy(alpha = 0.75f), radius = domeR, center = dPt, style = Stroke(width = 2.4f))
        drawCircle(goldAccent.copy(alpha = 0.45f), radius = domeR * 0.4f, center = Offset(dPt.x - domeR * 0.2f, dPt.y - domeR * 0.25f))
        // Spire crescent on top of dome
        val spireTop = Offset(dPt.x, dPt.y - domeR * 1.45f)
        drawLine(goldAccent, Offset(dPt.x, dPt.y - domeR * 0.8f), spireTop, strokeWidth = 3f, cap = StrokeCap.Round)
        drawCircle(goldAccent, radius = (domeR * 0.22f).coerceIn(3f, 8f), center = spireTop)
    }

    // Tall Mosque Minaret Tower on Northeast corner
    val minBase = cam.project(normToWorld(b.maxX + 0.01f, b.minY - 0.01f, 0f))
    val minTop = cam.project(normToWorld(b.maxX + 0.01f, b.minY - 0.01f, b.height3D + 11f))
    if (minBase != null && minTop != null) {
        val (mb, mDepth) = minBase
        val mt = minTop.first
        drawLine(Color(0xFFE2E8F0), mb, mt, strokeWidth = (120f / mDepth).coerceIn(4f, 13f), cap = StrokeCap.Square)
        drawCircle(domeColor, radius = (85f / mDepth).coerceIn(4f, 12f), center = mt)
        val beaconAlpha = 0.55f + sin(animClock * 3f) * 0.35f
        drawCircle(goldAccent.copy(alpha = beaconAlpha), radius = (55f / mDepth).coerceIn(3f, 9f), center = Offset(mt.x, mt.y - 8f))
    }

    val labelProj = cam.project(normToWorld(cx, cy, b.height3D + 9.5f))
    if (labelProj != null && labelProj.second < 85f) {
        val measured = textMeasurer.measure(
            "MASJID AGUNG SELARAS",
            TextStyle(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = goldAccent)
        )
        drawText(measured, topLeft = Offset(labelProj.first.x - measured.size.width / 2f, labelProj.first.y))
    }
}

private fun DrawScope.drawCar3D(cam: Camera3D, car: ParkedCar, nvgActive: Boolean, animClock: Float) {
    val base = cam.project(normToWorld(car.nx, car.ny, 0.2f)) ?: return
    val roof = cam.project(normToWorld(car.nx, car.ny, 1.8f)) ?: return
    val (bPt, depth) = base
    val rPt = roof.first
    val s = (130f / depth).coerceIn(4f, 22f)
    val wCar = if (car.horizontal) s * 2.2f else s * 1.15f
    val hCar = if (car.horizontal) s * 1.0f else s * 1.6f
    val bodyTint = if (nvgActive) Color(0xFF10B981) else car.bodyColor

    // Ground shadow
    drawOval(Color.Black.copy(alpha = 0.45f), Offset(bPt.x - wCar * 0.58f, bPt.y - hCar * 0.35f), Size(wCar * 1.16f, hCar * 0.85f))
    // Lower chassis
    drawRoundRect(
        color = bodyTint,
        topLeft = Offset(bPt.x - wCar * 0.5f, bPt.y - hCar * 0.5f),
        size = Size(wCar, hCar * 0.8f),
        cornerRadius = CornerRadiusCompat(4f)
    )
    // Glossy roof cabin reflection
    drawRoundRect(
        color = Color(0xFF93C5FD).copy(alpha = 0.65f),
        topLeft = Offset(rPt.x - wCar * 0.32f, rPt.y - hCar * 0.32f),
        size = Size(wCar * 0.64f, hCar * 0.5f),
        cornerRadius = CornerRadiusCompat(3f)
    )
    // Patrol car siren & headlights
    if (car.isPatrolCar) {
        val redBlue = if ((animClock * 4f).toInt() % 2 == 0) Color(0xFF38BDF8) else Color(0xFFEF4444)
        drawCircle(redBlue, radius = (s * 0.28f).coerceIn(2f, 6f), center = rPt)
    }
    drawCircle(Color(0xFFFDE047).copy(alpha = 0.7f), radius = (s * 0.22f).coerceIn(1.8f, 5f), center = Offset(bPt.x + wCar * 0.45f, bPt.y))
    drawCircle(Color(0xFFEF4444).copy(alpha = 0.75f), radius = (s * 0.20f).coerceIn(1.5f, 4.5f), center = Offset(bPt.x - wCar * 0.45f, bPt.y))
}

private fun DrawScope.drawGate3D(
    cam: Camera3D,
    nx: Float,
    ny: Float,
    status: GateStatus,
    label: String,
    textMeasurer: TextMeasurer
) {
    val gateColor = when (status) {
        GateStatus.OPEN -> Color(0xFF10B981)
        GateStatus.ALERT -> Color(0xFFF59E0B)
        GateStatus.LOCKED -> Color(0xFFEF4444)
    }
    val leftBase = cam.project(normToWorld(nx - 0.055f, ny, 0f))?.first ?: return
    val leftTop = cam.project(normToWorld(nx - 0.055f, ny, 4.8f))?.first ?: return
    val rightBase = cam.project(normToWorld(nx + 0.055f, ny, 0f))?.first ?: return
    val rightTop = cam.project(normToWorld(nx + 0.055f, ny, 4.8f))?.first ?: return

    drawLine(Color(0xFF475569), leftBase, leftTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Color(0xFF475569), rightBase, rightTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(gateColor, leftTop, rightTop, strokeWidth = 5f)

    val barPivot = cam.project(normToWorld(nx - 0.05f, ny, 1.5f))?.first
    val barTipWorld = if (status == GateStatus.LOCKED) {
        normToWorld(nx + 0.05f, ny, 1.5f)
    } else {
        normToWorld(nx - 0.02f, ny, 4.4f)
    }
    val barTip = cam.project(barTipWorld)?.first
    if (barPivot != null && barTip != null) {
        drawLine(gateColor, barPivot, barTip, strokeWidth = 5f, cap = StrokeCap.Round)
    }

    val tag = cam.project(normToWorld(nx, ny, 6.2f))
    if (tag != null && tag.second < 80f) {
        val m = textMeasurer.measure(
            "$label: ${status.labelId}",
            TextStyle(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = gateColor)
        )
        drawText(m, topLeft = Offset(tag.first.x - m.size.width / 2f, tag.first.y))
    }
}

private fun DrawScope.drawStreetLamp3D(cam: Camera3D, nx: Float, ny: Float, nvgActive: Boolean) {
    val base = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val top = cam.project(normToWorld(nx, ny, 5.2f)) ?: return
    val (bPt, depth) = base
    val tPt = top.first
    drawLine(Color(0xFF475569), bPt, tPt, strokeWidth = (45f / depth).coerceIn(2f, 6f))
    val glow = if (nvgActive) Color(0xFF6EE7B7) else Color(0xFFFDE047)
    drawCircle(glow.copy(alpha = 0.22f), radius = (190f / depth).coerceIn(8f, 38f), center = bPt)
    drawCircle(glow, radius = (45f / depth).coerceIn(2.5f, 7f), center = tPt)
}

private fun DrawScope.drawTree3D(cam: Camera3D, nx: Float, ny: Float, shX: Float, shY: Float) {
    val base = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val crown = cam.project(normToWorld(nx, ny, 4.0f)) ?: return
    val shadowPt = cam.project(normToWorld((nx + shX * 0.45f).coerceIn(0.05f, 0.95f), (ny + shY * 0.45f).coerceIn(0.05f, 0.95f), 0.02f))?.first
    val (bPt, depth) = base
    val cPt = crown.first
    val r = (140f / depth).coerceIn(6f, 28f)
    if (shadowPt != null) {
        drawOval(Color.Black.copy(alpha = 0.35f), Offset(shadowPt.x - r, shadowPt.y - r * 0.45f), Size(r * 2f, r * 0.9f))
    }
    drawLine(Color(0xFF78350F), bPt, cPt, strokeWidth = (60f / depth).coerceIn(2.5f, 8f))
    drawCircle(Color(0xFF065F46), radius = r, center = cPt)
    drawCircle(Color(0xFF10B981).copy(alpha = 0.72f), radius = r * 0.68f, center = Offset(cPt.x - r * 0.15f, cPt.y - r * 0.2f))
}

private fun DrawScope.drawGuard3D(
    cam: Camera3D,
    nx: Float,
    ny: Float,
    facingDeg: Float,
    walkPhase: Float,
    uniformId: String,
    weaponId: String,
    attributeId: String,
    isMe: Boolean,
    isThiefRole: Boolean,
    pttActive: Boolean,
    label: String,
    pulseScale: Float,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val footProj = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val headProj = cam.project(normToWorld(nx, ny, 3.2f)) ?: return
    val (footPt, depth) = footProj
    val uniform = UniformCatalog.byId(uniformId)
    val weapon = WeaponCatalog.byId(weaponId)
    val attr = AttributeCatalog.byId(attributeId)

    val charH = (footPt.y - headProj.first.y).coerceIn(18f, 160f)
    val s = charH / 48f
    val bodyColor = if (isThiefRole) Color(0xFFEF4444) else uniform.primaryColor
    val trimColor = if (pttActive) Color(0xFF22D3EE) else uniform.trimColor

    val relRad = Math.toRadians(facingDeg.toDouble()).toFloat() - cam.yaw
    val dirX = sin(relRad)
    val dirZ = cos(relRad)

    if (pttActive) {
        drawCircle(Color(0xFF22D3EE).copy(alpha = 0.35f), radius = 18f * s * pulseScale, center = footPt)
    }
    if (isMe) {
        drawOval(
            color = Color(0xFFFBBF24).copy(alpha = 0.45f),
            topLeft = Offset(footPt.x - 16f * s, footPt.y - 6f * s),
            size = Size(32f * s, 12f * s),
            style = Stroke(width = 2.4f)
        )
        // Weapon laser/aim sight line ahead
        val aheadProj = cam.project(
            normToWorld(
                (nx + sin(Math.toRadians(facingDeg.toDouble()).toFloat()) * weapon.rangeNorm * 0.50f).coerceIn(0.05f, 0.95f),
                (ny - cos(Math.toRadians(facingDeg.toDouble()).toFloat()) * weapon.rangeNorm * 0.50f).coerceIn(0.05f, 0.95f),
                0.08f
            )
        )?.first
        if (aheadProj != null) {
            drawLine(weapon.accentColor.copy(alpha = 0.24f), footPt, aheadProj, strokeWidth = 6f * s, cap = StrokeCap.Round)
        }
    }

    // Ground shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.48f),
        topLeft = Offset(footPt.x - 12f * s, footPt.y - 4f * s),
        size = Size(24f * s, 8f * s)
    )

    // Animated legs + Speed Boots
    val stride = sin(walkPhase) * 5.5f * s
    val bootColor = if (attr.style == AttributeStyle.SPEED_BOOTS) attr.accentColor else Color(0xFF0F172A)
    drawLine(Color(0xFF1E293B), Offset(footPt.x - 4f * s, footPt.y - 16f * s), Offset(footPt.x - 4f * s, footPt.y + stride * 0.5f), strokeWidth = 5f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFF1E293B), Offset(footPt.x + 4f * s, footPt.y - 16f * s), Offset(footPt.x + 4f * s, footPt.y - stride * 0.5f), strokeWidth = 5f * s, cap = StrokeCap.Round)
    drawCircle(bootColor, radius = 3.2f * s, center = Offset(footPt.x - 4f * s, footPt.y + stride * 0.5f))
    drawCircle(bootColor, radius = 3.2f * s, center = Offset(footPt.x + 4f * s, footPt.y - stride * 0.5f))

    // Cape / Collar for Armor Lv 4-5
    if (uniform.hasCapeOrCollar) {
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.65f),
            topLeft = Offset(footPt.x - 10f * s, footPt.y - 35f * s),
            size = Size(20f * s, 22f * s),
            cornerRadius = CornerRadiusCompat(4f * s)
        )
    }

    // Torso (Equipped Armor)
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 36f * s),
        size = Size(17f * s, 21f * s),
        cornerRadius = CornerRadiusCompat(4.5f * s)
    )
    if (uniform.hasShoulderPads) {
        drawCircle(trimColor, radius = 3.2f * s, center = Offset(footPt.x - 9f * s, footPt.y - 33f * s))
        drawCircle(trimColor, radius = 3.2f * s, center = Offset(footPt.x + 9f * s, footPt.y - 33f * s))
    }
    if (attr.style == AttributeStyle.KEVLAR_VEST) {
        drawRoundRect(
            color = attr.primaryColor,
            topLeft = Offset(footPt.x - 7f * s, footPt.y - 34f * s),
            size = Size(14f * s, 14f * s),
            cornerRadius = CornerRadiusCompat(3f * s)
        )
        drawRoundRect(
            color = attr.accentColor,
            topLeft = Offset(footPt.x - 7f * s, footPt.y - 34f * s),
            size = Size(14f * s, 14f * s),
            cornerRadius = CornerRadiusCompat(3f * s),
            style = Stroke(width = 1.4f * s)
        )
    }
    drawLine(trimColor, Offset(footPt.x, footPt.y - 34f * s), Offset(footPt.x, footPt.y - 17f * s), strokeWidth = 1.8f * s)

    // Head + NVG Goggles
    val headCenter = Offset(footPt.x, footPt.y - 43f * s)
    drawCircle(Color(0xFFF1C27D), radius = 7f * s, center = headCenter)
    drawArc(
        color = Color(0xFF0F172A),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headCenter.x - 7.5f * s, headCenter.y - 8f * s),
        size = Size(15f * s, 11f * s)
    )
    if (attr.style == AttributeStyle.NVG_GOGGLES) {
        drawCircle(attr.accentColor, radius = 2.4f * s, center = Offset(headCenter.x - 2.5f * s + dirX * 2f * s, headCenter.y))
        drawCircle(attr.accentColor, radius = 2.4f * s, center = Offset(headCenter.x + 2.5f * s + dirX * 2f * s, headCenter.y))
    }

    // Held Weapon in Right Hand (Pistol, Granat, AK-47, Bazoka)
    val handSide = if (dirZ >= 0f) 10f * s else -10f * s
    val handPos = Offset(footPt.x + handSide, footPt.y - 24f * s)
    when (weapon.style) {
        WeaponStyle.PISTOL -> {
            val muzzle = Offset(handPos.x + (dirX * 10f + 5f) * s, handPos.y - 5f * s)
            drawLine(weapon.primaryColor, handPos, muzzle, strokeWidth = 3.2f * s, cap = StrokeCap.Round)
            drawCircle(weapon.accentColor, radius = 2.2f * s, center = muzzle)
        }
        WeaponStyle.GRENADE -> {
            drawCircle(weapon.primaryColor, radius = 4f * s, center = handPos)
            drawCircle(weapon.accentColor, radius = 2f * s, center = Offset(handPos.x, handPos.y - 3f * s))
        }
        WeaponStyle.AK47 -> {
            val stock = Offset(handPos.x - (dirX * 6f) * s, handPos.y + 1f * s)
            val muzzle = Offset(handPos.x + (dirX * 15f + 7f) * s, handPos.y - 9f * s)
            drawLine(weapon.primaryColor, stock, muzzle, strokeWidth = 4f * s, cap = StrokeCap.Round)
            drawLine(weapon.accentColor, handPos, Offset(handPos.x + 4f * s, handPos.y + 4f * s), strokeWidth = 2.6f * s)
        }
        WeaponStyle.BAZOOKA -> {
            val back = Offset(handPos.x - (dirX * 10f) * s, handPos.y + 2f * s)
            val muzzle = Offset(handPos.x + (dirX * 18f + 8f) * s, handPos.y - 12f * s)
            drawLine(weapon.primaryColor, back, muzzle, strokeWidth = 6.2f * s, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFDE047), radius = 3.5f * s, center = muzzle)
        }
    }

    // Floating Scanner Drone over shoulder
    if (attr.style == AttributeStyle.SCANNER_DRONE) {
        val dronePt = Offset(footPt.x - 14f * s, footPt.y - 52f * s + sin(animClock * 4f) * 3f * s)
        drawRoundRect(
            color = attr.primaryColor,
            topLeft = Offset(dronePt.x - 5f * s, dronePt.y - 3f * s),
            size = Size(10f * s, 6f * s),
            cornerRadius = CornerRadiusCompat(3f * s)
        )
        drawCircle(attr.accentColor, radius = 2.2f * s, center = dronePt)
    }

    if (depth < 80f) {
        val tag = textMeasurer.measure(
            label,
            TextStyle(
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isMe) Color(0xFFFDE047) else Color.White
            )
        )
        drawText(tag, topLeft = Offset(headCenter.x - tag.size.width / 2f, headCenter.y - 24f * s))
    }
}

private fun DrawScope.drawThief3D(
    cam: Camera3D,
    nx: Float,
    ny: Float,
    stunned: Boolean,
    animClock: Float,
    pulseScale: Float,
    nvgActive: Boolean,
    textMeasurer: TextMeasurer
) {
    val footProj = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val headProj = cam.project(normToWorld(nx, ny, 3.1f)) ?: return
    val (footPt, depth) = footProj
    val charH = (footPt.y - headProj.first.y).coerceIn(16f, 150f)
    val s = charH / 46f

    val alertColor = if (stunned) Color(0xFF22D3EE) else Color(0xFFEF4444)
    drawCircle(alertColor.copy(alpha = 0.25f), radius = 20f * s * pulseScale, center = footPt)
    drawCircle(alertColor, radius = 14f * s, center = footPt, style = Stroke(width = 2.2f))

    val stride = if (stunned) 0f else sin(animClock * 7f) * 6f * s
    drawLine(Color(0xFF111827), Offset(footPt.x - 4f * s, footPt.y - 14f * s), Offset(footPt.x - 4f * s, footPt.y + stride * 0.4f), strokeWidth = 4.8f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFF111827), Offset(footPt.x + 4f * s, footPt.y - 14f * s), Offset(footPt.x + 4f * s, footPt.y - stride * 0.4f), strokeWidth = 4.8f * s, cap = StrokeCap.Round)

    drawRoundRect(
        color = Color(0xFF7F1D1D),
        topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 34f * s),
        size = Size(17f * s, 20f * s),
        cornerRadius = CornerRadiusCompat(4f * s)
    )
    if (nvgActive) {
        drawRoundRect(
            color = Color(0xFF34D399),
            topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 34f * s),
            size = Size(17f * s, 20f * s),
            cornerRadius = CornerRadiusCompat(4f * s),
            style = Stroke(width = 2f)
        )
    }

    val headPt = Offset(footPt.x, footPt.y - 40f * s)
    drawCircle(Color(0xFF1F2937), radius = 7f * s, center = headPt)
    drawCircle(alertColor, radius = 2.5f * s, center = Offset(headPt.x, headPt.y - 1f * s))

    // Thief weapon in hand
    val tHand = Offset(footPt.x + 10f * s, footPt.y - 23f * s)
    drawLine(Color(0xFF334155), tHand, Offset(tHand.x + 10f * s, tHand.y - 5f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)

    if (stunned) {
        drawCircle(Color(0xFF38BDF8), radius = 11f * s, center = headPt, style = Stroke(width = 2f))
    }

    if (depth < 85f) {
        val m = textMeasurer.measure(
            if (stunned) "TERLUKA!" else "PENCURI!",
            TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (stunned) Color(0xFF67E8F9) else Color(0xFFFCA5A5)
            )
        )
        drawText(m, topLeft = Offset(headPt.x - m.size.width / 2f, headPt.y - 24f * s))
    }
}

private fun CornerRadiusCompat(r: Float) = androidx.compose.ui.geometry.CornerRadius(r, r)
