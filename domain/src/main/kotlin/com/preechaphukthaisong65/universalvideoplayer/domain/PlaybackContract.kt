package com.preechaphukthaisong65.universalvideoplayer.domain

data class MediaSource(
    val uri: String,
    val title: String? = null,
    val mimeType: String? = null
)

data class PlaybackSnapshot(
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float
)

interface PlaybackPort {
    fun play(source: MediaSource)
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun snapshot(): PlaybackSnapshot
    fun release()
}
