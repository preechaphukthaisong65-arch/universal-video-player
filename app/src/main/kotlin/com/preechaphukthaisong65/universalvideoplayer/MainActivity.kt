package com.preechaphukthaisong65.universalvideoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.preechaphukthaisong65.universalvideoplayer.browser.BrowserScreen
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSessionManager
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSurface
import com.preechaphukthaisong65.universalvideoplayer.theme.UniversalPlayerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: PlayerSessionManager
    private val openMedia = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            playerManager.play(MediaSource(uri.toString(), title = it.lastPathSegment))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerManager = (application as PlayerApplication).playerManager
        intent?.data?.let { playerManager.play(MediaSource(it.toString(), it.lastPathSegment)) }
        setContent {
            UniversalPlayerTheme {
                var tab by remember { mutableStateOf("หน้าแรก") }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            listOf("หน้าแรก", "เบราว์เซอร์", "คลังวิดีโอ", "ตั้งค่า").forEach { item ->
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
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                        when (tab) {
                            "เบราว์เซอร์" -> BrowserScreen(Modifier.weight(1f))
                            "คลังวิดีโอ" -> {
                                Text("คลังวิดีโอ", style = MaterialTheme.typography.headlineMedium)
                                Text("เลือกไฟล์จากอุปกรณ์ผ่านตัวเลือกไฟล์ Android")
                                Button(onClick = { openMedia.launch(arrayOf("video/*", "audio/*")) }) { Text("เลือกไฟล์") }
                            }
                            "ตั้งค่า" -> {
                                Text("ตั้งค่า", style = MaterialTheme.typography.headlineMedium)
                                Text("ประวัติและการตั้งค่าจัดเก็บภายในอุปกรณ์")
                                Text("Playback speed: 0.25x – 3.0x")
                                Text("เว็บไซต์: HTTPS เป็นค่าเริ่มต้น")
                            }
                            else -> {
                                Text("Universal Video Player", style = MaterialTheme.typography.headlineMedium)
                                Text("เล่นวิดีโอและเสียงจากไฟล์ในอุปกรณ์")
                                Button(onClick = { openMedia.launch(arrayOf("video/*", "audio/*")) }) { Text("เปิดไฟล์วิดีโอหรือเสียง") }
                                Spacer(Modifier.height(20.dp))
                                PlayerSurface(playerManager, Modifier.fillMaxWidth().height(230.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        playerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) playerManager.release()
    }
}
