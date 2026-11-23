package com.example

import com.example.engine.GandiDispatcher
import com.example.model.EstateWorld
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PingType
import com.example.model.UniformCatalog
import org.junit.Assert.assertEquals
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
        val locked = GandiDispatcher.gateStatusChanged("North Main Gate", GateStatus.LOCKED)
        assertEquals(GandiMood.PROUD, locked.mood)
        assertTrue(locked.line.contains("North Main Gate"))

        val alert = GandiDispatcher.gateStatusChanged("South Service Portal", GateStatus.ALERT)
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
        assertTrue(broadcast.line.contains("amber jacket"))
    }

    @Test
    fun `uniform catalog starts with free rookie cadet and scales up`() {
        assertEquals(6, UniformCatalog.items.size)
        assertEquals(0L, UniformCatalog.items.first().price)
        assertEquals(2000L, UniformCatalog.items.last().price)
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
