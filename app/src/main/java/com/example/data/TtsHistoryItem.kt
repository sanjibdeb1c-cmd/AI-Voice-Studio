package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_history")
data class TtsHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val engine: String, // "NATIVE", "GEMINI", "CLONED"
    val voiceName: String,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val volume: Float = 1.0f,
    val stability: Float = 0.75f,
    val similarity: Float = 0.75f,
    val styleExaggeration: Float = 0.0f,
    val emotion: String = "Neutral",
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val format: String = "MP3",
    val quality: String = "320 kbps",
    val characterCount: Int = 0,
    val wordCount: Int = 0,
    val folderName: String = "All",
    val isFavorite: Boolean = false,
    val enhancedPath: String? = null
)
