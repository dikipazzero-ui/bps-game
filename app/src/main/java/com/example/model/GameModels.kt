package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GateStatus {
    OPEN, ALERT, LOCKED;

    companion object {
        fun safeValueOf(raw: String): GateStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class RoomStatus {
    ACTIVE, LOCKDOWN, ENDED;

    companion object {
        fun safeValueOf(raw: String): RoomStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class GandiMood {
    CHATTY, ALERT, PANICKED, PROUD, SUSPICIOUS;

    companion object {
        fun safeValueOf(raw: String): GandiMood =
            entries.firstOrNull { it.name == raw } ?: CHATTY
    }
}

enum class PlayerRole {
    GUARD, THIEF;

    companion object {
        fun safeValueOf(raw: String): PlayerRole =
            entries.firstOrNull { it.name == raw } ?: GUARD
    }
}

enum class PingType(val label: String) {
    SUSPECT("Suspect Spotted"),
    BACKUP("Need Backup"),
    BLOCK_GATE("Seal Exit"),
    CLEAR("Sector Clear");

    companion object {
        fun safeValueOf(raw: String): PingType =
            entries.firstOrNull { it.name == raw } ?: SUSPECT
    }
}

enum class CommsChannel {
    SQUAD, RADIO, GANDI, VOICE_LOG;

    companion object {
        fun safeValueOf(raw: String): CommsChannel =
            entries.firstOrNull { it.name == raw } ?: SQUAD
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Guard Cadet",
    val points: Long = 100L,
    val apprehensions: Long = 0L,
    val equippedUniformId: String = "rookie_cadet",
    val unlockedUniformIds: List<String> = listOf("rookie_cadet"),
    val voiceEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PatrolRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val title: String = "Emerald Crest Night Watch",
    val hostUid: String = "",
    val status: String = RoomStatus.ACTIVE.name,
    val memberUids: List<String> = emptyList(),
    val northGateStatus: String = GateStatus.OPEN.name,
    val southGateStatus: String = GateStatus.OPEN.name,
    val scoreTotal: Long = 0L,
    val apprehensionCount: Long = 0L,
    val activeThiefSectorId: String = "BLOCK_B",
    val activeThiefStreet: String = "Oakridge Boulevard",
    val activeThiefX: Double = 0.78,
    val activeThiefY: Double = 0.24,
    val thiefSpotted: Boolean = true,
    val gandiLastLine: String = "GANDI online! Keep your flashlights charged and eyes on Oakridge Blvd!",
    val gandiMood: String = GandiMood.CHATTY.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomPlayer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Patrol Unit",
    val role: String = PlayerRole.GUARD.name,
    val uniformId: String = "rookie_cadet",
    val sectorId: String = "GATE_NORTH",
    val posX: Double = 0.50,
    val posY: Double = 0.10,
    val pttActive: Boolean = false,
    val roomScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TacticalPing(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "Unit",
    val pingType: String = PingType.SUSPECT.name,
    val sectorId: String = "CROSSWALK",
    val streetName: String = "Central Crosswalk",
    val posX: Double = 0.50,
    val posY: Double = 0.50,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommsMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "Dispatch",
    val channel: String = CommsChannel.SQUAD.name,
    val text: String = "",
    val sectorTag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class SectorKind { GATE, BLOCK, PARK, INTERSECTION }

data class EstateSector(
    val id: String,
    val shortCode: String,
    val name: String,
    val streetName: String,
    val kind: SectorKind,
    val normX: Float,
    val normY: Float,
    val description: String,
    val neighbors: List<String>
)

object EstateWorld {
    val sectors: List<EstateSector> = listOf(
        EstateSector(
            id = "GATE_NORTH",
            shortCode = "N-GATE",
            name = "North Main Gate",
            streetName = "Oakridge Boulevard",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.09f,
            description = "Primary vehicle checkpoint & visitor barrier.",
            neighbors = listOf("BLOCK_A", "BLOCK_B", "CROSSWALK")
        ),
        EstateSector(
            id = "BLOCK_A",
            shortCode = "BLK-A",
            name = "Block A: Amberwood Towers",
            streetName = "Lantern Lane",
            kind = SectorKind.BLOCK,
            normX = 0.21f,
            normY = 0.24f,
            description = "Twin 18-story residential towers with rooftop access.",
            neighbors = listOf("GATE_NORTH", "PARK_WEST", "CROSSWALK")
        ),
        EstateSector(
            id = "BLOCK_B",
            shortCode = "BLK-B",
            name = "Block B: Cedar Court Villas",
            streetName = "Oakridge Boulevard",
            kind = SectorKind.BLOCK,
            normX = 0.79f,
            normY = 0.24f,
            description = "Gated garden villas with dense perimeter hedges.",
            neighbors = listOf("GATE_NORTH", "CROSSWALK", "BLOCK_D")
        ),
        EstateSector(
            id = "PARK_WEST",
            shortCode = "W-PARK",
            name = "Moonlight Playground",
            streetName = "Lantern Lane",
            kind = SectorKind.PARK,
            normX = 0.14f,
            normY = 0.50f,
            description = "Open west pocket park with low lighting.",
            neighbors = listOf("BLOCK_A", "PARK_CENTRAL", "BLOCK_C")
        ),
        EstateSector(
            id = "CROSSWALK",
            shortCode = "X-WALK",
            name = "Central Crosswalk",
            streetName = "Central Crosswalk",
            kind = SectorKind.INTERSECTION,
            normX = 0.50f,
            normY = 0.37f,
            description = "Main 4-way roundabout connecting Blocks A–D.",
            neighbors = listOf("GATE_NORTH", "BLOCK_A", "BLOCK_B", "PARK_CENTRAL")
        ),
        EstateSector(
            id = "PARK_CENTRAL",
            shortCode = "PARK",
            name = "Willow Central Park",
            streetName = "Crescent Drive",
            kind = SectorKind.PARK,
            normX = 0.50f,
            normY = 0.58f,
            description = "Expansive central plaza, fountain & jogging loop.",
            neighbors = listOf("CROSSWALK", "PARK_WEST", "BLOCK_C", "BLOCK_D", "GATE_SOUTH")
        ),
        EstateSector(
            id = "BLOCK_C",
            shortCode = "BLK-C",
            name = "Block C: Horizon Suites",
            streetName = "Lantern Lane",
            kind = SectorKind.BLOCK,
            normX = 0.21f,
            normY = 0.76f,
            description = "Mid-rise courtyard apartments near delivery alley.",
            neighbors = listOf("PARK_WEST", "PARK_CENTRAL", "GATE_SOUTH")
        ),
        EstateSector(
            id = "BLOCK_D",
            shortCode = "BLK-D",
            name = "Block D: Maple Ridge Condos",
            streetName = "Crescent Drive",
            kind = SectorKind.BLOCK,
            normX = 0.79f,
            normY = 0.76f,
            description = "Terraced condos bordering the eastern wall.",
            neighbors = listOf("BLOCK_B", "PARK_CENTRAL", "GATE_SOUTH")
        ),
        EstateSector(
            id = "GATE_SOUTH",
            shortCode = "S-GATE",
            name = "South Service Portal",
            streetName = "Sundown Way",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.91f,
            description = "Pedestrian turnstile & service delivery portal.",
            neighbors = listOf("PARK_CENTRAL", "BLOCK_C", "BLOCK_D")
        )
    )

    fun byId(id: String): EstateSector =
        sectors.firstOrNull { it.id == id } ?: sectors.first()

    fun nearestTo(x: Float, y: Float): EstateSector =
        sectors.minByOrNull { s ->
            val dx = s.normX - x
            val dy = s.normY - y
            dx * dx + dy * dy
        } ?: sectors.first()

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

data class UniformItem(
    val id: String,
    val name: String,
    val rankTitle: String,
    val price: Long,
    val primaryColor: Color,
    val trimColor: Color,
    val bonusFlair: String,
    val description: String
)

object UniformCatalog {
    val items: List<UniformItem> = listOf(
        UniformItem(
            id = "rookie_cadet",
            name = "Cadet Blue Patrol",
            rankTitle = "Cadet",
            price = 0L,
            primaryColor = Color(0xFF38BDF8),
            trimColor = Color(0xFFF8FAFC),
            bonusFlair = "Standard Issue",
            description = "Crisp estate navy shirt with reflective safety stripes and rookie badge."
        ),
        UniformItem(
            id = "neon_warden",
            name = "Cyber-Neon Warden",
            rankTitle = "Warden",
            price = 250L,
            primaryColor = Color(0xFF06B6D4),
            trimColor = Color(0xFF22D3EE),
            bonusFlair = "Cyan Radar Trail",
            description = "Illuminated night-shift tactical vest with glowing cyan scanner visor."
        ),
        UniformItem(
            id = "crimson_marshal",
            name = "Crimson Gate Marshal",
            rankTitle = "Marshal",
            price = 500L,
            primaryColor = Color(0xFFEF4444),
            trimColor = Color(0xFFFBBF24),
            bonusFlair = "Gate Lockdown Flair",
            description = "High-visibility crimson command coat favored by gate checkpoint veterans."
        ),
        UniformItem(
            id = "night_owl",
            name = "Night-Owl Recon",
            rankTitle = "Scout",
            price = 800L,
            primaryColor = Color(0xFF10B981),
            trimColor = Color(0xFF6EE7B7),
            bonusFlair = "Emerald Optics",
            description = "Stealth charcoal parka with emerald NVG goggles for Willow Park sweeps."
        ),
        UniformItem(
            id = "gandi_fanclub",
            name = "GANDI's Golden Favorite",
            rankTitle = "Chief Dispatcher",
            price = 1200L,
            primaryColor = Color(0xFFF59E0B),
            trimColor = Color(0xFFFEF08A),
            bonusFlair = "Special GANDI Praise",
            description = "Bright amber jacket with a megaphone pin. GANDI cannot stop complimenting it."
        ),
        UniformItem(
            id = "royal_sentinel",
            name = "Estate Sovereign Sentinel",
            rankTitle = "Grand Commander",
            price = 2000L,
            primaryColor = Color(0xFFA855F7),
            trimColor = Color(0xFFFDE047),
            bonusFlair = "Prestige Aura",
            description = "Ceremonial obsidian-and-gold armor reserved for top Emerald Crest legends."
        )
    )

    fun byId(id: String): UniformItem =
        items.firstOrNull { it.id == id } ?: items.first()
}

data class QuickRadioCommand(
    val id: String,
    val label: String,
    val messageTemplate: String,
    val pingType: PingType
)

object QuickRadioPresets {
    val commands = listOf(
        QuickRadioCommand("corner_gate", "Corner North Gate!", "Closing in! Corner suspect at North Main Gate!", PingType.BLOCK_GATE),
        QuickRadioCommand("need_backup", "Need Backup!", "Requesting immediate backup at %SECTOR% (%STREET%)!", PingType.BACKUP),
        QuickRadioCommand("visual", "Visual on Thief!", "Visual contact! Suspect spotted near %SECTOR% on %STREET%!", PingType.SUSPECT),
        QuickRadioCommand("cut_off", "Cut Off Sundown!", "Cutting off escape routes along Sundown Way!", PingType.BLOCK_GATE),
        QuickRadioCommand("seal_portal", "Seal South Portal!", "Locking down South Service Portal—watch the alley!", PingType.BLOCK_GATE),
        QuickRadioCommand("all_clear", "Sector Clear", "%SECTOR% sweep complete—perimeter clear.", PingType.CLEAR)
    )
}
