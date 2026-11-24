package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.CommsChannel
import com.example.model.CommsMessage
import com.example.model.EstateSector
import com.example.model.EstateWorld
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PatrolRoom
import com.example.model.PingType
import com.example.model.PlayerRole
import com.example.model.QuickRadioCommand
import com.example.model.QuickRadioPresets
import com.example.model.RoomPlayer
import com.example.model.UserProfile
import androidx.compose.material.icons.filled.ViewInAr
import com.example.ui.components.Estate3DViewport
import com.example.ui.components.EstateMapCanvas
import com.example.viewmodel.RoomBundle
import com.example.viewmodel.UiState

@Composable
fun PatrolHudScreen(
    roomBundleState: UiState<RoomBundle>,
    profile: UserProfile?,
    currentUserId: String,
    pttActive: Boolean,
    micLevel: Float,
    thiefStunnedUntilMs: Long,
    onExitRoom: () -> Unit,
    onMoveToSector: (EstateSector, Float, Float, Float) -> Unit,
    onDropPing: (PingType, EstateSector, Float, Float) -> Unit,
    onWeaponHitThief: () -> Unit,
    onToggleGate: (Boolean) -> Unit,
    onApprehend: () -> Unit,
    onRadarScan: () -> Unit,
    onToggleRole: () -> Unit,
    onSendRadio: (QuickRadioCommand) -> Unit,
    onSendChat: (String) -> Unit,
    onSetPtt: (Boolean, Boolean) -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onMissionVictory: (Long, Int) -> Unit,
    onSwitchWeapon: (String) -> Unit,
    onOpenShop: () -> Unit
) {
    BackHandler { onExitRoom() }

    val context = LocalContext.current
    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPerm = granted
        onSetPtt(true, granted)
    }

    var isPingMode by remember { mutableStateOf(false) }
    var is3DView by remember { mutableStateOf(true) }
    var selectedPingType by remember { mutableStateOf(PingType.SUSPECT) }
    var chatDraft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        when (roomBundleState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(roomBundleState.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onExitRoom) { Text("Kembali ke Markas") }
                }
            }
            is UiState.Success -> {
                val bundle = roomBundleState.data
                val room = bundle.room
                if (room == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@Column
                }

                val me = bundle.players.firstOrNull { it.userId == currentUserId }
                val voiceOn = profile?.voiceEnabled == true

                // 1. Top Tactical Bar
                PatrolTopBar(
                    room = room,
                    playerCount = bundle.players.size,
                    me = me,
                    voiceEnabled = voiceOn,
                    onExitRoom = onExitRoom,
                    onToggleRole = onToggleRole,
                    onToggleVoice = { onToggleVoice(!voiceOn) }
                )

                // 2. GANDI Live Dispatch Banner
                GandiDispatchTicker(room = room)

                // 3. Adaptive Layout: Side-by-side on Tablets, Stacked on Phones
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val isWide = maxWidth >= 720.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MapModeControls(
                                    is3DView = is3DView,
                                    isPingMode = isPingMode,
                                    selectedPingType = selectedPingType,
                                    onToggle3D = { is3DView = !is3DView },
                                    onSelectMoveMode = { isPingMode = false },
                                    onSelectPingType = {
                                        isPingMode = true
                                        selectedPingType = it
                                    }
                                )
                                if (is3DView) {
                                    Estate3DViewport(
                                        room = room,
                                        players = bundle.players,
                                        pings = bundle.pings,
                                        profile = profile,
                                        currentUserId = currentUserId,
                                        isPingMode = isPingMode,
                                        thiefStunnedUntilMs = thiefStunnedUntilMs,
                                        onMoveSync = onMoveToSector,
                                        onRadarPingTap = { sec, nx, ny ->
                                            onDropPing(selectedPingType, sec, nx, ny)
                                            isPingMode = false
                                        },
                                        onWeaponHitThief = onWeaponHitThief,
                                        onApprehend = onApprehend,
                                        onMissionVictory = onMissionVictory,
                                        onSwitchWeapon = onSwitchWeapon,
                                        onOpenShop = onOpenShop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    )
                                } else {
                                    EstateMapCanvas(
                                        room = room,
                                        players = bundle.players,
                                        pings = bundle.pings,
                                        currentUserId = currentUserId,
                                        isPingMode = isPingMode,
                                        onMapTap = { sector, nx, ny ->
                                            if (isPingMode) {
                                                onDropPing(selectedPingType, sector, nx, ny)
                                                isPingMode = false
                                            } else {
                                                onMoveToSector(sector, nx, ny, 0f)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    )
                                }
                                ActionDeck(
                                    room = room,
                                    pttActive = pttActive,
                                    micLevel = micLevel,
                                    onApprehend = onApprehend,
                                    onToggleNorthGate = { onToggleGate(true) },
                                    onToggleSouthGate = { onToggleGate(false) },
                                    onRadarScan = onRadarScan,
                                    onPttToggle = {
                                        if (pttActive) {
                                            onSetPtt(false, hasMicPerm)
                                        } else if (hasMicPerm) {
                                            onSetPtt(true, true)
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                )
                            }

                            CommsPanel(
                                messages = bundle.messages,
                                chatDraft = chatDraft,
                                onChatDraftChange = { chatDraft = it },
                                onSendChat = {
                                    onSendChat(chatDraft)
                                    chatDraft = ""
                                },
                                onSendRadio = onSendRadio,
                                modifier = Modifier
                                    .weight(0.85f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MapModeControls(
                                is3DView = is3DView,
                                isPingMode = isPingMode,
                                selectedPingType = selectedPingType,
                                onToggle3D = { is3DView = !is3DView },
                                onSelectMoveMode = { isPingMode = false },
                                onSelectPingType = {
                                    isPingMode = true
                                    selectedPingType = it
                                }
                            )
                            if (is3DView) {
                                Estate3DViewport(
                                    room = room,
                                    players = bundle.players,
                                    pings = bundle.pings,
                                    profile = profile,
                                    currentUserId = currentUserId,
                                    isPingMode = isPingMode,
                                    thiefStunnedUntilMs = thiefStunnedUntilMs,
                                    onMoveSync = onMoveToSector,
                                    onRadarPingTap = { sec, nx, ny ->
                                        onDropPing(selectedPingType, sec, nx, ny)
                                        isPingMode = false
                                    },
                                    onWeaponHitThief = onWeaponHitThief,
                                    onApprehend = onApprehend,
                                    onMissionVictory = onMissionVictory,
                                    onSwitchWeapon = onSwitchWeapon,
                                    onOpenShop = onOpenShop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.56f)
                                )
                            } else {
                                EstateMapCanvas(
                                    room = room,
                                    players = bundle.players,
                                    pings = bundle.pings,
                                    currentUserId = currentUserId,
                                    isPingMode = isPingMode,
                                    onMapTap = { sector, nx, ny ->
                                        if (isPingMode) {
                                            onDropPing(selectedPingType, sector, nx, ny)
                                            isPingMode = false
                                        } else {
                                            onMoveToSector(sector, nx, ny, 0f)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.56f)
                                )
                            }
                            ActionDeck(
                                room = room,
                                pttActive = pttActive,
                                micLevel = micLevel,
                                onApprehend = onApprehend,
                                onToggleNorthGate = { onToggleGate(true) },
                                onToggleSouthGate = { onToggleGate(false) },
                                onRadarScan = onRadarScan,
                                onPttToggle = {
                                    if (pttActive) {
                                        onSetPtt(false, hasMicPerm)
                                    } else if (hasMicPerm) {
                                        onSetPtt(true, true)
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            )
                            CommsPanel(
                                messages = bundle.messages,
                                chatDraft = chatDraft,
                                onChatDraftChange = { chatDraft = it },
                                onSendChat = {
                                    onSendChat(chatDraft)
                                    chatDraft = ""
                                },
                                onSendRadio = onSendRadio,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.44f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatrolTopBar(
    room: PatrolRoom,
    playerCount: Int,
    me: RoomPlayer?,
    voiceEnabled: Boolean,
    onExitRoom: () -> Unit,
    onToggleRole: () -> Unit,
    onToggleVoice: () -> Unit
) {
    val myRole = PlayerRole.safeValueOf(me?.role ?: PlayerRole.GUARD.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onExitRoom,
                modifier = Modifier.testTag("exit_room_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Markas")
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$playerCount Aktif",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Skor Regu: ${room.scoreTotal} • Tangkapan: ${room.apprehensionCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onToggleRole,
                color = if (myRole == PlayerRole.GUARD) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("toggle_role_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(myRole.labelId, style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier.testTag("toggle_voice_button")
            ) {
                Icon(
                    imageVector = if (voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (voiceEnabled) "Bisukan Suara GANDI" else "Aktifkan Suara GANDI",
                    tint = if (voiceEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GandiDispatchTicker(room: PatrolRoom) {
    val mood = GandiMood.safeValueOf(room.gandiMood)
    val accentColor by animateColorAsState(
        targetValue = when (mood) {
            GandiMood.PANICKED -> MaterialTheme.colorScheme.error
            GandiMood.ALERT -> MaterialTheme.colorScheme.primary
            GandiMood.PROUD -> MaterialTheme.colorScheme.tertiary
            GandiMood.SUSPICIOUS -> Color(0xFFA855F7)
            GandiMood.CHATTY -> MaterialTheme.colorScheme.secondary
        },
        label = "gandiMoodColor"
    )
    val sector = EstateWorld.byId(room.activeThiefSectorId)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("gandi_dispatch_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_gandi_avatar),
                contentDescription = "Operator GANDI",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, accentColor, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GANDI // ${mood.labelId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    Text(
                        text = "TARGET: ${sector.shortCode} • ${sector.streetName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = room.gandiLastLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MapModeControls(
    is3DView: Boolean,
    isPingMode: Boolean,
    selectedPingType: PingType,
    onToggle3D: () -> Unit,
    onSelectMoveMode: () -> Unit,
    onSelectPingType: (PingType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = is3DView,
            onClick = onToggle3D,
            label = { Text(if (is3DView) "Mode 3D" else "Peta 2D", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Filled.ViewInAr, null, Modifier.size(15.dp)) },
            modifier = Modifier.testTag("toggle_3d_mode_chip")
        )
        FilterChip(
            selected = !isPingMode,
            onClick = onSelectMoveMode,
            label = { Text("Gerak Patroli", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Filled.MyLocation, null, Modifier.size(15.dp)) },
            modifier = Modifier.testTag("mode_move_chip")
        )
        PingType.entries.forEach { type ->
            FilterChip(
                selected = isPingMode && selectedPingType == type,
                onClick = { onSelectPingType(type) },
                label = { Text("Ping: ${type.label}", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Filled.Campaign, null, Modifier.size(15.dp)) },
                modifier = Modifier.testTag("ping_type_${type.name}")
            )
        }
    }
}

@Composable
private fun ActionDeck(
    room: PatrolRoom,
    pttActive: Boolean,
    micLevel: Float,
    onApprehend: () -> Unit,
    onToggleNorthGate: () -> Unit,
    onToggleSouthGate: () -> Unit,
    onRadarScan: () -> Unit,
    onPttToggle: () -> Unit
) {
    val northLocked = room.northGateStatus == GateStatus.LOCKED.name
    val southLocked = room.southGateStatus == GateStatus.LOCKED.name

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onApprehend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("apprehend_button")
            ) {
                Icon(Icons.Filled.GppGood, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.btn_apprehend), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }

            OutlinedButton(
                onClick = onToggleNorthGate,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (northLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("north_gate_button")
            ) {
                Icon(
                    if (northLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    null,
                    tint = if (northLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (northLocked) "U-KUNCI" else "GERBANG-U",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = onToggleSouthGate,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (southLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("south_gate_button")
            ) {
                Icon(
                    if (southLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    null,
                    tint = if (southLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (southLocked) "S-KUNCI" else "GERBANG-S",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onRadarScan,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .testTag("radar_scan_button")
            ) {
                Icon(Icons.Filled.Radar, contentDescription = "Pindai Radar Kompleks", tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Baris Walkie-Talkie Push-to-Talk
        Surface(
            onClick = onPttToggle,
            color = if (pttActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (pttActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ptt_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (pttActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = "Radio Suara Push-to-Talk",
                        tint = if (pttActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (pttActive) stringResource(R.string.btn_ptt_live) else stringResource(R.string.btn_ptt_hold),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pttActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (pttActive) {
                    LinearProgressIndicator(
                        progress = { micLevel },
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .width(96.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun CommsPanel(
    messages: List<CommsMessage>,
    chatDraft: String,
    onChatDraftChange: (String) -> Unit,
    onSendChat: () -> Unit,
    onSendRadio: (QuickRadioCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Quick Radio Callouts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickRadioPresets.commands.forEach { cmd ->
                    Surface(
                        onClick = { onSendRadio(cmd) },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("radio_cmd_${cmd.id}")
                    ) {
                        Text(
                            text = cmd.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Real-Time Squad & GANDI Log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id.ifEmpty { "${it.senderId}_${it.text.hashCode()}" } }) { msg ->
                    val channel = CommsChannel.safeValueOf(msg.channel)
                    val tagColor = when (channel) {
                        CommsChannel.GANDI -> MaterialTheme.colorScheme.secondary
                        CommsChannel.RADIO -> MaterialTheme.colorScheme.primary
                        CommsChannel.VOICE_LOG -> MaterialTheme.colorScheme.tertiary
                        CommsChannel.SQUAD -> Color(0xFFE2E8F0)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = buildString {
                                append(msg.senderName)
                                if (msg.sectorTag.isNotBlank()) append(" [${msg.sectorTag}]")
                                append(": ")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tagColor
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Text Chat Composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = chatDraft,
                    onValueChange = { if (it.length <= 280) onChatDraftChange(it) },
                    placeholder = { Text(stringResource(R.string.comms_placeholder), style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("squad_chat_input")
                )
                IconButton(
                    onClick = onSendChat,
                    enabled = chatDraft.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (chatDraft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("squad_chat_send")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim pesan regu",
                        tint = if (chatDraft.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
