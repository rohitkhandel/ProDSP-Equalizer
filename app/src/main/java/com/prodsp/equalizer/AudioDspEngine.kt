package com.prodsp.equalizer

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Controls the Android audio effects attached to this app's ExoPlayer session.
 *
 * Android exposes the number and centre frequencies of the device EQ bands. We
 * use those real bands instead of pretending every device has the same layout.
 */
class AudioDspEngine(audioSessionId: Int) {

    private val equalizer = Equalizer(0, audioSessionId)
    private val bassBoost = BassBoost(0, audioSessionId)

    val bandFrequenciesHz: List<Int> = runCatching {
        (0 until equalizer.numberOfBands.toInt()).map { band ->
            (equalizer.getCenterFreq(band.toShort()) / 1_000).coerceAtLeast(1)
        }
    }.getOrDefault(emptyList())

    init {
        runCatching { equalizer.enabled = true }
        runCatching { bassBoost.enabled = true }
    }

    fun enableEqualizer(enabled: Boolean) {
        runCatching { equalizer.enabled = enabled }
    }

    fun enableBassBoost(enabled: Boolean) {
        runCatching { bassBoost.enabled = enabled }
    }

    /**
     * Android's built-in Equalizer has device-defined bands. The requested
     * frequency is therefore mapped to the nearest available device band.
     */
    fun setBandGain(frequencyHz: Int, gainDb: Float) {
        if (bandFrequenciesHz.isEmpty()) return

        val bandIndex = bandFrequenciesHz.indices.minByOrNull { index ->
            abs(bandFrequenciesHz[index] - frequencyHz)
        } ?: return

        val range = runCatching { equalizer.bandLevelRange }.getOrNull() ?: return
        if (range.size < 2) return

        val millibels = (gainDb.coerceIn(-24f, 24f) * 100f).roundToInt()
            .coerceIn(range[0].toInt(), range[1].toInt())

        runCatching {
            equalizer.setBandLevel(bandIndex.toShort(), millibels.toShort())
        }
    }

    fun setBassStrength(percent: Int) {
        val strength = (percent.coerceIn(0, 100) * 10).toShort()
        runCatching { bassBoost.setStrength(strength) }
    }

    fun release() {
        runCatching { equalizer.release() }
        runCatching { bassBoost.release() }
    }
}
