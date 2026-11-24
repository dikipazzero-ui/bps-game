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

enum class MissionOutcome {
    ACTIVE, VICTORY, WASTED, TIME_UP
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Petugas Selaras",
    val points: Long = 100L,
    val apprehensions: Long = 0L,
    val equippedUniformId: String = "rookie_cadet",
    val unlockedUniformIds: List<String> = listOf("rookie_cadet"),
    val equippedWeaponId: String = "pistol_9mm",
    val unlockedWeaponIds: List<String> = listOf("pistol_9mm"),
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
    val title: String = "Patroli Bumi Papan Selaras",
    val hostUid: String = "",
    val status: String = RoomStatus.ACTIVE.name,
    val memberUids: List<String> = emptyList(),
    val northGateStatus: String = GateStatus.OPEN.name,
    val southGateStatus: String = GateStatus.OPEN.name,
    val scoreTotal: Long = 0L,
    val apprehensionCount: Long = 0L,
    val activeThiefSectorId: String = "BLOK_CEMARA",
    val activeThiefStreet: String = "Jl. Cemara Indah",
    val activeThiefX: Double = 0.18,
    val activeThiefY: Double = 0.50,
    val thiefSpotted: Boolean = true,
    val gandiLastLine: String = "GANDI // Siaga! Penyusup terdeteksi masuk kawasan Bumi Papan Selaras dekat Blok Cemara!",
    val gandiMood: String = GandiMood.ALERT.name,
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
    val weaponId: String = "pistol_9mm",
    val attributeId: String = "utility_belt",
    val sectorId: String = "AREA_MASJID",
    val posX: Double = 0.50,
    val posY: Double = 0.60,
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
    val sectorId: String = "AREA_MASJID",
    val streetName: String = "Bundaran Masjid Selaras",
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

enum class SectorKind { GATE, BLOCK, LANDMARK }

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
    val accentColor: Color,
    val isMosque: Boolean = false
)

enum class VehicleType(val label: String, val speedMultiplier: Float, val armorBonus: Float) {
    CAR("Mobil Patroli", 2.40f, 0.35f),
    MOTORCYCLE("Motor Patroli", 2.10f, 0.12f)
}

data class WorldVehicle(
    val id: String,
    val type: VehicleType,
    val x: Float,
    val y: Float,
    val facingDeg: Float,
    val bodyColor: Color,
    val accentColor: Color
)

object EstateWorld {
    val buildings: List<BuildingFootprint> = listOf(
        BuildingFootprint(
            id = "BLOK_KRISAN",
            label = "BLOK KRISAN",
            minX = 0.14f, minY = 0.14f, maxX = 0.30f, maxY = 0.25f,
            height3D = 18f,
            roofColor = Color(0xFF283B5B),
            wallColor = Color(0xFF17253B),
            accentColor = Color(0xFF38BDF8)
        ),
        BuildingFootprint(
            id = "BLOK_CEMPAKA",
            label = "BLOK CEMPAKA",
            minX = 0.70f, minY = 0.14f, maxX = 0.86f, maxY = 0.25f,
            height3D = 19f,
            roofColor = Color(0xFF334155),
            wallColor = Color(0xFF1E293B),
            accentColor = Color(0xFFF59E0B)
        ),
        BuildingFootprint(
            id = "BLOK_CEMARA",
            label = "BLOK CEMARA",
            minX = 0.11f, minY = 0.43f, maxX = 0.25f, maxY = 0.56f,
            height3D = 20f,
            roofColor = Color(0xFF1E3A5F),
            wallColor = Color(0xFF122640),
            accentColor = Color(0xFF10B981)
        ),
        BuildingFootprint(
            id = "AREA_MASJID",
            label = "MASJID AGUNG SELARAS",
            minX = 0.44f, minY = 0.44f, maxX = 0.56f, maxY = 0.56f,
            height3D = 14f,
            roofColor = Color(0xFF047857),
            wallColor = Color(0xFFE2E8F0),
            accentColor = Color(0xFFFDE047),
            isMosque = true
        ),
        BuildingFootprint(
            id = "BLOK_AKASIA",
            label = "BLOK AKASIA",
            minX = 0.75f, minY = 0.43f, maxX = 0.89f, maxY = 0.56f,
            height3D = 21f,
            roofColor = Color(0xFF263859),
            wallColor = Color(0xFF162338),
            accentColor = Color(0xFF22D3EE)
        ),
        BuildingFootprint(
            id = "BLOK_CAMELIA",
            label = "BLOK CAMELIA",
            minX = 0.14f, minY = 0.75f, maxX = 0.30f, maxY = 0.86f,
            height3D = 17f,
            roofColor = Color(0xFF3B2F4A),
            wallColor = Color(0xFF241B30),
            accentColor = Color(0xFFF472B6)
        ),
        BuildingFootprint(
            id = "BLOK_FLAMBOYAN",
            label = "BLOK FLAMBOYAN",
            minX = 0.70f, minY = 0.75f, maxX = 0.86f, maxY = 0.86f,
            height3D = 22f,
            roofColor = Color(0xFF3A2A2E),
            wallColor = Color(0xFF251A1D),
            accentColor = Color(0xFFFB7185)
        )
    )

