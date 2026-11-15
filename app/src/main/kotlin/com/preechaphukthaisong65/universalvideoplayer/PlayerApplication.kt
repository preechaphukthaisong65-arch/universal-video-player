package com.preechaphukthaisong65.universalvideoplayer

import android.app.Application
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSessionManager

class PlayerApplication : Application() {
    lateinit var playerManager: PlayerSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerSessionManager(this)
    }
}
