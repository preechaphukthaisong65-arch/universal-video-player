package com.preechaphukthaisong65.universalvideoplayer.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val lastPositionMs: Long,
    val lastPlayedAt: Long
)

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC")
    fun observeAll(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE uri = :uri LIMIT 1")
    suspend fun find(uri: String): PlaybackHistoryEntity?

    @Query(
        "SELECT * FROM playback_history " +
            "WHERE title LIKE '%' || :query || '%' OR uri LIKE '%' || :query || '%' " +
            "ORDER BY lastPlayedAt DESC"
    )
    fun search(query: String): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaybackHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: PlaybackHistoryEntity)

    @Query("UPDATE playback_history SET title = :title, lastPlayedAt = :playedAt WHERE uri = :uri")
    suspend fun touch(uri: String, title: String, playedAt: Long)

    @Query(
        "UPDATE playback_history SET title = :title, lastPositionMs = :positionMs, lastPlayedAt = :playedAt " +
            "WHERE uri = :uri"
    )
    suspend fun updateProgress(uri: String, title: String, positionMs: Long, playedAt: Long): Int

    @Query("DELETE FROM playback_history WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}

@Database(entities = [PlaybackHistoryEntity::class], version = 1, exportSchema = false)
abstract class PlayerDatabase : RoomDatabase() {
    abstract fun historyDao(): PlaybackHistoryDao
}
