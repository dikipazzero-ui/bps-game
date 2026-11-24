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
                            text = "BPSGIM // BUMI PAPAN SELARAS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Operasi Keamanan Bumi Papan Selaras",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Game aksi patroli keamanan 3D/4D bergaya GTA dengan HUD taktis, baku tembak real-time, Toko Armor Level 1–5 & Senjata, serta multiplayer Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.SportsEsports,
                    title = "1. Alur State & Pertarungan (Gaya GTA)",
                    bullets = listOf(
                        "Alur State: Menu Markas -> Patroli / Misi (Timer 02:00) -> Pertarungan (Baku Tembak HP vs HP) -> Menang (+Poin) / Kalah (Wasted / Time Up) -> Toko 3D -> Misi Berikutnya.",
                        "HUD Pojok Kiri Bawah: Mini-Map melingkar ala GTA dengan arah hadap pemain, blok perumahan, ikon Masjid Agung, dan penunjuk arah pencuri + Joystick Analog di atasnya.",
                        "HUD Pojok Kanan Atas: Timer hitung mundur misi, Poin pemain, notifikasi blok ('MEMASUKI BLOK CEMARA'), tombol ganti senjata cepat, dan pintasan TOKO.",
                        "HUD Kiri Atas & Tengah: Bar Nyawa (HP), Level Armor & reduksi damage, Stamina, serta Bar Nyawa Pencuri saat duel berlangsung.",
                        "Visual 4D Realistis: Transisi cahaya senja–malam–fajar, bayangan tajam dinamis, pantulan jalan basah & mobil patroli, debu langkah kaki, asap, percikan peluru, dan ledakan partikel."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Map,
                    title = "2. Peta Kawasan Bumi Papan Selaras (9 Zona)",
                    bullets = EstateWorld.sectors.map { s ->
                        "${s.shortCode} • ${s.name} (${s.streetName}): ${s.description}"
                    }
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Checkroom,
                    title = "3. Toko Baju Armor (Lv.1–5), Senjata & Atribut",
                    bullets = listOf(
                        "Baju Armor Premium (Level 1 hingga 5): Meredam damage tembakan pencuri mulai dari 10% (Level 1) hingga 75% (Level 5 Titanium MAX).",
                        "4 Senjata Inti: Pistol Taktis 9mm (24 DMG, lincah), Granat Ledak MK-II (60 DMG Area), AK-47 Otomatis (22 DMG tembakan cepat), dan Bazoka Peluncur Roket (95 DMG daya ledak maksimal).",
                        "Atribut Kebutuhan (5 pilihan): Sabuk Utilitas, Sepatu Turbo (+38% lari), Pelindung Stamina (-50% drain), Kacamata Malam NVG, dan Drone Pemindai Mini."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Campaign,
                    title = "4. Operator GANDI & Radio Regu",
                    bullets = listOf(
                        "GANDI memantau seluruh blok Bumi Papan Selaras dan menyiarkan lokasi pencuri beserta mood dinamis (CEREWET, WASPADA, PANIK, BANGGA, CURIGA).",
                        "Push-to-Talk (PTT), Ping Lokasi 3D, dan Perintah Radio Cepat 1-ketuk tersinkron secara real-time antaranggota regu."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Forum,
                    title = "5. Sistem Komunikasi & Kunci Gerbang",
                    bullets = listOf(
                        "Kunci Gerbang Utara atau Gerbang Selatan untuk memutus pelarian pencuri dan mendapatkan bonus +50 Poin Patroli.",
                        "Obrolan Regu Real-Time di panel kanan/bawah tetap aktif selama misi berlangsung."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Architecture,
                    title = "6. Arsitektur Teknis",
                    bullets = listOf(
                        "Autentikasi: Google Sign-In via Jetpack Credential Manager (GetSignInWithGoogleOption).",
                        "Backend: Cloud Firestore (users/{uid}, rooms/{roomId}, players/, pings/, messages/).",
                        "Mesin 3D/4D Klien: World3DRenderer & Estate3DViewport di Jetpack Compose Canvas (60 FPS physics, partikel, bayangan dinamis, dan state machine misi)."
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
