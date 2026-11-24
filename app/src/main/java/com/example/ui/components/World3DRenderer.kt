package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
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
import com.example.model.ThiefEntity
import com.example.model.UniformCatalog
import com.example.model.VehicleType
import com.example.model.WeaponCatalog
import com.example.model.WeaponStyle
import com.example.model.WorldVehicle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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
        if (depth < 1.5f) return null
        val upCam = fwdHoriz * sinP + dy * cosP
        val sx = cx + (right / depth) * focal
        val sy = cy - (upCam / depth) * focal
        if (!sx.isFinite() || !sy.isFinite()) return null
        return Offset(sx, sy) to depth
    }
}

const val WORLD_SPAN = 360f

fun normToWorld(nx: Float, ny: Float, y: Float = 0f): Vec3 =
    Vec3((nx - 0.5f) * WORLD_SPAN, y, (ny - 0.5f) * WORLD_SPAN)

private data class DepthObject(
    val depth: Float,
    val draw: DrawScope.() -> Unit
)

fun DrawScope.renderEstate3DScene(
    room: PatrolRoom,
    players: List<RoomPlayer>,
    pings: List<TacticalPing>,
    vehicles: List<WorldVehicle>,
    mountedVehicleId: String?,
    currentUserId: String,
    localPlayerX: Float,
    localPlayerY: Float,
    localFacingDeg: Float,
    localWalkPhase: Float,
    localWeaponId: String,
    localAttributeId: String,
    localUniformId: String,
    thieves: List<ThiefEntity>,
    visibleThiefIds: Set<String>,
    projectiles: List<Projectile3D>,
    particles: List<Particle3D>,
    camYaw: Float,
    camPitch: Float,
    isNightMode: Boolean,
    camShakeX: Float,
    camShakeY: Float,
    pulseScale: Float,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f || !w.isFinite() || !h.isFinite()) return
    val focal = minOf(w, h) * 1.05f
    val attr = AttributeCatalog.byId(localAttributeId)
    val nvgActive = attr.style == AttributeStyle.NVG_GOGGLES && isNightMode
    val isInVehicle = mountedVehicleId != null

    val playerWorld = normToWorld(localPlayerX, localPlayerY, 0f)
    val camDist = if (isInVehicle) 25f else 17.5f
    val pitchClamped = camPitch.coerceIn(-0.22f, 0.85f)
    val horizDist = cos(pitchClamped) * camDist
    val camHeight = (3.9f + sin(pitchClamped) * camDist).coerceAtLeast(1.7f)

    val camPos = Vec3(
        x = playerWorld.x - sin(camYaw) * horizDist,
        y = camHeight,
        z = playerWorld.z + cos(camYaw) * horizDist
    )
    val cx = w * 0.50f + camShakeX
    val cy = h * 0.50f + camShakeY
    val cam = Camera3D(
        pos = camPos,
        yaw = camYaw,
        pitch = pitchClamped,
        focal = focal,
        cx = cx,
        cy = cy
    )

    // True 3D horizon screen Y based on camera pitch so looking up/down reveals sky or ground cleanly
    val horizonY = (cy - tan(pitchClamped) * focal).coerceIn(-h * 0.4f, h * 1.15f)

    // 1. Realistic Day (GTA San Andreas Golden Sun) vs Night Sky & Ground Horizon
    val skyTop = when {
        nvgActive -> Color(0xFF042116)
        isNightMode -> Color(0xFF050A18)
        else -> Color(0xFF3A75B5)
    }
    val skyMid = when {
        nvgActive -> Color(0xFF0A3A27)
        isNightMode -> Color(0xFF101D36)
        else -> Color(0xFF9EC3D9)
    }
    val skyHorizon = when {
        nvgActive -> Color(0xFF115238)
        isNightMode -> Color(0xFF1E2B44)
        else -> Color(0xFFF6C677)
    }
    val groundFarColor = when {
        nvgActive -> Color(0xFF0B2A1E)
        isNightMode -> Color(0xFF131B26)
        else -> Color(0xFF4B5535)
    }
    val groundNearColor = when {
        nvgActive -> Color(0xFF0E3424)
        isNightMode -> Color(0xFF19222E)
        else -> Color(0xFF5D5039)
    }

    if (horizonY > 1f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid, skyHorizon),
                startY = 0f,
                endY = horizonY.coerceAtLeast(40f)
            ),
            topLeft = Offset.Zero,
            size = Size(w, horizonY.coerceAtLeast(1f))
        )
    }
    if (horizonY < h - 1f) {
        val gStart = horizonY.coerceIn(0f, (h - 2f).coerceAtLeast(0f))
        val gEnd = h.coerceAtLeast(gStart + 1f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundFarColor, groundNearColor),
                startY = gStart,
                endY = gEnd
            ),
            topLeft = Offset(0f, gStart),
            size = Size(w, (h - gStart).coerceAtLeast(1f))
        )
    }

    // Sun (Day) or Moon + Stars (Night) in sky
    val minCelestialY = h * 0.06f
    val maxCelestialY = horizonY - 12f
    if (horizonY > 24f && maxCelestialY > minCelestialY + 1f) {
        val sunYawOffset = sin(camYaw + 0.55f) * w * 0.44f
        val celestialX = w * 0.52f + sunYawOffset
        val celestialY = (horizonY - h * 0.22f).coerceIn(minCelestialY, maxCelestialY)
        if (!isNightMode) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFDE047).copy(alpha = 0.72f), Color(0xFFF59E0B).copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(celestialX, celestialY),
                    radius = 165f
                ),
                radius = 165f,
                center = Offset(celestialX, celestialY)
            )
        } else {
            val moonColor = if (nvgActive) Color(0xFF6EE7B7) else Color(0xFFE2E8F0)
            drawCircle(moonColor.copy(alpha = 0.22f), radius = 44f, center = Offset(celestialX, celestialY))
            drawCircle(moonColor.copy(alpha = 0.92f), radius = 14f, center = Offset(celestialX, celestialY))
            val wMod = w.toInt().coerceAtLeast(120)
            for (i in 0..24) {
                val sx = ((i * 131 + (camYaw * 140f).toInt()) % wMod).toFloat().let { if (it < 0f) it + w else it }
                val sy = (i % 7) * (horizonY.coerceAtLeast(80f) * 0.11f) + 12f
                if (sy < horizonY - 10f) {
                    drawCircle(moonColor.copy(alpha = 0.65f), radius = if (i % 4 == 0) 2.0f else 1.2f, center = Offset(sx, sy))
                }
            }
        }
    }

    // 2. Real Ground Surfaces: Dirt Alleys, Grass Lawns, Concrete Sidewalks & Asphalt Roads (NO wireframe grids!)
    val lawnColor = when {
        nvgActive -> Color(0xFF0E3022)
        isNightMode -> Color(0xFF141E22)
        else -> Color(0xFF485A34)
    }
    val alleyDirtColor = when {
        nvgActive -> Color(0xFF153B2B)
        isNightMode -> Color(0xFF1E2428)
        else -> Color(0xFF7B6846)
    }
    val sidewalkColor = when {
        nvgActive -> Color(0xFF1B4433)
        isNightMode -> Color(0xFF2B313B)
        else -> Color(0xFF9A958B)
    }
    val roadColor = when {
        nvgActive -> Color(0xFF132E24)
        isNightMode -> Color(0xFF1F242C)
        else -> Color(0xFF3E4249)
    }

    // Main Estate Terrain Base (warm earth alley base with green residential lawns)
    drawGroundQuad3D(cam, 0.02f, 0.02f, 0.98f, 0.98f, alleyDirtColor, y = 0.0f)
    drawGroundQuad3D(cam, 0.11f, 0.05f, 0.31f, 0.26f, lawnColor, y = 0.01f)
    drawGroundQuad3D(cam, 0.69f, 0.05f, 0.89f, 0.26f, lawnColor, y = 0.01f)
    drawGroundQuad3D(cam, 0.11f, 0.33f, 0.30f, 0.67f, lawnColor, y = 0.01f)
    drawGroundQuad3D(cam, 0.70f, 0.33f, 0.89f, 0.67f, lawnColor, y = 0.01f)
    drawGroundQuad3D(cam, 0.11f, 0.74f, 0.31f, 0.95f, lawnColor, y = 0.01f)
    drawGroundQuad3D(cam, 0.69f, 0.74f, 0.89f, 0.95f, lawnColor, y = 0.01f)

    // Sidewalks framing main roads
    drawGroundQuad3D(cam, 0.04f, 0.258f, 0.96f, 0.332f, sidewalkColor, y = 0.015f)
    drawGroundQuad3D(cam, 0.04f, 0.668f, 0.96f, 0.742f, sidewalkColor, y = 0.015f)
    drawGroundQuad3D(cam, 0.452f, 0.04f, 0.548f, 0.96f, sidewalkColor, y = 0.015f)

    // Paved Asphalt Streets
    drawGroundQuad3D(cam, 0.04f, 0.266f, 0.96f, 0.324f, roadColor, y = 0.025f) // Jl. Krisan - Cempaka
    drawGroundQuad3D(cam, 0.04f, 0.676f, 0.96f, 0.734f, roadColor, y = 0.025f) // Jl. Camelia - Flamboyan
    drawGroundQuad3D(cam, 0.46f, 0.04f, 0.54f, 0.96f, roadColor, y = 0.025f)    // Poros Utama Masjid
    drawGroundQuad3D(cam, 0.312f, 0.05f, 0.363f, 0.95f, roadColor, y = 0.025f) // Poros Barat
    drawGroundQuad3D(cam, 0.637f, 0.05f, 0.688f, 0.95f, roadColor, y = 0.025f) // Poros Timur
    drawGroundQuad3D(cam, 0.26f, 0.472f, 0.74f, 0.528f, roadColor, y = 0.025f) // Jalan Masjid

    // Realistic muted road stripes
    val stripeColor = if (isNightMode) Color(0xFFCBD5E1).copy(alpha = 0.48f) else Color(0xFFE5DFC8).copy(alpha = 0.70f)
    drawGroundLine3D(cam, 0.06f, 0.295f, 0.94f, 0.295f, stripeColor, 2.2f)
    drawGroundLine3D(cam, 0.06f, 0.705f, 0.94f, 0.705f, stripeColor, 2.2f)
    drawGroundLine3D(cam, 0.50f, 0.05f, 0.50f, 0.95f, stripeColor, 2.2f)

    // Mosque Courtyard Stone Plaza
    val plazaColor = if (isNightMode) Color(0xFF164E3F) else Color(0xFF94A39B)
    drawGroundQuad3D(cam, 0.40f, 0.40f, 0.60f, 0.60f, plazaColor, y = 0.028f)

    // Soft realistic ground shadows cast by sun/moon
    val shadowShiftX = if (isNightMode) 0.010f else 0.016f
    val shadowShiftY = if (isNightMode) 0.010f else 0.014f
    val shadowColor = Color(0xFF000000).copy(alpha = if (isNightMode) 0.38f else 0.28f)
    EstateWorld.allBuildings.forEach { b ->
        val center = normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, 0f)
        if (distance3D(cam.pos, center) < 260f) {
            drawGroundQuad3D(
                cam,
                (b.minX + shadowShiftX).coerceIn(0.02f, 0.97f),
                (b.minY + shadowShiftY).coerceIn(0.02f, 0.97f),
                (b.maxX + shadowShiftX).coerceIn(0.03f, 0.98f),
                (b.maxY + shadowShiftY).coerceIn(0.03f, 0.98f),
                shadowColor,
                y = 0.032f
            )
        }
    }

    // 3. Collect Depth-Sorted Real 3D Scene Objects
    val depthObjects = ArrayList<DepthObject>(128)
    val cullDist = 270f

    // 3a. Real Solid Shaded Buildings, Masjid Agung Selaras, and Penjara Selaras
    EstateWorld.allBuildings.forEachIndexed { idx, b ->
        val center = normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, b.height3D * 0.5f)
        val dist = distance3D(cam.pos, center)
        if (dist <= cullDist) {
            depthObjects.add(DepthObject(dist) {
                when {
                    b.isMosque -> drawRealisticMosque3D(cam, b, isNightMode, nvgActive, animClock, textMeasurer)
                    b.isPrison -> drawRealisticPrison3D(cam, b, isNightMode, nvgActive, textMeasurer)
                    else -> drawRealisticBuilding3D(cam, b, idx, isNightMode, nvgActive, textMeasurer)
                }
            })
        }
    }

    // 3b. Realistic Brick Base + Wooden Slat Fences framing residential alleys (like GTA San Andreas reference!)
    val fenceSegments = listOf(
        // Alley walls flanking Krisan / Cemara / Cempaka / Akasia / Camelia / Flamboyan
        listOf(0.11f, 0.335f, 0.25f, 0.335f),
        listOf(0.75f, 0.335f, 0.89f, 0.335f),
        listOf(0.11f, 0.665f, 0.25f, 0.665f),
        listOf(0.75f, 0.665f, 0.89f, 0.665f),
        listOf(0.305f, 0.14f, 0.305f, 0.25f),
        listOf(0.695f, 0.14f, 0.695f, 0.25f),
        listOf(0.255f, 0.43f, 0.255f, 0.56f),
        listOf(0.745f, 0.43f, 0.745f, 0.56f),
        listOf(0.305f, 0.75f, 0.305f, 0.86f),
        listOf(0.695f, 0.75f, 0.695f, 0.86f)
    )
    fenceSegments.forEach { seg ->
        val mx = (seg[0] + seg[2]) * 0.5f
        val my = (seg[1] + seg[3]) * 0.5f
        val dist = distance3D(cam.pos, normToWorld(mx, my, 1.8f))
        if (dist <= 185f) {
            depthObjects.add(DepthObject(dist) {
                drawWoodBrickFence3D(cam, seg[0], seg[1], seg[2], seg[3], isNightMode, nvgActive)
            })
        }
    }

    // 3c. Drivable Cars & Motorcycles
    vehicles.forEach { veh ->
        val vw = normToWorld(veh.x, veh.y, 1f)
        val dist = distance3D(cam.pos, vw)
        if (dist <= cullDist) {
            val isMyVehicle = veh.id == mountedVehicleId
            depthObjects.add(DepthObject(dist) {
                drawVehicle3D(
                    cam = cam,
                    vehicle = veh,
                    isMountedByMe = isMyVehicle,
                    uniformId = localUniformId,
                    weaponId = localWeaponId,
                    attributeId = localAttributeId,
                    isNightMode = isNightMode,
                    nvgActive = nvgActive,
                    animClock = animClock,
                    textMeasurer = textMeasurer
                )
            })
        }
    }

    // 3d. North & South Security Gates
    listOf(
        Triple("GERBANG UTARA", 0.50f to 0.05f, GateStatus.safeValueOf(room.northGateStatus)),
        Triple("GERBANG SELATAN", 0.50f to 0.95f, GateStatus.safeValueOf(room.southGateStatus))
    ).forEach { (label, pos, status) ->
        val gw = normToWorld(pos.first, pos.second, 2f)
        val dist = distance3D(cam.pos, gw)
        if (dist <= cullDist) {
            depthObjects.add(DepthObject(dist) {
                drawGate3D(cam, pos.first, pos.second, status, label, textMeasurer)
            })
        }
    }

    // 3e. Street Lamps (glowing warmly in Night Mode)
    val lampCoords = listOf(
        0.38f to 0.26f, 0.62f to 0.26f,
        0.38f to 0.74f, 0.62f to 0.74f,
        0.27f to 0.42f, 0.73f to 0.42f,
        0.27f to 0.58f, 0.73f to 0.58f,
        0.12f to 0.26f, 0.88f to 0.26f,
        0.12f to 0.74f, 0.88f to 0.74f,
        0.45f to 0.14f, 0.55f to 0.86f
    )
    lampCoords.forEach { (lx, ly) ->
        val lw = normToWorld(lx, ly, 2.5f)
        val dist = distance3D(cam.pos, lw)
        if (dist <= 210f) {
            depthObjects.add(DepthObject(dist) {
                drawStreetLamp3D(cam, lx, ly, isNightMode, nvgActive)
            })
        }
    }

    // 3f. Realistic Multi-Canopy Trees & Tall GTA Palm Trees
    val treeCoords = listOf(
        Triple(0.41f, 0.39f, true), Triple(0.59f, 0.39f, true),
        Triple(0.41f, 0.61f, true), Triple(0.59f, 0.61f, true),
        Triple(0.27f, 0.37f, false), Triple(0.73f, 0.37f, false),
        Triple(0.27f, 0.63f, false), Triple(0.73f, 0.63f, false),
        Triple(0.13f, 0.36f, true), Triple(0.87f, 0.36f, true),
        Triple(0.13f, 0.64f, false), Triple(0.87f, 0.64f, false),
        Triple(0.38f, 0.12f, false), Triple(0.62f, 0.12f, true),
        Triple(0.38f, 0.88f, true), Triple(0.62f, 0.88f, false),
        Triple(0.08f, 0.48f, false), Triple(0.92f, 0.48f, false)
    )
    treeCoords.forEach { (tx, ty, isPalm) ->
        val tw = normToWorld(tx, ty, 3f)
        val dist = distance3D(cam.pos, tw)
        if (dist <= 230f) {
            depthObjects.add(DepthObject(dist) {
                if (isPalm) {
                    drawPalmTree3D(cam, tx, ty, isNightMode, nvgActive)
                } else {
                    drawRealisticTree3D(cam, tx, ty, isNightMode, nvgActive)
                }
            })
        }
    }

    // 3g. Tactical Ping Beacons
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
            val baseProj = cam.project(normToWorld(px, py, 0.05f))
            val topProj = cam.project(normToWorld(px, py, 14f))
            if (baseProj != null && topProj != null) {
                val (bPt, bDepth) = baseProj
                val (tPt, _) = topProj
                val r = (220f / bDepth).coerceIn(8f, 36f) * pulseScale
                drawCircle(pingColor.copy(alpha = 0.26f), r, bPt)
                drawLine(pingColor.copy(alpha = 0.82f), bPt, tPt, strokeWidth = (80f / bDepth).coerceIn(3f, 11f), cap = StrokeCap.Round)
                drawCircle(pingColor, (60f / bDepth).coerceIn(4f, 11f), tPt)
            }
        })
    }

    // 3h. Multiple Thieves (Active hiding/fleeing OR Captured walking straight to Prison!)
    thieves.forEach { thief ->
        if (thief.goingToPrison || thief.id in visibleThiefIds) {
            val tw = normToWorld(thief.x, thief.y, 1.5f)
            val dist = distance3D(cam.pos, tw)
            if (dist <= cullDist) {
                depthObjects.add(DepthObject(dist) {
                    drawThief3D(
                        cam = cam,
                        thief = thief,
                        stunned = thief.goingToPrison || (System.currentTimeMillis() - thief.lastHitMs) < 1800L,
                        animClock = animClock,
                        pulseScale = pulseScale,
                        nvgActive = nvgActive,
                        textMeasurer = textMeasurer
                    )
                })
            }
        }
    }

    // 3i. Guard Players (Third-Person GTA Hero when on foot)
    players.forEach { player ->
        val isMe = player.userId == currentUserId
        if (isMe && isInVehicle) return@forEach
        val px = if (isMe) localPlayerX else player.posX.toFloat()
        val py = if (isMe) localPlayerY else player.posY.toFloat()
        val facing = if (isMe) localFacingDeg else player.facingDeg.toFloat()
        val walkPhase = if (isMe) localWalkPhase else (animClock * 3f)
        val uId = if (isMe) localUniformId else player.uniformId
        val wId = if (isMe) localWeaponId else player.weaponId
        val aId = if (isMe) localAttributeId else player.attributeId
        val pw = normToWorld(px, py, 1.5f)
        val dist = distance3D(cam.pos, pw)
        if (dist <= cullDist) {
            depthObjects.add(DepthObject(dist) {
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
                    label = if (isMe) "" else player.displayName.take(9),
                    pulseScale = pulseScale,
                    animClock = animClock,
                    textMeasurer = textMeasurer
                )
            })
        }
    }

    if (!isInVehicle && players.none { it.userId == currentUserId }) {
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
                label = "",
                pulseScale = pulseScale,
                animClock = animClock,
                textMeasurer = textMeasurer
            )
        })
    }

    depthObjects.sortByDescending { it.depth }
    depthObjects.forEach { it.draw(this) }

    // 4. Projectiles
    projectiles.forEach { proj ->
        val p = proj.progress.coerceIn(0f, 1f)
        val s3 = normToWorld(proj.fromX, proj.fromY, 2.4f)
        val e3 = normToWorld(proj.toX, proj.toY, if (proj.isArc) 0.5f else 2.2f)
        val arcLift = if (proj.isArc) sin(p * PI.toFloat()) * 11f else 0f
        val cur3 = Vec3(
            s3.x + (e3.x - s3.x) * p,
            s3.y + (e3.y - s3.y) * p + arcLift,
            s3.z + (e3.z - s3.z) * p
        )
        val tailP = (p - 0.14f).coerceAtLeast(0f)
        val tailLift = if (proj.isArc) sin(tailP * PI.toFloat()) * 11f else 0f
        val tail3 = Vec3(
            s3.x + (e3.x - s3.x) * tailP,
            s3.y + (e3.y - s3.y) * tailP + tailLift,
            s3.z + (e3.z - s3.z) * tailP
        )
        val tProj = cam.project(tail3)
        val cProj = cam.project(cur3)
        if (tProj != null && cProj != null) {
            drawLine(
                color = proj.color.copy(alpha = 0.94f),
                start = tProj.first,
                end = cProj.first,
                strokeWidth = if (proj.isArc) 7.5f else 5.2f,
                cap = StrokeCap.Round
            )
            drawCircle(Color.White, radius = if (proj.isEnemy) 5f else 6.5f, center = cProj.first)
            drawCircle(proj.color.copy(alpha = 0.45f), radius = 14f, center = cProj.first)
        }
    }

    // 5. High-Particle Explosion & Dust VFX
    particles.forEach { pt ->
        val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
        val screenProj = cam.project(Vec3(pt.x, pt.y, pt.z)) ?: return@forEach
        val (pos2D, depth) = screenProj
        val scaledR = ((pt.size * 24f) / depth).coerceIn(2f, 92f)
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

private fun DrawScope.drawSafeWorldText(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerX: Float,
    topY: Float
) {
    if (text.isBlank() || !centerX.isFinite() || !topY.isFinite()) return
    if (centerX < -220f || centerX > size.width + 220f || topY < -140f || topY > size.height + 140f) return
    val measured = textMeasurer.measure(text, style)
    drawText(measured, topLeft = Offset(centerX - measured.size.width / 2f, topY))
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

private val realisticBuildingPalettes = listOf(
    Triple(Color(0xFFCBBBA4), Color(0xFF7B5E4B), Color(0xFF5A4335)), // Warm Stucco & Terracotta
    Triple(Color(0xFF9AA7B2), Color(0xFF475569), Color(0xFF334155)), // Concrete & Slate
    Triple(Color(0xFFD6C7B2), Color(0xFF6B4F3F), Color(0xFF4F3A2E)), // Sand Plaster & Brown Brick
    Triple(Color(0xFFB5BAC2), Color(0xFF525866), Color(0xFF373C47)), // Urban Plaster & Dark Roof
    Triple(Color(0xFFC4A78B), Color(0xFF7C3F2C), Color(0xFF592B1D))  // Warm Brick & Clay Tile
)

private fun DrawScope.drawRealisticBuilding3D(
    cam: Camera3D,
    b: BuildingFootprint,
    index: Int,
    isNightMode: Boolean,
    nvgActive: Boolean,
    textMeasurer: TextMeasurer
) {
    val hWall = b.height3D * 1.15f
    val cornersBase = listOf(
        normToWorld(b.minX, b.minY, 0f),
        normToWorld(b.maxX, b.minY, 0f),
        normToWorld(b.maxX, b.maxY, 0f),
        normToWorld(b.minX, b.maxY, 0f)
    )
    val cornersTop = listOf(
        normToWorld(b.minX, b.minY, hWall),
        normToWorld(b.maxX, b.minY, hWall),
        normToWorld(b.maxX, b.maxY, hWall),
        normToWorld(b.minX, b.maxY, hWall)
    )

    val pBase = cornersBase.map { cam.project(it)?.first }
    val pTop = cornersTop.map { cam.project(it)?.first }

    val (dayWall, dayBrick, dayRoof) = realisticBuildingPalettes[index % realisticBuildingPalettes.size]
    val wallBase = when {
        nvgActive -> Color(0xFF174A36)
        isNightMode -> lerp(dayWall, Color(0xFF182232), 0.72f)
        else -> dayWall
    }
    val plinthColor = when {
        nvgActive -> Color(0xFF103627)
        isNightMode -> Color(0xFF1F242D)
        else -> dayBrick
    }
    val roofBase = when {
        nvgActive -> Color(0xFF1F5D44)
        isNightMode -> Color(0xFF1E2633)
        else -> dayRoof
    }

    // Outward normals for each face (North, East, South, West)
    val faceNormals = listOf(
        0f to -1f,
        1f to 0f,
        0f to 1f,
        -1f to 0f
    )
    val sunDirX = -0.62f
    val sunDirZ = -0.78f

    val faces = (0..3).map { i ->
        val j = (i + 1) % 4
        val mid = Vec3(
            (cornersBase[i].x + cornersBase[j].x) * 0.5f,
            hWall * 0.5f,
            (cornersBase[i].z + cornersBase[j].z) * 0.5f
        )
        Triple(i, j, distance3D(cam.pos, mid))
    }.sortedByDescending { it.third }

    faces.forEach { (i, j, dist) ->
        val b1 = pBase[i] ?: return@forEach
        val b2 = pBase[j] ?: return@forEach
        val t2 = pTop[j] ?: return@forEach
        val t1 = pTop[i] ?: return@forEach

        val (nx, nz) = faceNormals[i]
        val sunDot = ((nx * sunDirX + nz * sunDirZ) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
        val litWall = lerp(wallBase.copy(red = wallBase.red * 0.52f, green = wallBase.green * 0.52f, blue = wallBase.blue * 0.56f), wallBase, sunDot)
        val litPlinth = lerp(plinthColor.copy(red = plinthColor.red * 0.55f, green = plinthColor.green * 0.55f, blue = plinthColor.blue * 0.55f), plinthColor, sunDot)

        val facePath = Path().apply {
            moveTo(b1.x, b1.y)
            lineTo(b2.x, b2.y)
            lineTo(t2.x, t2.y)
            lineTo(t1.x, t1.y)
            close()
        }
        val rawMinY = minOf(t1.y, t2.y)
        val rawMaxY = maxOf(b1.y, b2.y)
        val clampedStartY = rawMinY.coerceIn(-size.height * 2f, size.height * 3f)
        val clampedEndY = rawMaxY.coerceIn(-size.height * 2f, size.height * 3f).coerceAtLeast(clampedStartY + 1f)
        drawPath(
            path = facePath,
            brush = Brush.verticalGradient(
                0.0f to litWall,
                0.74f to litWall,
                0.76f to litPlinth,
                1.0f to lerp(litPlinth, Color.Black, 0.25f),
                startY = clampedStartY,
                endY = clampedEndY
            )
        )

        // Realistic glass windows with frames & warm interior glow at night
        if (dist < 165f) {
            for (row in 1..2) {
                val fy = row * 0.28f
                for (col in 1..3) {
                    val fx = col * 0.24f
                    val wx1 = b1.x + (b2.x - b1.x) * fx
                    val wy1 = b1.y + (t1.y - b1.y) * fy
                    val wx2 = b1.x + (b2.x - b1.x) * (fx + 0.11f)
                    val wy2 = b2.y + (t2.y - b2.y) * fy
                    val winGlass = when {
                        isNightMode && (row + col + i) % 2 == 0 -> Color(0xFFFDE047).copy(alpha = 0.72f)
                        isNightMode -> Color(0xFF0F172A)
                        else -> Color(0xFF334E68).copy(alpha = 0.85f)
                    }
                    // Window frame & glass pane
                    drawLine(Color(0xFF1F2937).copy(alpha = 0.8f), Offset(wx1, wy1), Offset(wx2, wy1 + (wy2 - wy1) * 0.35f), strokeWidth = 7f)
                    drawLine(winGlass, Offset(wx1, wy1), Offset(wx2, wy1 + (wy2 - wy1) * 0.35f), strokeWidth = 5f)
                }
            }
        }
    }

    // Solid Concrete / Tile Roof Slab with parapet rim (No neon glow)
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
        drawPath(roofPath, roofBase)
        drawPath(roofPath, Color(0xFF0F172A).copy(alpha = 0.35f), style = Stroke(width = 1.5f))
    }

    val labelProj = cam.project(normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, hWall + 2.4f))
    if (labelProj != null && labelProj.second < 110f) {
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = b.label,
            style = TextStyle(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f)),
            centerX = labelProj.first.x,
            topY = labelProj.first.y
        )
    }
}

