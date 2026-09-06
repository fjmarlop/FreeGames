package com.freesudoku.app.ui.stats

import com.freesudoku.app.data.repository.StatsRepository
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.domain.usecase.ObservePlayerStats
import app.cash.turbine.test
import com.freesudoku.app.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StatsViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    @Test fun `exposes mapped stats from the repository`() = runTest {
        val stats = PlayerStats(
            totalCompleted = 9,
            bestDurationMs = 60_000,
            averageDurationMs = 120_000,
            currentStreakDays = 3,
            longestStreakDays = 5,
            completedByBand = mapOf(DifficultyBand.FACIL to 6, DifficultyBand.MEDIO to 3),
        )
        val repo = mockk<StatsRepository>()
        every { repo.observeStats() } returns flowOf(stats)

        val vm = StatsViewModel(ObservePlayerStats(repo))

        vm.stats.test {
            var s = awaitItem()
            if (s == PlayerStats.EMPTY) s = awaitItem()
            assertThat(s).isEqualTo(stats)
            cancelAndConsumeRemainingEvents()
        }
    }
}
