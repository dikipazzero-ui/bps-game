package com.example

import com.example.engine.GandiDispatcher
import com.example.model.AttributeCatalog
import com.example.model.EstateWorld
import com.example.model.GameMatchMode
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PingType
import com.example.model.RankCatalog
import com.example.model.UniformCatalog
import com.example.model.WeaponCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    @Test
    fun `bumi papan selaras has 9 connected sectors including 6 blocks masjid and 2 gates`() {
        assertEquals(9, EstateWorld.sectors.size)
        val ids = EstateWorld.sectors.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf(
            "BLOK_KRISAN", "BLOK_CEMPAKA", "BLOK_CEMARA",
            "BLOK_AKASIA", "BLOK_CAMELIA", "BLOK_FLAMBOYAN",
            "AREA_MASJID", "GERBANG_UTARA", "GERBANG_SELATAN"
        )))
        EstateWorld.sectors.forEach { sector ->
            assertTrue("Sector ${sector.id} must have neighbors", sector.neighbors.isNotEmpty())
            assertTrue("Sector ${sector.id} must have a street name", sector.streetName.isNotBlank())
        }
        // Legacy ID fallback still resolves cleanly
        assertEquals("BLOK_CEMPAKA", EstateWorld.byId("BLOCK_B").id)
        assertEquals("AREA_MASJID", EstateWorld.byId("CROSSWALK").id)
    }

    @Test
    fun `gandi dispatcher names street and block when spotting thief`() {
        val cemara = EstateWorld.byId("BLOK_CEMARA")
        val broadcast = GandiDispatcher.thiefSpotted(cemara)
        assertTrue(broadcast.line.contains(cemara.streetName))
        assertTrue(broadcast.line.contains(cemara.name))
        assertEquals(GandiMood.ALERT, broadcast.mood)
    }

    @Test
    fun `gandi dispatcher reacts dramatically to gate status`() {
        val locked = GandiDispatcher.gateStatusChanged("Gerbang Utama Utara", GateStatus.LOCKED)
        assertEquals(GandiMood.PROUD, locked.mood)
        assertTrue(locked.line.contains("Gerbang Utama Utara"))

        val alert = GandiDispatcher.gateStatusChanged("Portal Servis Selatan", GateStatus.ALERT)
        assertEquals(GandiMood.PANICKED, alert.mood)
    }

    @Test
    fun `gandi gives bonus compliment for golden favorite uniform`() {
        val masjid = EstateWorld.byId("AREA_MASJID")
        val broadcast = GandiDispatcher.apprehensionCelebration(
            guardName = "Sgt. Lee",
            sector = masjid,
            pointsEarned = 145L,
            uniformId = "gandi_fanclub"
        )
        assertEquals(GandiMood.PROUD, broadcast.mood)
        assertTrue(broadcast.line.contains("145"))
        assertTrue(broadcast.line.contains("jaket amber"))
    }

    @Test
    fun `uniform armor levels 1 to 5 weapons pistol grenade ak47 bazooka and attributes scale cleanly`() {
        assertEquals(5, UniformCatalog.items.size)
        assertEquals(0L, UniformCatalog.items.first().price)
        assertEquals(1, UniformCatalog.items.first().armorLevel)
        assertEquals(5, UniformCatalog.items.last().armorLevel)
        assertTrue(UniformCatalog.items.last().damageReduction > UniformCatalog.items.first().damageReduction)

        assertEquals(8, WeaponCatalog.items.size)
        assertEquals(
            listOf(
                "pistol_9mm",
                "shotgun_tactical",
                "smg_mp5",
                "granat_frag",
                "ak47_assault",
                "sniper_awp",
                "bazoka_rpg",
                "minigun_vulcan"
            ),
            WeaponCatalog.items.map { it.id }
        )
        assertEquals(0L, WeaponCatalog.items.first().price)
        val prices = WeaponCatalog.items.map { it.price }
        assertTrue("Weapon prices must be strictly increasing", prices.zipWithNext().all { (a, b) -> b > a })
        assertTrue(WeaponCatalog.byId("bazoka_rpg").damage > WeaponCatalog.byId("pistol_9mm").damage)
        assertTrue(WeaponCatalog.byId("minigun_vulcan").price > WeaponCatalog.byId("bazoka_rpg").price)
        // Legacy weapon aliasing
        assertEquals("pistol_9mm", WeaponCatalog.byId("baton_standard").id)

        assertEquals(5, AttributeCatalog.items.size)
        assertEquals(0L, AttributeCatalog.items.first().price)
        assertTrue(AttributeCatalog.byId("speed_boots").speedMultiplier > 1.2f)
    }

    @Test
    fun `3d world collision blocks walking inside masjid and residential blocks`() {
        assertFalse(EstateWorld.isWalkable(0.50f, 0.50f)) // Inside Masjid Agung Selaras
        assertFalse(EstateWorld.isWalkable(0.20f, 0.20f)) // Inside Blok Krisan
        assertTrue(EstateWorld.isWalkable(0.50f, 0.65f))  // Open street south of Masjid
        val (nx, ny) = EstateWorld.moveWithCollision(0.50f, 0.65f, 0.50f, 0.50f)
        assertEquals(0.50f, nx, 0.001f)
        assertEquals(0.65f, ny, 0.001f)
    }

    @Test
    fun `gandi ping react handles all ping types`() {
        val masjid = EstateWorld.byId("AREA_MASJID")
        PingType.entries.forEach { pingType ->
            val msg = GandiDispatcher.pingReact("Unit 1", pingType, masjid)
            assertTrue(msg.line.isNotBlank())
        }
    }

    @Test
    fun `estate has drivable cars motorcycles and flyable helicopters with building line of sight occlusion`() {
        assertTrue(EstateWorld.defaultVehicles.any { it.type == com.example.model.VehicleType.CAR })
        assertTrue(EstateWorld.defaultVehicles.any { it.type == com.example.model.VehicleType.MOTORCYCLE })
        assertTrue(EstateWorld.defaultVehicles.any { it.type == com.example.model.VehicleType.HELICOPTER })
        EstateWorld.defaultVehicles.forEach { v ->
            assertTrue("Vehicle ${v.id} must spawn on walkable ground", EstateWorld.isWalkable(v.x, v.y))
        }
        // Line of sight across Masjid Agung Selaras (0.50, 0.50) should be blocked
        assertFalse(EstateWorld.hasLineOfSight(0.40f, 0.50f, 0.60f, 0.50f))
        // Open street along Jl. Krisan-Cempaka (y=0.295) should be clear
        assertTrue(EstateWorld.hasLineOfSight(0.20f, 0.295f, 0.45f, 0.295f))
    }

    @Test
    fun `multi thief wave spawns 5 walkable thieves across distinct areas and prison door is accessible`() {
        val wave = EstateWorld.spawnThiefWave(wave = 1, playerX = 0.50f, playerY = 0.60f)
        assertEquals(5, wave.size)
        wave.forEach { thief ->
            assertTrue("Thief ${thief.id} must spawn on walkable tile", EstateWorld.isWalkable(thief.x, thief.y))
            assertFalse(thief.goingToPrison)
            assertTrue(thief.hp >= 300f)
        }
        assertEquals(5, wave.map { it.x to it.y }.toSet().size)
        assertTrue("Prison door must be walkable", EstateWorld.isWalkable(EstateWorld.prisonDoorX, EstateWorld.prisonDoorY))
        assertFalse("Inside Penjara Selaras should block walking", EstateWorld.isWalkable(0.415f, 0.08f))
    }

    @Test
    fun `camera3d projection and celestial horizon bounds stay finite and valid for all pitches`() {
        val w = 1280f
        val h = 720f
        val focal = minOf(w, h) * 1.05f
        val pitches = listOf(-0.22f, 0f, 0.18f, 0.45f, 0.85f)
        pitches.forEach { pitch ->
            val horizonY = (h * 0.5f - kotlin.math.tan(pitch) * focal).coerceIn(-h * 0.4f, h * 1.15f)
            val minCelestialY = h * 0.06f
            val maxCelestialY = horizonY - 12f
            if (horizonY > 24f && maxCelestialY > minCelestialY + 1f) {
                val celestialY = (horizonY - h * 0.22f).coerceIn(minCelestialY, maxCelestialY)
                assertTrue(celestialY.isFinite())
                assertTrue(celestialY in minCelestialY..maxCelestialY)
            }
            val cam = com.example.ui.components.Camera3D(
                pos = com.example.ui.components.Vec3(0f, 10f, 20f),
                yaw = 0.5f,
                pitch = pitch,
                focal = focal,
                cx = w * 0.5f,
                cy = h * 0.5f
            )
            val proj = cam.project(com.example.ui.components.Vec3(0f, 0f, 0f))
            if (proj != null) {
                assertTrue(proj.first.x.isFinite())
                assertTrue(proj.first.y.isFinite())
                assertTrue(proj.second >= 1.5f)
            }
        }
    }

    @Test
    fun `rank catalog has 10 ascending tiers and match modes parse safely`() {
        assertEquals(10, RankCatalog.tiers.size)
        assertEquals((1..10).toList(), RankCatalog.tiers.map { it.level })

        val thresholds = RankCatalog.tiers.map { it.minStars }
        assertTrue("Rank star thresholds must be strictly increasing", thresholds.zipWithNext().all { (a, b) -> b > a })
        assertEquals(0, RankCatalog.tiers.first().minStars)
        assertEquals(null, RankCatalog.tiers.last().nextLevelStars)

        // Verify tier lookup across boundaries
        assertEquals(1, RankCatalog.tierForStars(-3L).level)
        assertEquals(1, RankCatalog.tierForStars(0L).level)
        assertEquals(2, RankCatalog.tierForStars(2L).level)
        assertEquals(5, RankCatalog.tierForStars(10L).level)
        assertEquals(9, RankCatalog.tierForStars(34L).level)
        assertEquals(10, RankCatalog.tierForStars(35L).level)
        assertEquals(10, RankCatalog.tierForStars(999L).level)

        // Progress bounds
        assertEquals(0f, RankCatalog.progressInTier(0L), 0.001f)
        assertEquals(0.5f, RankCatalog.progressInTier(1L), 0.001f)
        assertEquals(1f, RankCatalog.progressInTier(35L), 0.001f)
        assertEquals(1f, RankCatalog.progressInTier(120L), 0.001f)

        // Match mode fallback
        assertEquals(GameMatchMode.CLASSIC, GameMatchMode.safeValueOf("CLASSIC"))
        assertEquals(GameMatchMode.RANKED, GameMatchMode.safeValueOf("RANKED"))
        assertEquals(GameMatchMode.RANKED, GameMatchMode.safeValueOf("UNKNOWN"))
    }
}