private fun DrawScope.drawRealisticPrison3D(
    cam: Camera3D,
    b: BuildingFootprint,
    isNightMode: Boolean,
    nvgActive: Boolean,
    textMeasurer: TextMeasurer
) {
    val hWall = b.height3D * 1.1f
    val cornersBase = listOf(
        normToWorld(b.minX, b.minY, 0f),
        normToWorld(b.maxX, b.minY, 0f),
        normToWorld(b.maxX, b.maxY, 0f),
        normToWorld(b.minX, b.maxY, 0f)
    )
    val cornersTop = listOf(
        normToWorld(b.minX, b.minY, hWall),
        normToWorld(b.maxX, b.minY, hWall),
        normToWorld(b.maxX, b.maxY, hWall),
        normToWorld(b.minX, b.maxY, hWall)
    )
    val pBase = cornersBase.map { cam.project(it)?.first }
    val pTop = cornersTop.map { cam.project(it)?.first }

    val stoneColor = if (nvgActive) Color(0xFF1C4736) else if (isNightMode) Color(0xFF334155) else Color(0xFF787D86)
    val darkStone = if (isNightMode) Color(0xFF1E293B) else Color(0xFF4B5563)

    val faces = (0..3).map { i ->
        val j = (i + 1) % 4
        val mid = Vec3((cornersBase[i].x + cornersBase[j].x) * 0.5f, hWall * 0.5f, (cornersBase[i].z + cornersBase[j].z) * 0.5f)
        Triple(i, j, distance3D(cam.pos, mid))
    }.sortedByDescending { it.third }

    faces.forEach { (i, j, _) ->
        val b1 = pBase[i] ?: return@forEach
        val b2 = pBase[j] ?: return@forEach
        val t2 = pTop[j] ?: return@forEach
        val t1 = pTop[i] ?: return@forEach
        val path = Path().apply {
            moveTo(b1.x, b1.y)
            lineTo(b2.x, b2.y)
            lineTo(t2.x, t2.y)
            lineTo(t1.x, t1.y)
            close()
        }
        drawPath(path, if (i % 2 == 0) stoneColor else darkStone)

        // Draw iron prison cell bars on South-facing front wall (i == 2)
        if (i == 2) {
            for (bar in 1..6) {
                val f = bar / 7f
                val bx = b1.x + (b2.x - b1.x) * f
                val by = b1.y + (b2.y - b1.y) * f
                val tx = t1.x + (t2.x - t1.x) * f
                val ty = t1.y + (t2.y - t1.y) * f
                drawLine(Color(0xFF0F172A), Offset(bx, by), Offset(tx, (by + ty) * 0.5f), strokeWidth = 3f)
            }
        }
    }

    if (pTop.all { it != null }) {
        val roofPath = Path().apply {
            moveTo(pTop[0]!!.x, pTop[0]!!.y)
            lineTo(pTop[1]!!.x, pTop[1]!!.y)
            lineTo(pTop[2]!!.x, pTop[2]!!.y)
            lineTo(pTop[3]!!.x, pTop[3]!!.y)
            close()
        }
        drawPath(roofPath, Color(0xFF334155))
    }

    val labelProj = cam.project(normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, hWall + 3.2f))
    if (labelProj != null && labelProj.second < 130f) {
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = "PENJARA",
            style = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color(0xFFFDE047)),
            centerX = labelProj.first.x,
            topY = labelProj.first.y
        )
    }
}

