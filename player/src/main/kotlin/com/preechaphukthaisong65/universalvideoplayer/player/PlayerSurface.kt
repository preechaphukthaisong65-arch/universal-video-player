package com.preechaphukthaisong65.universalvideoplayer.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

@Composable
fun PlayerSurface(manager: PlayerSessionManager, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                player = manager.getOrCreatePlayer()
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { it.player = manager.getOrCreatePlayer() }
    )
}
