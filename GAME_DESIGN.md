# BPSGIM — Dokumen Desain Game & Arsitektur Teknis

## 1. Konsep & Alur Permainan Utama
**BPSGIM** adalah game patroli keamanan perumahan multiplayer real-time berlatar **Kompleks Emerald Crest**, sebuah kawasan hunian luas. Pemain bertugas sebagai petugas keamanan (atau bertukar peran) untuk berpatroli di jalanan kompleks, memantau blok hunian dan taman terbuka, berkoordinasi mencegat penyusup, mengunci gerbang perimeter, serta menangkap pencuri.

### Alur Inti
1. **Login & Persiapan**: Masuk dengan Google Sign-In lalu cek Poin Patroli, jumlah tangkapan, dan seragam aktif di Markas.
2. **Buka / Gabung Regu**: Buat Shift Patroli multiplayer baru (menghasilkan Kode Ruang 6 karakter) atau gabung ke ruang regu teman lewat kode.
3. **Patroli & Lacak**: Ketuk sektor atau koordinat di Peta Taktis 2D secara langsung untuk berpatroli sementara GANDI melaporkan lokasi penyusup dan status gerbang.
4. **Koordinasi & Kunci Jalur**: Pasang ping lokasi (`Ada Penyusup`, `Butuh Bantuan`, `Tutup Gerbang`, `Sektor Aman`), kirim perintah Radio Cepat 1-ketuk, obrolan teks real-time, atau aktifkan walkie-talkie Push-to-Talk (PTT). Kunci **Gerbang Utama Utara** atau **Portal Servis Selatan** untuk memutus rute kabur.
5. **Tangkap & Raih Poin**: Dekati sektor lokasi penyusup lalu tekan **TANGKAP** (+100 poin dasar, +50 bonus Gerbang Terkunci, +25 bonus Seragam Prestise).
6. **Progresi Gudang Seragam**: Belanjakan Poin Patroli di Gudang Seragam untuk membuka dan memakai 6 seragam berbeda yang mengubah warna token peta, bingkai aura, dan pangkat Anda.

---

## 2. Peta Kawasan & Sektor (9 Zona Terhubung)
| ID | Nama Sektor | Jalan | Jenis | Peran Taktis |
|---|---|---|---|---|
| `GATE_NORTH` | Gerbang Utama Utara | Jl. Oakridge Boulevard | Gerbang/Portal | Pos pemeriksaan kendaraan utama & palang pintu |
| `GATE_SOUTH` | Portal Servis Selatan | Jl. Senja Raya | Gerbang/Portal | Pintu putar pejalan kaki & portal akses logistik |
| `BLOCK_A` | Blok A: Menara Amberwood | Lorong Lentera | Kompleks | Menara hunian kembar 18 lantai dengan akses atap |
| `BLOCK_B` | Blok B: Vila Taman Cedar | Jl. Oakridge Boulevard | Kompleks | Kompleks vila taman berpagar di sisi timur laut |
| `BLOCK_C` | Blok C: Apartemen Horizon | Lorong Lentera | Kompleks | Apartemen halaman tengah dekat gang pengiriman |
| `BLOCK_D` | Blok D: Kondominium Maple | Jl. Bulan Sabit | Kompleks | Kondominium bertingkat di sepanjang tembok timur |
| `PARK_CENTRAL` | Taman Pusat Willow | Jl. Bulan Sabit | Taman Terbuka | Alun-alun taman luas, air mancur, dan lintasan lari |
| `PARK_WEST` | Taman Bermain Rembulan | Lorong Lentera | Taman Terbuka | Ruang terbuka barat dengan pencahayaan minim |
| `CROSSWALK` | Bundaran Penyeberangan Pusat | Jl. Simpang Tengah | Persimpangan | Simpang 4 arah utama penghubung seluruh sektor |

---

## 3. Karakter Pemandu: "GANDI"
- **Kepribadian**: Sangat cerewet, ekspresif, jenaka, panik sekaligus sigap memantau seluruh CCTV perumahan.
- **Status Suasana Hati**: `CEREWET`, `WASPADA`, `PANIK`, `BANGGA`, `CURIGA` (mengubah warna aksen HUD dan dibacakan via `TextToSpeech` berbahasa Indonesia).
- **Fungsi**:
  - **Laporan Jalan + Blok**: Menyebutkan nama jalan dan blok secara spesifik saat penyusup terdeteksi.
  - **Telemetri Gerbang**: Merayakan penguncian gerbang dan panik saat sensor gerbang terpicu.
  - **Komentar Regu & Seragam**: Merespons ping regu dan memberi pujian spesial untuk pemakai *Jaket Emas Favorit GANDI*.

---

## 4. Sistem Skor & Gudang Seragam
| ID Seragam | Nama | Harga | Pangkat & Tampilan Peta |
|---|---|---|---|
| `rookie_cadet` | Seragam Kadet Biru | 0 poin | Kadet — Token biru langit, bingkai putih |
| `neon_warden` | Penjaga Siber-Neon | 250 poin | Sipir — Token sian neon & visor pemindai |
| `crimson_marshal` | Marsekal Gerbang Merah | 500 poin | Marsekal — Jaket merah & bingkai emas |
| `night_owl` | Pengintai Burung Hantu | 800 poin | Pengintai — Optik malam zamrud |
| `gandi_fanclub` | Jaket Emas Favorit GANDI | 1.200 poin | Kepala Operator — Amber emas + pujian khusus GANDI |
| `royal_sentinel` | Panglima Agung Kompleks | 2.000 poin | Komandan Utama — Aura ungu-emas prestise |

---

## 5. Sistem Komunikasi Multiplayer
- **Walkie-Talkie Push-to-Talk (PTT)**: Menangkap amplitudo mikrofon (`AudioRecord` dengan izin runtime `RECORD_AUDIO` dan fallback aman) serta menyinkronkan `pttActive` ke Firestore agar rekan regu melihat cincin sinyal sian di peta.
- **Ping Lokasi**: Ketuk peta dalam Mode Ping untuk meletakkan suar `Ada Penyusup`, `Butuh Bantuan`, `Tutup Gerbang`, atau `Sektor Aman`.
- **Perintah Radio Cepat**: Tombol taktis 1-ketuk dengan tag sektor dan jalan otomatis.
- **Obrolan Teks Regu Real-Time**: Kanal komunikasi Firestore yang tersinkron langsung bersama siaran GANDI.

---

## 6. Arsitektur Teknis & Struktur Proyek
- **Klien**: Kotlin + Jetpack Compose (Material 3, `Canvas` peta taktis, `StateFlow` MVVM).
- **Autentikasi**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- **Backend Multiplayer (Cloud Firestore)**:
  - `/users/{userId}` — Profil pribadi, poin, jumlah tangkapan, dan daftar seragam.
  - `/rooms/{roomId}` — Kode ruang, status gerbang, koordinat/jalan penyusup, mood/kalimat GANDI, skor regu.
  - `/rooms/{roomId}/players/{playerUid}` — Posisi real-time, peran, seragam, status PTT.
  - `/rooms/{roomId}/pings/{pingId}` — Suar ping taktis di peta.
  - `/rooms/{roomId}/messages/{messageId}` — Obrolan regu, radio cepat, log suara, dan laporan GANDI.