private fun DrawScope.drawWoodBrickFence3D(
    cam: Camera3D,
    x1: Float, y1: Float, x2: Float, y2: Float,
    isNightMode: Boolean,
    nvgActive: Boolean
) {
    val brickHeight = 1.6f
    val woodTop = 4.2f
    val b1 = cam.project(normToWorld(x1, y1, 0f))?.first ?: return
    val b2 = cam.project(normToWorld(x2, y2, 0f))?.first ?: return
    val m1 = cam.project(normToWorld(x1, y1, brickHeight))?.first ?: return
    val m2 = cam.project(normToWorld(x2, y2, brickHeight))?.first ?: return
    val t1 = cam.project(normToWorld(x1, y1, woodTop))?.first ?: return
    val t2 = cam.project(normToWorld(x2, y2, woodTop))?.first ?: return

    val brickCol = when {
        nvgActive -> Color(0xFF14422E)
        isNightMode -> Color(0xFF2E2A27)
        else -> Color(0xFF6B5B51)
    }
    val woodCol = when {
        nvgActive -> Color(0xFF1E573E)
        isNightMode -> Color(0xFF3B2C20)
        else -> Color(0xFF9C724A)
    }

    // Lower stone/brick retaining wall
    drawPath(
        Path().apply {
            moveTo(b1.x, b1.y)
            lineTo(b2.x, b2.y)
            lineTo(m2.x, m2.y)
            lineTo(m1.x, m1.y)
            close()
        },
        brickCol
    )
    // Upper wooden plank fence
    drawPath(
        Path().apply {
            moveTo(m1.x, m1.y)
            lineTo(m2.x, m2.y)
            lineTo(t2.x, t2.y)
            lineTo(t1.x, t1.y)
            close()
        },
        woodCol
    )
    // Vertical wood plank seams
    val plankColor = Color(0xFF2B1C10).copy(alpha = 0.45f)
    for (p in 1..7) {
        val f = p / 8f
        val px = m1.x + (m2.x - m1.x) * f
        val py = m1.y + (m2.y - m1.y) * f
        val qx = t1.x + (t2.x - t1.x) * f
        val qy = t1.y + (t2.y - t1.y) * f
        drawLine(plankColor, Offset(px, py), Offset(qx, qy), strokeWidth = 1.4f)
    }
}

