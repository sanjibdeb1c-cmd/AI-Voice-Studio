package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // === TTS History Queries ===
    @Query("SELECT * FROM tts_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TtsHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: TtsHistoryItem): Long

    @Update
    suspend fun updateHistoryItem(item: TtsHistoryItem)

    @Query("DELETE FROM tts_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM tts_history")
    suspend fun clearHistory()

    // === Cloned Voice Queries ===
    @Query("SELECT * FROM cloned_voices ORDER BY timestamp DESC")
    fun getAllClonedVoices(): Flow<List<ClonedVoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClonedVoice(voice: ClonedVoice): Long

    @Update
    suspend fun updateClonedVoice(voice: ClonedVoice)

    @Query("DELETE FROM cloned_voices WHERE id = :id")
    suspend fun deleteClonedVoice(id: Int)

    @Query("DELETE FROM cloned_voices")
    suspend fun clearClonedVoices()
}
