package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.sqrt

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
    val equippedWeaponId: String = "baton_standard",
    val unlockedWeaponIds: List<String> = listOf("baton_standard"),
    val equippedAttributeId: String = "utility_belt",
    val unlockedAttributeIds: List<String> = listOf("utility_belt"),
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
    val weaponId: String = "baton_standard",
    val attributeId: String = "utility_belt",
    val sectorId: String = "GATE_NORTH",
    val posX: Double = 0.50,
    val posY: Double = 0.10,
    val facingDeg: Double = 0.0,
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

data class BuildingFootprint(
    val id: String,
    val label: String,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val height3D: Float,
    val roofColor: Color,
    val wallColor: Color,
    val accentColor: Color
)

object EstateWorld {
    val buildings: List<BuildingFootprint> = listOf(
        BuildingFootprint(
            id = "BLOK_A",
            label = "BLOK A • AMBERWOOD",
            minX = 0.17f, minY = 0.21f, maxX = 0.34f, maxY = 0.31f,
            height3D = 14f,
            roofColor = Color(0xFF1E3A5F),
            wallColor = Color(0xFF132640),
            accentColor = Color(0xFF38BDF8)
        ),
        BuildingFootprint(
            id = "BLOK_B",
            label = "BLOK B • VILA CEDAR",
            minX = 0.66f, minY = 0.21f, maxX = 0.83f, maxY = 0.31f,
            height3D = 11f,
            roofColor = Color(0xFF283B5B),
            wallColor = Color(0xFF17253B),
            accentColor = Color(0xFFF59E0B)
        ),
        BuildingFootprint(
            id = "BLOK_C",
            label = "BLOK C • HORIZON",
            minX = 0.17f, minY = 0.68f, maxX = 0.34f, maxY = 0.78f,
            height3D = 13f,
            roofColor = Color(0xFF1E3A5F),
            wallColor = Color(0xFF132640),
            accentColor = Color(0xFF22D3EE)
        ),
        BuildingFootprint(
            id = "BLOK_D",
            label = "BLOK D • MAPLE",
            minX = 0.66f, minY = 0.68f, maxX = 0.83f, maxY = 0.78f,
            height3D = 16f,
            roofColor = Color(0xFF263859),
            wallColor = Color(0xFF162338),
            accentColor = Color(0xFFA855F7)
        )
    )

