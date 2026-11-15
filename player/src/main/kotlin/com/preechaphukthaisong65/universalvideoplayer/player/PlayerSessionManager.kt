package com.preechaphukthaisong65.universalvideoplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.domain.PlaybackPort
import com.preechaphukthaisong65.universalvideoplayer.domain.PlaybackSpeedPolicy
import com.preechaphukthaisong65.universalvideoplayer.domain.PlaybackSnapshot

/**
 * Owns the application's only ExoPlayer instance. Always use applicationContext.
 * The Activity must pause on background and explicitly release when the app session closes.
 */
class PlayerSessionManager(context: Context) : PlaybackPort {
    private val appContext = context.applicationContext
    @Volatile private var player: ExoPlayer? = null

    @Synchronized
    fun getOrCreatePlayer(): ExoPlayer = player ?: ExoPlayer.Builder(appContext).build().also { player = it }

    override fun play(source: MediaSource) {
        val item = MediaItem.Builder().setUri(source.uri).apply {
            source.mimeType?.let(::setMimeType)
            source.title?.let { setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(it).build()) }
        }.build()
        getOrCreatePlayer().apply {
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    override fun pause() { player?.pause() }

    override fun seekTo(positionMs: Long) { player?.seekTo(positionMs.coerceAtLeast(0L)) }

    override fun setSpeed(speed: Float) {
        require(PlaybackSpeedPolicy.isAllowed(speed)) { "Playback speed must be between 0.25x and 3.0x" }
        player?.setPlaybackParameters(PlaybackParameters(speed))
    }

    override fun snapshot(): PlaybackSnapshot {
        val p = player
        return PlaybackSnapshot(
            isPlaying = p?.isPlaying == true,
            positionMs = p?.currentPosition ?: 0L,
            durationMs = (p?.duration ?: 0L).coerceAtLeast(0L),
            playbackSpeed = p?.playbackParameters?.speed ?: 1.0f
        )
    }

    override fun release() {
        synchronized(this) {
            player?.release()
            player = null
        }
    }
}