private fun DrawScope.drawRealisticMosque3D(
    cam: Camera3D,
    b: BuildingFootprint,
    isNightMode: Boolean,
    nvgActive: Boolean,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val hWall = b.height3D * 1.25f
    val cornersBase = listOf(
        normToWorld(b.minX, b.minY, 0f),
        normToWorld(b.maxX, b.minY, 0f),
        normToWorld(b.maxX, b.maxY, 0f),
        normToWorld(b.minX, b.maxY, 0f)
    )
    val cornersTop = listOf(
        normToWorld(b.minX, b.minY, hWall),
        normToWorld(b.maxX, b.minY, hWall),
        normToWorld(b.maxX, b.maxY, hWall),
        normToWorld(b.minX, b.maxY, hWall)
    )
    val pBase = cornersBase.map { cam.project(it)?.first }
    val pTop = cornersTop.map { cam.project(it)?.first }

    val wallTint = when {
        nvgActive -> Color(0xFF4BA882)
        isNightMode -> Color(0xFFCBD5E1)
        else -> Color(0xFFF5F2EB)
    }
    val wallShade = when {
        nvgActive -> Color(0xFF287456)
        isNightMode -> Color(0xFF94A3B8)
        else -> Color(0xFFD6CFC2)
    }
    val domeColor = if (nvgActive) Color(0xFF10B981) else Color(0xFF047857)
    val goldAccent = Color(0xFFFDE047)

    val faces = (0..3).map { i ->
        val j = (i + 1) % 4
        val mid = Vec3((cornersBase[i].x + cornersBase[j].x) * 0.5f, hWall * 0.5f, (cornersBase[i].z + cornersBase[j].z) * 0.5f)
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
        drawPath(facePath, if (i == 0 || i == 3) wallTint else wallShade)
        val midBot = Offset((b1.x + b2.x) * 0.5f, (b1.y + b2.y) * 0.5f)
        val archTop = Offset((t1.x + t2.x) * 0.5f, ((t1.y + t2.y) * 0.5f + midBot.y) * 0.5f)
        drawLine(Color(0xFF065F46), midBot, archTop, strokeWidth = 8f, cap = StrokeCap.Round)
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
    }

    val cx = (b.minX + b.maxX) * 0.5f
    val cy = (b.minY + b.maxY) * 0.5f
    val domeProj = cam.project(normToWorld(cx, cy, hWall + 3.4f))
    if (domeProj != null) {
        val (dPt, depth) = domeProj
        val domeR = (280f / depth).coerceIn(10f, 54f)
        drawCircle(domeColor, radius = domeR, center = dPt)
        drawCircle(goldAccent.copy(alpha = 0.45f), radius = domeR * 0.38f, center = Offset(dPt.x - domeR * 0.2f, dPt.y - domeR * 0.25f))
        val spireTop = Offset(dPt.x, dPt.y - domeR * 1.45f)
        drawLine(goldAccent, Offset(dPt.x, dPt.y - domeR * 0.8f), spireTop, strokeWidth = 3f, cap = StrokeCap.Round)
        drawCircle(goldAccent, radius = (domeR * 0.22f).coerceIn(3f, 8f), center = spireTop)
    }

    val minBase = cam.project(normToWorld(b.maxX + 0.008f, b.minY - 0.008f, 0f))
    val minTop = cam.project(normToWorld(b.maxX + 0.008f, b.minY - 0.008f, hWall + 15f))
    if (minBase != null && minTop != null) {
        val (mb, mDepth) = minBase
        val mt = minTop.first
        drawLine(Color(0xFFE2E8F0), mb, mt, strokeWidth = (150f / mDepth).coerceIn(4f, 14f), cap = StrokeCap.Square)
        drawCircle(domeColor, radius = (100f / mDepth).coerceIn(4f, 12f), center = mt)
        val beaconAlpha = if (isNightMode) 0.7f + sin(animClock * 3f) * 0.25f else 0.9f
        drawCircle(goldAccent.copy(alpha = beaconAlpha), radius = (65f / mDepth).coerceIn(3f, 9f), center = Offset(mt.x, mt.y - 8f))
    }

    val labelProj = cam.project(normToWorld(cx, cy, hWall + 12.5f))
    if (labelProj != null && labelProj.second < 150f) {
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = "MASJID AGUNG SELARAS",
            style = TextStyle(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = goldAccent),
            centerX = labelProj.first.x,
            topY = labelProj.first.y
        )
    }
}

