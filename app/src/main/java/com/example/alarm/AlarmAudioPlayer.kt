package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlarmAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startPlaying(ringtoneId: String) {
        stopPlaying()
        Log.d("AlarmAudioPlayer", "Starting playback for $ringtoneId")
        if (ringtoneId == "system_default") {
            playSystemDefault()
        } else {
            playSynthesizedTone(ringtoneId)
        }
    }

    private fun playSystemDefault() {
        try {
            val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alert)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmAudioPlayer", "Failed to play default ringtone, playing backup beep", e)
            playSynthesizedTone("digital_beep")
        }
    }

    private fun playSynthesizedTone(toneId: String) {
        synthesisJob = scope.launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
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
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                when (toneId) {
                    "zen_bell" -> playZenBellLoop(track, sampleRate)
                    "digital_beep" -> playDigitalBeepLoop(track, sampleRate)
                    "gentle_breeze" -> playGentleBreezeLoop(track, sampleRate)
                    "deep_pulse" -> playDeepPulseLoop(track, sampleRate)
                    else -> playZenBellLoop(track, sampleRate)
                }
            } catch (e: Exception) {
                Log.e("AlarmAudioPlayer", "Error during sound synthesis setup", e)
                // Fallback attempt to play system default
                playSystemDefault()
            }
        }
    }

    private suspend fun playZenBellLoop(track: AudioTrack, sampleRate: Int) {
        val durationSec = 3.0
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(2048)

        while (synthesisJob?.isActive == true) {
            var sampleIndex = 0
            while (sampleIndex < totalSamples && synthesisJob?.isActive == true) {
                val chunkSize = minOf(buffer.size, totalSamples - sampleIndex)
                for (i in 0 until chunkSize) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val decay = kotlin.math.exp(-1.5 * (t % 3.0))
                    val value = sin(2.0 * Math.PI * 523.25 * t) +
                            0.5 * sin(2.0 * Math.PI * 1046.5 * t) * decay +
                            0.25 * sin(2.0 * Math.PI * 1569.75 * t) * decay
                    buffer[i] = (value * 12000.0 * decay).toInt().coerceIn(-32768, 32767).toShort()
                }
                track.write(buffer, 0, chunkSize)
                sampleIndex += chunkSize
                delay(5)
            }
            delay(500)
        }
    }

    private suspend fun playDigitalBeepLoop(track: AudioTrack, sampleRate: Int) {
        val beepDuration = 0.2
        val beepSamples = (sampleRate * beepDuration).toInt()
        val buffer = ShortArray(2048)

        while (synthesisJob?.isActive == true) {
            var sampleIndex = 0
            while (sampleIndex < beepSamples && synthesisJob?.isActive == true) {
                val chunkSize = minOf(buffer.size, beepSamples - sampleIndex)
                for (i in 0 until chunkSize) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val value = if (sin(2.0 * Math.PI * 1800.0 * t) >= 0) 1.0 else -1.0
                    buffer[i] = (value * 8000.0).toInt().toShort()
                }
                track.write(buffer, 0, chunkSize)
                sampleIndex += chunkSize
                delay(5)
            }
            delay(300)
        }
    }

    private suspend fun playGentleBreezeLoop(track: AudioTrack, sampleRate: Int) {
        val durationSec = 4.0
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(2048)

        while (synthesisJob?.isActive == true) {
            var sampleIndex = 0
            while (sampleIndex < totalSamples && synthesisJob?.isActive == true) {
                val chunkSize = minOf(buffer.size, totalSamples - sampleIndex)
                for (i in 0 until chunkSize) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val breathingMod = 0.5 * (1.0 + sin(2.0 * Math.PI * 0.25 * t))
                    val value = sin(2.0 * Math.PI * 110.0 * t) * 0.7 + sin(2.0 * Math.PI * 165.0 * t) * 0.3
                    buffer[i] = (value * 10000.0 * breathingMod).toInt().toShort()
                }
                track.write(buffer, 0, chunkSize)
                sampleIndex += chunkSize
                delay(5)
            }
        }
    }

    private suspend fun playDeepPulseLoop(track: AudioTrack, sampleRate: Int) {
        val durationSec = 2.0
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(2048)

        while (synthesisJob?.isActive == true) {
            var sampleIndex = 0
            while (sampleIndex < totalSamples && synthesisJob?.isActive == true) {
                val chunkSize = minOf(buffer.size, totalSamples - sampleIndex)
                for (i in 0 until chunkSize) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val pulseMod = 0.5 * (1.0 + sin(2.0 * Math.PI * 2.0 * t))
                    val value = sin(2.0 * Math.PI * 85.0 * t)
                    buffer[i] = (value * 12000.0 * pulseMod).toInt().toShort()
                }
                track.write(buffer, 0, chunkSize)
                sampleIndex += chunkSize
                delay(5)
            }
        }
    }

    fun stopPlaying() {
        Log.d("AlarmAudioPlayer", "Stopping playback")
        synthesisJob?.cancel()
        synthesisJob = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AlarmAudioPlayer", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        stop()
                    } catch (e: Exception) {}
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmAudioPlayer", "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }
}
