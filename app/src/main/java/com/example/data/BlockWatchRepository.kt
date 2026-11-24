package com.example.data

import android.content.Context
import com.example.R
import com.example.engine.GandiDispatcher
import com.example.model.AttributeCatalog
import com.example.model.CommsChannel
import com.example.model.CommsMessage
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PatrolRoom
import com.example.model.PingType
import com.example.model.PlayerRole
import com.example.model.RoomPlayer
import com.example.model.RoomStatus
import com.example.model.TacticalPing
import com.example.model.UniformCatalog
import com.example.model.UserProfile
import com.example.model.WeaponCatalog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class BlockWatchRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun handleStreamError(error: Throwable, operation: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operation, path)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile observation." }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeMyRooms(userId: String): Flow<List<PatrolRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user room listing." }
        val path = "rooms"
        emitAll(
            db.collection("rooms")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PatrolRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status != RoomStatus.ENDED.name }
                        .sortedByDescending { it.updatedAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeRoom(roomId: String): Flow<PatrolRoom?> = flow {
        val uid = requireUserId()
        val path = "rooms/$roomId"
        emitAll(
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PatrolRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observePlayers(roomId: String): Flow<List<RoomPlayer>> = flow {
        val uid = requireUserId()
        val path = "rooms/$roomId/players"
        emitAll(
            db.collection("rooms").document(roomId).collection("players")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RoomPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.roomScore }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePings(roomId: String): Flow<List<TacticalPing>> = flow {
        val uid = requireUserId()
        val path = "rooms/$roomId/pings"
        emitAll(
            db.collection("rooms").document(roomId).collection("pings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(12)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TacticalPing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMessages(roomId: String): Flow<List<CommsMessage>> = flow {
        val uid = requireUserId()
        val path = "rooms/$roomId/messages"
        emitAll(
            db.collection("rooms").document(roomId).collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(35)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommsMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .reversed()
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(rawDisplayName: String?): Result<UserProfile> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val rawData = snap.data
                if (rawData != null && (!rawData.containsKey("equippedWeaponId") || !rawData.containsKey("equippedAttributeId"))) {
                    docRef.update(
                        mapOf(
                            "equippedWeaponId" to "pistol_9mm",
                            "unlockedWeaponIds" to listOf("pistol_9mm"),
                            "equippedAttributeId" to "utility_belt",
                            "unlockedAttributeIds" to listOf("utility_belt"),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    val migrated = docRef.get().await()
                        .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (migrated != null) return Result.success(migrated)
                }
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    val normWpn = WeaponCatalog.byId(existing.equippedWeaponId).id
                    val normUnlocked = existing.unlockedWeaponIds.map { WeaponCatalog.byId(it).id }.distinct()
                    if (normWpn != existing.equippedWeaponId || normUnlocked != existing.unlockedWeaponIds) {
                        docRef.update(
                            mapOf(
                                "equippedWeaponId" to normWpn,
                                "unlockedWeaponIds" to normUnlocked,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        return Result.success(existing.copy(equippedWeaponId = normWpn, unlockedWeaponIds = normUnlocked))
                    }
                    return Result.success(existing)
                }
            }
            val safeName = (rawDisplayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Penjaga_${uid.take(4)}").take(40)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "points" to 150L,
                "apprehensions" to 0L,
                "equippedUniformId" to "rookie_cadet",
                "unlockedUniformIds" to listOf("rookie_cadet"),
                "equippedWeaponId" to "pistol_9mm",
                "unlockedWeaponIds" to listOf("pistol_9mm"),
                "equippedAttributeId" to "utility_belt",
                "unlockedAttributeIds" to listOf("utility_belt"),
                "voiceEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, displayName = safeName, points = 150L)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createPatrolRoom(
        titleInput: String,
        profile: UserProfile
    ): Result<String> {
        val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val roomCode = buildString {
            append("EC")
            repeat(4) { append(codeChars[Random.nextInt(codeChars.length)]) }
        }
        val path = "rooms/$roomCode"
        return try {
            val uid = requireUserId()
            val cleanTitle = titleInput.trim().ifEmpty { "Patroli Bumi Papan Selaras" }.take(50).let {
                if (it.length < 3) "Patroli Selaras" else it
            }
            val startThiefSector = EstateWorld.byId("BLOK_CEMARA")
            val welcome = GandiDispatcher.shiftWelcome(profile.displayName, roomCode)

            val roomPayload = mapOf(
                "roomCode" to roomCode,
                "title" to cleanTitle,
                "hostUid" to uid,
                "status" to RoomStatus.ACTIVE.name,
                "memberUids" to listOf(uid),
                "northGateStatus" to GateStatus.OPEN.name,
                "southGateStatus" to GateStatus.OPEN.name,
                "scoreTotal" to 0L,
                "apprehensionCount" to 0L,
                "activeThiefSectorId" to startThiefSector.id,
                "activeThiefStreet" to startThiefSector.streetName,
                "activeThiefX" to startThiefSector.normX.toDouble(),
                "activeThiefY" to startThiefSector.normY.toDouble(),
                "thiefSpotted" to true,
                "gandiLastLine" to welcome.line.take(300),
                "gandiMood" to welcome.mood.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(roomCode).set(roomPayload).await()

            val startSector = EstateWorld.byId("AREA_MASJID")
            val playerPayload = mapOf(
                "userId" to uid,
                "displayName" to profile.displayName.take(40),
                "role" to PlayerRole.GUARD.name,
                "uniformId" to profile.equippedUniformId,
                "weaponId" to WeaponCatalog.byId(profile.equippedWeaponId).id,
                "attributeId" to profile.equippedAttributeId,
                "sectorId" to startSector.id,
                "posX" to startSector.normX.toDouble(),
                "posY" to startSector.normY.toDouble(),
                "facingDeg" to 0.0,
                "pttActive" to false,
                "roomScore" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(roomCode)
                .collection("players").document(uid)
                .set(playerPayload).await()

            postGandiDispatch(roomCode, uid, welcome.line, startSector.shortCode)
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(
        rawCode: String,
        profile: UserProfile
    ): Result<String> {
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        if (code.length < 3 || code.length > 24) {
            return Result.failure(IllegalArgumentException("Masukkan kode ruang 3–24 karakter yang valid."))
        }
        val path = "rooms/$code"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("rooms").document(code)
            val snap = roomRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Ruang patroli $code tidak ditemukan."))
            }
            val room = snap.toObject(PatrolRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Gagal memuat ruang $code."))
            if (room.status == RoomStatus.ENDED.name) {
                return Result.failure(IllegalStateException("Shift patroli $code sudah berakhir."))
            }
            if (uid !in room.memberUids) {
                if (room.memberUids.size >= 12) {
                    return Result.failure(IllegalStateException("Ruang regu $code sudah penuh (12/12)."))
                }
                roomRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val playerRef = roomRef.collection("players").document(uid)
            val playerSnap = playerRef.get().await()
            if (!playerSnap.exists()) {
                val startSector = EstateWorld.byId("AREA_MASJID")
                playerRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to profile.displayName.take(40),
                        "role" to PlayerRole.GUARD.name,
                        "uniformId" to profile.equippedUniformId,
                        "weaponId" to WeaponCatalog.byId(profile.equippedWeaponId).id,
                        "attributeId" to profile.equippedAttributeId,
                        "sectorId" to startSector.id,
                        "posX" to startSector.normX.toDouble(),
                        "posY" to startSector.normY.toDouble(),
                        "facingDeg" to 0.0,
                        "pttActive" to false,
                        "roomScore" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                playerRef.update(
                    mapOf(
                        "uniformId" to profile.equippedUniformId,
                        "weaponId" to WeaponCatalog.byId(profile.equippedWeaponId).id,
                        "attributeId" to profile.equippedAttributeId,
                        "displayName" to profile.displayName.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun movePlayer(
        roomId: String,
        sector: EstateSector,
        normX: Float,
        normY: Float,
        facingDeg: Float = 0f
    ): Result<Unit> {
        var path = "rooms/$roomId/players"
        return try {
            val uid = requireUserId()
            path = "rooms/$roomId/players/$uid"
            db.collection("rooms").document(roomId)
                .collection("players").document(uid)
                .update(
                    mapOf(
                        "sectorId" to sector.id,
                        "posX" to normX.coerceIn(0.05f, 0.95f).toDouble(),
                        "posY" to normY.coerceIn(0.05f, 0.95f).toDouble(),
                        "facingDeg" to facingDeg.coerceIn(-360f, 360f).toDouble(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePttState(roomId: String, active: Boolean): Result<Unit> {
        var path = "rooms/$roomId/players"
        return try {
            val uid = requireUserId()
            path = "rooms/$roomId/players/$uid"
            db.collection("rooms").document(roomId)
                .collection("players").document(uid)
                .update(
                    mapOf(
                        "pttActive" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePlayerRole(roomId: String, currentRole: PlayerRole): Result<Unit> {
        var path = "rooms/$roomId/players"
        return try {
            val uid = requireUserId()
            path = "rooms/$roomId/players/$uid"
            val nextRole = if (currentRole == PlayerRole.GUARD) PlayerRole.THIEF else PlayerRole.GUARD
            db.collection("rooms").document(roomId)
                .collection("players").document(uid)
                .update(
                    mapOf(
                        "role" to nextRole.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleGateStatus(
        roomId: String,
        isNorthGate: Boolean,
        currentStatus: GateStatus
    ): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            val nextStatus = when (currentStatus) {
                GateStatus.OPEN -> GateStatus.LOCKED
                GateStatus.ALERT -> GateStatus.LOCKED
                GateStatus.LOCKED -> GateStatus.OPEN
            }
            val gateName = if (isNorthGate) "Gerbang Utama Utara" else "Portal Servis Selatan"
            val gandi = GandiDispatcher.gateStatusChanged(gateName, nextStatus)
            val fieldKey = if (isNorthGate) "northGateStatus" else "southGateStatus"
            val nextRoomStatus = if (nextStatus == GateStatus.LOCKED) RoomStatus.LOCKDOWN.name else RoomStatus.ACTIVE.name

            db.collection("rooms").document(roomId)
                .update(
                    mapOf(
                        fieldKey to nextStatus.name,
                        "status" to nextRoomStatus,
                        "gandiLastLine" to gandi.line.take(300),
                        "gandiMood" to gandi.mood.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            postGandiDispatch(roomId, uid, gandi.line, if (isNorthGate) "GERBANG-U" else "GERBANG-S")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun dropPing(
        roomId: String,
        senderName: String,
        pingType: PingType,
        sector: EstateSector,
        posX: Float,
        posY: Float
    ): Result<Unit> {
        val path = "rooms/$roomId/pings"
        return try {
            val uid = requireUserId()
            val pingRef = db.collection("rooms").document(roomId).collection("pings").document()
            pingRef.set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.take(40),
                    "pingType" to pingType.name,
                    "sectorId" to sector.id,
                    "streetName" to sector.streetName.take(60),
                    "posX" to posX.coerceIn(0.05f, 0.95f).toDouble(),
                    "posY" to posY.coerceIn(0.05f, 0.95f).toDouble(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val gandi = GandiDispatcher.pingReact(senderName, pingType, sector)
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "gandiLastLine" to gandi.line.take(300),
                    "gandiMood" to gandi.mood.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        roomId: String,
        senderName: String,
        channel: CommsChannel,
        text: String,
        sectorTag: String = ""
    ): Result<Unit> {
        val cleanText = text.trim().take(300)
        if (cleanText.isEmpty()) return Result.success(Unit)
        val path = "rooms/$roomId/messages"
        return try {
            val uid = requireUserId()
            val msgRef = db.collection("rooms").document(roomId).collection("messages").document()
            msgRef.set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.take(40),
                    "channel" to channel.name,
                    "text" to cleanText,
                    "sectorTag" to sectorTag.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun postGandiDispatch(roomId: String, uid: String, line: String, sectorTag: String) {
        try {
            db.collection("rooms").document(roomId).collection("messages").document()
                .set(
                    mapOf(
                        "senderId" to uid,
                        "senderName" to "GANDI // OPERATOR",
                        "channel" to CommsChannel.GANDI.name,
                        "text" to line.take(300),
                        "sectorTag" to sectorTag.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "rooms/$roomId/messages")
        }
    }

    suspend fun apprehendThief(
        roomId: String,
        room: PatrolRoom,
        profile: UserProfile,
        bonusExtra: Long = 0L
    ): Result<Long> {
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            val sector = EstateWorld.byId(room.activeThiefSectorId)
            val gateBonus = if (room.northGateStatus == GateStatus.LOCKED.name || room.southGateStatus == GateStatus.LOCKED.name) 50L else 0L
            val uniformBonus = if (profile.equippedUniformId == "gandi_fanclub" || profile.equippedUniformId == "royal_sentinel") 25L else 0L
            val weaponBonus = WeaponCatalog.byId(profile.equippedWeaponId).bonusPoints
            val earned = 120L + gateBonus + uniformBonus + weaponBonus + bonusExtra.coerceIn(0L, 250L)

            // Respawn next intruder at a different sector
            val candidates = EstateWorld.sectors.filter { it.id != sector.id }
            val nextSector = candidates[Random.nextInt(candidates.size)]
            val gandi = GandiDispatcher.apprehensionCelebration(
                guardName = profile.displayName,
                sector = sector,
                pointsEarned = earned,
                uniformId = profile.equippedUniformId
            )

            // Update room state with new intruder sighting + score
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "scoreTotal" to FieldValue.increment(earned),
                    "apprehensionCount" to FieldValue.increment(1L),
                    "activeThiefSectorId" to nextSector.id,
                    "activeThiefStreet" to nextSector.streetName,
                    "activeThiefX" to nextSector.normX.toDouble(),
                    "activeThiefY" to nextSector.normY.toDouble(),
                    "thiefSpotted" to true,
                    "gandiLastLine" to gandi.line.take(300),
                    "gandiMood" to gandi.mood.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Increment room player score & persistent user career points
            db.collection("rooms").document(roomId).collection("players").document(uid)
                .update(
                    mapOf(
                        "roomScore" to FieldValue.increment(earned),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "points" to FieldValue.increment(earned),
                        "apprehensions" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            postGandiDispatch(roomId, uid, gandi.line, sector.shortCode)
            Result.success(earned)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceSimulatedThief(roomId: String, currentRoom: PatrolRoom): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            val currentSector = EstateWorld.byId(currentRoom.activeThiefSectorId)
            val availableNeighbors = currentSector.neighbors.filter { neighborId ->
                when (neighborId) {
                    "GERBANG_UTARA", "GATE_NORTH" -> currentRoom.northGateStatus != GateStatus.LOCKED.name
                    "GERBANG_SELATAN", "GATE_SOUTH" -> currentRoom.southGateStatus != GateStatus.LOCKED.name
                    else -> true
                }
            }
            val nextId = if (availableNeighbors.isNotEmpty()) {
                availableNeighbors[Random.nextInt(availableNeighbors.size)]
            } else {
                currentSector.id
            }
            val nextSector = EstateWorld.byId(nextId)
            val jitterX = (nextSector.normX + (Random.nextFloat() - 0.5f) * 0.05f).coerceIn(0.08f, 0.92f)
            val jitterY = (nextSector.normY + (Random.nextFloat() - 0.5f) * 0.04f).coerceIn(0.08f, 0.92f)
            val gandi = GandiDispatcher.thiefSpotted(nextSector)

            val updates = mutableMapOf<String, Any>(
                "activeThiefSectorId" to nextSector.id,
                "activeThiefStreet" to nextSector.streetName,
                "activeThiefX" to jitterX.toDouble(),
                "activeThiefY" to jitterY.toDouble(),
                "thiefSpotted" to true,
                "gandiLastLine" to gandi.line.take(300),
                "gandiMood" to gandi.mood.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (nextSector.id == "GERBANG_UTARA" && currentRoom.northGateStatus == GateStatus.OPEN.name) {
                updates["northGateStatus"] = GateStatus.ALERT.name
            } else if (nextSector.id == "GERBANG_SELATAN" && currentRoom.southGateStatus == GateStatus.OPEN.name) {
                updates["southGateStatus"] = GateStatus.ALERT.name
            }

            db.collection("rooms").document(roomId).update(updates).await()
            postGandiDispatch(roomId, uid, gandi.line, nextSector.shortCode)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncThiefEscape(
        roomId: String,
        sector: EstateSector,
        nx: Float,
        ny: Float
    ): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            requireUserId()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "activeThiefSectorId" to sector.id,
                    "activeThiefStreet" to sector.streetName,
                    "activeThiefX" to nx.coerceIn(0.05f, 0.95f).toDouble(),
                    "activeThiefY" to ny.coerceIn(0.05f, 0.95f).toDouble(),
                    "thiefSpotted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipUniform(uniformId: String, profile: UserProfile): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val item = UniformCatalog.byId(uniformId)
            if (uniformId in profile.unlockedUniformIds) {
                return equipUniform(uniformId)
            }
            if (profile.points < item.price) {
                return Result.failure(IllegalStateException("Butuh ${item.price - profile.points} Poin Patroli lagi."))
            }
            val updatedUnlocked = (profile.unlockedUniformIds + uniformId).distinct().take(20)
            db.collection("users").document(uid).update(
                mapOf(
                    "points" to (profile.points - item.price),
                    "unlockedUniformIds" to updatedUnlocked,
                    "equippedUniformId" to uniformId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun equipUniform(uniformId: String): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "equippedUniformId" to uniformId.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipWeapon(weaponId: String, profile: UserProfile): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val item = WeaponCatalog.byId(weaponId)
            if (weaponId in profile.unlockedWeaponIds) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "equippedWeaponId" to weaponId.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.success(Unit)
            }
            if (profile.points < item.price) {
                return Result.failure(IllegalStateException("Butuh ${item.price - profile.points} Poin Patroli lagi."))
            }
            val updatedUnlocked = (profile.unlockedWeaponIds + weaponId).distinct().take(20)
            db.collection("users").document(uid).update(
                mapOf(
                    "points" to (profile.points - item.price),
                    "unlockedWeaponIds" to updatedUnlocked,
                    "equippedWeaponId" to weaponId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipAttribute(attributeId: String, profile: UserProfile): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val item = AttributeCatalog.byId(attributeId)
            if (attributeId in profile.unlockedAttributeIds) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "equippedAttributeId" to attributeId.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.success(Unit)
            }
            if (profile.points < item.price) {
                return Result.failure(IllegalStateException("Butuh ${item.price - profile.points} Poin Patroli lagi."))
            }
            val updatedUnlocked = (profile.unlockedAttributeIds + attributeId).distinct().take(20)
            db.collection("users").document(uid).update(
                mapOf(
                    "points" to (profile.points - item.price),
                    "unlockedAttributeIds" to updatedUnlocked,
                    "equippedAttributeId" to attributeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncPlayerLoadoutInRoom(roomId: String, profile: UserProfile): Result<Unit> {
        var path = "rooms/$roomId/players"
        return try {
            val uid = requireUserId()
            path = "rooms/$roomId/players/$uid"
            db.collection("rooms").document(roomId)
                .collection("players").document(uid)
                .update(
                    mapOf(
                        "uniformId" to profile.equippedUniformId,
                        "weaponId" to WeaponCatalog.byId(profile.equippedWeaponId).id,
                        "attributeId" to profile.equippedAttributeId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleVoiceEnabled(enabled: Boolean): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "voiceEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
