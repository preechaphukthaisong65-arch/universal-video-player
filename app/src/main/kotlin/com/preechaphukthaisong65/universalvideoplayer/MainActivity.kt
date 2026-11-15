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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.preechaphukthaisong65.universalvideoplayer.data.PlaybackHistoryEntity
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerScreen
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSessionManager
import com.preechaphukthaisong65.universalvideoplayer.browser.BrowserScreen
import com.preechaphukthaisong65.universalvideoplayer.theme.UniversalPlayerTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: PlayerSessionManager
    private lateinit var mediaLibraryRepository: MediaLibraryRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private val libraryItems = MutableStateFlow<List<MediaSource>>(emptyList())

    private val openMedia = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) startPlayback(uris)
    }

    private val openFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { selected ->
            runCatching {
                contentResolver.takePersistableUriPermission(
                    selected,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            getSharedPreferences("media-library", MODE_PRIVATE).edit()
                .putString("selected-tree-uri", selected.toString()).apply()
            scanFolder(selected)
        }
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
        mediaLibraryRepository = MediaLibraryRepository(this)
        settingsRepository = AppSettingsRepository(this)
        handleIncomingIntent(intent)
        getSharedPreferences("media-library", MODE_PRIVATE).getString("selected-tree-uri", null)
            ?.let { saved -> runCatching { Uri.parse(saved) }.getOrNull()?.let(::scanFolder) }

        val app = application as PlayerApplication
        setContent {
            val indexedMedia by libraryItems.collectAsState()
            var keepScreenAwake by remember { mutableStateOf(settingsRepository.keepScreenAwake()) }
            var javascriptEnabled by remember { mutableStateOf(settingsRepository.browserJavaScriptEnabled()) }
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
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                    Column(
                        Modifier.fillMaxSize().widthIn(max = 900.dp).padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        when (tab) {
                            "เบราว์เซอร์" -> BrowserScreen(Modifier.weight(1f), enableJavaScript = javascriptEnabled)
                            "คลัง" -> LibraryScreen(
                                app = app,
                                indexedMedia = indexedMedia,
                                onPickFiles = { openMedia.launch(arrayOf("video/*", "audio/*")) },
                                onPickFolder = { openFolder.launch(null) },
                                onPlaySource = { source ->
                                    playerManager.play(source)
                                    app.recordPlayback(source)
                                    tab = "เล่น"
                                },
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
                            "ตั้งค่า" -> SettingsScreen(keepScreenAwake, { keepScreenAwake = it; settingsRepository.setKeepScreenAwake(it) }, javascriptEnabled, { javascriptEnabled = it; settingsRepository.setBrowserJavaScriptEnabled(it) }, Modifier.weight(1f))
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
                                    },
                                    keepScreenOn = keepScreenAwake
                                )
                            }
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

    private fun scanFolder(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            libraryItems.value = runCatching { mediaLibraryRepository.scanFolder(uri) }
                .getOrDefault(emptyList())
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
            if (current != null) app.recordPlaybackProgress(current, playerManager.snapshot().positionMs)
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
    indexedMedia: List<MediaSource>,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onPlaySource: (MediaSource) -> Unit,
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
    val filteredLibrary = remember(indexedMedia, query) {
        if (query.isBlank()) indexedMedia
        else indexedMedia.filter {
            it.title.orEmpty().contains(query, ignoreCase = true) ||
                it.uri.contains(query, ignoreCase = true)
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
            Button(onClick = onPickFiles) { Text("เลือกไฟล์") }
            TextButton(onClick = onPickFolder) { Text("เลือกโฟลเดอร์") }
            val scope = rememberCoroutineScope()
            TextButton(
                onClick = {
                    scope.launch(Dispatchers.IO) { app.database.historyDao().clear() }
                }
            ) { Text("ล้างประวัติ") }
        }
        Text("ไฟล์ในโฟลเดอร์ที่เลือก (${filteredLibrary.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredLibrary, key = { "file:" + it.uri }) { source ->
                Column(
                    Modifier.fillMaxWidth().clickable { onPlaySource(source) }.padding(vertical = 10.dp)
                ) {
                    Text(source.title ?: "ไฟล์สื่อ", style = MaterialTheme.typography.titleMedium)
                    Text(source.mimeType ?: "ชนิดไฟล์ไม่ระบุ", style = MaterialTheme.typography.bodySmall)
                }
            }
            item { Text("ประวัติการเล่น (${filtered.size})", style = MaterialTheme.typography.titleMedium) }
            items(filtered, key = { "history:" + it.uri }) { item ->
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

@Composable
private fun SettingsScreen(
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    javascriptEnabled: Boolean,
    onJavascriptEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Text("ตั้งค่า", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("ป้องกันหน้าจอดับขณะเล่น")
                Text("ทำงานเฉพาะระหว่างเล่นสื่อ", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = keepScreenAwake, onCheckedChange = onKeepScreenAwakeChange, modifier = Modifier.semantics { contentDescription = "ป้องกันหน้าจอดับขณะเล่น" })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("เปิด JavaScript ใน Browser")
                Text("บางเว็บไซต์ต้องใช้ JavaScript; ปิดได้เพื่อจำกัดการทำงานของหน้าเว็บ", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = javascriptEnabled, onCheckedChange = onJavascriptEnabledChange, modifier = Modifier.semantics { contentDescription = "เปิด JavaScript ใน Browser" })
        }
        Spacer(Modifier.height(12.dp))
        Text("Playback Engine: Media3 ExoPlayer")
        Text("ไฟล์และประวัติจัดเก็บในอุปกรณ์")
        Text("ขนาดตัวอักษรอ้างอิงการตั้งค่าระบบ Android และรองรับ TalkBack")
        Text("รุ่นแรกไม่มี VPN")
    }
}

private fun formatPosition(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