private fun DrawScope.drawVehicle3D(
    cam: Camera3D,
    vehicle: WorldVehicle,
    isMountedByMe: Boolean,
    uniformId: String,
    weaponId: String,
    attributeId: String,
    isNightMode: Boolean,
    nvgActive: Boolean,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val base = cam.project(normToWorld(vehicle.x, vehicle.y, 0.25f)) ?: return
    val roof = cam.project(normToWorld(vehicle.x, vehicle.y, if (vehicle.type == VehicleType.CAR) 2.6f else 2.2f)) ?: return
    val (bPt, depth) = base
    val rPt = roof.first

    val bodyTint = if (nvgActive) Color(0xFF10B981) else vehicle.bodyColor
    val trimTint = if (nvgActive) Color(0xFF6EE7B7) else vehicle.accentColor
    val relRad = Math.toRadians(vehicle.facingDeg.toDouble()).toFloat() - cam.yaw
    val dirX = sin(relRad)
    val dirZ = cos(relRad)
    val horizontalLook = kotlin.math.abs(dirX) > kotlin.math.abs(dirZ)

    if (vehicle.type == VehicleType.CAR) {
        val s = (195f / depth).coerceIn(5f, 36f)
        val wCar = if (horizontalLook) s * 2.5f else s * 1.45f
        val hCar = if (horizontalLook) s * 1.1f else s * 1.85f

        drawOval(
            Color.Black.copy(alpha = 0.45f),
            Offset(bPt.x - wCar * 0.60f, bPt.y - hCar * 0.35f),
            Size(wCar * 1.2f, hCar * 0.85f)
        )
        drawRoundRect(
            color = bodyTint,
            topLeft = Offset(bPt.x - wCar * 0.5f, bPt.y - hCar * 0.5f),
            size = Size(wCar, hCar * 0.82f),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawRoundRect(
            color = Color(0xFF93C5FD).copy(alpha = 0.72f),
            topLeft = Offset(rPt.x - wCar * 0.34f, rPt.y - hCar * 0.32f),
            size = Size(wCar * 0.68f, hCar * 0.52f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        val sirenColor = if ((animClock * 5f).toInt() % 2 == 0) Color(0xFF38BDF8) else Color(0xFFEF4444)
        drawCircle(sirenColor, radius = (s * 0.30f).coerceIn(2.5f, 8f), center = rPt)
        val headAlpha = if (isNightMode) 0.95f else 0.55f
        val headOffset = Offset(dirX * wCar * 0.45f, -dirZ * hCar * 0.38f)
        drawCircle(Color(0xFFFDE047).copy(alpha = headAlpha), radius = (s * 0.25f).coerceIn(2f, 6f), center = bPt + headOffset)
    } else {
        val s = (170f / depth).coerceIn(4f, 30f)
        val wMoto = if (horizontalLook) s * 1.75f else s * 0.85f
        val hMoto = if (horizontalLook) s * 0.75f else s * 1.35f

        drawOval(Color.Black.copy(alpha = 0.42f), Offset(bPt.x - wMoto * 0.55f, bPt.y - hMoto * 0.3f), Size(wMoto * 1.1f, hMoto * 0.75f))
        drawCircle(Color(0xFF0F172A), radius = (s * 0.42f).coerceIn(2.5f, 8f), center = Offset(bPt.x - dirX * s * 0.55f, bPt.y + dirZ * s * 0.3f))
        drawCircle(Color(0xFF0F172A), radius = (s * 0.42f).coerceIn(2.5f, 8f), center = Offset(bPt.x + dirX * s * 0.55f, bPt.y - dirZ * s * 0.3f))

        drawRoundRect(
            color = bodyTint,
            topLeft = Offset(bPt.x - wMoto * 0.48f, bPt.y - hMoto * 0.62f),
            size = Size(wMoto * 0.96f, hMoto * 0.85f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawLine(trimTint, bPt, rPt, strokeWidth = (s * 0.35f).coerceIn(2.5f, 7f), cap = StrokeCap.Round)
        drawCircle(Color(0xFF60A5FA), radius = (s * 0.28f).coerceIn(2f, 6f), center = Offset(rPt.x + dirX * s * 0.35f, rPt.y - dirZ * s * 0.2f))

        if (isMountedByMe) {
            val uniform = UniformCatalog.byId(uniformId)
            val weapon = WeaponCatalog.byId(weaponId)
            val attr = AttributeCatalog.byId(attributeId)
            val riderHip = Offset(rPt.x, rPt.y - s * 0.15f)
            drawRoundRect(
                color = uniform.primaryColor,
                topLeft = Offset(riderHip.x - s * 0.42f, riderHip.y - s * 0.85f),
                size = Size(s * 0.84f, s * 0.95f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            val headPt = Offset(riderHip.x, riderHip.y - s * 1.15f)
            drawCircle(Color(0xFF8D5524), radius = s * 0.35f, center = headPt)
            drawArc(
                color = Color(0xFF0F172A),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headPt.x - s * 0.38f, headPt.y - s * 0.38f),
                size = Size(s * 0.76f, s * 0.55f)
            )
            if (attr.style == AttributeStyle.SCANNER_DRONE) {
                drawCircle(attr.accentColor, radius = s * 0.16f, center = Offset(headPt.x - s * 0.75f, headPt.y - s * 0.3f + sin(animClock * 4f) * 2f))
            }
            drawCircle(weapon.accentColor, radius = s * 0.18f, center = Offset(riderHip.x + s * 0.55f, riderHip.y - s * 0.4f))
        }
    }

    if (!isMountedByMe && depth < 65f) {
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = if (vehicle.type == VehicleType.CAR) "MOBIL" else "MOTOR",
            style = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = trimTint),
            centerX = rPt.x,
            topY = rPt.y - 16f
        )
    }
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
    val leftBase = cam.project(normToWorld(nx - 0.04f, ny, 0f))?.first ?: return
    val leftTop = cam.project(normToWorld(nx - 0.04f, ny, 5.5f))?.first ?: return
    val rightBase = cam.project(normToWorld(nx + 0.04f, ny, 0f))?.first ?: return
    val rightTop = cam.project(normToWorld(nx + 0.04f, ny, 5.5f))?.first ?: return

    drawLine(Color(0xFF475569), leftBase, leftTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Color(0xFF475569), rightBase, rightTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(gateColor, leftTop, rightTop, strokeWidth = 5f)

    val barPivot = cam.project(normToWorld(nx - 0.036f, ny, 1.8f))?.first
    val barTipWorld = if (status == GateStatus.LOCKED) {
        normToWorld(nx + 0.036f, ny, 1.8f)
    } else {
        normToWorld(nx - 0.015f, ny, 5.0f)
    }
    val barTip = cam.project(barTipWorld)?.first
    if (barPivot != null && barTip != null) {
        drawLine(gateColor, barPivot, barTip, strokeWidth = 5f, cap = StrokeCap.Round)
    }

    val tag = cam.project(normToWorld(nx, ny, 7.2f))
    if (tag != null && tag.second < 110f) {
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = "$label: ${status.labelId}",
            style = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = gateColor),
            centerX = tag.first.x,
            topY = tag.first.y
        )
    }
}

private fun DrawScope.drawStreetLamp3D(cam: Camera3D, nx: Float, ny: Float, isNightMode: Boolean, nvgActive: Boolean) {
    val base = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val top = cam.project(normToWorld(nx, ny, 7.2f)) ?: return
    val (bPt, depth) = base
    val tPt = top.first
    drawLine(Color(0xFF475569), bPt, tPt, strokeWidth = (55f / depth).coerceIn(2f, 6f))
    if (isNightMode) {
        val glow = if (nvgActive) Color(0xFF6EE7B7) else Color(0xFFFDE047)
        drawCircle(glow.copy(alpha = 0.22f), radius = (240f / depth).coerceIn(8f, 40f), center = bPt)
        drawCircle(glow, radius = (55f / depth).coerceIn(2.5f, 7f), center = tPt)
    } else {
        drawCircle(Color(0xFFCBD5E1), radius = (42f / depth).coerceIn(2f, 5f), center = tPt)
    }
}

private fun DrawScope.drawRealisticTree3D(cam: Camera3D, nx: Float, ny: Float, isNightMode: Boolean, nvgActive: Boolean) {
    val base = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val crown = cam.project(normToWorld(nx, ny, 6.5f)) ?: return
    val (bPt, depth) = base
    val cPt = crown.first
    val r = (230f / depth).coerceIn(6f, 42f)

    drawOval(Color.Black.copy(alpha = if (isNightMode) 0.35f else 0.25f), Offset(bPt.x - r * 0.8f, bPt.y - r * 0.25f), Size(r * 1.6f, r * 0.6f))
    val trunkColor = if (isNightMode) Color(0xFF3E2723) else Color(0xFF5D4037)
    drawLine(trunkColor, bPt, cPt, strokeWidth = (95f / depth).coerceIn(3f, 14f), cap = StrokeCap.Round)

    val darkLeaf = when {
        nvgActive -> Color(0xFF064E3B)
        isNightMode -> Color(0xFF14291E)
        else -> Color(0xFF2D4C22)
    }
    val midLeaf = when {
        nvgActive -> Color(0xFF047857)
        isNightMode -> Color(0xFF1D3B2B)
        else -> Color(0xFF476E31)
    }
    val sunLeaf = when {
        nvgActive -> Color(0xFF10B981)
        isNightMode -> Color(0xFF264E39)
        else -> Color(0xFF6B8E42)
    }
    drawCircle(darkLeaf, radius = r, center = cPt)
    drawCircle(midLeaf, radius = r * 0.78f, center = Offset(cPt.x - r * 0.25f, cPt.y - r * 0.2f))
    drawCircle(sunLeaf, radius = r * 0.62f, center = Offset(cPt.x + r * 0.22f, cPt.y - r * 0.34f))
}

private fun DrawScope.drawPalmTree3D(cam: Camera3D, nx: Float, ny: Float, isNightMode: Boolean, nvgActive: Boolean) {
    val base = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val mid = cam.project(normToWorld(nx + 0.002f, ny, 5.5f)) ?: return
    val top = cam.project(normToWorld(nx + 0.004f, ny, 11.2f)) ?: return
    val (bPt, depth) = base
    val mPt = mid.first
    val tPt = top.first
    val r = (195f / depth).coerceIn(5f, 38f)

    val trunkColor = if (isNightMode) Color(0xFF3E2723) else Color(0xFF6D4C41)
    drawLine(trunkColor, bPt, mPt, strokeWidth = (70f / depth).coerceIn(2.5f, 10f), cap = StrokeCap.Round)
    drawLine(trunkColor, mPt, tPt, strokeWidth = (55f / depth).coerceIn(2f, 8f), cap = StrokeCap.Round)

    val frondColor = when {
        nvgActive -> Color(0xFF10B981)
        isNightMode -> Color(0xFF1B3A29)
        else -> Color(0xFF4A6B2F)
    }
    for (f in 0..5) {
        val ang = f * (PI.toFloat() / 3f) + 0.3f
        val tip = Offset(tPt.x + cos(ang) * r * 1.45f, tPt.y + sin(ang) * r * 0.75f + r * 0.28f)
        drawLine(frondColor, tPt, tip, strokeWidth = (36f / depth).coerceIn(2f, 5f), cap = StrokeCap.Round)
    }
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
    val headProj = cam.project(normToWorld(nx, ny, 3.6f)) ?: return
    val (footPt, depth) = footProj
    val uniform = UniformCatalog.byId(uniformId)
    val weapon = WeaponCatalog.byId(weaponId)
    val attr = AttributeCatalog.byId(attributeId)

    val charH = (footPt.y - headProj.first.y).coerceIn(16f, 180f)
    val s = charH / 48f
    val bodyColor = if (isThiefRole) Color(0xFFEF4444) else uniform.primaryColor
    val trimColor = if (pttActive) Color(0xFF22D3EE) else uniform.trimColor

    val relRad = Math.toRadians(facingDeg.toDouble()).toFloat() - cam.yaw
    val dirX = sin(relRad)
    val dirZ = cos(relRad)

    if (pttActive) {
        drawCircle(Color(0xFF22D3EE).copy(alpha = 0.35f), radius = 18f * s * pulseScale, center = footPt)
    }

    drawOval(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = Offset(footPt.x - 12f * s, footPt.y - 4f * s),
        size = Size(24f * s, 8f * s)
    )

    val stride = sin(walkPhase) * 5.5f * s
    val bootColor = if (attr.style == AttributeStyle.SPEED_BOOTS) attr.accentColor else Color(0xFF111827)
    // Jeans / Tactical Pants
    drawLine(Color(0xFF29436A), Offset(footPt.x - 4f * s, footPt.y - 17f * s), Offset(footPt.x - 4f * s, footPt.y + stride * 0.5f), strokeWidth = 5.4f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFF29436A), Offset(footPt.x + 4f * s, footPt.y - 17f * s), Offset(footPt.x + 4f * s, footPt.y - stride * 0.5f), strokeWidth = 5.4f * s, cap = StrokeCap.Round)
    drawCircle(bootColor, radius = 3.2f * s, center = Offset(footPt.x - 4f * s, footPt.y + stride * 0.5f))
    drawCircle(bootColor, radius = 3.2f * s, center = Offset(footPt.x + 4f * s, footPt.y - stride * 0.5f))

    if (uniform.hasCapeOrCollar) {
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.65f),
            topLeft = Offset(footPt.x - 10f * s, footPt.y - 35f * s),
            size = Size(20f * s, 22f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )
    }

    // Torso (Equipped Uniform / Armor)
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 36f * s),
        size = Size(17f * s, 21f * s),
        cornerRadius = CornerRadius(4.5f * s, 4.5f * s)
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
            cornerRadius = CornerRadius(3f * s, 3f * s)
        )
    }
    drawLine(trimColor, Offset(footPt.x, footPt.y - 34f * s), Offset(footPt.x, footPt.y - 17f * s), strokeWidth = 1.6f * s)

    // Arms & Head
    drawLine(Color(0xFF8D5524), Offset(footPt.x - 10f * s, footPt.y - 33f * s), Offset(footPt.x - 11f * s, footPt.y - 20f * s), strokeWidth = 3.8f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFF8D5524), Offset(footPt.x + 10f * s, footPt.y - 33f * s), Offset(footPt.x + 11f * s, footPt.y - 20f * s), strokeWidth = 3.8f * s, cap = StrokeCap.Round)

    val headCenter = Offset(footPt.x, footPt.y - 43f * s)
    drawCircle(Color(0xFF8D5524), radius = 7f * s, center = headCenter)
    drawArc(
        color = Color(0xFF111827),
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

    val handSide = if (dirZ >= 0f) 10f * s else -10f * s
    val handPos = Offset(footPt.x + handSide, footPt.y - 22f * s)
    when (weapon.style) {
        WeaponStyle.PISTOL -> {
            val muzzle = Offset(handPos.x + (dirX * 10f + 5f) * s, handPos.y - 5f * s)
            drawLine(weapon.primaryColor, handPos, muzzle, strokeWidth = 3.2f * s, cap = StrokeCap.Round)
        }
        WeaponStyle.GRENADE -> {
            drawCircle(weapon.primaryColor, radius = 4f * s, center = handPos)
        }
        WeaponStyle.AK47 -> {
            val stock = Offset(handPos.x - (dirX * 6f) * s, handPos.y + 1f * s)
            val muzzle = Offset(handPos.x + (dirX * 15f + 7f) * s, handPos.y - 9f * s)
            drawLine(weapon.primaryColor, stock, muzzle, strokeWidth = 4f * s, cap = StrokeCap.Round)
        }
        WeaponStyle.BAZOOKA -> {
            val back = Offset(handPos.x - (dirX * 10f) * s, handPos.y + 2f * s)
            val muzzle = Offset(handPos.x + (dirX * 18f + 8f) * s, handPos.y - 12f * s)
            drawLine(weapon.primaryColor, back, muzzle, strokeWidth = 6.2f * s, cap = StrokeCap.Round)
        }
    }

    if (attr.style == AttributeStyle.SCANNER_DRONE) {
        val dronePt = Offset(footPt.x - 14f * s, footPt.y - 52f * s + sin(animClock * 4f) * 3f * s)
        drawRoundRect(
            color = attr.primaryColor,
            topLeft = Offset(dronePt.x - 5f * s, dronePt.y - 3f * s),
            size = Size(10f * s, 6f * s),
            cornerRadius = CornerRadius(3f * s, 3f * s)
        )
        drawCircle(attr.accentColor, radius = 2.2f * s, center = dronePt)
    }

    if (label.isNotBlank() && depth < 110f) {
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = label,
            style = TextStyle(
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isMe) Color(0xFFFDE047) else Color.White
            ),
            centerX = headCenter.x,
            topY = headCenter.y - 24f * s
        )
    }
}

