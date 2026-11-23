package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * On-device TextToSpeech engine for GANDI dispatches & radio callouts (zero permissions required).
 */
class GandiVoiceSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val idLocale = Locale.forLanguageTag("id-ID")
                val res = tts?.setLanguage(idLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setPitch(1.08f)
                tts?.setSpeechRate(1.05f)
                ready = true
            }
        }
    }

    fun speak(text: String, enabled: Boolean) {
        if (!enabled || !ready) return
        val clean = text.replace("//", " ").take(220)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "gandi_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

/**
 * Push-to-Talk microphone amplitude meter using AudioRecord when RECORD_AUDIO is granted,
 * with a clean fallback meter when running without mic access.
 */
class PttAudioMonitor(private val scope: CoroutineScope) {
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(hasMicPermission: Boolean) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            if (hasMicPermission) {
                val sampleRate = 8000
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf > 0) {
                    var recorder: AudioRecord? = null
                    try {
                        recorder = AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBuf
                        )
                        if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                            recorder.startRecording()
                            val buffer = ShortArray(minBuf / 2)
                            while (isActive) {
                                val read = recorder.read(buffer, 0, buffer.size)
                                if (read > 0) {
                                    var peak = 0
                                    for (i in 0 until read) {
                                        val v = abs(buffer[i].toInt())
                                        if (v > peak) peak = v
                                    }
                                    _level.value = (peak / 12000f).coerceIn(0.12f, 1.0f)
                                }
                                delay(60L)
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.w("PttAudio", "AudioRecord fallback active: ${e.message}")
                    } finally {
                        try {
                            recorder?.stop()
                        } catch (_: Exception) {
                        }
                        recorder?.release()
                    }
                }
            }
            // Graceful fallback animation when mic unavailable/denied
            var phase = 0f
            while (isActive) {
                phase += 0.45f
                _level.value = (0.35f + 0.45f * abs(kotlin.math.sin(phase))).coerceIn(0.15f, 0.95f)
                delay(75L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _level.value = 0f
    }
}
