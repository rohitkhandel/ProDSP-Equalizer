
package com.prodsp.equalizer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MusicPlayerEngine(context: Context) {

    private val player = ExoPlayer.Builder(
        context.applicationContext
    ).build()

    val audioSessionId: Int
        get() = player.audioSessionId

    val isPlaying: Boolean
        get() = player.isPlaying

    val duration: Long
        get() = player.duration

    val currentPosition: Long
        get() = player.currentPosition

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

    fun release() {
        player.release()
    }
}
