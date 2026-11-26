package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.WeaponStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mesin efek suara senjata prosedural berbasis AudioTrack PCM
 * untuk Pistol, AK-47, Granat, Bazoka, dan suara borgol/penangkapan.
 */
class WeaponSoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    private val pistolPcm: ByteArray by lazy { synthesizePistol() }
    private val shotgunPcm: ByteArray by lazy { synthesizeShotgun() }
    private val smgPcm: ByteArray by lazy { synthesizeSmg() }
    private val ak47Pcm: ByteArray by lazy { synthesizeAk47() }
    private val sniperPcm: ByteArray by lazy { synthesizeSniper() }
    private val grenadePcm: ByteArray by lazy { synthesizeGrenade() }
    private val bazookaPcm: ByteArray by lazy { synthesizeBazooka() }
    private val minigunPcm: ByteArray by lazy { synthesizeMinigun() }
    private val arrestPcm: ByteArray by lazy { synthesizeArrestChime() }
    private val heliRotorPcm: ByteArray by lazy { synthesizeHeliSpin() }

    fun playWeaponFire(style: WeaponStyle) {
        val pcm = when (style) {
            WeaponStyle.PISTOL -> pistolPcm
            WeaponStyle.SHOTGUN -> shotgunPcm
            WeaponStyle.SMG -> smgPcm
            WeaponStyle.AK47 -> ak47Pcm
            WeaponStyle.SNIPER -> sniperPcm
            WeaponStyle.GRENADE -> grenadePcm
            WeaponStyle.BAZOOKA -> bazookaPcm
            WeaponStyle.MINIGUN -> minigunPcm
        }
        playPcmAsync(pcm)
    }

    fun playArrest() {
        playPcmAsync(arrestPcm)
    }

    fun playHelicopterRotor() {
        playPcmAsync(heliRotorPcm)
    }

    private fun playPcmAsync(pcm: ByteArray) {
        scope.launch {
            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size)
                    .build()

                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    val durationMs = ((pcm.size / 2) * 1000L) / sampleRate
                    kotlinx.coroutines.delay(durationMs + 80L)
                }
            } catch (_: Throwable) {
                // Abaikan bila perangkat tidak mendukung AudioTrack saat pengujian unit
            } finally {
                try {
                    track?.stop()
                } catch (_: Throwable) {
                }
                try {
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    // Pistol 9mm: dentuman cepat + klik slide tajam (~150ms)
    private fun synthesizePistol(): ByteArray {
        val samples = (sampleRate * 0.16f).toInt()
        val rng = Random(101)
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val crack = (rng.nextFloat() * 2f - 1f) * exp(-t * 36f) * 0.9f
            val body = sin(2f * PI.toFloat() * (190f - t * 650f) * t) * exp(-t * 28f) * 0.75f
            val snap = if (t in 0.045f..0.075f) sin(2f * PI.toFloat() * 1400f * t) * 0.18f else 0f
            (crack + body + snap).coerceIn(-1f, 1f)
        }
    }

    // AK-47: hentakan mid-bass kuat + gema mekanis (~180ms)
    private fun synthesizeAk47(): ByteArray {
        val samples = (sampleRate * 0.18f).toInt()
        val rng = Random(202)
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val punch = sin(2f * PI.toFloat() * (145f - t * 420f) * t) * exp(-t * 20f) * 0.92f
            val blast = (rng.nextFloat() * 2f - 1f) * exp(-t * 24f) * 0.85f
            val clack = sin(2f * PI.toFloat() * 880f * t) * exp(-t * 45f) * 0.35f
            (punch + blast + clack).coerceIn(-1f, 1f)
        }
    }

    // Granat: desis lemparan singkat -> ledakan besar area (~650ms)
    private fun synthesizeGrenade(): ByteArray {
        val samples = (sampleRate * 0.65f).toInt()
        val rng = Random(303)
        var lpNoise = 0f
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val rawNoise = rng.nextFloat() * 2f - 1f
            lpNoise = lpNoise * 0.86f + rawNoise * 0.14f
            if (t < 0.12f) {
                // Suara pin & lemparan cepat
                rawNoise * exp(-t * 16f) * 0.25f
            } else {
                // Ledakan fragmen
                val bt = t - 0.12f
                val subBoom = sin(2f * PI.toFloat() * (75f - bt * 45f).coerceAtLeast(28f) * bt) * exp(-bt * 5.2f) * 0.95f
                val rumble = lpNoise * exp(-bt * 4.5f) * 1.15f
                val crackle = rawNoise * exp(-bt * 12f) * 0.45f
                (subBoom + rumble + crackle).coerceIn(-1f, 1f)
            }
        }
    }

    // Bazoka RPG: desingan luncur roket -> dentuman hulu ledak raksasa (~820ms)
    private fun synthesizeBazooka(): ByteArray {
        val samples = (sampleRate * 0.82f).toInt()
        val rng = Random(404)
        var lpNoise = 0f
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val rawNoise = rng.nextFloat() * 2f - 1f
            lpNoise = lpNoise * 0.90f + rawNoise * 0.10f
            if (t < 0.22f) {
                // Roket meluncur (fwoosh!)
                val whoosh = (lpNoise * 0.7f + rawNoise * 0.3f) * (0.4f + t * 2.4f)
                val thrust = sin(2f * PI.toFloat() * (160f + t * 500f) * t) * 0.35f
                (whoosh + thrust).coerceIn(-1f, 1f)
            } else {
                // Ledakan roket dahsyat
                val bt = t - 0.22f
                val megaSub = sin(2f * PI.toFloat() * (62f - bt * 32f).coerceAtLeast(22f) * bt) * exp(-bt * 3.6f) * 1.1f
                val shockwave = lpNoise * exp(-bt * 3.8f) * 1.25f
                val shrapnel = rawNoise * exp(-bt * 9.5f) * 0.5f
                (megaSub + shockwave + shrapnel).coerceIn(-1f, 1f)
            }
        }
    }

    // Suara borgol & sirine singkat saat pencuri dikirim ke penjara (~280ms)
    private fun synthesizeArrestChime(): ByteArray {
        val samples = (sampleRate * 0.28f).toInt()
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val freq = if (t < 0.13f) 760f else 1040f
            val env = if (t < 0.13f) exp(-t * 14f) else exp(-(t - 0.13f) * 14f)
            (sin(2f * PI.toFloat() * freq * t) * env * 0.55f).coerceIn(-1f, 1f)
        }
    }

    // Shotgun 12G: dentuman berat melebar + kokang pompa (~340ms)
    private fun synthesizeShotgun(): ByteArray {
        val samples = (sampleRate * 0.34f).toInt()
        val rng = Random(505)
        var lp = 0f
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val raw = rng.nextFloat() * 2f - 1f
            lp = lp * 0.78f + raw * 0.22f
            val boom = sin(2f * PI.toFloat() * (120f - t * 240f).coerceAtLeast(35f) * t) * exp(-t * 14f) * 1.0f
            val spread = lp * exp(-t * 15f) * 1.05f
            val pump = if (t in 0.21f..0.28f) sin(2f * PI.toFloat() * 920f * t) * exp(-(t - 0.21f) * 28f) * 0.28f else 0f
            (boom + spread + pump).coerceIn(-1f, 1f)
        }
    }

    // SMG MP5: letupan sangat singkat dan padat (~110ms)
    private fun synthesizeSmg(): ByteArray {
        val samples = (sampleRate * 0.11f).toInt()
        val rng = Random(606)
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val snap = (rng.nextFloat() * 2f - 1f) * exp(-t * 42f) * 0.85f
            val body = sin(2f * PI.toFloat() * (220f - t * 900f).coerceAtLeast(70f) * t) * exp(-t * 34f) * 0.7f
            (snap + body).coerceIn(-1f, 1f)
        }
    }

    // Sniper AWP: ledakan supersonik tajam + gema panjang (~460ms)
    private fun synthesizeSniper(): ByteArray {
        val samples = (sampleRate * 0.46f).toInt()
        val rng = Random(707)
        var lp = 0f
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val raw = rng.nextFloat() * 2f - 1f
            lp = lp * 0.85f + raw * 0.15f
            val crack = raw * exp(-t * 38f) * 1.0f
            val sonic = sin(2f * PI.toFloat() * (290f - t * 550f).coerceAtLeast(45f) * t) * exp(-t * 11f) * 0.9f
            val echo = lp * exp(-t * 6.5f) * 0.55f
            (crack + sonic + echo).coerceIn(-1f, 1f)
        }
    }

    // Minigun Vulcan: 2 rentetan beruntun + dengungan motor rotary (~140ms)
    private fun synthesizeMinigun(): ByteArray {
        val samples = (sampleRate * 0.14f).toInt()
        val rng = Random(808)
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val p1 = exp(-t * 36f)
            val p2 = if (t >= 0.055f) exp(-(t - 0.055f) * 36f) else 0f
            val pulse = p1 + p2
            val blast = (rng.nextFloat() * 2f - 1f) * pulse * 0.85f
            val rotaryWhirr = sin(2f * PI.toFloat() * 420f * t) * 0.22f
            (blast + rotaryWhirr).coerceIn(-1f, 1f)
        }
    }

    // Helikopter patroli naik: deru baling-baling berputar (~520ms)
    private fun synthesizeHeliSpin(): ByteArray {
        val samples = (sampleRate * 0.52f).toInt()
        val rng = Random(909)
        var lp = 0f
        return buildPcm(samples) { i ->
            val t = i / sampleRate.toFloat()
            val raw = rng.nextFloat() * 2f - 1f
            lp = lp * 0.88f + raw * 0.12f
            val bladePulse = (0.4f + 0.6f * kotlin.math.abs(sin(2f * PI.toFloat() * (8f + t * 14f) * t)))
            val thump = sin(2f * PI.toFloat() * 58f * t) * bladePulse * 0.45f
            val wind = lp * bladePulse * 0.72f
            val fade = (1f - t / 0.52f).coerceIn(0f, 1f)
            ((thump + wind) * fade).coerceIn(-1f, 1f)
        }
    }

    private inline fun buildPcm(sampleCount: Int, sampleAt: (Int) -> Float): ByteArray {
        val bytes = ByteArray(sampleCount * 2)
        for (i in 0 until sampleCount) {
            val s = (sampleAt(i).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
            bytes[i * 2] = (s and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
