package com.prodsp.equalizer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener

data class PlaybackSnapshot(
    val title: String = "No track selected",
    val isPlaying: Boolean = false,
    val hasTrack: Boolean = false,
    val isReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Owns app-local music playback and the Android audio effects for its session.
 * This does not claim to process audio from other apps or the whole device.
 */
class MusicPlayerEngine(context: Context) {

    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private var dspEngine: AudioDspEngine? = null

    private val pendingBandGains = linkedMapOf<Int, Float>()
    private var pendingBassStrength = 0
    private var equalizerEnabled = true
    private var bassBoostEnabled = true
    private var selectedTitle = "No track selected"
    private var lastError: String? = null

    var onPlaybackChanged: ((PlaybackSnapshot) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()

        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
            publish()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()

        override fun onPlayerError(error: PlaybackException) {
            lastError = error.localizedMessage ?: "Playback failed"
            publish()
        }
    }

    init {
        player.addListener(playerListener)
        player.addAnalyticsListener(
            object : AnalyticsListener {
                override fun onAudioSessionIdChanged(
                    eventTime: AnalyticsListener.EventTime,
                    audioSessionId: Int
                ) {
                    dspEngine?.release()
                    dspEngine = null

                    if (audioSessionId > 0) {
                        runCatching { AudioDspEngine(audioSessionId) }
                            .onSuccess { engine ->
                                dspEngine = engine
                                engine.enableEqualizer(equalizerEnabled)
                                engine.enableBassBoost(bassBoostEnabled)
                                engine.setBassStrength(pendingBassStrength)
                                pendingBandGains.forEach { (frequency, gain) ->
                                    engine.setBandGain(frequency, gain)
                                }
                            }
                    }
                    publish()
                }
            }
        )
    }

    val playbackSnapshot: PlaybackSnapshot
        get() {
            val metadataTitle = player.mediaMetadata.title?.toString()
                ?.takeIf { it.isNotBlank() }
            val duration = player.duration.takeIf { it > 0L } ?: 0L
            return PlaybackSnapshot(
                title = metadataTitle ?: selectedTitle,
                isPlaying = player.isPlaying,
                hasTrack = player.currentMediaItem != null,
                isReady = player.playbackState == Player.STATE_READY,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                errorMessage = lastError
            )
        }

    /**
     * Shows real device bands after an audio session is created. Before that,
     * these are temporary control points; settings are applied when playback
     * provides its session ID.
     */
    val equalizerBandFrequenciesHz: List<Int>
        get() = dspEngine?.bandFrequenciesHz?.takeIf { it.isNotEmpty() }
            ?: listOf(60, 230, 910, 3_600, 14_000)

    fun play(uri: Uri, displayName: String) {
        selectedTitle = displayName.ifBlank { "Selected audio" }
        lastError = null
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        publish()
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else if (player.currentMediaItem != null) {
            player.play()
        }
        publish()
    }

    fun stop() {
        player.stop()
        publish()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun setEqualizerBand(frequencyHz: Int, gainDb: Float) {
        val safeGain = gainDb.coerceIn(-12f, 12f)
        pendingBandGains[frequencyHz] = safeGain
        dspEngine?.setBandGain(frequencyHz, safeGain)
    }

    fun setBassBoost(percent: Int) {
        pendingBassStrength = percent.coerceIn(0, 100)
        dspEngine?.setBassStrength(pendingBassStrength)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerEnabled = enabled
        dspEngine?.enableEqualizer(enabled)
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        bassBoostEnabled = enabled
        dspEngine?.enableBassBoost(enabled)
    }

    fun release() {
        player.removeListener(playerListener)
        dspEngine?.release()
        dspEngine = null
        player.release()
        onPlaybackChanged = null
    }

    private fun publish() {
        onPlaybackChanged?.invoke(playbackSnapshot)
    }
}