    val subBuildings: List<BuildingFootprint> = listOf(
        BuildingFootprint("CLUSTER_KRISAN_U", "RUMAH KRISAN", 0.14f, 0.06f, 0.25f, 0.11f, 12f, Color(0xFF22334D), Color(0xFF162235), Color(0xFF38BDF8)),
        BuildingFootprint("CLUSTER_CEMPAKA_U", "RUMAH CEMPAKA", 0.75f, 0.06f, 0.86f, 0.11f, 13f, Color(0xFF2D394B), Color(0xFF1B2432), Color(0xFFF59E0B)),
        BuildingFootprint("CLUSTER_UTARA_B", "KLASTER UTARA-B", 0.36f, 0.14f, 0.44f, 0.24f, 15f, Color(0xFF263852), Color(0xFF17253B), Color(0xFF38BDF8)),
        BuildingFootprint("CLUSTER_UTARA_T", "KLASTER UTARA-T", 0.56f, 0.14f, 0.64f, 0.24f, 15f, Color(0xFF2D3A4F), Color(0xFF192231), Color(0xFFF59E0B)),
        BuildingFootprint("CLUSTER_SELATAN_B", "KLASTER SELATAN-B", 0.36f, 0.75f, 0.44f, 0.85f, 14f, Color(0xFF332842), Color(0xFF21182C), Color(0xFFF472B6)),
        BuildingFootprint("CLUSTER_SELATAN_T", "KLASTER SELATAN-T", 0.56f, 0.75f, 0.64f, 0.85f, 16f, Color(0xFF36282E), Color(0xFF22171B), Color(0xFFFB7185)),
        BuildingFootprint("CLUSTER_CAMELIA_S", "RUMAH CAMELIA", 0.14f, 0.89f, 0.25f, 0.94f, 12f, Color(0xFF342942), Color(0xFF221A2D), Color(0xFFF472B6)),
        BuildingFootprint("CLUSTER_FLAMBOYAN_S", "RUMAH FLAMBOYAN", 0.75f, 0.89f, 0.86f, 0.94f, 13f, Color(0xFF38282B), Color(0xFF23181A), Color(0xFFFB7185))
    )

    val allBuildings: List<BuildingFootprint> = buildings + subBuildings

    val defaultVehicles: List<WorldVehicle> = listOf(
        WorldVehicle("car_masjid", VehicleType.CAR, 0.475f, 0.635f, 90f, Color(0xFF0F172A), Color(0xFF38BDF8)),
        WorldVehicle("moto_masjid", VehicleType.MOTORCYCLE, 0.53f, 0.635f, 0f, Color(0xFF1E293B), Color(0xFFF59E0B)),
        WorldVehicle("moto_krisan", VehicleType.MOTORCYCLE, 0.335f, 0.295f, 180f, Color(0xFF0284C7), Color(0xFFE2E8F0)),
        WorldVehicle("car_flamboyan", VehicleType.CAR, 0.66f, 0.705f, 270f, Color(0xFFE2E8F0), Color(0xFFEF4444)),
        WorldVehicle("moto_akasia", VehicleType.MOTORCYCLE, 0.665f, 0.295f, 90f, Color(0xFF10B981), Color(0xFFFDE047))
    )

