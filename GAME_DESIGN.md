# BlockWatch — Game Design Document & Technical Blueprint

## 1. Concept & Core Gameplay Loop
**BlockWatch** is a real-time multiplayer tactical security game set inside **Emerald Crest Estates**, a sprawling residential community. Players deploy as security guards (or toggle to infiltrator roles) to patrol streets, monitor residential blocks and open parks, coordinate squad intercepts, lock perimeter gates, and apprehend lurking intruders.

### Core Loop
1. **Sign In & Loadout**: Authenticate with Google Sign-In and inspect career Patrol Points, arrest count, and equipped uniform in the Dispatch Lobby.
2. **Deploy / Join Squad**: Launch a new multiplayer Patrol Shift (generating a 6-character Room Code) or join an active squad room by code.
3. **Patrol & Track**: Tap sectors or coordinates on the live 2D Tactical Estate Map to patrol streets and complexes while GANDI broadcasts intruder sightings and gate alerts.
4. **Coordinate & Seal**: Drop pulsing tactical location pings (`SUSPECT`, `BACKUP`, `BLOCK_GATE`, `CLEAR`), fire 1-tap Quick Radio callouts, chat in real time, or key the Push-to-Talk (PTT) walkie-talkie. Lock **North Main Gate** or **South Service Portal** to cut off escape vectors.
5. **Apprehend & Earn**: Move within range of the spotted intruder and press **APPREHEND** (+100 base pts, +50 Gate Lockdown bonus, +25 Prestige Uniform bonus).
6. **Armory Progression**: Spend accumulated Patrol Points in the Guard Armory Shop to unlock and equip 6 distinct uniforms that customize your live map token, trim halo, and callsign rank.

---

## 2. Estate Map & Sectors (9 Connected Zones)
| ID | Sector Name | Street | Type | Tactical Role |
|---|---|---|---|---|
| `GATE_NORTH` | North Main Gate | Oakridge Boulevard | Gate/Portal | Primary vehicle checkpoint & sealable barrier |
| `GATE_SOUTH` | South Service Portal | Sundown Way | Gate/Portal | Pedestrian turnstile & service delivery exit |
| `BLOCK_A` | Block A: Amberwood Towers | Lantern Lane | Complex | Twin NW high-rise towers |
| `BLOCK_B` | Block B: Cedar Court Villas | Oakridge Boulevard | Complex | NE garden villas with perimeter hedges |
| `BLOCK_C` | Block C: Horizon Suites | Lantern Lane | Complex | SW mid-rise apartments near delivery alley |
| `BLOCK_D` | Block D: Maple Ridge Condos | Crescent Drive | Complex | SE terraced condos along eastern wall |
| `PARK_CENTRAL` | Willow Central Park | Crescent Drive | Open Park | Central plaza & jogging loop with wide sightlines |
| `PARK_WEST` | Moonlight Playground | Lantern Lane | Open Park | Low-light west pocket park |
| `CROSSWALK` | Central Crosswalk | Central Crosswalk | Intersection | Central 4-way hub connecting all sectors |

---

## 3. Guide Character: "GANDI"
- **Personality**: Hyper-talkative, expressive, caffeinated, humorous, and dramatic neighborhood watch dispatcher.
- **Mood States**: `CHATTY`, `ALERT`, `PANICKED`, `PROUD`, `SUSPICIOUS` (driving HUD accent colors and optional on-device `TextToSpeech` readout).
- **Capabilities**:
  - **Street + Block Thief Alerts**: Reports exact street names and block complexes (*"HOLD MY ESPRESSO! Somebody in suspicious sneakers just darted past Block B: Cedar Court Villas on Oakridge Boulevard!"*).
  - **Gate Telemetry**: Celebrates lockdowns (*"CLANG! North Main Gate is SEALED tight!"*) and panics on breaches.
  - **Squad & Uniform Commentary**: Echoes player pings and gives bonus compliments to guards wearing *GANDI's Golden Favorite*.

---

## 4. Scoring & Armory Uniform System
| Uniform ID | Name | Cost | Rank & Map Visual |
|---|---|---|---|
| `rookie_cadet` | Cadet Blue Patrol | 0 pts | Cadet — Sky-blue token, white trim |
| `neon_warden` | Cyber-Neon Warden | 250 pts | Warden — Electric cyan token & visor ring |
| `crimson_marshal` | Crimson Gate Marshal | 500 pts | Marshal — Crimson coat & gold trim |
| `night_owl` | Night-Owl Recon | 800 pts | Scout — Stealth emerald optics |
| `gandi_fanclub` | GANDI's Golden Favorite | 1,200 pts | Chief Dispatcher — Amber-gold + bonus GANDI praise |
| `royal_sentinel` | Estate Sovereign Sentinel | 2,000 pts | Grand Commander — Purple-and-gold prestige aura |

---

## 5. Multiplayer Communication Suite
- **Push-to-Talk (PTT) Walkie-Talkie**: Captures live mic amplitude (`AudioRecord` with runtime `RECORD_AUDIO` permission and graceful fallback) and syncs `pttActive` to Firestore so squadmates see a live pulsing cyan radio ring around your map token.
- **Location Pings**: Tap map in Ping Mode to place `SUSPECT`, `BACKUP`, `BLOCK_GATE`, or `CLEAR` beacons.
- **Quick Radio Commands**: 1-tap tactical callouts auto-tagged with the player's current sector and street.
- **Real-Time Squad Text Chat**: Instant Firestore-backed squad channel interleaved with GANDI dispatches.

---

## 6. Technical Architecture & Project Structure
- **Client**: Kotlin + Jetpack Compose (Material 3, `Canvas` tactical renderer, `StateFlow` MVVM).
- **Auth**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- **Multiplayer Backend (Cloud Firestore)**:
  - `/users/{userId}` — Owner-only profile, spendable points, arrests, unlocked & equipped uniforms.
  - `/rooms/{roomId}` — Room code, gate states, intruder coordinates/street, GANDI mood/line, squad score.
  - `/rooms/{roomId}/players/{playerUid}` — Real-time positions, role, uniform, PTT flag.
  - `/rooms/{roomId}/pings/{pingId}` — Immutable tactical map beacons.
  - `/rooms/{roomId}/messages/{messageId}` — Immutable chat, radio macros, voice logs, and GANDI broadcasts.
- **Key Packages**:
  - `com.example.auth` — Credential Manager flows & `AuthScreen`
  - `com.example.model` — Firestore models, `EstateWorld`, `UniformCatalog`, `QuickRadioPresets`
  - `com.example.engine` — `GandiDispatcher`, `GandiVoiceSpeaker` (TTS), `PttAudioMonitor` (`AudioRecord`)
  - `com.example.data` — `BlockWatchRepository`, `FirestoreErrorHandler`
  - `com.example.viewmodel` — `BlockWatchViewModel`
  - `com.example.ui` — `EstateMapCanvas`, `LobbyScreen`, `PatrolHudScreen`, `ShopScreen`, `GameDesignScreen`