    val sectors: List<EstateSector> = listOf(
        EstateSector(
            id = "GATE_NORTH",
            shortCode = "GERBANG-U",
            name = "Gerbang Utama Utara",
            streetName = "Jl. Oakridge Boulevard",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.10f,
            description = "Pos pemeriksaan kendaraan utama dan palang pintu tamu.",
            neighbors = listOf("BLOCK_A", "BLOCK_B", "CROSSWALK")
        ),
        EstateSector(
            id = "BLOCK_A",
            shortCode = "BLOK-A",
            name = "Blok A: Menara Amberwood",
            streetName = "Lorong Lentera",
            kind = SectorKind.BLOCK,
            normX = 0.25f,
            normY = 0.34f,
            description = "Menara hunian kembar 18 lantai dengan akses atap.",
            neighbors = listOf("GATE_NORTH", "PARK_WEST", "CROSSWALK")
        ),
        EstateSector(
            id = "BLOCK_B",
            shortCode = "BLOK-B",
            name = "Blok B: Vila Taman Cedar",
            streetName = "Jl. Oakridge Boulevard",
            kind = SectorKind.BLOCK,
            normX = 0.75f,
            normY = 0.17f,
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
            normY = 0.57f,
            description = "Alun-alun taman luas, air mancur, dan lintasan lari.",
            neighbors = listOf("CROSSWALK", "PARK_WEST", "BLOCK_C", "BLOCK_D", "GATE_SOUTH")
        ),
        EstateSector(
            id = "BLOCK_C",
            shortCode = "BLOK-C",
            name = "Blok C: Apartemen Horizon",
            streetName = "Lorong Lentera",
            kind = SectorKind.BLOCK,
            normX = 0.25f,
            normY = 0.83f,
            description = "Apartemen halaman tengah di dekat gang pengiriman barang.",
            neighbors = listOf("PARK_WEST", "PARK_CENTRAL", "GATE_SOUTH")
        ),
        EstateSector(
            id = "BLOCK_D",
            shortCode = "BLOK-D",
            name = "Blok D: Kondominium Maple",
            streetName = "Jl. Bulan Sabit",
            kind = SectorKind.BLOCK,
            normX = 0.75f,
            normY = 0.83f,
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
            normY = 0.90f,
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
        return sqrt(dx * dx + dy * dy)
    }

    fun isWalkable(nx: Float, ny: Float, pad: Float = 0.018f): Boolean {
        if (nx < 0.06f || nx > 0.94f || ny < 0.06f || ny > 0.94f) return false
        return buildings.none { b ->
            nx in (b.minX - pad)..(b.maxX + pad) && ny in (b.minY - pad)..(b.maxY + pad)
        }
    }

    fun moveWithCollision(oldX: Float, oldY: Float, targetX: Float, targetY: Float): Pair<Float, Float> {
        val clampedX = targetX.coerceIn(0.06f, 0.94f)
        val clampedY = targetY.coerceIn(0.06f, 0.94f)
        if (isWalkable(clampedX, clampedY)) return clampedX to clampedY
        if (isWalkable(clampedX, oldY)) return clampedX to oldY
        if (isWalkable(oldX, clampedY)) return oldX to clampedY
        return oldX to oldY
    }
}

enum class ShopCategory(val label: String) {
    UNIFORM("Baju & Seragam"),
    WEAPON("Senjata Dipegang"),
    ATTRIBUTE("Atribut Kebutuhan")
}

data class UniformItem(
    val id: String,
    val name: String,
    val rankTitle: String,
    val price: Long,
    val primaryColor: Color,
    val trimColor: Color,
    val bonusFlair: String,
    val description: String,
    val hasShoulderPads: Boolean = false,
    val hasCapeOrCollar: Boolean = false
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
            description = "Kemeja patroli biru dengan garis reflektif malam dan lencana kadet."
        ),
        UniformItem(
            id = "neon_warden",
            name = "Jaket Siber-Neon",
            rankTitle = "Sipir",
            price = 250L,
            primaryColor = Color(0xFF06B6D4),
            trimColor = Color(0xFF22D3EE),
            bonusFlair = "Jejak Radar Sian",
            description = "Jaket taktis patroli malam lengkap dengan pelindung bahu menyala.",
            hasShoulderPads = true
        ),
        UniformItem(
            id = "crimson_marshal",
            name = "Jaket Marsekal Merah",
            rankTitle = "Marsekal",
            price = 500L,
            primaryColor = Color(0xFFEF4444),
            trimColor = Color(0xFFFBBF24),
            bonusFlair = "Efek Kunci Gerbang",
            description = "Jaket komando merah menyala andalan petugas pos penjagaan gerbang.",
            hasShoulderPads = true
        ),
        UniformItem(
            id = "night_owl",
            name = "Parka Pengintai Malam",
            rankTitle = "Pengintai",
            price = 800L,
            primaryColor = Color(0xFF10B981),
            trimColor = Color(0xFF6EE7B7),
            bonusFlair = "Optik Zamrud",
            description = "Parka gelap anti-radar berkerah tinggi untuk menyisir taman malam.",
            hasCapeOrCollar = true
        ),
        UniformItem(
            id = "gandi_fanclub",
            name = "Jaket Emas GANDI",
            rankTitle = "Kepala Operator",
            price = 1200L,
            primaryColor = Color(0xFFF59E0B),
            trimColor = Color(0xFFFEF08A),
            bonusFlair = "+25 Poin Tangkapan",
            description = "Jaket amber cerah berpin megafon. GANDI selalu heboh memuji seragam ini!",
            hasShoulderPads = true
        ),
        UniformItem(
            id = "royal_sentinel",
            name = "Zirah Panglima Agung",
            rankTitle = "Komandan Utama",
            price = 2000L,
            primaryColor = Color(0xFFA855F7),
            trimColor = Color(0xFFFDE047),
            bonusFlair = "+25 Poin & Jubah",
            description = "Zirah seremonial obsidian-emas lengkap dengan jubah komando Emerald Crest.",
            hasShoulderPads = true,
            hasCapeOrCollar = true
        )
    )

    fun byId(id: String): UniformItem =
        items.firstOrNull { it.id == id } ?: items.first()
}

