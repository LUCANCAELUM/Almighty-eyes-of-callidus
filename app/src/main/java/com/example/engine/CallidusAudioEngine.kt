package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural tactical audio synthesizer matching the ops-room sound cues
 * of God's Eye View (radar sweep pings, target locks, shader switches).
 */
class CallidusAudioEngine(
    private val scope: CoroutineScope
) {
    @Volatile
    var isEnabled: Boolean = false

    private var playJob: Job? = null

    fun playRadarPing() {
        if (!isEnabled) return
        playTone(startFreqHz = 1280.0, endFreqHz = 860.0, durationMs = 95, gain = 0.16)
    }

    fun playTargetLock() {
        if (!isEnabled) return
        playTone(startFreqHz = 640.0, endFreqHz = 1480.0, durationMs = 110, gain = 0.18)
    }

    fun playOpticClick() {
        if (!isEnabled) return
        playTone(startFreqHz = 920.0, endFreqHz = 520.0, durationMs = 55, gain = 0.14)
    }

    fun playSeismicAlert() {
        if (!isEnabled) return
        playTone(startFreqHz = 440.0, endFreqHz = 280.0, durationMs = 180, gain = 0.2)
    }

    private fun playTone(startFreqHz: Double, endFreqHz: Double, durationMs: Int, gain: Double) {
        playJob?.cancel()
        playJob = scope.launch(Dispatchers.Default) {
            runCatching {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationMs) / 1000
                if (numSamples <= 0) return@launch
                val samples = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples
                    val freq = startFreqHz + (endFreqHz - startFreqHz) * t
                    val env = exp(-t * 4.2) * (1.0 - exp(-t * 28.0))
                    phase += 2.0 * PI * freq / sampleRate
                    val v = (sin(phase) * env * gain * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = v.toShort()
                }
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(samples.size * 2)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 40L)
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }
}
