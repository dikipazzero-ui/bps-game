package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSyncThiefEscape: (EstateSector, Float, Float) -> Unit,
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
    var showSquadDrawer by remember { mutableStateOf(false) }

    BackHandler {
        if (showSquadDrawer) {
            showSquadDrawer = false
        } else {
            onExitRoom()
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040814))
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
                    return@Box
                }

                val me = bundle.players.firstOrNull { it.userId == currentUserId }
                val voiceOn = profile?.voiceEnabled == true

                // 1. Fullscreen Horizontal Battlefield (3D or 2D)
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
                        onSyncThiefEscape = onSyncThiefEscape,
                        onRadarPingTap = { sec, nx, ny ->
                            onDropPing(selectedPingType, sec, nx, ny)
                            isPingMode = false
                        },
                        onWeaponHitThief = onWeaponHitThief,
                        onApprehend = onApprehend,
                        onMissionVictory = onMissionVictory,
                        onSwitchWeapon = onSwitchWeapon,
                        onOpenShop = onOpenShop,
                        onToggleDrawer = { showSquadDrawer = !showSquadDrawer },
                        onExitRoom = onExitRoom,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 34.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { is3DView = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("KEMBALI KE 3D", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showSquadDrawer = !showSquadDrawer },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("REGU", fontSize = 11.sp, color = Color.White)
                            }
                            IconButton(onClick = onExitRoom) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Keluar", tint = Color.White)
                            }
                        }
                    }
                }

                // 2. Pemandu GANDI Cerewet — Teks berjalan transparan di BAWAH di antara analog dan tombol aksi
                BoxWithConstraints(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    val useWidePadding = maxWidth >= 460.dp
                    GandiTransparentMarquee(
                        room = room,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (useWidePadding) 122.dp else 16.dp,
                                end = if (useWidePadding) 164.dp else 16.dp
                            )
                    )
                }

                // 3. Collapsible Squad / Gate / Radio Side Drawer
                AnimatedVisibility(
                    visible = showSquadDrawer,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Surface(
                        color = Color(0xEB0B1222),
                        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                            .imePadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PatrolDrawerHeader(
                                room = room,
                                playerCount = bundle.players.size,
                                me = me,
                                voiceEnabled = voiceOn,
                                onClose = { showSquadDrawer = false },
                                onToggleRole = onToggleRole,
                                onToggleVoice = { onToggleVoice(!voiceOn) }
                            )

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
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GandiTransparentMarquee(
    room: PatrolRoom,
    modifier: Modifier = Modifier
) {
    val mood = GandiMood.safeValueOf(room.gandiMood)
    val accentColor by animateColorAsState(
        targetValue = when (mood) {
            GandiMood.PANICKED -> Color(0xFFFCA5A5)
            GandiMood.ALERT -> Color(0xFFFDE047)
            GandiMood.PROUD -> Color(0xFF6EE7B7)
            GandiMood.SUSPICIOUS -> Color(0xFFD8B4FE)
            GandiMood.CHATTY -> Color(0xFF67E8F9)
        },
        label = "gandiMarqueeColor"
    )
    val sector = EstateWorld.byId(room.activeThiefSectorId)
    val chatterExtra = when (mood) {
        GandiMood.PANICKED -> "Waduh komandan! Banyak maling mencar di gang sempit! Jangan sampai lolos ke kebun belakang ya, langsung tekan TANGKAP biar digelandang ke Penjara Selaras!"
        GandiMood.ALERT -> "Awas mata elang! Periksa sela-sela rumah Blok Krisan, Cempaka, Cemara, sampai Flamboyan. Kalau jauh naik mobil atau motor patroli aja biar ngebut!"
        GandiMood.PROUD -> "Mantap jiwa komandan! Satu lagi masuk sel Penjara Selaras, tapi awas masih ada komplotannya sembunyi di sudut komplek!"
        GandiMood.SUSPICIOUS -> "Hati-hati komandan, gerak-gerik mencurigakan terpantau dekat Masjid Agung dan gerbang komplek. Siapkan senjata atau borgolnya!"
        GandiMood.CHATTY -> "Laporan radio GANDI standby 24 jam siang-malam! Geser layar atas-bawah buat cek atap & gang, jangan lupa mampir Toko kalau poin udah tebel!"
    }
    val marqueeText = "GANDI [${mood.labelId}] • ${room.gandiLastLine} • ZONA: ${sector.shortCode} (${sector.streetName}) — $chatterExtra"

    Box(
        modifier = modifier
            .background(Color.Transparent)
            .testTag("gandi_dispatch_banner")
    ) {
        Text(
            text = marqueeText,
            color = accentColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                shadow = Shadow(
                    color = Color(0xE6000000),
                    offset = Offset(1.5f, 2f),
                    blurRadius = 4f
                )
            ),
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 54.dp
                )
        )
    }
}

