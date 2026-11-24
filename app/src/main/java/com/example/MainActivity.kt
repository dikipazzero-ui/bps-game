package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.data.BlockWatchRepository
import com.example.engine.GandiVoiceSpeaker
import com.example.ui.screens.GameDesignScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.PatrolHudScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.BlockWatchTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BlockWatchViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockWatchTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            BlockWatchRoot(
                currentUserId = user.uid,
                displayName = user.displayName
            )
        }
    }
}

@Composable
fun BlockWatchRoot(
    currentUserId: String,
    displayName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: BlockWatchViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BlockWatchViewModel(
                    repository = BlockWatchRepository(db),
                    currentUserId = currentUserId,
                    displayName = displayName,
                    voiceSpeaker = GandiVoiceSpeaker(app)
                )
            }
        }
    )

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val roomsState by viewModel.myRoomsState.collectAsStateWithLifecycle()
    val roomBundleState by viewModel.roomBundleState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val pttActive by viewModel.pttActive.collectAsStateWithLifecycle()
    val micLevel by viewModel.micLevel.collectAsStateWithLifecycle()
    val thiefStunnedUntilMs by viewModel.thiefStunnedUntilMs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (currentScreen == AppScreen.PATROL_ROOM) {
        Box(modifier = Modifier.fillMaxSize()) {
            PatrolHudScreen(
                roomBundleState = roomBundleState,
                profile = (profileState as? UiState.Success)?.data,
                currentUserId = currentUserId,
                pttActive = pttActive,
                micLevel = micLevel,
                thiefStunnedUntilMs = thiefStunnedUntilMs,
                onExitRoom = { viewModel.exitRoomToLobby() },
                onMoveToSector = { sector, nx, ny, facing -> viewModel.moveToSector(sector, nx, ny, facing) },
                onDropPing = { type, sector, nx, ny -> viewModel.dropTacticalPing(type, sector, nx, ny) },
                onWeaponHitThief = { viewModel.onWeaponHitThief() },
                onToggleGate = { isNorth -> viewModel.toggleGate(isNorth) },
                onApprehend = { viewModel.attemptApprehend() },
                onRadarScan = { viewModel.triggerManualRadarScan() },
                onToggleRole = { viewModel.toggleRole() },
                onSendRadio = { cmd -> viewModel.sendQuickRadio(cmd) },
                onSendChat = { msg -> viewModel.sendSquadChat(msg) },
                onSetPtt = { active, hasPerm -> viewModel.setPttTransmitting(active, hasPerm) },
                onToggleVoice = { enabled -> viewModel.toggleVoiceSetting(enabled) },
                onMissionVictory = { bonus, wave -> viewModel.onMissionVictory(bonus, wave) },
                onSwitchWeapon = { wpnId -> viewModel.quickSwitchWeapon(wpnId) },
                onOpenShop = { viewModel.openShopFromRoom() }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.LOBBY,
                            onClick = { viewModel.navigateTo(AppScreen.LOBBY) },
                            icon = {
                                Icon(
                                    if (currentScreen == AppScreen.LOBBY) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    contentDescription = stringResource(R.string.tab_lobby)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_lobby)) },
                            modifier = Modifier.testTag("nav_lobby")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SHOP,
                            onClick = { viewModel.navigateTo(AppScreen.SHOP) },
                            icon = {
                                Icon(
                                    if (currentScreen == AppScreen.SHOP) Icons.Filled.Checkroom else Icons.Outlined.Checkroom,
                                    contentDescription = stringResource(R.string.tab_shop)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_shop)) },
                            modifier = Modifier.testTag("nav_shop")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.FIELD_GUIDE,
                            onClick = { viewModel.navigateTo(AppScreen.FIELD_GUIDE) },
                            icon = {
                                Icon(
                                    if (currentScreen == AppScreen.FIELD_GUIDE) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                    contentDescription = stringResource(R.string.tab_gdd)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_gdd)) },
                            modifier = Modifier.testTag("nav_gdd")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = currentScreen == AppScreen.LOBBY,
                            onClick = { viewModel.navigateTo(AppScreen.LOBBY) },
                            icon = {
                                Icon(
                                    if (currentScreen == AppScreen.LOBBY) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    contentDescription = stringResource(R.string.tab_lobby)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_lobby)) }
                        )
                        NavigationRailItem(
                            selected = currentScreen == AppScreen.SHOP,
                            onClick = { viewModel.navigateTo(AppScreen.SHOP) },
                            icon = {
                                Icon(
                                    if (currentScreen == AppScreen.SHOP) Icons.Filled.Checkroom else Icons.Outlined.Checkroom,
                                    contentDescription = stringResource(R.string.tab_shop)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_shop)) }
                        )
                        NavigationRailItem(
                            selected = currentScreen == AppScreen.FIELD_GUIDE,
                            onClick = { viewModel.navigateTo(AppScreen.FIELD_GUIDE) },
                            icon = {
                                Icon(
                                    if (currentScreen == AppScreen.FIELD_GUIDE) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                    contentDescription = stringResource(R.string.tab_gdd)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_gdd)) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        AppScreen.LOBBY -> LobbyScreen(
                            profileState = profileState,
                            roomsState = roomsState,
                            isBusy = isBusy,
                            onStartShift = { title -> viewModel.startNewPatrolShift(title) },
                            onJoinByCode = { code -> viewModel.joinRoomByCode(code) },
                            onEnterRoom = { room -> viewModel.enterExistingRoom(room) },
                            onOpenShop = { viewModel.navigateTo(AppScreen.SHOP) },
                            onOpenFieldGuide = { viewModel.navigateTo(AppScreen.FIELD_GUIDE) },
                            onSignOut = {
                                signOut(credentialManager, scope)
                            }
                        )
                        AppScreen.SHOP -> ShopScreen(
                            profileState = profileState,
                            isBusy = isBusy,
                            onPurchaseOrEquipUniform = { uniformId -> viewModel.purchaseOrEquipUniform(uniformId) },
                            onPurchaseOrEquipWeapon = { weaponId -> viewModel.purchaseOrEquipWeapon(weaponId) },
                            onPurchaseOrEquipAttribute = { attrId -> viewModel.purchaseOrEquipAttribute(attrId) },
                            onBack = { viewModel.closeShop() }
                        )
                        AppScreen.FIELD_GUIDE -> GameDesignScreen(
                            onBack = { viewModel.navigateTo(AppScreen.LOBBY) }
                        )
                        AppScreen.PATROL_ROOM -> Unit
                    }
                }
            }
        }
    }
}
