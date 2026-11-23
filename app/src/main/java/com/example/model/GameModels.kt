package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GateStatus(val labelId: String) {
    OPEN("TERBUKA"),
    ALERT("SIAGA"),
    LOCKED("TERKUNCI");

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

enum class GandiMood(val labelId: String) {
    CHATTY("CEREWET"),
    ALERT("WASPADA"),
    PANICKED("PANIK"),
    PROUD("BANGGA"),
    SUSPICIOUS("CURIGA");

    companion object {
        fun safeValueOf(raw: String): GandiMood =
            entries.firstOrNull { it.name == raw } ?: CHATTY
    }
}

enum class PlayerRole(val labelId: String) {
    GUARD("PENJAGA"),
    THIEF("PENCURI");

    companion object {
        fun safeValueOf(raw: String): PlayerRole =
            entries.firstOrNull { it.name == raw } ?: GUARD
    }
}

enum class PingType(val label: String) {
    SUSPECT("Ada Penyusup"),
    BACKUP("Butuh Bantuan"),
    BLOCK_GATE("Tutup Gerbang"),
    CLEAR("Sektor Aman");

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
    val displayName: String = "Kadet Penjaga",
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
    val title: String = "Patroli Malam Emerald Crest",
    val hostUid: String = "",
    val status: String = RoomStatus.ACTIVE.name,
    val memberUids: List<String> = emptyList(),
    val northGateStatus: String = GateStatus.OPEN.name,
    val southGateStatus: String = GateStatus.OPEN.name,
    val scoreTotal: Long = 0L,
    val apprehensionCount: Long = 0L,
    val activeThiefSectorId: String = "BLOCK_B",
    val activeThiefStreet: String = "Jl. Oakridge Boulevard",
    val activeThiefX: Double = 0.78,
    val activeThiefY: Double = 0.24,
    val thiefSpotted: Boolean = true,
    val gandiLastLine: String = "GANDI siap bertugas! Pastikan senter menyala dan pantau terus Jl. Oakridge!",
    val gandiMood: String = GandiMood.CHATTY.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomPlayer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Unit Patroli",
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
    val streetName: String = "Bundaran Utama",
    val posX: Double = 0.50,
    val posY: Double = 0.50,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommsMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "Operator",
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
            shortCode = "GERBANG-U",
            name = "Gerbang Utama Utara",
            streetName = "Jl. Oakridge Boulevard",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.09f,
            description = "Pos pemeriksaan kendaraan utama dan palang pintu tamu.",
            neighbors = listOf("BLOCK_A", "BLOCK_B", "CROSSWALK")
        ),
        EstateSector(
            id = "BLOCK_A",
            shortCode = "BLOK-A",
            name = "Blok A: Menara Amberwood",
            streetName = "Lorong Lentera",
            kind = SectorKind.BLOCK,
            normX = 0.21f,
            normY = 0.24f,
            description = "Menara hunian kembar 18 lantai dengan akses atap.",
            neighbors = listOf("GATE_NORTH", "PARK_WEST", "CROSSWALK")
        ),
        EstateSector(
            id = "BLOCK_B",
            shortCode = "BLOK-B",
            name = "Blok B: Vila Taman Cedar",
            streetName = "Jl. Oakridge Boulevard",
            kind = SectorKind.BLOCK,
            normX = 0.79f,
            normY = 0.24f,
            description = "Kompleks vila taman berpagar dengan semak perimeter rapat.",
            neighbors = listOf("GATE_NORTH", "CROSSWALK", "BLOCK_D")
        ),
        EstateSector(
            id = "PARK_WEST",
            shortCode = "TAMAN-B",
            name = "Taman Bermain Rembulan",
            streetName = "Lorong Lentera",
            kind = SectorKind.PARK,
            normX = 0.14f,
            normY = 0.50f,
            description = "Ruang terbuka hijau di sisi barat dengan penerangan minim.",
            neighbors = listOf("BLOCK_A", "PARK_CENTRAL", "BLOCK_C")
        ),
        EstateSector(
            id = "CROSSWALK",
            shortCode = "SIMPANG",
            name = "Bundaran Penyeberangan Pusat",
            streetName = "Jl. Simpang Tengah",
            kind = SectorKind.INTERSECTION,
            normX = 0.50f,
            normY = 0.37f,
            description = "Persimpangan 4 arah utama penghubung Blok A hingga D.",
            neighbors = listOf("GATE_NORTH", "BLOCK_A", "BLOCK_B", "PARK_CENTRAL")
        ),
        EstateSector(
            id = "PARK_CENTRAL",
            shortCode = "TAMAN",
            name = "Taman Pusat Willow",
            streetName = "Jl. Bulan Sabit",
            kind = SectorKind.PARK,
            normX = 0.50f,
            normY = 0.58f,
            description = "Alun-alun taman luas, air mancur, dan lintasan lari.",
            neighbors = listOf("CROSSWALK", "PARK_WEST", "BLOCK_C", "BLOCK_D", "GATE_SOUTH")
        ),
        EstateSector(
            id = "BLOCK_C",
            shortCode = "BLOK-C",
            name = "Blok C: Apartemen Horizon",
            streetName = "Lorong Lentera",
            kind = SectorKind.BLOCK,
            normX = 0.21f,
            normY = 0.76f,
            description = "Apartemen halaman tengah di dekat gang pengiriman barang.",
            neighbors = listOf("PARK_WEST", "PARK_CENTRAL", "GATE_SOUTH")
        ),
        EstateSector(
            id = "BLOCK_D",
            shortCode = "BLOK-D",
            name = "Blok D: Kondominium Maple",
            streetName = "Jl. Bulan Sabit",
            kind = SectorKind.BLOCK,
            normX = 0.79f,
            normY = 0.76f,
            description = "Kondominium bertingkat yang berbatasan langsung dengan tembok timur.",
            neighbors = listOf("BLOCK_B", "PARK_CENTRAL", "GATE_SOUTH")
        ),
        EstateSector(
            id = "GATE_SOUTH",
            shortCode = "GERBANG-S",
            name = "Portal Servis Selatan",
            streetName = "Jl. Senja Raya",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.91f,
            description = "Pintu putar pejalan kaki dan portal akses pengiriman logistik.",
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
            name = "Seragam Kadet Biru",
            rankTitle = "Kadet",
            price = 0L,
            primaryColor = Color(0xFF38BDF8),
            trimColor = Color(0xFFF8FAFC),
            bonusFlair = "Perlengkapan Standar",
            description = "Kemeja biru kompleks dengan garis reflektif malam dan lencana kadet."
        ),
        UniformItem(
            id = "neon_warden",
            name = "Penjaga Siber-Neon",
            rankTitle = "Sipir",
            price = 250L,
            primaryColor = Color(0xFF06B6D4),
            trimColor = Color(0xFF22D3EE),
            bonusFlair = "Jejak Radar Sian",
            description = "Rompi taktis patroli malam lengkap dengan visor pemindai menyala."
        ),
        UniformItem(
            id = "crimson_marshal",
            name = "Marsekal Gerbang Merah",
            rankTitle = "Marsekal",
            price = 500L,
            primaryColor = Color(0xFFEF4444),
            trimColor = Color(0xFFFBBF24),
            bonusFlair = "Efek Kunci Gerbang",
            description = "Jaket komando merah menyala andalan petugas pos penjagaan gerbang."
        ),
        UniformItem(
            id = "night_owl",
            name = "Pengintai Burung Hantu",
            rankTitle = "Pengintai",
            price = 800L,
            primaryColor = Color(0xFF10B981),
            trimColor = Color(0xFF6EE7B7),
            bonusFlair = "Optik Zamrud",
            description = "Parka gelap anti-radar dengan kacamata malam untuk menyisir taman."
        ),
        UniformItem(
            id = "gandi_fanclub",
            name = "Jaket Emas Favorit GANDI",
            rankTitle = "Kepala Operator",
            price = 1200L,
            primaryColor = Color(0xFFF59E0B),
            trimColor = Color(0xFFFEF08A),
            bonusFlair = "Pujian Khusus GANDI",
            description = "Jaket amber cerah berpin megafon. GANDI selalu heboh memuji seragam ini!"
        ),
        UniformItem(
            id = "royal_sentinel",
            name = "Panglima Agung Kompleks",
            rankTitle = "Komandan Utama",
            price = 2000L,
            primaryColor = Color(0xFFA855F7),
            trimColor = Color(0xFFFDE047),
            bonusFlair = "Aura Prestise",
            description = "Zirah seremonial obsidian-emas khusus penjaga legendaris Emerald Crest."
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
        QuickRadioCommand("corner_gate", "Kepung Gerbang U!", "Merapat! Kepung tersangka di Gerbang Utama Utara!", PingType.BLOCK_GATE),
        QuickRadioCommand("need_backup", "Butuh Bantuan!", "Minta bantuan segera di %SECTOR% (%STREET%)!", PingType.BACKUP),
        QuickRadioCommand("visual", "Maling Terlihat!", "Kontak visual! Pencuri terlihat di dekat %SECTOR% (%STREET%)!", PingType.SUSPECT),
        QuickRadioCommand("cut_off", "Tutup Jl. Senja!", "Memotong jalur kabur di sepanjang Jl. Senja Raya!", PingType.BLOCK_GATE),
        QuickRadioCommand("seal_portal", "Kunci Portal Selatan!", "Mengunci Portal Servis Selatan—awasi gang belakang!", PingType.BLOCK_GATE),
        QuickRadioCommand("all_clear", "Sektor Aman", "Penyisiran %SECTOR% selesai—perimeter aman terkendali.", PingType.CLEAR)
    )
}
