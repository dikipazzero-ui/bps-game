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
                            text = "BPSGIM // DOKUMEN DESAIN GAME",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Operasi Keamanan Emerald Crest",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Patroli perumahan multiplayer & koordinasi taktis berbasis Jetpack Compose + Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.SportsEsports,
                    title = "1. Alur Permainan Utama",
                    bullets = listOf(
                        "Masuk ke Ruang Patroli Kompleks (sendiri atau regu via Kode Ruang 6 karakter).",
                        "Patroli Blok A–D, jalanan kompleks, dan taman; pantau radar & laporan jalanan dari GANDI.",
                        "Koordinasi dengan rekan regu via Ping Peta, Radio Cepat, Suara PTT, dan Obrolan Teks.",
                        "Kunci Gerbang Utama Utara atau Portal Servis Selatan untuk memotong rute kabur penyusup.",
                        "Kejar dan TANGKAP penyusup untuk meraih Poin Patroli (+100 dasar, +50 bonus Gerbang Terkunci, +25 bonus Seragam Prestise).",
                        "Belanjakan poin di Gudang Seragam untuk membuka & memakai 6 seragam penjaga."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Map,
                    title = "2. Peta Kawasan Perumahan (9 Sektor)",
                    bullets = EstateWorld.sectors.map { s ->
                        "${s.shortCode} • ${s.name} (${s.streetName}): ${s.description}"
                    }
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Campaign,
                    title = "3. Karakter Pemandu GANDI",
                    bullets = listOf(
                        "Kepribadian: Cerewet, penuh semangat, ekspresif, dan proaktif memantau CCTV kompleks.",
                        "Suasana Hati: CEREWET, WASPADA, PANIK, BANGGA, CURIGA — tampil dinamis di layar patroli.",
                        "Fungsi: Melaporkan nama jalan & blok lokasi penyusup, status gerbang, memuji penangkapan, dan merespons ping regu."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Forum,
                    title = "4. Sistem Komunikasi Multiplayer",
                    bullets = listOf(
                        "Radio Push-to-Talk (PTT): Menangkap amplitudo mikrofon (AudioRecord) dan menyalakan cincin sinyal sian di peta.",
                        "Perintah Radio Cepat: Tombol 1-ketuk (Kepung Gerbang U, Butuh Bantuan, Maling Terlihat, Kunci Portal Selatan) dengan tag sektor otomatis.",
                        "Ping Lokasi: Ketuk peta dalam Mode Ping untuk memasang suar PENYUSUP, BUTUH BANTUAN, TUTUP GERBANG, atau AMAN.",
                        "Obrolan Regu Real-Time: Aliran pesan Firestore sinkron langsung dengan laporan GANDI."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Checkroom,
                    title = "5. Skor & Koleksi Seragam",
                    bullets = listOf(
                        "Seragam Kadet Biru (0 poin) -> Penjaga Siber-Neon (250 poin) -> Marsekal Gerbang Merah (500 poin).",
                        "Pengintai Burung Hantu (800 poin) -> Jaket Emas Favorit GANDI (1.200 poin) -> Panglima Agung Kompleks (2.000 poin).",
                        "Seragam yang dipakai langsung mengubah warna ikon, bingkai aura, dan pangkat Anda di peta multiplayer."
                    )
                )
            }

            item {
                GddSectionCard(
                    icon = Icons.Filled.Architecture,
                    title = "6. Arsitektur Teknis",
                    bullets = listOf(
                        "Autentikasi: Google Sign-In via Jetpack Credential Manager (GetSignInWithGoogleOption).",
                        "Backend: Cloud Firestore Enterprise (koleksi users/{uid}, rooms/{roomId}, serta subkoleksi players/, pings/, messages/).",
                        "Keamanan: Validasi skema zero-trust, akses subkoleksi khusus anggota regu, log obrolan/ping permanen.",
                        "Klien: MVVM dengan StateFlow, peta taktis Jetpack Compose Canvas, dan suara TextToSpeech GANDI."
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
