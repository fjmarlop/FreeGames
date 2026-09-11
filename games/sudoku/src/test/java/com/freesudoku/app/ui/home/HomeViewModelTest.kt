package com.freesudoku.app.ui.home

import app.cash.turbine.test
import com.freesudoku.app.data.repository.GameRepository
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.data.settings.SettingsRepository
import com.freesudoku.app.domain.campaign.CampaignProgress
import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.domain.usecase.GetNextCampaignPuzzle
import com.freesudoku.app.domain.usecase.GetPuzzleForBand
import com.freesudoku.app.domain.usecase.ObserveCampaignProgress
import com.freesudoku.app.domain.usecase.ObservePlayerStats
import com.freesudoku.app.domain.usecase.StartGame
import com.freesudoku.app.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val puzzle = Puzzle(
        "p", 1,
        GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        10.0, DifficultyBand.FACIL,
    )

    private fun vm(
        currentGame: GameSnapshot?,
        progress: CampaignProgress = CampaignProgress.START,
        gameRepo: GameRepository = mockk(relaxed = true),
        getNext: GetNextCampaignPuzzle = mockk(),
        getPuzzleForBand: GetPuzzleForBand = mockk(),
        start: StartGame = mockk(),
    ): HomeViewModel {
        coEvery { gameRepo.currentGame() } returns currentGame
        coEvery { gameRepo.observeCurrentGame() } returns flowOf(currentGame)
        val settings = mockk<SettingsRepository>()
        every { settings.settings } returns flowOf(GameSettings())
        return HomeViewModel(
            observeCampaignProgress = ObserveCampaignProgress(mockk { every { observeProgress() } returns flowOf(progress) }),
            observePlayerStats = ObservePlayerStats(mockk { every { observeStats() } returns flowOf(PlayerStats.EMPTY) }),
            gameRepository = gameRepo,
            getNextCampaignPuzzle = getNext,
            getPuzzleForBand = getPuzzleForBand,
            startGame = start,
            settingsRepository = settings,
        )
    }

    @Test fun `resumable game just calls onReady`() = runTest {
        val snapshot = GameSnapshot(
            puzzle, Board.fromGivens(puzzle.givens), emptyList(), emptyList(),
            0, 0, 0, GameStatus.IN_PROGRESS, true,
        )
        val getNext = mockk<GetNextCampaignPuzzle>()
        val model = vm(currentGame = snapshot, getNext = getNext)

        var ready = false
        model.onPlayOrContinue { ready = true }
        advanceUntilIdle()

        assertThat(ready).isTrue()
        coVerify(exactly = 0) { getNext() }
    }

    @Test fun `no game generates and starts one before onReady`() = runTest {
        val getNext = mockk<GetNextCampaignPuzzle>()
        coEvery { getNext() } returns puzzle
        val start = mockk<StartGame>()
        coEvery { start(any(), any()) } returns mockk(relaxed = true)

        val model = vm(currentGame = null, getNext = getNext, start = start)

        var ready = false
        model.onPlayOrContinue { ready = true }
        advanceUntilIdle()

        assertThat(ready).isTrue()
        coVerify(exactly = 1) { getNext() }
        coVerify(exactly = 1) { start(puzzle, GameSettings()) }
    }

    @Test fun `state exposes current number and stats`() = runTest {
        val model = vm(currentGame = null, progress = CampaignProgress(currentNumber = 4, highestCompleted = 3))
        model.uiState.test {
            var s = awaitItem()
            while (s.loading) s = awaitItem()
            assertThat(s.currentNumber).isEqualTo(4)
            assertThat(s.hasResumableGame).isFalse()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `a resumable quick-play game shows no puzzle number`() = runTest {
        val quickPuzzle = puzzle.copy(id = "q", number = null)
        val snapshot = GameSnapshot(
            quickPuzzle, Board.fromGivens(quickPuzzle.givens), emptyList(), emptyList(),
            0, 0, 0, GameStatus.IN_PROGRESS, true,
        )
        val model = vm(currentGame = snapshot, progress = CampaignProgress(currentNumber = 4, highestCompleted = 3))
        model.uiState.test {
            var s = awaitItem()
            while (s.loading) s = awaitItem()
            assertThat(s.currentNumber).isNull()
            assertThat(s.hasResumableGame).isTrue()
            assertThat(s.currentBand).isEqualTo(DifficultyBand.FACIL)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `onStartQuickPlay generates a puzzle for the band and starts it`() = runTest {
        val quickPuzzle = puzzle.copy(id = "q", number = null)
        val getPuzzleForBand = mockk<GetPuzzleForBand>()
        coEvery { getPuzzleForBand(DifficultyBand.DIFICIL) } returns quickPuzzle
        val start = mockk<StartGame>()
        coEvery { start(any(), any()) } returns mockk(relaxed = true)
        val model = vm(currentGame = null, getPuzzleForBand = getPuzzleForBand, start = start)

        var ready = false
        model.onStartQuickPlay(DifficultyBand.DIFICIL) { ready = true }
        advanceUntilIdle()

        assertThat(ready).isTrue()
        coVerify(exactly = 1) { getPuzzleForBand(DifficultyBand.DIFICIL) }
        coVerify(exactly = 1) { start(quickPuzzle, GameSettings()) }
    }
}