    val sectors: List<EstateSector> = listOf(
        EstateSector(
            id = "GERBANG_UTARA",
            shortCode = "GERBANG-U",
            name = "Gerbang Utama Utara",
            streetName = "Jl. Selaras Utara",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.08f,
            description = "Pos pemeriksaan utama Bumi Papan Selaras dengan palang portal otomatis.",
            neighbors = listOf("BLOK_KRISAN", "BLOK_CEMPAKA", "AREA_MASJID")
        ),
        EstateSector(
            id = "BLOK_KRISAN",
            shortCode = "KRISAN",
            name = "Blok Krisan",
            streetName = "Jl. Krisan Raya",
            kind = SectorKind.BLOCK,
            normX = 0.22f,
            normY = 0.29f,
            description = "Klaster hunian barat laut dengan akses gang menuju Blok Cemara.",
            neighbors = listOf("GERBANG_UTARA", "BLOK_CEMARA", "AREA_MASJID")
        ),
        EstateSector(
            id = "BLOK_CEMPAKA",
            shortCode = "CEMPAKA",
            name = "Blok Cempaka",
            streetName = "Jl. Cempaka Asri",
            kind = SectorKind.BLOCK,
            normX = 0.78f,
            normY = 0.29f,
            description = "Deretan rumah sudut timur laut dekat jalan utama kompleks.",
            neighbors = listOf("GERBANG_UTARA", "BLOK_AKASIA", "AREA_MASJID")
        ),
        EstateSector(
            id = "BLOK_CEMARA",
            shortCode = "CEMARA",
            name = "Blok Cemara",
            streetName = "Jl. Cemara Indah",
            kind = SectorKind.BLOCK,
            normX = 0.29f,
            normY = 0.50f,
            description = "Kawasan rindang berderet pohon cemara dengan gang sempit favorit penyusup.",
            neighbors = listOf("BLOK_KRISAN", "AREA_MASJID", "BLOK_CAMELIA")
        ),
        EstateSector(
            id = "AREA_MASJID",
            shortCode = "MASJID",
            name = "Area Masjid Agung Selaras",
            streetName = "Bundaran Masjid Selaras",
            kind = SectorKind.LANDMARK,
            normX = 0.50f,
            normY = 0.60f,
            description = "Landmark ikonik & titik orientasi utama kawasan Bumi Papan Selaras.",
            neighbors = listOf("GERBANG_UTARA", "BLOK_KRISAN", "BLOK_CEMPAKA", "BLOK_CEMARA", "BLOK_AKASIA", "BLOK_CAMELIA", "BLOK_FLAMBOYAN", "GERBANG_SELATAN")
        ),
        EstateSector(
            id = "BLOK_AKASIA",
            shortCode = "AKASIA",
            name = "Blok Akasia",
            streetName = "Jl. Akasia Timur",
            kind = SectorKind.BLOCK,
            normX = 0.71f,
            normY = 0.50f,
            description = "Blok hunian timur dengan penerangan jalan utama dan tikungan cepat.",
            neighbors = listOf("BLOK_CEMPAKA", "AREA_MASJID", "BLOK_FLAMBOYAN")
        ),
        EstateSector(
            id = "BLOK_CAMELIA",
            shortCode = "CAMELIA",
            name = "Blok Camelia",
            streetName = "Jl. Camelia Barat",
            kind = SectorKind.BLOCK,
            normX = 0.22f,
            normY = 0.71f,
            description = "Klaster barat daya yang terhubung langsung ke gerbang selatan.",
            neighbors = listOf("BLOK_CEMARA", "AREA_MASJID", "GERBANG_SELATAN")
        ),
        EstateSector(
            id = "BLOK_FLAMBOYAN",
            shortCode = "FLAMBOYAN",
            name = "Blok Flamboyan",
            streetName = "Jl. Flamboyan Raya",
            kind = SectorKind.BLOCK,
            normX = 0.78f,
            normY = 0.71f,
            description = "Area hunian bertingkat di sisi tenggara yang rawan pelarian.",
            neighbors = listOf("BLOK_AKASIA", "AREA_MASJID", "GERBANG_SELATAN")
        ),
        EstateSector(
            id = "GERBANG_SELATAN",
            shortCode = "GERBANG-S",
            name = "Gerbang Servis Selatan",
            streetName = "Jl. Selaras Selatan",
            kind = SectorKind.GATE,
            normX = 0.50f,
            normY = 0.92f,
            description = "Portal akses selatan perumahan Bumi Papan Selaras.",
            neighbors = listOf("AREA_MASJID", "BLOK_CAMELIA", "BLOK_FLAMBOYAN")
        )
    )

