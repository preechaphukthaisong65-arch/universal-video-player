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
    }

    fun recordPlayback(source: MediaSource) {
        applicationScope.launch {
            database.historyDao().upsert(
                PlaybackHistoryEntity(
                    uri = source.uri,
                    title = source.title ?: "Untitled media",
                    lastPositionMs = 0L,
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
