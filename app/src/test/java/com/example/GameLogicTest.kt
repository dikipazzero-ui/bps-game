package com.example

import com.example.engine.GandiDispatcher
import com.example.model.AttributeCatalog
import com.example.model.EstateWorld
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PingType
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

        assertEquals(4, WeaponCatalog.items.size)
        assertEquals(listOf("pistol_9mm", "granat_frag", "ak47_assault", "bazoka_rpg"), WeaponCatalog.items.map { it.id })
        assertEquals(0L, WeaponCatalog.items.first().price)
        assertTrue(WeaponCatalog.byId("bazoka_rpg").damage > WeaponCatalog.byId("pistol_9mm").damage)
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
}
