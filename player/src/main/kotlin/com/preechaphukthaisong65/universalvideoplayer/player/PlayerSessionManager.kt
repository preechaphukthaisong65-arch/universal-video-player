package com.preechaphukthaisong65.universalvideoplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.domain.PlaybackPort
import com.preechaphukthaisong65.universalvideoplayer.domain.PlaybackSpeedPolicy
import com.preechaphukthaisong65.universalvideoplayer.domain.PlaybackSnapshot

data class PlayerTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean,
    val supported: Boolean
)

/** Owns the single application-scoped ExoPlayer instance. */
class PlayerSessionManager(context: Context) : PlaybackPort {
    private val appContext = context.applicationContext
    @Volatile private var player: ExoPlayer? = null

    @Synchronized
    fun getOrCreatePlayer(): ExoPlayer =
        player ?: ExoPlayer.Builder(appContext).build().also { player = it }

    override fun play(source: MediaSource) = playQueue(listOf(source))

    fun playQueue(sources: List<MediaSource>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        require(sources.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in sources.indices) { "Queue start index is out of range" }
        val items = sources.map(::toMediaItem)
        getOrCreatePlayer().apply {
            setMediaItems(items, startIndex, startPositionMs.coerceAtLeast(0L))
            prepare()
            playWhenReady = true
        }
    }

    fun appendToQueue(sources: List<MediaSource>) {
        if (sources.isEmpty()) return
        getOrCreatePlayer().addMediaItems(sources.map(::toMediaItem))
    }

    fun playNext() {
        val p = player ?: return
        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
    }

    fun playPrevious() {
        val p = player ?: return
        if (p.currentPosition > 3_000L) p.seekTo(0L)
        else if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
    }

    fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceAtLeast(0L)
        val duration = p.duration
        p.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
    }

    fun currentMediaSource(): MediaSource? {
        val item = player?.currentMediaItem ?: return null
        val uri = item.localConfiguration?.uri?.toString() ?: return null
        return MediaSource(uri = uri, title = item.mediaMetadata.title?.toString())
    }

    fun queueTitles(): List<String> = player?.let { p ->
        (0 until p.mediaItemCount).map { index ->
            p.getMediaItemAt(index).mediaMetadata.title?.toString()
                ?: p.getMediaItemAt(index).localConfiguration?.uri?.lastPathSegment
                ?: "รายการ ${index + 1}"
        }
    }.orEmpty()

    fun trackOptions(trackType: Int): List<PlayerTrackOption> {
        val tracks = player?.currentTracks ?: return emptyList()
        return buildList {
            tracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == trackType) {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val label = listOfNotNull(
                            format.label?.takeIf(String::isNotBlank),
                            format.language?.takeIf(String::isNotBlank),
                            format.sampleMimeType?.substringAfter('/')?.uppercase()
                        ).distinct().joinToString(" · ").ifBlank { "Track ${trackIndex + 1}" }
                        add(
                            PlayerTrackOption(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                label = label,
                                selected = group.isTrackSelected(trackIndex),
                                supported = group.isTrackSupported(trackIndex)
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectTrack(trackType: Int, groupIndex: Int, trackIndex: Int) {
        val p = player ?: return
        val group = p.currentTracks.groups.getOrNull(groupIndex) ?: return
        if (group.type != trackType || trackIndex !in 0 until group.length ||
            !group.isTrackSupported(trackIndex)
        ) return
        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(override)
            .build()
    }

    fun disableTrack(trackType: Int) {
        val p = player ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(trackType)
            .setTrackTypeDisabled(trackType, true)
            .build()
    }

    fun attachSubtitle(uri: Uri, mimeType: String? = null) {
        val p = player ?: return
        val index = p.currentMediaItemIndex
        val current = p.currentMediaItem ?: return
        val local = current.localConfiguration ?: return
        val resolvedMime = mimeType ?: when (uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase()) {
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "vtt" -> MimeTypes.TEXT_VTT
            else -> return
        }
        if (resolvedMime != MimeTypes.APPLICATION_SUBRIP && resolvedMime != MimeTypes.TEXT_VTT) return
        val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(resolvedMime)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val updated = current.buildUpon()
            .setSubtitleConfigurations(local.subtitleConfigurations + subtitle)
            .build()
        val position = p.currentPosition
        val wasPlaying = p.playWhenReady
        p.replaceMediaItem(index, updated)
        p.seekTo(index, position)
        p.playWhenReady = wasPlaying
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
            positionMs = p?.currentPosition?.coerceAtLeast(0L) ?: 0L,
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

    private fun toMediaItem(source: MediaSource): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(source.uri)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(source.title ?: Uri.parse(source.uri).lastPathSegment).build()
            )
        source.mimeType?.let(builder::setMimeType)
        source.subtitleUri?.let { subtitleUri ->
            val mime = source.subtitleMimeType ?: when (
                subtitleUri.substringAfterLast('.', "").lowercase()
            ) {
                "srt" -> MimeTypes.APPLICATION_SUBRIP
                "vtt" -> MimeTypes.TEXT_VTT
                else -> null
            }
            if (mime == MimeTypes.APPLICATION_SUBRIP || mime == MimeTypes.TEXT_VTT) {
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUri))
                            .setMimeType(mime)
                            .setLanguage("und")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                )
            }
        }
        return builder.build()
    }
}
