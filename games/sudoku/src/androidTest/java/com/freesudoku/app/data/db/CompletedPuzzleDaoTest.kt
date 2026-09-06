package com.freesudoku.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletedPuzzleDaoTest : DaoTestBase() {

    private fun row(number: Int, band: String, durationMs: Long) = CompletedPuzzleEntity(
        puzzleNumber = number, difficultyScore = 20.0, band = band,
        durationMs = durationMs, mistakes = 0, hintsUsed = 0, completedAt = number * 1000L,
    )

    @Test fun aggregates_and_grouping() = runTest {
        val dao = db.completedPuzzleDao()
        dao.insert(row(1, "FACIL", 120_000))
        dao.insert(row(2, "FACIL", 90_000))
        dao.insert(row(3, "MEDIO", 200_000))
        dao.insert(row(4, "MEDIO", 210_000))

        assertThat(dao.totalCompleted().first()).isEqualTo(4)
        assertThat(dao.bestDurationMs().first()).isEqualTo(90_000)
        assertThat(dao.averageDurationMs().first()).isWithin(1.0).of(155_000.0)

        val byBand = dao.countByBand().first().associate { it.band to it.count }
        assertThat(byBand).containsEntry("FACIL", 2)
        assertThat(byBand).containsEntry("MEDIO", 2)

        assertThat(dao.completionTimestamps().first()).isEqualTo(listOf(4000L, 3000L, 2000L, 1000L))
    }
}
