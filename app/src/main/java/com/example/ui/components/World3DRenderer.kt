package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    attackProgress: Float,
    attackOrigin: Pair<Float, Float>,
    attackTarget: Pair<Float, Float>,
    camYaw: Float,
    pulseScale: Float,
    animClock: Float,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val focal = minOf(w, h) * 0.92f
    val attr = AttributeCatalog.byId(localAttributeId)
    val nvgActive = attr.style == AttributeStyle.NVG_GOGGLES

    val playerWorld = normToWorld(localPlayerX, localPlayerY, 0f)
    val camDist = 20f
    val camPos = Vec3(
        x = playerWorld.x - sin(camYaw) * camDist,
        y = 10.5f,
        z = playerWorld.z + cos(camYaw) * camDist
    )
    val cam = Camera3D(
        pos = camPos,
        yaw = camYaw,
        pitch = 0.40f,
        focal = focal,
        cx = w * 0.50f,
        cy = h * 0.44f
    )

    // 1. 3D Sky & Horizon Backdrop
    val skyTop = if (nvgActive) Color(0xFF031A12) else Color(0xFF050914)
    val skyHorizon = if (nvgActive) Color(0xFF093826) else Color(0xFF10203A)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyHorizon, Color(0xFF0B1322)),
            startY = 0f,
            endY = h
        )
    )

    // Distant horizon stars & city glow
    for (i in 0..16) {
        val sx = ((i * 97 + (camYaw * 120f).toInt()) % w.toInt().coerceAtLeast(100)).toFloat().let {
            if (it < 0f) it + w else it
        }
        val sy = h * (0.04f + (i % 5) * 0.035f)
        drawCircle(
            color = if (nvgActive) Color(0xFF6EE7B7).copy(alpha = 0.45f) else Color(0xFFE2E8F0).copy(alpha = 0.4f),
            radius = if (i % 3 == 0) 2.2f else 1.4f,
            center = Offset(sx, sy)
        )
    }

    // 2. Ground Plane Quads (Grass base, Paved Roads, Parks, Fountain)
    drawGroundQuad3D(cam, 0.03f, 0.03f, 0.97f, 0.97f, if (nvgActive) Color(0xFF0C2B1E) else Color(0xFF0C1524))

    // Tactical grid lines on ground
    val gridColor = if (nvgActive) Color(0xFF10B981).copy(alpha = 0.20f) else Color(0xFF1E293B).copy(alpha = 0.55f)
    for (i in 1..9) {
        val t = i / 10f
        drawGroundLine3D(cam, t, 0.05f, t, 0.95f, gridColor, 1.4f)
        drawGroundLine3D(cam, 0.05f, t, 0.95f, t, gridColor, 1.4f)
    }

    // Paved Asphalt Streets
    val roadColor = if (nvgActive) Color(0xFF15382B) else Color(0xFF1E293B)
    drawGroundQuad3D(cam, 0.08f, 0.135f, 0.92f, 0.20f, roadColor) // Jl. Oakridge
    drawGroundQuad3D(cam, 0.08f, 0.80f, 0.92f, 0.865f, roadColor) // Jl. Senja Raya
    drawGroundQuad3D(cam, 0.08f, 0.135f, 0.14f, 0.865f, roadColor) // Lorong Lentera
    drawGroundQuad3D(cam, 0.86f, 0.135f, 0.92f, 0.865f, roadColor) // Jl. Bulan Sabit
    drawGroundQuad3D(cam, 0.46f, 0.06f, 0.54f, 0.94f, roadColor) // Poros Tengah
    drawGroundQuad3D(cam, 0.08f, 0.34f, 0.92f, 0.40f, roadColor) // Jl. Simpang

    // Yellow/White Dashed Street Stripes
    val stripe = Color(0xFF64748B)
    drawGroundLine3D(cam, 0.10f, 0.168f, 0.90f, 0.168f, stripe, 2.5f)
    drawGroundLine3D(cam, 0.10f, 0.832f, 0.90f, 0.832f, stripe, 2.5f)
    drawGroundLine3D(cam, 0.50f, 0.08f, 0.50f, 0.92f, stripe, 2.5f)

    // Green Parks (Taman Pusat Willow & Taman Rembulan)
    drawGroundQuad3D(cam, 0.35f, 0.44f, 0.65f, 0.68f, Color(0xFF064E3B))
    drawGroundQuad3D(cam, 0.145f, 0.44f, 0.29f, 0.60f, Color(0xFF065F46))
    // Fountain pool in Taman Willow
    drawGroundQuad3D(cam, 0.46f, 0.53f, 0.54f, 0.61f, Color(0xFF0284C7))

    // 3. Collect Depth-Sorted 3D Objects (Buildings, Gates, Lamps, Trees, Pings, Characters)
    val depthObjects = ArrayList<DepthObject>(48)

    // 3a. 3D Buildings (Blok A, B, C, D)
    EstateWorld.buildings.forEach { b ->
        val center = normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, b.height3D * 0.4f)
        val proj = cam.project(center)
        val dist = proj?.second ?: distance3D(cam.pos, center)
        depthObjects.add(DepthObject(dist) {
            drawBuilding3D(cam, b, nvgActive, textMeasurer)
        })
    }

    // 3b. North & South 3D Gates with animated barrier pole
    listOf(
        Triple("GERBANG UTARA", 0.50f to 0.08f, GateStatus.safeValueOf(room.northGateStatus)),
        Triple("PORTAL SELATAN", 0.50f to 0.92f, GateStatus.safeValueOf(room.southGateStatus))
    ).forEach { (label, pos, status) ->
        val gw = normToWorld(pos.first, pos.second, 2f)
        val dist = distance3D(cam.pos, gw)
        depthObjects.add(DepthObject(dist) {
            drawGate3D(cam, pos.first, pos.second, status, label, textMeasurer)
        })
    }

    // 3c. 3D Street Lamps & Park Trees
    val lampCoords = listOf(
        0.38f to 0.14f, 0.62f to 0.14f,
        0.38f to 0.86f, 0.62f to 0.86f,
        0.15f to 0.36f, 0.85f to 0.36f,
        0.36f to 0.45f, 0.64f to 0.67f
    )
    lampCoords.forEach { (lx, ly) ->
        val lw = normToWorld(lx, ly, 2.5f)
        depthObjects.add(DepthObject(distance3D(cam.pos, lw)) {
            drawStreetLamp3D(cam, lx, ly, nvgActive)
        })
    }

    val treeCoords = listOf(
        0.38f to 0.50f, 0.62f to 0.50f, 0.38f to 0.63f, 0.62f to 0.63f,
        0.21f to 0.49f, 0.22f to 0.56f
    )
    treeCoords.forEach { (tx, ty) ->
        val tw = normToWorld(tx, ty, 2f)
        depthObjects.add(DepthObject(distance3D(cam.pos, tw)) {
            drawTree3D(cam, tx, ty)
        })
    }

    // 3d. Tactical Ping 3D Light Pillars
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

    // 3e. Intruder / Thief 3D Character
    if (room.thiefSpotted) {
        val (tx, ty) = thiefPos
        val tw = normToWorld(tx, ty, 1.5f)
        depthObjects.add(DepthObject(distance3D(cam.pos, tw)) {
            drawThief3D(cam, tx, ty, thiefStunned, animClock, pulseScale, nvgActive, textMeasurer)
        })
    }

    // 3f. Multiplayer Guard Characters (Self + Squadmates)
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

    // If players list doesn't have local player yet (syncing initial join), draw self
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

    // Painter's Algorithm Sort: Draw farthest objects first!
    depthObjects.sortByDescending { it.depth }
    depthObjects.forEach { it.draw(this) }

    // 4. Active 3D Weapon Projectile / Beam VFX
    if (attackProgress > 0f && attackProgress < 1f) {
        val wpn = WeaponCatalog.byId(localWeaponId)
        val start3D = normToWorld(attackOrigin.first, attackOrigin.second, 2.2f)
        val end3D = normToWorld(attackTarget.first, attackTarget.second, 2.0f)
        val cur3D = Vec3(
            start3D.x + (end3D.x - start3D.x) * attackProgress,
            start3D.y + (end3D.y - start3D.y) * attackProgress,
            start3D.z + (end3D.z - start3D.z) * attackProgress
        )
        val sProj = cam.project(start3D)
        val cProj = cam.project(cur3D)
        if (sProj != null && cProj != null) {
            drawLine(
                color = wpn.accentColor.copy(alpha = 0.9f),
                start = sProj.first,
                end = cProj.first,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawCircle(Color.White, radius = 7f, center = cProj.first)
            drawCircle(wpn.accentColor.copy(alpha = 0.45f), radius = 15f, center = cProj.first)
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

    // Draw visible vertical faces sorted back-to-front
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
        drawPath(facePath, trimTint.copy(alpha = 0.42f), style = Stroke(width = 1.6f))

        // Lit apartment windows along wall
        for (row in 1..3) {
            val fy = row * 0.24f
            for (col in 1..3) {
                val fx = col * 0.24f
                val w1 = Offset(b1.x + (b2.x - b1.x) * fx, b1.y + (t1.y - b1.y) * fy)
                val w2 = Offset(b1.x + (b2.x - b1.x) * (fx + 0.08f), b1.y + (t1.y - b1.y) * fy)
                val winColor = if ((row + col + i) % 2 == 0) Color(0xFFFDE047).copy(alpha = 0.55f) else trimTint.copy(alpha = 0.35f)
                drawLine(winColor, w1, w2, strokeWidth = 3.2f)
            }
        }
    }

    // Roof
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
        drawPath(roofPath, trimTint.copy(alpha = 0.8f), style = Stroke(width = 2.2f))
    }

    // Floating 3D label above roof
    val labelProj = cam.project(normToWorld((b.minX + b.maxX) * 0.5f, (b.minY + b.maxY) * 0.5f, b.height3D + 2.2f))
    if (labelProj != null && labelProj.second < 75f) {
        val measured = textMeasurer.measure(
            b.label,
            TextStyle(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
        )
        drawText(measured, topLeft = Offset(labelProj.first.x - measured.size.width / 2f, labelProj.first.y))
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
    val leftBase = cam.project(normToWorld(nx - 0.055f, ny, 0f))?.first ?: return
    val leftTop = cam.project(normToWorld(nx - 0.055f, ny, 4.8f))?.first ?: return
    val rightBase = cam.project(normToWorld(nx + 0.055f, ny, 0f))?.first ?: return
    val rightTop = cam.project(normToWorld(nx + 0.055f, ny, 4.8f))?.first ?: return

    drawLine(Color(0xFF475569), leftBase, leftTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Color(0xFF475569), rightBase, rightTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(gateColor, leftTop, rightTop, strokeWidth = 5f)

    // Physical barrier pole (lowered when LOCKED, raised when OPEN)
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
    drawCircle(glow.copy(alpha = 0.20f), radius = (180f / depth).coerceIn(8f, 36f), center = bPt)
    drawCircle(glow, radius = (45f / depth).coerceIn(2.5f, 7f), center = tPt)
}

private fun DrawScope.drawTree3D(cam: Camera3D, nx: Float, ny: Float) {
    val base = cam.project(normToWorld(nx, ny, 0f)) ?: return
    val crown = cam.project(normToWorld(nx, ny, 4.0f)) ?: return
    val (bPt, depth) = base
    val cPt = crown.first
    drawLine(Color(0xFF78350F), bPt, cPt, strokeWidth = (60f / depth).coerceIn(2.5f, 8f))
    val r = (140f / depth).coerceIn(6f, 28f)
    drawCircle(Color(0xFF065F46), radius = r, center = cPt)
    drawCircle(Color(0xFF10B981).copy(alpha = 0.7f), radius = r * 0.68f, center = Offset(cPt.x - r * 0.15f, cPt.y - r * 0.2f))
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

    // Relative facing direction for 3D silhouette
    val relRad = Math.toRadians(facingDeg.toDouble()).toFloat() - cam.yaw
    val dirX = sin(relRad)
    val dirZ = cos(relRad)

    // PTT Live Ring / Me Ring on ground
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
        // 3D Flashlight / Weapon aim cone on ground ahead
        val aheadProj = cam.project(
            normToWorld(
                (nx + sin(Math.toRadians(facingDeg.toDouble()).toFloat()) * weapon.rangeNorm * 0.55f).coerceIn(0.05f, 0.95f),
                (ny - cos(Math.toRadians(facingDeg.toDouble()).toFloat()) * weapon.rangeNorm * 0.55f).coerceIn(0.05f, 0.95f),
                0.08f
            )
        )?.first
        if (aheadProj != null) {
            val coneColor = if (weapon.style == WeaponStyle.FLASHLIGHT) Color(0xFFFDE047) else weapon.accentColor
            drawLine(coneColor.copy(alpha = 0.25f), footPt, aheadProj, strokeWidth = 10f * s, cap = StrokeCap.Round)
        }
    }

    // Ground shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
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

    // Cape / Collar for upgraded uniform
    if (uniform.hasCapeOrCollar) {
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.65f),
            topLeft = Offset(footPt.x - 10f * s, footPt.y - 35f * s),
            size = Size(20f * s, 22f * s),
            cornerRadius = CornerRadiusCompat(4f * s)
        )
    }

    // Torso (Equipped Uniform)
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(footPt.x - 8.5f * s, footPt.y - 36f * s),
        size = Size(17f * s, 21f * s),
        cornerRadius = CornerRadiusCompat(4.5f * s)
    )
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

    // Held Weapon in Right Hand!
    val handSide = if (dirZ >= 0f) 10f * s else -10f * s
    val handPos = Offset(footPt.x + handSide, footPt.y - 24f * s)
    val muzzlePos = Offset(handPos.x + (dirX * 12f + 6f) * s, handPos.y - 8f * s)
    drawLine(weapon.primaryColor, handPos, muzzlePos, strokeWidth = 3.8f * s, cap = StrokeCap.Round)
    drawCircle(weapon.accentColor, radius = 2.8f * s, center = muzzlePos)

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

    // Overhead Label
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

    // Crimson pulsing alert ring
    val alertColor = if (stunned) Color(0xFF22D3EE) else Color(0xFFEF4444)
    drawCircle(alertColor.copy(alpha = 0.25f), radius = 20f * s * pulseScale, center = footPt)
    drawCircle(alertColor, radius = 14f * s, center = footPt, style = Stroke(width = 2.2f))

    val stride = if (stunned) 0f else sin(animClock * 7f) * 6f * s
    drawLine(Color(0xFF111827), Offset(footPt.x - 4f * s, footPt.y - 14f * s), Offset(footPt.x - 4f * s, footPt.y + stride * 0.4f), strokeWidth = 4.8f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFF111827), Offset(footPt.x + 4f * s, footPt.y - 14f * s), Offset(footPt.x + 4f * s, footPt.y - stride * 0.4f), strokeWidth = 4.8f * s, cap = StrokeCap.Round)

    // Dark Hoodie
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

    if (stunned) {
        drawCircle(Color(0xFF38BDF8), radius = 11f * s, center = headPt, style = Stroke(width = 2f))
    }

    if (depth < 85f) {
        val m = textMeasurer.measure(
            if (stunned) "LUMPUH!" else "PENYUSUP!",
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
