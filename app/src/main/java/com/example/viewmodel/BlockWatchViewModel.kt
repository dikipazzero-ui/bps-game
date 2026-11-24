package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlockWatchRepository
import com.example.engine.GandiVoiceSpeaker
import com.example.engine.PttAudioMonitor
import com.example.model.AttributeCatalog
import com.example.model.CommsChannel
import com.example.model.CommsMessage
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.GateStatus
import com.example.model.PatrolRoom
import com.example.model.PingType
import com.example.model.PlayerRole
import com.example.model.QuickRadioCommand
import com.example.model.RoomPlayer
import com.example.model.TacticalPing
import com.example.model.UserProfile
import com.example.model.WeaponCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppScreen {
    LOBBY, PATROL_ROOM, SHOP, FIELD_GUIDE
}

data class RoomBundle(
    val room: PatrolRoom?,
    val players: List<RoomPlayer>,
    val pings: List<TacticalPing>,
    val messages: List<CommsMessage>
)

@OptIn(ExperimentalCoroutinesApi::class)
class BlockWatchViewModel(
    private val repository: BlockWatchRepository,
    private val currentUserId: String,
    displayName: String?,
    private val voiceSpeaker: GandiVoiceSpeaker
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.LOBBY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _pttActive = MutableStateFlow(false)
    val pttActive: StateFlow<Boolean> = _pttActive.asStateFlow()

    val pttAudioMonitor = PttAudioMonitor(viewModelScope)
    val micLevel: StateFlow<Float> = pttAudioMonitor.level

    private var thiefLoopJob: Job? = null
    private var lastSpokenLine: String = ""

    private val _thiefStunnedUntilMs = MutableStateFlow(0L)
    val thiefStunnedUntilMs: StateFlow<Long> = _thiefStunnedUntilMs.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Loading
        }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreError(error, "Gagal memuat profil penjaga")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val myRoomsState: StateFlow<UiState<List<PatrolRoom>>> = repository.observeMyRooms(currentUserId)
        .map<List<PatrolRoom>, UiState<List<PatrolRoom>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreError(error, "Gagal memuat ruang patroli")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val roomBundleState: StateFlow<UiState<RoomBundle>> = _activeRoomId
        .flatMapLatest { roomId ->
            if (roomId == null) {
                flowOf<UiState<RoomBundle>>(
                    UiState.Success(RoomBundle(null, emptyList(), emptyList(), emptyList()))
                )
            } else {
                combine(
                    repository.observeRoom(roomId),
                    repository.observePlayers(roomId),
                    repository.observePings(roomId),
                    repository.observeMessages(roomId)
                ) { room, players, pings, messages ->
                    UiState.Success(RoomBundle(room, players, pings, messages)) as UiState<RoomBundle>
                }.catch { error ->
                    emit(UiState.Error(unwrapFirestoreError(error, "Gagal menyinkronkan ruang patroli")))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(displayName)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Gagal menyiapkan profil" }
        }

        // Speak new GANDI dispatches when voice is enabled and inside a room
        viewModelScope.launch {
            combine(roomBundleState, profileState) { roomUi, profileUi ->
                val room = (roomUi as? UiState.Success)?.data?.room
                val voiceOn = (profileUi as? UiState.Success)?.data?.voiceEnabled == true
                room to voiceOn
            }.collect { (room, voiceOn) ->
                if (room != null && room.gandiLastLine.isNotBlank() && room.gandiLastLine != lastSpokenLine) {
                    lastSpokenLine = room.gandiLastLine
                    if (voiceOn && _currentScreen.value == AppScreen.PATROL_ROOM) {
                        voiceSpeaker.speak(room.gandiLastLine, enabled = true)
                    }
                }
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("BlockWatchVM", fallback, rootError)
        return rootError.message ?: fallback
    }

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.PATROL_ROOM) {
            stopThiefPatrolLoop()
            voiceSpeaker.stop()
        }
        _currentScreen.value = screen
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun startNewPatrolShift(shiftTitle: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.createPatrolRoom(shiftTitle, profile)
                .onSuccess { roomId ->
                    openRoom(roomId, isHost = true)
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Gagal membuat ruang patroli"
                }
            _isBusy.value = false
        }
    }

    fun joinRoomByCode(code: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.joinRoomByCode(code, profile)
                .onSuccess { roomId ->
                    openRoom(roomId, isHost = false)
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Gagal bergabung ke regu"
                }
            _isBusy.value = false
        }
    }

    fun enterExistingRoom(room: PatrolRoom) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.joinRoomByCode(room.roomCode, profile)
                .onSuccess { roomId ->
                    openRoom(roomId, isHost = (room.hostUid == currentUserId))
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Gagal masuk kembali ke shift"
                }
            _isBusy.value = false
        }
    }

    private fun openRoom(roomId: String, isHost: Boolean) {
        _activeRoomId.value = roomId
        _currentScreen.value = AppScreen.PATROL_ROOM
        if (isHost) {
            startThiefPatrolLoop(roomId)
        }
    }

    fun exitRoomToLobby() {
        stopThiefPatrolLoop()
        if (_pttActive.value) {
            setPttTransmitting(active = false, hasMicPermission = false)
        }
        voiceSpeaker.stop()
        _activeRoomId.value = null
        _currentScreen.value = AppScreen.LOBBY
    }

    private fun startThiefPatrolLoop(roomId: String) {
        stopThiefPatrolLoop()
        thiefLoopJob = viewModelScope.launch {
            delay(5000L)
            while (isActive) {
                val bundle = (roomBundleState.value as? UiState.Success)?.data
                val room = bundle?.room
                val nowMs = System.currentTimeMillis()
                if (room != null && room.id == roomId && room.hostUid == currentUserId && nowMs >= _thiefStunnedUntilMs.value) {
                    repository.advanceSimulatedThief(roomId, room)
                }
                delay(9000L)
            }
        }
    }

    private fun stopThiefPatrolLoop() {
        thiefLoopJob?.cancel()
        thiefLoopJob = null
    }

    fun triggerManualRadarScan() {
        val roomId = _activeRoomId.value ?: return
        val room = (roomBundleState.value as? UiState.Success)?.data?.room ?: return
        viewModelScope.launch {
            repository.advanceSimulatedThief(roomId, room)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun moveToSector(
        sector: EstateSector,
        normX: Float = sector.normX,
        normY: Float = sector.normY,
        facingDeg: Float = 0f
    ) {
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            repository.movePlayer(roomId, sector, normX, normY, facingDeg)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun onWeaponHitThief() {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val weapon = WeaponCatalog.byId(profile.equippedWeaponId)
        val durationMs = (weapon.stunSeconds * 1000L).toLong()
        _thiefStunnedUntilMs.value = System.currentTimeMillis() + durationMs
        _statusBanner.value = "Kena! ${weapon.name} melumpuhkan penyusup (${weapon.stunSeconds.toInt()} dtk) — segera TANGKAP!"
    }

    fun toggleGate(isNorthGate: Boolean) {
        val roomId = _activeRoomId.value ?: return
        val room = (roomBundleState.value as? UiState.Success)?.data?.room ?: return
        val current = GateStatus.safeValueOf(
            if (isNorthGate) room.northGateStatus else room.southGateStatus
        )
        viewModelScope.launch {
            repository.toggleGateStatus(roomId, isNorthGate, current)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun attemptApprehend() {
        val roomId = _activeRoomId.value ?: return
        val bundle = (roomBundleState.value as? UiState.Success)?.data ?: return
        val room = bundle.room ?: return
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val me = bundle.players.firstOrNull { it.userId == currentUserId }
        val attr = AttributeCatalog.byId(profile.equippedAttributeId)
        val catchRange = 0.24f + attr.catchRadiusBonus

        if (me != null) {
            val dist = EstateWorld.distance(
                me.posX.toFloat(), me.posY.toFloat(),
                room.activeThiefX.toFloat(), room.activeThiefY.toFloat()
            )
            if (dist > catchRange && me.sectorId != room.activeThiefSectorId) {
                val thiefSector = EstateWorld.byId(room.activeThiefSectorId)
                _statusBanner.value = "Dekati ${thiefSector.name} (${thiefSector.streetName}) untuk menangkap!"
                return
            }
        }

        viewModelScope.launch {
            repository.apprehendThief(roomId, room, profile)
                .onSuccess { pts ->
                    _thiefStunnedUntilMs.value = 0L
                    _statusBanner.value = "Penyusup tertangkap! +$pts Poin Patroli"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Gagal menangkap penyusup"
                }
        }
    }

    fun dropTacticalPing(pingType: PingType, sector: EstateSector, normX: Float = sector.normX, normY: Float = sector.normY) {
        val roomId = _activeRoomId.value ?: return
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.dropPing(roomId, profile.displayName, pingType, sector, normX, normY)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun sendQuickRadio(command: QuickRadioCommand) {
        val roomId = _activeRoomId.value ?: return
        val bundle = (roomBundleState.value as? UiState.Success)?.data ?: return
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val me = bundle.players.firstOrNull { it.userId == currentUserId }
        val sector = EstateWorld.byId(me?.sectorId ?: bundle.room?.activeThiefSectorId ?: "CROSSWALK")

        val text = command.messageTemplate
            .replace("%SECTOR%", sector.name)
            .replace("%STREET%", sector.streetName)

        viewModelScope.launch {
            repository.sendMessage(roomId, profile.displayName, CommsChannel.RADIO, text, sector.shortCode)
            repository.dropPing(roomId, profile.displayName, command.pingType, sector, sector.normX, sector.normY)
        }
    }

    fun sendSquadChat(text: String) {
        val roomId = _activeRoomId.value ?: return
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val bundle = (roomBundleState.value as? UiState.Success)?.data
        val me = bundle?.players?.firstOrNull { it.userId == currentUserId }
        val sectorTag = me?.sectorId?.let { EstateWorld.byId(it).shortCode } ?: ""
        viewModelScope.launch {
            repository.sendMessage(roomId, profile.displayName, CommsChannel.SQUAD, text, sectorTag)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun setPttTransmitting(active: Boolean, hasMicPermission: Boolean) {
        val roomId = _activeRoomId.value ?: return
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        _pttActive.value = active
        if (active) {
            pttAudioMonitor.start(hasMicPermission)
        } else {
            pttAudioMonitor.stop()
        }
        viewModelScope.launch {
            repository.updatePttState(roomId, active)
            if (!active) {
                val bundle = (roomBundleState.value as? UiState.Success)?.data
                val me = bundle?.players?.firstOrNull { it.userId == currentUserId }
                val sector = EstateWorld.byId(me?.sectorId ?: "CROSSWALK")
                repository.sendMessage(
                    roomId = roomId,
                    senderName = profile.displayName,
                    channel = CommsChannel.VOICE_LOG,
                    text = "Transmisi radio suara dari ${sector.name} (${sector.streetName})",
                    sectorTag = sector.shortCode
                )
            }
        }
    }

    fun toggleRole() {
        val roomId = _activeRoomId.value ?: return
        val bundle = (roomBundleState.value as? UiState.Success)?.data ?: return
        val me = bundle.players.firstOrNull { it.userId == currentUserId } ?: return
        val role = PlayerRole.safeValueOf(me.role)
        viewModelScope.launch {
            repository.togglePlayerRole(roomId, role)
        }
    }

    fun purchaseOrEquipUniform(uniformId: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.purchaseAndEquipUniform(uniformId, profile)
                .onSuccess {
                    _statusBanner.value = "Baju seragam dipakai untuk patroli 3D!"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Pembelian gagal"
                }
            _isBusy.value = false
        }
    }

    fun purchaseOrEquipWeapon(weaponId: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.purchaseAndEquipWeapon(weaponId, profile)
                .onSuccess {
                    val weapon = WeaponCatalog.byId(weaponId)
                    _statusBanner.value = "${weapon.name} dipegang di tangan!"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Pembelian senjata gagal"
                }
            _isBusy.value = false
        }
    }

    fun purchaseOrEquipAttribute(attributeId: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.purchaseAndEquipAttribute(attributeId, profile)
                .onSuccess {
                    val attr = AttributeCatalog.byId(attributeId)
                    _statusBanner.value = "${attr.name} aktif memenuhi kebutuhan patroli!"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Pembelian atribut gagal"
                }
            _isBusy.value = false
        }
    }

    fun toggleVoiceSetting(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleVoiceEnabled(enabled)
            if (!enabled) voiceSpeaker.stop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopThiefPatrolLoop()
        pttAudioMonitor.stop()
        voiceSpeaker.shutdown()
    }
}
