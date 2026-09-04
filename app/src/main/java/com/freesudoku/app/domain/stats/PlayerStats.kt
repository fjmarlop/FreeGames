package com.freesudoku.app.domain.stats

import com.freesudoku.app.domain.model.DifficultyBand

data class PlayerStats(
    val totalCompleted: Int,
    val bestDurationMs: Long?,
    val averageDurationMs: Long?,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val completedByBand: Map<DifficultyBand, Int>,
) {
    companion object {
        val EMPTY = PlayerStats(0, null, null, 0, 0, emptyMap())
    }
}