    fun byId(id: String): EstateSector {
        val mapped = when (id) {
            "GATE_NORTH" -> "GERBANG_UTARA"
            "GATE_SOUTH" -> "GERBANG_SELATAN"
            "BLOCK_A" -> "BLOK_KRISAN"
            "BLOCK_B" -> "BLOK_CEMPAKA"
            "BLOCK_C" -> "BLOK_CAMELIA"
            "BLOCK_D" -> "BLOK_FLAMBOYAN"
            "PARK_WEST" -> "BLOK_CEMARA"
            "PARK_CENTRAL", "CROSSWALK" -> "AREA_MASJID"
            else -> id
        }
        return sectors.firstOrNull { it.id == mapped } ?: sectors.first()
    }

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

    fun isWalkable(nx: Float, ny: Float, pad: Float = 0.015f): Boolean {
        if (nx < 0.04f || nx > 0.96f || ny < 0.04f || ny > 0.96f) return false
        return allBuildings.none { b ->
            nx in (b.minX - pad)..(b.maxX + pad) && ny in (b.minY - pad)..(b.maxY + pad)
        }
    }

    fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val steps = 10
        for (i in 1 until steps) {
            val t = i / steps.toFloat()
            val sx = x1 + (x2 - x1) * t
            val sy = y1 + (y2 - y1) * t
            if (allBuildings.any { b -> sx in b.minX..b.maxX && sy in b.minY..b.maxY }) {
                return false
            }
        }
        return true
    }

    fun moveWithCollision(oldX: Float, oldY: Float, targetX: Float, targetY: Float): Pair<Float, Float> {
        val clampedX = targetX.coerceIn(0.04f, 0.96f)
        val clampedY = targetY.coerceIn(0.04f, 0.96f)
        if (isWalkable(clampedX, clampedY)) return clampedX to clampedY
        if (isWalkable(clampedX, oldY)) return clampedX to oldY
        if (isWalkable(oldX, clampedY)) return oldX to clampedY
        return oldX to oldY
    }
}

enum class ShopCategory(val label: String) {
    UNIFORM("Baju Armor"),
    WEAPON("Senjata"),
    ATTRIBUTE("Atribut")
}

