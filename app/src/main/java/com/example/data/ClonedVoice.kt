package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloned_voices")
data class ClonedVoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val filePath: String, // Path to the reference audio
    val qualityScore: Int = 92,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isNoiseCleaned: Boolean = false,
    val isSilenceRemoved: Boolean = false,
    val folderName: String = "All"
)
