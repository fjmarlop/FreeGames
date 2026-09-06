package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import kotlinx.coroutines.flow.Flow

data class BandCount(val band: String, val count: Int)

@Dao
interface CompletedPuzzleDao {

    @Insert
    suspend fun insert(entity: CompletedPuzzleEntity)

    @Query("SELECT COUNT(*) FROM completed_puzzle")
    fun totalCompleted(): Flow<Int>

    @Query("SELECT MIN(durationMs) FROM completed_puzzle")
    fun bestDurationMs(): Flow<Long?>

    @Query("SELECT AVG(durationMs) FROM completed_puzzle")
    fun averageDurationMs(): Flow<Double?>

    @Query("SELECT band AS band, COUNT(*) AS count FROM completed_puzzle GROUP BY band")
    fun countByBand(): Flow<List<BandCount>>

    @Query("SELECT completedAt FROM completed_puzzle ORDER BY completedAt DESC")
    fun completionTimestamps(): Flow<List<Long>>
}
