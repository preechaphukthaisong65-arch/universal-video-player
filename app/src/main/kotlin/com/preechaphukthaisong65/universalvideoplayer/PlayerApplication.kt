package com.preechaphukthaisong65.universalvideoplayer

import android.app.Application
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
        applicationScope.launch {
            val previous = database.historyDao().find(source.uri)
            database.historyDao().upsert(
                PlaybackHistoryEntity(
                    uri = source.uri,
                    title = source.title?.take(250) ?: previous?.title ?: "Untitled media",
                    lastPositionMs = previous?.lastPositionMs ?: 0L,
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun recordPlaybackProgress(source: MediaSource, positionMs: Long) {
        applicationScope.launch {
            val previous = database.historyDao().find(source.uri)
            database.historyDao().upsert(
                PlaybackHistoryEntity(
                    uri = source.uri,
                    title = source.title?.take(250) ?: previous?.title ?: "Untitled media",
                    lastPositionMs = positionMs.coerceAtLeast(0L),
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun persistCurrentProgress() {
        val source = playerManager.currentMediaSource() ?: return
        val snapshot = playerManager.snapshot()
        val now = System.currentTimeMillis()
        val existing = database.historyDao().find(source.uri)
        database.historyDao().upsert(
            PlaybackHistoryEntity(
                uri = source.uri,
                title = source.title?.take(250) ?: existing?.title ?: "Untitled media",
                lastPositionMs = snapshot.positionMs.coerceAtLeast(0L),
                lastPlayedAt = now
            )
        )
    }

    companion object {
        private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
    }
}