private fun DrawScope.drawThief3D(
    cam: Camera3D,
    thief: ThiefEntity,
    stunned: Boolean,
    animClock: Float,
    pulseScale: Float,
    nvgActive: Boolean,
    textMeasurer: TextMeasurer
) {
    val footProj = cam.project(normToWorld(thief.x, thief.y, 0f)) ?: return
    val headProj = cam.project(normToWorld(thief.x, thief.y, 3.5f)) ?: return
    val (footPt, depth) = footProj
    val charH = (footPt.y - headProj.first.y).coerceIn(14f, 150f)
    val s = charH / 46f

    val isJailedWalk = thief.goingToPrison
    val alertColor = when {
        isJailedWalk -> Color(0xFF38BDF8)
        stunned -> Color(0xFF22D3EE)
        else -> Color(0xFFEF4444)
    }
    drawCircle(alertColor.copy(alpha = 0.25f), radius = 16f * s * pulseScale, center = footPt)

    val stride = if (stunned) 0f else sin(thief.walkPhase) * 6f * s
    drawLine(Color(0xFF111827), Offset(footPt.x - 4f * s, footPt.y - 14f * s), Offset(footPt.x - 4f * s, footPt.y + stride * 0.4f), strokeWidth = 4.8f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFF111827), Offset(footPt.x + 4f * s, footPt.y - 14f * s), Offset(footPt.x + 4f * s, footPt.y - stride * 0.4f), strokeWidth = 4.8f * s, cap = StrokeCap.Round)

    drawRoundRect(
        color = if (isJailedWalk) Color(0xFFEA580C) else thief.hoodieColor,
        topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 34f * s),
        size = Size(17f * s, 20f * s),
        cornerRadius = CornerRadius(4f * s, 4f * s)
    )
    if (nvgActive) {
        drawRoundRect(
            color = Color(0xFF34D399),
            topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 34f * s),
            size = Size(17f * s, 20f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s),
            style = Stroke(width = 2f)
        )
    }

    val headPt = Offset(footPt.x, footPt.y - 40f * s)
    drawCircle(Color(0xFF1F2937), radius = 7f * s, center = headPt)

    if (isJailedWalk) {
        // Raised cuffed hands while escorted to Penjara
        drawLine(Color(0xFFFBBF24), Offset(footPt.x - 8f * s, footPt.y - 34f * s), Offset(footPt.x - 10f * s, footPt.y - 46f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
        drawLine(Color(0xFFFBBF24), Offset(footPt.x + 8f * s, footPt.y - 34f * s), Offset(footPt.x + 10f * s, footPt.y - 46f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
    } else {
        val tHand = Offset(footPt.x + 10f * s, footPt.y - 23f * s)
        drawLine(Color(0xFF334155), tHand, Offset(tHand.x + 10f * s, tHand.y - 5f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
    }

    if (depth < 120f) {
        val tagText = when {
            isJailedWalk -> "KE PENJARA!"
            stunned -> "${thief.label} TERLUKA"
            else -> thief.label
        }
        drawSafeWorldText(
            textMeasurer = textMeasurer,
            text = tagText,
            style = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isJailedWalk) Color(0xFF38BDF8) else Color(0xFFFCA5A5)
            ),
            centerX = headPt.x,
            topY = headPt.y - 24f * s
        )
    }
}
