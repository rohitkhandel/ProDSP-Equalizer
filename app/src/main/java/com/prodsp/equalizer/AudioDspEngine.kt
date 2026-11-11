
package com.prodsp.equalizer

import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost

class AudioDspEngine(audioSessionId: Int) {

    private val equalizer = Equalizer(0, audioSessionId)
    private val bassBoost = BassBoost(0, audioSessionId)

    val numberOfBands: Int
        get() = equalizer.numberOfBands.toInt()

    init {
        equalizer.enabled = true
        bassBoost.enabled = true
    }

    fun enableEqualizer(enabled: Boolean) {
        equalizer.enabled = enabled
    }

    fun enableBassBoost(enabled: Boolean) {
        bassBoost.enabled = enabled
    }

    fun setBandGain(frequencyHz: Int, gainDb: Float) {

        val count = equalizer.numberOfBands.toInt()
        var closestBand: Short = 0
        var smallestDifference = Int.MAX_VALUE

        for (i in 0 until count) {
            val centerFrequency =
                equalizer.getCenterFreq(i.toShort()) / 1000

            val difference =
                kotlin.math.abs(centerFrequency - frequencyHz)

            if (difference < smallestDifference) {
                smallestDifference = difference
                closestBand = i.toShort()
            }
        }

        val range = equalizer.bandLevelRange

        val level = (gainDb * 100)
            .toInt()
            .coerceIn(range[0].toInt(), range[1].toInt())
            .toShort()

        equalizer.setBandLevel(closestBand, level)
    }

    fun setBassStrength(percent: Int) {
        val strength = (percent.coerceIn(0, 100) * 10)
            .toShort()

        bassBoost.setStrength(strength)
    }

    fun release() {
        equalizer.release()
        bassBoost.release()
    }
}
