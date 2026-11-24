# BPSGIM — Bumi Papan Selaras (Dokumen Desain & Arsitektur)

## 1. Premis & Alur State Permainan
**BPSGIM** adalah game aksi keamanan perumahan 3D/4D bergaya GTA berlatar kawasan luas **Bumi Papan Selaras**.
- **Alur State**: `Menu (Markas) -> Patroli / Misi (Timer 02:00) -> Pertarungan (Baku Tembak HP vs HP) -> Menang (+Poin) / Kalah (Wasted / Time Up) -> Toko 3D -> Misi Berikutnya`.
- **Menang (`VICTORY`)**: HP Pencuri habis (0) sebelum waktu habis -> mendapat hadiah Poin Patroli.
- **Kalah (`WASTED` / `TIME_UP`)**: HP Pemain habis (0) atau countdown timer mencapai `00:00`.

## 2. Peta Kawasan Bumi Papan Selaras
| ID | Nama Lokasi | Jalan | Jenis |
|---|---|---|---|
| `BLOK_KRISAN` | Blok Krisan | Jl. Krisan Raya | Klaster Hunian Barat Laut |
| `BLOK_CEMPAKA` | Blok Cempaka | Jl. Cempaka Asri | Klaster Hunian Timur Laut |
| `BLOK_CEMARA` | Blok Cemara | Jl. Cemara Indah | Kawasan Rindang Barat |
| `AREA_MASJID` | Area Masjid Agung Selaras | Bundaran Masjid Selaras | Landmark Ikonik Utama (Kubah & Menara) |
| `BLOK_AKASIA` | Blok Akasia | Jl. Akasia Timur | Klaster Hunian Timur |
| `BLOK_CAMELIA` | Blok Camelia | Jl. Camelia Barat | Klaster Hunian Barat Daya |
| `BLOK_FLAMBOYAN` | Blok Flamboyan | Jl. Flamboyan Raya | Klaster Hunian Tenggara |
| `GERBANG_UTARA` | Gerbang Utama Utara | Jl. Selaras Utara | Portal Pemeriksaan Utara |
| `GERBANG_SELATAN` | Gerbang Servis Selatan | Jl. Selaras Selatan | Portal Akses Selatan |

## 3. Antarmuka (UI / HUD) Ala GTA
- **Pojok Kiri Bawah**: Mini-Map melingkar bergaya GTA dengan arah hadap pemain, blok perumahan, Masjid Agung, dan penunjuk kompas ke posisi pencuri.
- **Pojok Kanan Atas**: Timer hitung mundur (`MM:SS`), Poin pemain, notifikasi masuk blok (`"MEMASUKI BLOK ..."`), tombol ganti senjata cepat, dan shortcut `TOKO`.
- **Pojok Kiri Atas & Tengah**: Bar HP Pemain, Level Armor + persentase reduksi damage, Stamina, dan Bar HP Pencuri.

## 4. Toko Baju Armor (Level 1–5) & Toko Senjata
- **Baju Armor**: Level 1 (10% reduksi) -> Level 2 (28%) -> Level 3 (45%) -> Level 4 (60%) -> Level 5 Titanium MAX (75% reduksi damage).
- **Senjata Inti**:
  1. `pistol_9mm` — Pistol Taktis 9mm (24 DMG, mobilitas tinggi)
  2. `granat_frag` — Granat Ledak MK-II (60 DMG, ledakan area)
  3. `ak47_assault` — AK-47 Otomatis (22 DMG, tembakan cepat)
  4. `bazoka_rpg` — Bazoka Peluncur Roket (95 DMG, daya ledak maksimal)

## 5. Arsitektur Teknis
- `GameModels.kt`: Model Firestore, `EstateWorld`, `UniformCatalog`, `WeaponCatalog`, `AttributeCatalog`, `MissionOutcome`.
- `World3DRenderer.kt`: Engine 3D/4D Compose Canvas (siklus waktu senja-malam-fajar, bayangan tajam real-time, refleksi genangan jalan & mobil patroli, landmark Masjid Agung, `Particle3D` debu/asap/percikan/ledakan).
- `Estate3DViewport.kt`: Loop gameplay 60 FPS, fisika tabrakan, pertarungan HP vs HP, timer misi, dan GTA HUD.
- `BlockWatchRepository.kt` & `BlockWatchViewModel.kt`: Sinkronisasi Cloud Firestore & Google Sign-In (`CredentialManager`).