data class UniformItem(
    val id: String,
    val name: String,
    val rankTitle: String,
    val armorLevel: Int,
    val damageReduction: Float,
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
            name = "Seragam Patroli Dasar",
            rankTitle = "Armor Level 1",
            armorLevel = 1,
            damageReduction = 0.10f,
            price = 0L,
            primaryColor = Color(0xFF38BDF8),
            trimColor = Color(0xFFF8FAFC),
            bonusFlair = "Redam 10% Damage",
            description = "Baju dinas keamanan standar Bumi Papan Selaras dengan proteksi dasar Level 1."
        ),
        UniformItem(
            id = "neon_warden",
            name = "Jaket Taktis Selaras",
            rankTitle = "Armor Level 2",
            armorLevel = 2,
            damageReduction = 0.28f,
            price = 250L,
            primaryColor = Color(0xFF06B6D4),
            trimColor = Color(0xFF22D3EE),
            bonusFlair = "Redam 28% Damage",
            description = "Baju premium berlapis pelindung ringan Level 2 untuk menahan tembakan pistol.",
            hasShoulderPads = true
        ),
        UniformItem(
            id = "crimson_marshal",
            name = "Rompi Balistik Kevlar",
            rankTitle = "Armor Level 3",
            armorLevel = 3,
            damageReduction = 0.45f,
            price = 550L,
            primaryColor = Color(0xFFEF4444),
            trimColor = Color(0xFFFBBF24),
            bonusFlair = "Redam 45% Damage",
            description = "Jaket tempur balistik Level 3 berpelindung bahu untuk baku tembak sengit.",
            hasShoulderPads = true
        ),
        UniformItem(
            id = "gandi_fanclub",
            name = "Zirah Komando Elit",
            rankTitle = "Armor Level 4",
            armorLevel = 4,
            damageReduction = 0.60f,
            price = 1000L,
            primaryColor = Color(0xFFF59E0B),
            trimColor = Color(0xFFFEF08A),
            bonusFlair = "Redam 60% +25 Poin",
            description = "Baju armor premium Level 4 beraksen emas yang meredam 60% serangan pencuri!",
            hasShoulderPads = true,
            hasCapeOrCollar = true
        ),
        UniformItem(
            id = "royal_sentinel",
            name = "Juggernaut Titanium Selaras",
            rankTitle = "Armor Level 5 MAX",
            armorLevel = 5,
            damageReduction = 0.75f,
            price = 1800L,
            primaryColor = Color(0xFFA855F7),
            trimColor = Color(0xFFFDE047),
            bonusFlair = "Redam 75% +25 Poin",
            description = "Armor tempur tertinggi Level 5 berbahan titanium dengan ketahanan maksimal.",
            hasShoulderPads = true,
            hasCapeOrCollar = true
        )
    )

    fun byId(id: String): UniformItem =
        items.firstOrNull { it.id == id } ?: items.first()
}

enum class WeaponStyle {
    PISTOL, GRENADE, AK47, BAZOOKA
}

data class WeaponItem(
    val id: String,
    val name: String,
    val categoryTag: String,
    val price: Long,
    val primaryColor: Color,
    val accentColor: Color,
    val style: WeaponStyle,
    val damage: Int,
    val rangeNorm: Float,
    val aoeRadius: Float,
    val cooldownMs: Long,
    val stunSeconds: Float,
    val bonusPoints: Long,
    val description: String
)

object WeaponCatalog {
    val items: List<WeaponItem> = listOf(
        WeaponItem(
            id = "pistol_9mm",
            name = "Pistol Taktis 9mm",
            categoryTag = "Mobilitas Tinggi",
            price = 0L,
            primaryColor = Color(0xFF64748B),
            accentColor = Color(0xFF38BDF8),
            style = WeaponStyle.PISTOL,
            damage = 24,
            rangeNorm = 0.30f,
            aoeRadius = 0f,
            cooldownMs = 420L,
            stunSeconds = 1.5f,
            bonusPoints = 0L,
            description = "Pistol jangkauan menengah dengan mobilitas tinggi untuk serangan dasar cepat."
        ),
        WeaponItem(
            id = "granat_frag",
            name = "Granat Ledak MK-II",
            categoryTag = "Ledakan Area",
            price = 250L,
            primaryColor = Color(0xFF15803D),
            accentColor = Color(0xFFF59E0B),
            style = WeaponStyle.GRENADE,
            damage = 60,
            rangeNorm = 0.36f,
            aoeRadius = 0.14f,
            cooldownMs = 1150L,
            stunSeconds = 3.0f,
            bonusPoints = 15L,
            description = "Lemparan granat berdaya ledak area untuk menghantam pencuri saat berlindung."
        ),
        WeaponItem(
            id = "ak47_assault",
            name = "AK-47 Otomatis",
            categoryTag = "Rifle Cepat",
            price = 650L,
            primaryColor = Color(0xFF92400E),
            accentColor = Color(0xFFFBBF24),
            style = WeaponStyle.AK47,
            damage = 22,
            rangeNorm = 0.43f,
            aoeRadius = 0f,
            cooldownMs = 180L,
            stunSeconds = 2.5f,
            bonusPoints = 25L,
            description = "Rifle otomatis AK-47 berdaya tembak kuat dan cepat untuk duel jarak jauh."
        ),
        WeaponItem(
            id = "bazoka_rpg",
            name = "Bazoka Peluncur Roket",
            categoryTag = "Daya Ledak Maks",
            price = 1400L,
            primaryColor = Color(0xFF475569),
            accentColor = Color(0xFFEF4444),
            style = WeaponStyle.BAZOOKA,
            damage = 95,
            rangeNorm = 0.52f,
            aoeRadius = 0.18f,
            cooldownMs = 1600L,
            stunSeconds = 4.5f,
            bonusPoints = 50L,
            description = "Senjata peluncur roket berdaya ledak terbesar untuk meruntuhkan pertahanan pencuri!"
        )
    )

