package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** History: one row per successfully completed puzzle. */
@Entity(tableName = "completed_puzzle")
data class CompletedPuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleNumber: Int,
    val difficultyScore: Double,
    val band: String,
    val durationMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val completedAt: Long,
)
