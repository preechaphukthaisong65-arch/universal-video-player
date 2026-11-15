package com.preechaphukthaisong65.universalvideoplayer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.preechaphukthaisong65.universalvideoplayer.browser.BrowserDownloadHandler
import com.preechaphukthaisong65.universalvideoplayer.data.PlaybackHistoryEntity
import com.preechaphukthaisong65.universalvideoplayer.data.PlayerDatabase
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import com.preechaphukthaisong65.universalvideoplayer.player.PlayerSessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerLifecycleInstrumentedTest {

    @Test
    fun managerOwnsOnePlayerAndQueueContainsAllItems() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = PlayerSessionManager(context)
        try {
            val player = manager.getOrCreatePlayer()
            assertSame(player, manager.getOrCreatePlayer())

            manager.playQueue(
                listOf(
                    MediaSource("https://example.invalid/first.mp4", title = "First"),
                    MediaSource("https://example.invalid/second.webm", title = "Second")
                )
            )

            assertEquals(2, player.mediaItemCount)
            assertEquals(listOf("First", "Second"), manager.queueTitles())
            manager.setSpeed(1.5f)
            assertEquals(1.5f, player.playbackParameters.speed, 0.001f)
        } finally {
            manager.release()
        }
    }

    @Test
    fun roomPersistsAndUpdatesResumePositionWithoutReplacingTheRow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(
            context,
            PlayerDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val dao = database.historyDao()
            val entity = PlaybackHistoryEntity(
                uri = "content://test/media/one",
                title = "Test media",
                lastPositionMs = 0L,
                lastPlayedAt = 1_700_000_000_000L
            )
            dao.insertIfAbsent(entity)
            val changed = dao.updateProgress(
                uri = entity.uri,
                title = entity.title,
                positionMs = 12_345L,
                playedAt = 1_700_000_000_100L
            )

            assertEquals(1, changed)
            val restored = dao.find(entity.uri)
            assertEquals(12_345L, restored?.lastPositionMs)
            assertEquals(entity.title, restored?.title)
            assertTrue((restored?.lastPlayedAt ?: 0L) > entity.lastPlayedAt)
        } finally {
            database.close()
        }
    }


    @Test
    fun browserDownloadHandlerRejectsInsecureAndCredentialBearingUrls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = BrowserDownloadHandler(context)

        assertNull(handler.enqueue("http://example.com/video.mp4", null, null, "video/mp4"))
        assertNull(handler.enqueue("https://user:password@example.com/video.mp4", null, null, "video/mp4"))
    }

    @Test
    fun applicationScopedPlayerSurvivesActivityRecreation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            var originalPlayer: androidx.media3.exoplayer.ExoPlayer? = null
            scenario.onActivity { activity ->
                originalPlayer = (activity.application as PlayerApplication)
                    .playerManager.getOrCreatePlayer()
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                val recreatedPlayer = (activity.application as PlayerApplication)
                    .playerManager.getOrCreatePlayer()
                assertSame(originalPlayer, recreatedPlayer)
            }
        } finally {
            scenario.close()
        }
    }
}