    fun byId(id: String): WeaponItem {
        val mapped = when (id) {
            "baton_standard", "tactical_flashlight" -> "pistol_9mm"
            "stun_pistol" -> "pistol_9mm"
            "net_launcher" -> "granat_frag"
            "cyber_carbine" -> "ak47_assault"
            else -> id
        }
        return items.firstOrNull { it.id == mapped } ?: items.first()
    }
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
            needLabel = "Perlengkapan Standar",
            price = 0L,
            primaryColor = Color(0xFF475569),
            accentColor = Color(0xFFF59E0B),
            style = AttributeStyle.BELT,
            speedMultiplier = 1.0f,
            visionBoost = 0.0f,
            catchRadiusBonus = 0.0f,
            staminaDrainFactor = 1.0f,
            description = "Sabuk taktis dasar untuk borgol, amunisi cadangan, dan radio patroli."
        ),
        AttributeItem(
            id = "speed_boots",
            name = "Sepatu Taktis Turbo",
            needLabel = "Mobilitas Pengejaran",
            price = 250L,
            primaryColor = Color(0xFF0284C7),
            accentColor = Color(0xFF38BDF8),
            style = AttributeStyle.SPEED_BOOTS,
            speedMultiplier = 1.38f,
            visionBoost = 0.05f,
            catchRadiusBonus = 0.02f,
            staminaDrainFactor = 0.72f,
            description = "Menambah kecepatan lari +38% di kawasan Bumi Papan Selaras."
        ),
        AttributeItem(
            id = "kevlar_vest",
            name = "Pelindung Stamina & Medkit",
            needLabel = "Daya Tahan Tempur",
            price = 450L,
            primaryColor = Color(0xFF1E293B),
            accentColor = Color(0xFF10B981),
            style = AttributeStyle.KEVLAR_VEST,
            speedMultiplier = 1.12f,
            visionBoost = 0.05f,
            catchRadiusBonus = 0.03f,
            staminaDrainFactor = 0.48f,
            description = "Menghemat stamina lari hingga 50% lebih awet saat mengejar antarblok."
        ),
        AttributeItem(
            id = "nvg_goggles",
            name = "Kacamata Malam NVG",
            needLabel = "Visibilitas Malam",
            price = 750L,
            primaryColor = Color(0xFF065F46),
            accentColor = Color(0xFF34D399),
            style = AttributeStyle.NVG_GOGGLES,
            speedMultiplier = 1.15f,
            visionBoost = 0.85f,
            catchRadiusBonus = 0.04f,
            staminaDrainFactor = 0.75f,
            description = "Menerangi seluruh gang gelap & menyorot siluet pencuri di malam hari."
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
            catchRadiusBonus = 0.12f,
            staminaDrainFactor = 0.60f,
            description = "Drone pendamping di bahu yang memperluas radius tangkap & radar pelacak."
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
        QuickRadioCommand("cut_off", "Kepung Masjid!", "Potong jalur pelarian di Bundaran Area Masjid Selaras!", PingType.BLOCK_GATE),
        QuickRadioCommand("seal_portal", "Kunci Gerbang Selatan!", "Mengunci Gerbang Servis Selatan—blokir jalur kabur!", PingType.BLOCK_GATE),
        QuickRadioCommand("all_clear", "Blok Aman", "Penyisiran %SECTOR% selesai—wilayah aman terkendali.", PingType.CLEAR)
    )
}
