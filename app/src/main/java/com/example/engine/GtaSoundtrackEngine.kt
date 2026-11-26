package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mesin backsound prosedural ala GTA West-Coast / Tactical G-Funk (96 BPM, loop mulus 10 detik)
 * dengan bassline tebal, beat drum 90-an, chord Rhodes, dan melodi synth G-Funk.
 */
class GtaSoundtrackEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    private var track: AudioTrack? = null
    private var pcmData: ByteArray? = null
    private var isStarted = false
    @Volatile
    private var isMuted = false

    fun start() {
        if (isStarted) {
            resume()
            return
        }
        isStarted = true
        scope.launch {
            try {
                val pcm = pcmData ?: synthesizeGtaLoop().also { pcmData = it }
                val sampleCount = pcm.size / 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.write(pcm, 0, pcm.size)
                    audioTrack.setLoopPoints(0, sampleCount, -1)
                    audioTrack.setVolume(if (isMuted) 0f else 0.34f)
                    audioTrack.play()
                    track = audioTrack
                } else {
                    audioTrack.release()
                }
            } catch (_: Throwable) {
                // Abaikan bila perangkat/unit test tidak memiliki output audio aktif
            }
        }
    }

    fun pause() {
        try {
            if (track?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track?.pause()
            }
        } catch (_: Throwable) {
        }
    }

    fun resume() {
        try {
            if (!isMuted && track?.state == AudioTrack.STATE_INITIALIZED && track?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track?.setVolume(0.34f)
                track?.play()
            }
        } catch (_: Throwable) {
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        try {
            track?.setVolume(if (muted) 0f else 0.34f)
            if (muted) {
                if (track?.playState == AudioTrack.PLAYSTATE_PLAYING) track?.pause()
            } else {
                resume()
            }
        } catch (_: Throwable) {
        }
    }

    fun release() {
        scope.cancel()
        try {
            track?.stop()
        } catch (_: Throwable) {
        }
        try {
            track?.release()
        } catch (_: Throwable) {
        }
        track = null
        isStarted = false
    }

    /**
     * Menghasilkan 4 bar @ 96 BPM (tepat 10.0 detik pada 22050 Hz = 220500 sampel)
     * agar titik loop nyambung sempurna tanpa klik.
     */
    private fun synthesizeGtaLoop(): ByteArray {
        val bpm = 96f
        val beatDur = 60f / bpm // 0.625s per beat
        val barDur = beatDur * 4f // 2.5s per bar
        val loopDur = barDur * 4f // 10.0s exact
        val totalSamples = (sampleRate * loopDur).toInt()
        val rng = Random(777)

        // Progresi chord G-Funk: Em7 -> Cmaj7 -> Am7 -> B7sus4
        val bassRoots = floatArrayOf(82.41f, 65.41f, 55.00f, 61.74f) // E2, C2, A1, B1
        val chordNotes = arrayOf(
            floatArrayOf(164.81f, 196.00f, 246.94f, 293.66f), // Em7
            floatArrayOf(130.81f, 164.81f, 196.00f, 246.94f), // Cmaj7
            floatArrayOf(110.00f, 130.81f, 164.81f, 196.00f), // Am7
            floatArrayOf(123.47f, 164.81f, 185.00f, 220.00f)  // B7sus4
        )
        // Melodi lead G-Funk khas GTA (tinggi dengan vibrato halus)
        val leadStepNotes = floatArrayOf(
            659.25f, 783.99f, 987.77f, 0f,
            880.00f, 783.99f, 659.25f, 587.33f,
            523.25f, 659.25f, 783.99f, 0f,
            659.25f, 587.33f, 493.88f, 587.33f
        )

        val bytes = ByteArray(totalSamples * 2)
        var leadPhase = 0f
        var smoothLeadFreq = 659.25f
        var hpNoisePrev = 0f

        for (i in 0 until totalSamples) {
            val t = i / sampleRate.toFloat()
            val barIdx = ((t / barDur).toInt()).coerceIn(0, 3)
            val barT = t - barIdx * barDur
            val beatIdx = ((barT / beatDur).toInt()).coerceIn(0, 3)
            val beatT = barT - beatIdx * beatDur
            val sixteenthDur = beatDur * 0.25f
            val sixteenthInBar = ((barT / sixteenthDur).toInt()).coerceIn(0, 15)
            val sixteenthT = barT - sixteenthInBar * sixteenthDur

            // 1. Kick drum boom-bap khas GTA SA (pada ketukan 0, 1.5, 2.5 atau sinkopasi)
            val kickHitT = when (sixteenthInBar) {
                0, 1, 2 -> barT
                6, 7, 8 -> barT - 6 * sixteenthDur
                10, 11 -> barT - 10 * sixteenthDur
                else -> -1f
            }
            val kick = if (kickHitT in 0f..0.24f) {
                val pitch = 46f + 115f * exp(-kickHitT * 26f)
                sin(2f * PI.toFloat() * pitch * kickHitT) * exp(-kickHitT * 12f) * 0.68f
            } else 0f

            // 2. Snare / Clap tajam di ketukan 2 dan 4 (sixteenth 4 dan 12)
            val snareHitT = when {
                sixteenthInBar in 4..6 -> barT - 4 * sixteenthDur
                sixteenthInBar in 12..14 -> barT - 12 * sixteenthDur
                else -> -1f
            }
            val rawNoise = rng.nextFloat() * 2f - 1f
            hpNoisePrev = hpNoisePrev * 0.55f + rawNoise * 0.45f
            val hiNoise = rawNoise - hpNoisePrev
            val snare = if (snareHitT in 0f..0.18f) {
                val tone = sin(2f * PI.toFloat() * 195f * snareHitT) * exp(-snareHitT * 22f) * 0.35f
                val snap = hiNoise * exp(-snareHitT * 19f) * 0.50f
                tone + snap
            } else 0f

            // 3. Hi-hat 8th-note groove
            val eighthDur = beatDur * 0.5f
            val eighthT = barT % eighthDur
            val hihatAccent = if (sixteenthInBar % 2 == 0) 0.14f else 0.08f
            val hihat = hiNoise * exp(-eighthT * 58f) * hihatAccent

            // 4. Funky G-Funk Bassline (pumping di ketukan 0, 3, 6, 8, 12, 14)
            val bassActive = sixteenthInBar in setOf(0, 1, 3, 6, 8, 9, 12, 14)
            val bassRoot = bassRoots[barIdx]
            val bassNote = if (sixteenthInBar == 14) bassRoot * 1.122f else bassRoot
            val bassEnv = if (bassActive) exp(-sixteenthT * 4.5f) else 0.12f
            val bassWave = (sin(2f * PI.toFloat() * bassNote * t) * 0.72f +
                sin(2f * PI.toFloat() * bassNote * 2f * t) * 0.25f) * bassEnv * 0.42f

            // 5. Rhodes Electric Piano Stabs (awal bar & sinkopasi ketukan ke-3)
            val stabT = when {
                barT < 0.75f -> barT
                barT >= 2.5f * beatDur -> barT - 2.5f * beatDur
                else -> -1f
            }
            var chordSum = 0f
            if (stabT in 0f..0.75f) {
                val stabEnv = exp(-stabT * 4.2f)
                val notes = chordNotes[barIdx]
                for (n in notes) {
                    chordSum += sin(2f * PI.toFloat() * n * t)
                }
                chordSum = (chordSum / notes.size) * stabEnv * 0.22f
            }

            // 6. West Coast G-Funk Whistling Synth Lead (muncul di bar 2 dan bar 4 agar tidak monoton)
            var lead = 0f
            val globalStep = ((t / (beatDur * 0.5f)).toInt()) % leadStepNotes.size
            val targetFreq = leadStepNotes[globalStep]
            if ((barIdx == 1 || barIdx == 3) && targetFreq > 0f) {
                smoothLeadFreq += (targetFreq - smoothLeadFreq) * 0.0045f
                val vibrato = sin(2f * PI.toFloat() * 5.2f * t) * 4.5f
                leadPhase += 2f * PI.toFloat() * (smoothLeadFreq + vibrato) / sampleRate
                if (leadPhase > 2f * PI.toFloat()) leadPhase -= 2f * PI.toFloat()
                val stepT = t % (beatDur * 0.5f)
                val leadEnv = (1f - exp(-stepT * 25f)) * exp(-stepT * 1.8f)
                lead = (sin(leadPhase) * 0.75f + sin(leadPhase * 2f) * 0.12f) * leadEnv * 0.16f
            }

            // Soft fade 15ms di awal & akhir untuk anti-pop saat loop mengulang
            val edgeFade = when {
                t < 0.015f -> t / 0.015f
                t > loopDur - 0.015f -> (loopDur - t) / 0.015f
                else -> 1f
            }.coerceIn(0f, 1f)

            val mix = ((kick + snare + hihat + bassWave + chordSum + lead) * edgeFade).coerceIn(-0.98f, 0.98f)
            val s = (mix * Short.MAX_VALUE * 0.90f).toInt()
            bytes[i * 2] = (s and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