@Composable
private fun PatrolDrawerHeader(
    room: PatrolRoom,
    playerCount: Int,
    me: RoomPlayer?,
    voiceEnabled: Boolean,
    onClose: () -> Unit,
    onToggleRole: () -> Unit,
    onToggleVoice: () -> Unit
) {
    val myRole = PlayerRole.safeValueOf(me?.role ?: PlayerRole.GUARD.name)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "$playerCount Aktif • Skor ${room.scoreTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                onClick = onToggleRole,
                color = if (myRole == PlayerRole.GUARD) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("toggle_role_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(myRole.labelId, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("toggle_voice_button")
            ) {
                Icon(
                    imageVector = if (voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (voiceEnabled) "Bisukan Suara GANDI" else "Aktifkan Suara GANDI",
                    tint = if (voiceEnabled) MaterialTheme.colorScheme.secondary else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup Panel Regu", tint = Color.White, modifier = Modifier.size(18.dp))
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
            leadingIcon = { Icon(Icons.Filled.ViewInAr, null, Modifier.size(14.dp)) },
            modifier = Modifier.testTag("toggle_3d_mode_chip")
        )
        FilterChip(
            selected = !isPingMode,
            onClick = onSelectMoveMode,
            label = { Text("Gerak", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Filled.MyLocation, null, Modifier.size(14.dp)) },
            modifier = Modifier.testTag("mode_move_chip")
        )
        PingType.entries.forEach { type ->
            FilterChip(
                selected = isPingMode && selectedPingType == type,
                onClick = { onSelectPingType(type) },
                label = { Text("Ping: ${type.label}", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Filled.Campaign, null, Modifier.size(14.dp)) },
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
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Button(
                onClick = onApprehend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(40.dp)
                    .testTag("apprehend_button")
            ) {
                Icon(Icons.Filled.GppGood, null, Modifier.size(15.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.btn_apprehend), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }

            OutlinedButton(
                onClick = onToggleNorthGate,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (northLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .height(40.dp)
                    .testTag("north_gate_button")
            ) {
                Icon(
                    if (northLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    null,
                    tint = if (northLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(if (northLocked) "U-KUNCI" else "GERBANG-U", style = MaterialTheme.typography.labelSmall, maxLines = 1, fontSize = 10.sp)
            }

            OutlinedButton(
                onClick = onToggleSouthGate,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (southLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .height(40.dp)
                    .testTag("south_gate_button")
            ) {
                Icon(
                    if (southLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    null,
                    tint = if (southLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(if (southLocked) "S-KUNCI" else "GERBANG-S", style = MaterialTheme.typography.labelSmall, maxLines = 1, fontSize = 10.sp)
            }

            IconButton(
                onClick = onRadarScan,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .testTag("radar_scan_button")
            ) {
                Icon(Icons.Filled.Radar, contentDescription = "Pindai Radar Kompleks", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            }
        }

        Surface(
            onClick = onPttToggle,
            color = if (pttActive) MaterialTheme.colorScheme.secondaryContainer else Color(0xFF141E33),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (pttActive) MaterialTheme.colorScheme.secondary else Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("ptt_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (pttActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = "Radio Suara Push-to-Talk",
                        tint = if (pttActive) MaterialTheme.colorScheme.secondary else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (pttActive) stringResource(R.string.btn_ptt_live) else stringResource(R.string.btn_ptt_hold),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pttActive) MaterialTheme.colorScheme.secondary else Color.White
                    )
                }
                if (pttActive) {
                    LinearProgressIndicator(
                        progress = { micLevel },
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = Color(0xFF090E1A),
                        modifier = Modifier
                            .width(72.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            QuickRadioPresets.commands.forEach { cmd ->
                Surface(
                    onClick = { onSendRadio(cmd) },
                    color = Color(0xFF141E33),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("radio_cmd_${cmd.id}")
                ) {
                    Text(
                        text = cmd.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

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
                        .background(Color(0xFF141E33), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = buildString {
                            append(msg.senderName)
                            if (msg.sectorTag.isNotBlank()) append(" [${msg.sectorTag}]")
                            append(": ")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tagColor,
                        fontSize = 10.sp
                    )
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            OutlinedTextField(
                value = chatDraft,
                onValueChange = { if (it.length <= 280) onChatDraftChange(it) },
                placeholder = { Text(stringResource(R.string.comms_placeholder), style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("squad_chat_input")
            )
            IconButton(
                onClick = onSendChat,
                enabled = chatDraft.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (chatDraft.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                        RoundedCornerShape(10.dp)
                    )
                    .testTag("squad_chat_send")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim pesan regu",
                    tint = if (chatDraft.isNotBlank()) MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
                )
            }
        }
    }
}
