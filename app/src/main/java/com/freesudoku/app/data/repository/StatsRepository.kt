package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.domain.stats.computeStreaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val dao: CompletedPuzzleDao,
) {
    fun observeStats(): Flow<PlayerStats> = combine(
        dao.totalCompleted(),
        dao.bestDurationMs(),
        dao.averageDurationMs(),
        dao.countByBand(),
        dao.completionTimestamps(),
    ) { total, best, avg, byBand, timestamps ->
        val zoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() + zoneOffset)
        val streaks = computeStreaks(timestamps, todayEpochDay, zoneOffset)

        PlayerStats(
            totalCompleted = total,
            bestDurationMs = best,
            averageDurationMs = avg?.roundToLong(),
            currentStreakDays = streaks.current,
            longestStreakDays = streaks.longest,
            completedByBand = byBand.mapNotNull { row ->
                runCatching { DifficultyBand.valueOf(row.band) }.getOrNull()?.let { it to row.count }
            }.toMap(),
        )
    }
}
