package com.preechaphukthaisong65.universalvideoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.preechaphukthaisong65.universalvideoplayer.data.PlaybackHistoryEntity
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerScreen
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSessionManager
import com.preechaphukthaisong65.universalvideoplayer.browser.BrowserScreen
import com.preechaphukthaisong65.universalvideoplayer.theme.UniversalPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: PlayerSessionManager

    private val openMedia = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) startPlayback(uris)
    }

    private val openSubtitle = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            playerManager.attachSubtitle(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerManager = (application as PlayerApplication).playerManager
        handleIncomingIntent(intent)

        val app = application as PlayerApplication
        setContent {
            UniversalPlayerTheme {
                var tab by remember { mutableStateOf("เล่น") }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            listOf("เล่น", "เบราว์เซอร์", "คลัง", "ตั้งค่า").forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    icon = {},
                                    label = { Text(item) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        when (tab) {
                            "เบราว์เซอร์" -> BrowserScreen(Modifier.weight(1f))
                            "คลัง" -> LibraryScreen(
                                app = app,
                                onPickFiles = { openMedia.launch(arrayOf("video/*", "audio/*")) },
                                onPlay = { item ->
                                    playerManager.playQueue(
                                        listOf(MediaSource(item.uri, item.title)),
                                        startPositionMs = item.lastPositionMs.coerceAtLeast(0L)
                                    )
                                    app.recordPlayback(MediaSource(item.uri, item.title))
                                    tab = "เล่น"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            "ตั้งค่า" -> SettingsScreen(Modifier.weight(1f))
                            else -> {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Universal Video Player", style = MaterialTheme.typography.titleLarge)
                                    TextButton(onClick = { openMedia.launch(arrayOf("video/*", "audio/*")) }) {
                                        Text("เปิดไฟล์")
                                    }
                                }
                                PlayerScreen(
                                    manager = playerManager,
                                    modifier = Modifier.weight(1f),
                                    onPickSubtitle = {
                                        openSubtitle.launch(
                                            arrayOf("application/x-subrip", "text/vtt", "application/octet-stream")
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) return
        when (incoming.action) {
            Intent.ACTION_VIEW -> incoming.data?.let { startPlayback(listOf(it)) }
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)
                uri?.let { startPlayback(listOf(it)) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(::startPlayback)
            }
        }
    }

    private fun startPlayback(uris: List<Uri>) {
        val sources = uris.distinct().map { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            MediaSource(uri.toString(), title = uri.lastPathSegment?.take(250))
        }
        if (sources.isEmpty()) return
        playerManager.playQueue(sources)
        sources.forEach { (application as PlayerApplication).recordPlayback(it) }
    }

    override fun onStop() {
        (application as? PlayerApplication)?.let { app ->
            val current = playerManager.currentMediaSource()
            if (current != null) app.recordPlayback(current)
        }
        playerManager.pause()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) playerManager.release()
    }
}

@Composable
private fun LibraryScreen(
    app: PlayerApplication,
    onPickFiles: () -> Unit,
    onPlay: (PlaybackHistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val history by app.database.historyDao().observeAll().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    val filtered = remember(history, query) {
        if (query.isBlank()) history
        else history.filter {
            it.title.contains(query, ignoreCase = true) || it.uri.contains(query, ignoreCase = true)
        }
    }

    Column(modifier.fillMaxSize()) {
        Text("คลังและประวัติการเล่น", style = MaterialTheme.typography.titleLarge)
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("ค้นหาชื่อไฟล์หรือที่อยู่ไฟล์") }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickFiles) { Text("เลือกไฟล์จากเครื่อง") }
            TextButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch { app.database.historyDao().clear() }
                }
            ) { Text("ล้างประวัติ") }
        }
        if (filtered.isEmpty()) {
            Text("ยังไม่มีประวัติการเล่น")
        } else {
            LazyColumn {
                items(filtered, key = { it.uri }) { item ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onPlay(item) }.padding(vertical = 10.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text("ตำแหน่งล่าสุด: ${formatPosition(item.lastPositionMs)}")
                        Text(item.uri, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Text("ตั้งค่า", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("ความเร็วเล่น: 0.5x – 3.0x")
        Text("การเล่นใช้ Media3 ExoPlayer")
        Text("Browser ใช้ Android WebView และ HTTPS เป็นค่าเริ่มต้น")
        Text("ประวัติและตำแหน่งเล่นเก็บไว้ในอุปกรณ์")
        Text("ไม่มี VPN ในรุ่นแรก")
    }
}

private fun formatPosition(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
