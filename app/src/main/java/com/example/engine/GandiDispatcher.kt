package com.example.engine

import com.example.model.EstateSector
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PingType

data class GandiBroadcast(
    val line: String,
    val mood: GandiMood
)

/**
 * Generates expressive, chatty, humorous, and situationally aware dispatches
 * from GANDI, Emerald Crest's hyper-talkative security AI dispatcher.
 */
object GandiDispatcher {

    private var lineCounter = 0

    fun thiefSpotted(sector: EstateSector): GandiBroadcast {
        lineCounter++
        val templates = listOf(
            "PEGANG KOPI SAYA! Ada orang bersepatu mencurigakan lari melewati ${sector.name} di ${sector.streetName}! Kejar, kejar, KEJAR!",
            "Sensor gerak bunyi nyaring di ${sector.streetName}! Penyusup lagi mengendap di sekitar ${sector.name}—jangan sampai lolos!",
            "Waduh, lihat CCTV #4! Ada bayangan bertudung mengintip di ${sector.name} (${sector.streetName})! Kepung dia!",
            "PERHATIAN SEMUA UNIT! Ada pelari misterius—jelas BUKAN warga—terlihat di dekat ${sector.name} menuju ${sector.streetName}!",
            "Kenapa sih maling selalu beraksi pas jadwal jaga saya?! Penyusup terdeteksi di ${sector.name}, ${sector.streetName}! Tutup jalannya!"
        )
        val line = templates[lineCounter % templates.size]
        val mood = if (sector.kind.name == "GATE") GandiMood.PANICKED else GandiMood.ALERT
        return GandiBroadcast(line = line, mood = mood)
    }

    fun gateStatusChanged(gateName: String, newStatus: GateStatus): GandiBroadcast {
        lineCounter++
        return when (newStatus) {
            GateStatus.LOCKED -> GandiBroadcast(
                line = "KLANG! $gateName sudah DIKUNCI rapat! Coba saja lompati pagar besi 3 meter itu kalau bisa, dasar penyusup!",
                mood = GandiMood.PROUD
            )
            GateStatus.ALERT -> GandiBroadcast(
                line = "AWAS! Alarm kawat pengaman di $gateName berbunyi! Cepat cek pintu putar sebelum mereka kabur!",
                mood = GandiMood.PANICKED
            )
            GateStatus.OPEN -> GandiBroadcast(
                line = "Eh, regu? $gateName TERBUKA lebar lagi! Tolong bilang itu cuma kurir paket, bukan si maling!",
                mood = GandiMood.SUSPICIOUS
            )
        }
    }

    fun apprehensionCelebration(
        guardName: String,
        sector: EstateSector,
        pointsEarned: Long,
        uniformId: String
    ): GandiBroadcast {
        lineCounter++
        val uniformNote = if (uniformId == "gandi_fanclub") {
            " sambil tampil kece dengan jaket amber favorit saya!"
        } else {
            ""
        }
        val lines = listOf(
            "MANTAP! $guardName membekuk penyusup di ${sector.streetName} tepat dekat ${sector.name}! +$pointsEarned poin$uniformNote",
            "SYUKURLAH! Penangkapan sempurna di ${sector.name} ($guardName +$pointsEarned poin)! Masuk buletin warga nih$uniformNote!",
            "KENA DEH! $guardName menutup jalur di ${sector.name} (${sector.streetName})! +$pointsEarned Poin Patroli—langsung belanja di Gudang!"
        )
        return GandiBroadcast(
            line = lines[lineCounter % lines.size],
            mood = GandiMood.PROUD
        )
    }

    fun pingReact(senderName: String, pingType: PingType, sector: EstateSector): GandiBroadcast {
        return when (pingType) {
            PingType.SUSPECT -> GandiBroadcast(
                line = "Diterima $senderName! Menandai lokasi penyusup di ${sector.name} (${sector.streetName})! Regu, merapat!",
                mood = GandiMood.ALERT
            )
            PingType.BACKUP -> GandiBroadcast(
                line = "$senderName minta bantuan darurat di ${sector.name}! Siapa pun yang dekat ${sector.streetName}, lari ke sana sekarang!",
                mood = GandiMood.PANICKED
            )
            PingType.BLOCK_GATE -> GandiBroadcast(
                line = "Langkah cerdas, $senderName! Semua tutup pintu keluar di sekitar ${sector.name} (${sector.streetName})!",
                mood = GandiMood.CHATTY
            )
            PingType.CLEAR -> GandiBroadcast(
                line = "Sisir mantap dari $senderName—${sector.name} terlihat bersih. Tetap siaga ya, ${sector.streetName} agak mencurigakan sepinya...",
                mood = GandiMood.SUSPICIOUS
            )
        }
    }

    fun shiftWelcome(guardName: String, roomCode: String): GandiBroadcast = GandiBroadcast(
        line = "Selamat bertugas di Shift $roomCode, $guardName! Ketuk jalan atau blok di peta untuk patroli, pasang ping, atau tekan PTT. Jaga Emerald Crest tetap aman!",
        mood = GandiMood.CHATTY
    )
}
