package com.preechaphukthaisong65.universalvideoplayer.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    manager: PlayerSessionManager,
    modifier: Modifier = Modifier,
    onPickSubtitle: () -> Unit = {},
    keepScreenOn: Boolean = true
) {
    val player = remember(manager) { manager.getOrCreatePlayer() }
    var playing by remember { mutableStateOf(player.isPlaying) }
    var positionMs by remember { mutableStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var durationMs by remember { mutableStateOf(player.duration.coerceAtLeast(0L)) }
    var speed by remember { mutableStateOf(player.playbackParameters.speed) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var locked by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var trackOptions by remember { mutableStateOf(emptyList<PlayerTrackOption>()) }
    var queueTitles by remember { mutableStateOf(emptyList<String>()) }
    var playerError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = player.duration.coerceAtLeast(0L)
                if (playbackState == Player.STATE_READY) playerError = null
            }
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.message?.take(180) ?: "ไม่สามารถเล่นไฟล์นี้ได้"
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                if (dialog == "audio") trackOptions = manager.trackOptions(C.TRACK_TYPE_AUDIO)
                if (dialog == "subtitle") trackOptions = manager.trackOptions(C.TRACK_TYPE_TEXT)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            speed = player.playbackParameters.speed
            delay(500)
        }
    }

    Column(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        this.resizeMode = resizeMode
                        this.keepScreenOn = playing && keepScreenOn
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeMode
                    view.keepScreenOn = playing && keepScreenOn
                }
            )
            if (locked) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x55000000))
                        .clickable { locked = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("หน้าจอถูกล็อก · แตะเพื่อปลดล็อก", color = Color.White)
                }
            }
        }

        playerError?.let { message ->
            Text("เกิดข้อผิดพลาด: $message", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Slider(
            value = if (durationMs > 0L) positionMs.coerceIn(0L, durationMs).toFloat() else 0f,
            onValueChange = { positionMs = it.toLong() },
            onValueChangeFinished = { manager.seekTo(positionMs) },
            valueRange = 0f..(durationMs.coerceAtLeast(1L).toFloat()),
            enabled = durationMs > 0L && !locked,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(positionMs))
            Text(formatTime(durationMs))
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { manager.playPrevious() }, enabled = !locked) { Text("ก่อนหน้า") }
            TextButton(onClick = { manager.seekBy(-10_000L) }, enabled = !locked) { Text("−10 วิ") }
            Button(onClick = {
                if (player.isPlaying) player.pause() else player.play()
            }, enabled = !locked) { Text(if (playing) "หยุดชั่วคราว" else "เล่น") }
            TextButton(onClick = { manager.seekBy(10_000L) }, enabled = !locked) { Text("+10 วิ") }
            TextButton(onClick = { manager.playNext() }, enabled = !locked && player.hasNextMediaItem()) { Text("ถัดไป") }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                resizeMode = when (resizeMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }) {
                Text(
                    when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "ซูม"
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "ยืดเต็ม"
                        else -> "พอดีจอ"
                    }
                )
            }
            TextButton(onClick = {
                speed = when (speed) {
                    0.5f -> 0.75f
                    0.75f -> 1f
                    1f -> 1.25f
                    1.25f -> 1.5f
                    1.5f -> 2f
                    2f -> 3f
                    else -> 0.5f
                }
                manager.setSpeed(speed)
            }, enabled = !locked) { Text("${speed}x") }
            TextButton(onClick = { locked = !locked }) { Text(if (locked) "ปลดล็อก" else "ล็อกจอ") }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                trackOptions = manager.trackOptions(C.TRACK_TYPE_AUDIO)
                dialog = "audio"
            }, enabled = !locked) { Text("เสียง") }
            TextButton(onClick = {
                trackOptions = manager.trackOptions(C.TRACK_TYPE_TEXT)
                dialog = "subtitle"
            }, enabled = !locked) { Text("คำบรรยาย") }
            TextButton(onClick = onPickSubtitle, enabled = !locked) { Text("เพิ่ม SRT/VTT") }
            TextButton(onClick = {
                queueTitles = manager.queueTitles()
                dialog = "queue"
            }) { Text("คิว") }
        }
    }

    if (dialog == "audio" || dialog == "subtitle") {
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(if (dialog == "audio") "เลือกแทร็กเสียง" else "เลือกคำบรรยาย") },
            text = {
                Column {
                    TextButton(onClick = {
                        manager.disableTrack(if (dialog == "audio") C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT)
                        dialog = ""
                    }) { Text("ปิด") }
                    trackOptions.forEach { option ->
                        TextButton(
                            onClick = {
                                manager.selectTrack(
                                    if (dialog == "audio") C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT,
                                    option.groupIndex,
                                    option.trackIndex
                                )
                                dialog = ""
                            },
                            enabled = option.supported
                        ) {
                            Text((if (option.selected) "✓ " else "") + option.label)
                        }
                    }
                    if (trackOptions.isEmpty()) Text("ไฟล์นี้ไม่มีแทร็กให้เลือก")
                }
            },
            confirmButton = { TextButton(onClick = { dialog = "" }) { Text("ปิดหน้าต่าง") } }
        )
    }

    if (dialog == "queue") {
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text("คิวเล่น") },
            text = {
                LazyColumn {
                    itemsIndexed(queueTitles) { index, title ->
                        TextButton(
                            onClick = {
                                player.seekTo(index, 0L)
                                player.play()
                                dialog = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("${index + 1}. $title") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = "" }) { Text("เสร็จ") } }
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
