package com.example.data

import kotlinx.coroutines.flow.Flow

class TtsHistoryRepository(private val dao: AppDao) {
    val allHistory: Flow<List<TtsHistoryItem>> = dao.getAllHistory()
    val allClonedVoices: Flow<List<ClonedVoice>> = dao.getAllClonedVoices()

    // === History CRUD ===
    suspend fun insertHistoryItem(item: TtsHistoryItem): Long = dao.insertHistoryItem(item)
    suspend fun updateHistoryItem(item: TtsHistoryItem) = dao.updateHistoryItem(item)
    suspend fun deleteHistoryItem(id: Int) = dao.deleteHistoryItem(id)
    suspend fun clearHistory() = dao.clearHistory()

    // === Cloned Voice CRUD ===
    suspend fun insertClonedVoice(voice: ClonedVoice): Long = dao.insertClonedVoice(voice)
    suspend fun updateClonedVoice(voice: ClonedVoice) = dao.updateClonedVoice(voice)
    suspend fun deleteClonedVoice(id: Int) = dao.deleteClonedVoice(id)
    suspend fun clearClonedVoices() = dao.clearClonedVoices()
}