enum class WeaponStyle {
    BATON, FLASHLIGHT, TASER_PISTOL, NET_LAUNCHER, CYBER_CARBINE
}

data class WeaponItem(
    val id: String,
    val name: String,
    val categoryTag: String,
    val price: Long,
    val primaryColor: Color,
    val accentColor: Color,
    val style: WeaponStyle,
    val rangeNorm: Float,
    val stunSeconds: Float,
    val bonusPoints: Long,
    val description: String
)

object WeaponCatalog {
    val items: List<WeaponItem> = listOf(
        WeaponItem(
            id = "baton_standard",
            name = "Tongkat Patroli Standar",
            categoryTag = "Jarak Dekat",
            price = 0L,
            primaryColor = Color(0xFF64748B),
            accentColor = Color(0xFF38BDF8),
            style = WeaponStyle.BATON,
            rangeNorm = 0.22f,
            stunSeconds = 2.0f,
            bonusPoints = 0L,
            description = "Tongkat kejut ringan di tangan kanan untuk melumpuhkan penyusup jarak dekat."
        ),
        WeaponItem(
            id = "tactical_flashlight",
            name = "Senter Kejut LED Pro",
            categoryTag = "Cahaya & Kejut",
            price = 200L,
            primaryColor = Color(0xFF334155),
            accentColor = Color(0xFFFDE047),
            style = WeaponStyle.FLASHLIGHT,
            rangeNorm = 0.28f,
            stunSeconds = 3.0f,
            bonusPoints = 10L,
            description = "Senter logam berat dengan sorot lampu 3D terang untuk menyilaukan pencuri."
        ),
        WeaponItem(
            id = "stun_pistol",
            name = "Pistol Bius Taser X2",
            categoryTag = "Tembak Menengah",
            price = 450L,
            primaryColor = Color(0xFFEAB308),
            accentColor = Color(0xFF22D3EE),
            style = WeaponStyle.TASER_PISTOL,
            rangeNorm = 0.36f,
            stunSeconds = 4.0f,
            bonusPoints = 15L,
            description = "Menembakkan busur listrik biru untuk menghentikan langkah maling dari jauh."
        ),
        WeaponItem(
            id = "net_launcher",
            name = "Peluncur Jaring Taktis",
            categoryTag = "Penjerat Luas",
            price = 850L,
            primaryColor = Color(0xFF0EA5E9),
            accentColor = Color(0xFF6EE7B7),
            style = WeaponStyle.NET_LAUNCHER,
            rangeNorm = 0.42f,
            stunSeconds = 5.5f,
            bonusPoints = 20L,
            description = "Senjata peluncur jaring energi untuk mengunci pergerakan penyusup lebih lama."
        ),
        WeaponItem(
            id = "cyber_carbine",
            name = "Senapan Pulsa Siber",
            categoryTag = "Jarak Jauh Elit",
            price = 1500L,
            primaryColor = Color(0xFF8B5CF6),
            accentColor = Color(0xFFF43F5E),
            style = WeaponStyle.CYBER_CARBINE,
            rangeNorm = 0.50f,
            stunSeconds = 6.5f,
            bonusPoints = 35L,
            description = "Karabin energi futuristik berjangkauan terjauh +35 Poin bonus tangkap."
        )
    )

