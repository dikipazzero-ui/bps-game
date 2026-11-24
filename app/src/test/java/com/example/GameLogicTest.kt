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
    fun `estate world has 9 connected sectors including gates parks and blocks`() {
        assertEquals(9, EstateWorld.sectors.size)
        EstateWorld.sectors.forEach { sector ->
            assertTrue("Sector ${sector.id} must have neighbors", sector.neighbors.isNotEmpty())
            assertTrue("Sector ${sector.id} must have a street name", sector.streetName.isNotBlank())
        }
    }

    @Test
    fun `gandi dispatcher names street and block when spotting thief`() {
        val blockB = EstateWorld.byId("BLOCK_B")
        val broadcast = GandiDispatcher.thiefSpotted(blockB)
        assertTrue(broadcast.line.contains(blockB.streetName))
        assertTrue(broadcast.line.contains(blockB.name))
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
        val park = EstateWorld.byId("PARK_CENTRAL")
        val broadcast = GandiDispatcher.apprehensionCelebration(
            guardName = "Sgt. Lee",
            sector = park,
            pointsEarned = 125L,
            uniformId = "gandi_fanclub"
        )
        assertEquals(GandiMood.PROUD, broadcast.mood)
        assertTrue(broadcast.line.contains("125"))
        assertTrue(broadcast.line.contains("jaket amber"))
    }

    @Test
    fun `uniform weapon and attribute catalogs start with free items`() {
        assertEquals(6, UniformCatalog.items.size)
        assertEquals(0L, UniformCatalog.items.first().price)
        assertEquals(2000L, UniformCatalog.items.last().price)

        assertEquals(5, WeaponCatalog.items.size)
        assertEquals(0L, WeaponCatalog.items.first().price)
        assertTrue(WeaponCatalog.items.last().rangeNorm > WeaponCatalog.items.first().rangeNorm)

        assertEquals(5, AttributeCatalog.items.size)
        assertEquals(0L, AttributeCatalog.items.first().price)
        assertTrue(AttributeCatalog.byId("speed_boots").speedMultiplier > 1.2f)
    }

    @Test
    fun `3d world collision blocks walking inside residential buildings`() {
        assertFalse(EstateWorld.isWalkable(0.25f, 0.25f)) // Inside Blok A
        assertTrue(EstateWorld.isWalkable(0.50f, 0.50f))  // Inside Taman Willow / Crosswalk
        val (nx, ny) = EstateWorld.moveWithCollision(0.50f, 0.25f, 0.25f, 0.25f)
        assertEquals(0.50f, nx, 0.001f)
        assertEquals(0.25f, ny, 0.001f)
    }

    @Test
    fun `gandi ping react handles all ping types`() {
        val crosswalk = EstateWorld.byId("CROSSWALK")
        PingType.entries.forEach { pingType ->
            val msg = GandiDispatcher.pingReact("Unit 1", pingType, crosswalk)
            assertTrue(msg.line.isNotBlank())
        }
    }
}
