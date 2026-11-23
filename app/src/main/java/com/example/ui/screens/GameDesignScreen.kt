package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.model.EstateWorld

@Composable
fun GameDesignScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BLOCKWATCH // GAME DESIGN DOC",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Emerald Crest Security Operations",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Multiplayer residential patrol & tactical coordination built on Jetpack Compose + Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.SportsEsports,
                    title = "1. Core Gameplay Loop",
                    bullets = listOf(
                        "Deploy into an Estate Patrol Room (solo or squad via 6-char Room Code).",
                        "Patrol Blocks A–D, streets, and parks; monitor GANDI's live radar & street alerts.",
                        "Coordinate with squadmates via Map Pings, Quick Radio, PTT Voice, and Text Chat.",
                        "Seal North Main Gate or South Service Portal to cut off suspect escape routes.",
                        "Intercept and APPREHEND intruders to earn Patrol Points (+100 base, +50 Gate Seal bonus, +25 Prestige Uniform bonus).",
                        "Spend accumulated points in the Armory Shop to unlock & equip 6 guard uniforms."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Map,
                    title = "2. Residential Estate Map (9 Sectors)",
                    bullets = EstateWorld.sectors.map { s ->
                        "${s.shortCode} • ${s.name} (${s.streetName}): ${s.description}"
                    }
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Campaign,
                    title = "3. GANDI Dispatch Personality",
                    bullets = listOf(
                        "Persona: Hyper-talkative, caffeinated, expressive residential watch dispatcher.",
                        "Moods: CHATTY, ALERT, PANICKED, PROUD, SUSPICIOUS — dynamically reflected on the HUD.",
                        "Functions: Names exact street & block of intruder sightings, reacts to gate locks/breaches, celebrates takedowns, and echoes squad pings."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Forum,
                    title = "4. Multiplayer Communication Suite",
                    bullets = listOf(
                        "Push-to-Talk (PTT) Radio: Captures live mic amplitude (AudioRecord) and broadcasts an active cyan transmitting ring on your map token.",
                        "Quick Radio Commands: 1-tap tactical macros (Corner North Gate, Need Backup, Visual on Thief, Seal South Portal) with auto-sector tagging.",
                        "Location Pings: Tap map in Ping Mode to drop pulsing SUSPECT, BACKUP, BLOCK_GATE, or CLEAR beacons.",
                        "Real-Time Squad Chat: Instant Firestore subcollection feed synchronized with GANDI dispatches."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Checkroom,
                    title = "5. Scoring & Armory Progression",
                    bullets = listOf(
                        "Cadet Blue Patrol (0 pts) -> Cyber-Neon Warden (250 pts) -> Crimson Gate Marshal (500 pts).",
                        "Night-Owl Recon (800 pts) -> GANDI's Golden Favorite (1,200 pts) -> Estate Sovereign Sentinel (2,000 pts).",
                        "Equipped uniforms change your live multiplayer map token color, trim halo, and rank callsign."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Architecture,
                    title = "6. Technical Architecture",
                    bullets = listOf(
                        "Auth: Google Sign-In via Jetpack Credential Manager (GetSignInWithGoogleOption).",
                        "Backend: Cloud Firestore Enterprise (users/{uid}, rooms/{roomId}, plus players/, pings/, messages/ subcollections).",
                        "Security: Zero-trust schema validation, member-gated subcollection reads, immutable chat/ping logs, and action-scoped room updates.",
                        "Client: MVVM with StateFlow, Jetpack Compose Canvas tactical renderer, and Android TextToSpeech for GANDI voice."
                    )
                )
            }
        }
    }
}

@Composable
private fun GddSectionCard(
    icon: ImageVector,
    title: String,
    bullets: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}
