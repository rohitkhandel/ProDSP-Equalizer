
package com.prodsp.equalizer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener

class MusicPlayerEngine(context: Context) {

    private val player = ExoPlayer.Builder(
        context.applicationContext
    ).build()

    private var dspEngine: AudioDspEngine? = null

    init {
        player.addAnalyticsListener(
            object : AnalyticsListener {
                override fun onAudioSessionIdChanged(
                    eventTime: AnalyticsListener.EventTime,
                    audioSessionId: Int
                ) {
                    dspEngine?.release()
                    dspEngine = null

                    if (audioSessionId > 0) {
                        try {
                            dspEngine = AudioDspEngine(audioSessionId)
                        } catch (e: Exception) {
                            dspEngine = null
                        }
                    }
                }
            }
        )
    }

    val isPlaying: Boolean
        get() = player.isPlaying

    val currentPosition: Long
        get() = player.currentPosition

    val duration: Long
        get() = player.duration

    fun play(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun resume() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    fun setEqualizerBand(
        frequencyHz: Int,
        gainDb: Float
    ) {
        dspEngine?.setBandGain(frequencyHz, gainDb)
    }

    fun setBassBoost(percent: Int) {
        dspEngine?.setBassStrength(percent)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        dspEngine?.enableEqualizer(enabled)
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        dspEngine?.enableBassBoost(enabled)
    }

    fun release() {
        dspEngine?.release()
        dspEngine = null
        player.release()
    }
}