    fun byId(id: String): WeaponItem =
        items.firstOrNull { it.id == id } ?: items.first()
}

enum class AttributeStyle {
    BELT, SPEED_BOOTS, KEVLAR_VEST, NVG_GOGGLES, SCANNER_DRONE
}

data class AttributeItem(
    val id: String,
    val name: String,
    val needLabel: String,
    val price: Long,
    val primaryColor: Color,
    val accentColor: Color,
    val style: AttributeStyle,
    val speedMultiplier: Float,
    val visionBoost: Float,
    val catchRadiusBonus: Float,
    val staminaDrainFactor: Float,
    val description: String
)

object AttributeCatalog {
    val items: List<AttributeItem> = listOf(
        AttributeItem(
            id = "utility_belt",
            name = "Sabuk Utilitas Dasar",
            needLabel = "Kebutuhan Dasar",
            price = 0L,
            primaryColor = Color(0xFF475569),
            accentColor = Color(0xFFF59E0B),
            style = AttributeStyle.BELT,
            speedMultiplier = 1.0f,
            visionBoost = 0.0f,
            catchRadiusBonus = 0.0f,
            staminaDrainFactor = 1.0f,
            description = "Sabuk perlengkapan standar untuk borgol dan walkie-talkie patroli."
        ),
        AttributeItem(
            id = "speed_boots",
            name = "Sepatu Taktis Turbo",
            needLabel = "Mobilitas & Stamina",
            price = 250L,
            primaryColor = Color(0xFF0284C7),
            accentColor = Color(0xFF38BDF8),
            style = AttributeStyle.SPEED_BOOTS,
            speedMultiplier = 1.38f,
            visionBoost = 0.05f,
            catchRadiusBonus = 0.02f,
            staminaDrainFactor = 0.72f,
            description = "Memenuhi kebutuhan lari cepat 3D (+38% kecepatan) saat mengejar pencuri."
        ),
        AttributeItem(
            id = "kevlar_vest",
            name = "Rompi Pelindung Kevlar",
            needLabel = "Pertahanan & Daya Tahan",
            price = 450L,
            primaryColor = Color(0xFF1E293B),
            accentColor = Color(0xFF10B981),
            style = AttributeStyle.KEVLAR_VEST,
            speedMultiplier = 1.10f,
            visionBoost = 0.05f,
            catchRadiusBonus = 0.03f,
            staminaDrainFactor = 0.50f,
            description = "Rompi taktis 3D yang menghemat stamina patroli hingga 50% lebih awet."
        ),
        AttributeItem(
            id = "nvg_goggles",
            name = "Helm Kacamata Malam NVG",
            needLabel = "Visibilitas Malam",
            price = 750L,
            primaryColor = Color(0xFF065F46),
            accentColor = Color(0xFF34D399),
            style = AttributeStyle.NVG_GOGGLES,
            speedMultiplier = 1.15f,
            visionBoost = 0.85f,
            catchRadiusBonus = 0.04f,
            staminaDrainFactor = 0.75f,
            description = "Memenuhi kebutuhan penglihatan malam: menerangi arena 3D & sorot target."
        ),
        AttributeItem(
            id = "scanner_drone",
            name = "Drone Pemindai Mini",
            needLabel = "Radar & Jangkauan",
            price = 1300L,
            primaryColor = Color(0xFF0891B2),
            accentColor = Color(0xFFFDE047),
            style = AttributeStyle.SCANNER_DRONE,
            speedMultiplier = 1.24f,
            visionBoost = 0.45f,
            catchRadiusBonus = 0.11f,
            staminaDrainFactor = 0.60f,
            description = "Drone pendamping 3D melayang di bahu, memperluas radius tangkap & pelacakan!"
        )
    )

    fun byId(id: String): AttributeItem =
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
