package com.preechaphukthaisong65.universalvideoplayer

import android.app.Application
import android.net.Uri
import androidx.room.Room
import com.preechaphukthaisong65.universalvideoplayer.data.PlaybackHistoryEntity
import com.preechaphukthaisong65.universalvideoplayer.data.PlayerDatabase
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerApplication : Application() {
    lateinit var playerManager: PlayerSessionManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: PlayerDatabase by lazy {
        Room.databaseBuilder(this, PlayerDatabase::class.java, "universal-player.db").build()
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerSessionManager(this)
        applicationScope.launch {
            while (isActive) {
                runCatching { persistCurrentProgress() }
                delay(PROGRESS_SAVE_INTERVAL_MS)
            }
        }
    }

    fun recordPlayback(source: MediaSource) {
        if (!isPersistableLocalUri(source.uri)) return
        applicationScope.launch {
            val now = System.currentTimeMillis()
            val title = source.title?.take(250)?.ifBlank { null } ?: "Untitled media"
            val dao = database.historyDao()
            dao.insertIfAbsent(
                PlaybackHistoryEntity(
                    uri = source.uri,
                    title = title,
                    lastPositionMs = 0L,
                    lastPlayedAt = now
                )
            )
            dao.touch(source.uri, title, now)
        }
    }

    fun recordPlaybackProgress(source: MediaSource, positionMs: Long) {
        if (!isPersistableLocalUri(source.uri)) return
        applicationScope.launch {
            persistProgress(source, positionMs)
        }
    }

    private suspend fun persistCurrentProgress() {
        // ExoPlayer is application-main-thread confined. Never read it from Dispatchers.IO.
        val current = withContext(Dispatchers.Main.immediate) {
            val source = playerManager.currentMediaSource()
            source?.let { it to playerManager.snapshot().positionMs }
        } ?: return
        persistProgress(current.first, current.second)
    }

    private suspend fun persistProgress(source: MediaSource, positionMs: Long) {
        val now = System.currentTimeMillis()
        val title = source.title?.take(250)?.ifBlank { null } ?: "Untitled media"
        val dao = database.historyDao()
        val position = positionMs.coerceAtLeast(0L)
        val updated = dao.updateProgress(source.uri, title, position, now)
        if (updated == 0) {
            dao.insertIfAbsent(
                PlaybackHistoryEntity(
                    uri = source.uri,
                    title = title,
                    lastPositionMs = position,
                    lastPlayedAt = now
                )
            )
        }
    }

    private fun isPersistableLocalUri(value: String): Boolean =
        Uri.parse(value).scheme?.lowercase() in setOf("content", "file")

    companion object {
        private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
    }
}
