@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.freesudoku.app.ui.game

import androidx.lifecycle.SavedStateHandle
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.data.settings.SettingsRepository
import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.freesudoku.app.domain.usecase.AbandonGame
import com.freesudoku.app.domain.usecase.CompletePuzzle
import com.freesudoku.app.domain.usecase.GetNextCampaignPuzzle
import com.freesudoku.app.domain.usecase.ResumeGame
import com.freesudoku.app.domain.usecase.SaveGame
import com.freesudoku.app.domain.usecase.StartGame
import com.freesudoku.app.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val puzzle = Puzzle(
        "p", 5,
        GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        14.0, DifficultyBand.FACIL,
    )

    private fun freshSnapshot(limit: Boolean = true) = GameSnapshot(
        puzzle, Board.fromGivens(puzzle.givens), emptyList(), emptyList(),
        elapsedMs = 0, mistakes = 0, hintsUsed = 0,
        status = GameStatus.IN_PROGRESS, mistakeLimitEnabled = limit,
    )

    private fun build(
        resume: GameSnapshot? = freshSnapshot(),
        saveGame: SaveGame = mockk(relaxed = true),
        completePuzzle: CompletePuzzle = mockk(relaxed = true),
        startGame: StartGame = mockk(relaxed = true),
        getNext: GetNextCampaignPuzzle = mockk(relaxed = true),
    ): GameViewModel {
        val resumeGame = mockk<ResumeGame>()
        coEvery { resumeGame() } returns resume
        val settings = mockk<SettingsRepository>()
        io.mockk.every { settings.settings } returns flowOf(GameSettings())
        return GameViewModel(
            resumeGame = resumeGame,
            getNextCampaignPuzzle = getNext,
            startGame = startGame,
            saveGame = saveGame,
            completePuzzle = completePuzzle,
            abandonGame = mockk<AbandonGame>().also { coEvery { it() } just Runs },
            settingsRepository = settings,
            ticker = Ticker { emptyFlow() },
            appScope = CoroutineScope(mainRule.dispatcher),
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun emptyIndex(vm: GameViewModel): Int =
        vm.uiState.value.cells.indexOfFirst { !it.given && it.value == 0 }

    @Test fun `resumes an existing snapshot without generating`() = runTest {
        val getNext = mockk<GetNextCampaignPuzzle>(relaxed = true)
        val vm = build(resume = freshSnapshot(), getNext = getNext)
        assertThat(vm.uiState.value.loading).isFalse()
        assertThat(vm.uiState.value.puzzleNumber).isEqualTo(5)
        coVerify(exactly = 0) { getNext() }
    }

    @Test fun `correct number input fills the cell with no mistake`() = runTest {
        val vm = build()
        vm.onCellTap(2) // (0,2), solution digit 4
        vm.onNumberInput(4)
        assertThat(vm.uiState.value.cells[2].value).isEqualTo(4)
        assertThat(vm.uiState.value.mistakes).isEqualTo(0)
    }

    @Test fun `three wrong inputs fail the game in limit mode`() = runTest {
        val vm = build(resume = freshSnapshot(limit = true))
        vm.onCellTap(2) // solution is 4
        vm.onNumberInput(1)
        vm.onNumberInput(2)
        vm.onNumberInput(3)
        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.FAILED)
    }

    @Test fun `notes mode writes a pencil mark`() = runTest {
        val vm = build()
        val idx = emptyIndex(vm)
        vm.onToggleNotesMode()
        vm.onCellTap(idx)
        vm.onNumberInput(7)
        assertThat(vm.uiState.value.cells[idx].notes).contains(7)
        assertThat(vm.uiState.value.cells[idx].value).isEqualTo(0)
    }

    @Test fun `undo reverts the last move`() = runTest {
        val vm = build()
        vm.onCellTap(2)
        vm.onNumberInput(4)
        vm.onUndo()
        assertThat(vm.uiState.value.cells[2].value).isEqualTo(0)
    }

    @Test fun `onRetry restarts the same puzzle from scratch mid-game`() = runTest {
        val startGame = mockk<StartGame>()
        coEvery { startGame(any(), any()) } returns freshSnapshot()
        val vm = build(startGame = startGame)

        vm.onCellTap(2)
        vm.onNumberInput(4) // correct
        vm.onCellTap(3)
        vm.onNumberInput(9) // wrong -> a mistake

        assertThat(vm.uiState.value.mistakes).isEqualTo(1)

        vm.onRetry()
        advanceUntilIdle()

        coVerify(exactly = 1) { startGame(puzzle, any()) }
        assertThat(vm.uiState.value.mistakes).isEqualTo(0)
        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(vm.uiState.value.canUndo).isFalse()
        assertThat(vm.uiState.value.cells[2].value).isEqualTo(0)
    }

    @Test fun `completing the board marks COMPLETED but does not record until advance`() = runTest {
        val complete = mockk<CompletePuzzle>(relaxed = true)
        val vm = build(completePuzzle = complete)
        for (i in 0 until 81) {
            if (vm.uiState.value.cells[i].value == 0) {
                vm.onCellTap(i)
                vm.onNumberInput(puzzle.solution.valueAt(i / 9, i % 9))
            }
        }
        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.COMPLETED)
        coVerify(exactly = 0) { complete(any()) }

        vm.onAdvance {}
        coVerify(exactly = 1) { complete(any()) }
    }

    @Test fun `finishing via Volver a Home also records the completion`() = runTest {
        val complete = mockk<CompletePuzzle>(relaxed = true)
        val vm = build(completePuzzle = complete)
        for (i in 0 until 81) {
            if (vm.uiState.value.cells[i].value == 0) {
                vm.onCellTap(i)
                vm.onNumberInput(puzzle.solution.valueAt(i / 9, i % 9))
            }
        }
        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.COMPLETED)

        var exited = false
        vm.onFinishAndGoHome { exited = true }

        coVerify(exactly = 1) { complete(any()) }
        assertThat(exited).isTrue()
    }

    @Test fun `finishing cancels the pending debounced save so it cannot resurrect the old game`() = runTest {
        // Regression: a mutation schedules a 1s-debounced save; if it fires *after*
        // onFinishAndGoHome clears the game, it silently re-inserts the completed snapshot
        // as a "current game" (reproduced manually: Home kept offering "Continuar" on a
        // puzzle that had already been recorded as completed).
        val saveGame = mockk<SaveGame>(relaxed = true)
        val complete = mockk<CompletePuzzle>(relaxed = true)
        val vm = build(saveGame = saveGame, completePuzzle = complete)
        for (i in 0 until 81) {
            if (vm.uiState.value.cells[i].value == 0) {
                vm.onCellTap(i)
                vm.onNumberInput(puzzle.solution.valueAt(i / 9, i % 9))
            }
        }
        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.COMPLETED)

        vm.onFinishAndGoHome {}
        advanceUntilIdle()

        coVerify(exactly = 1) { complete(any()) }
        coVerify(exactly = 0) { saveGame(any()) }
    }

    @Test fun `onPause firing after Volver a Home does not resurrect the completed game`() = runTest {
        // Regression (found by manual on-device testing): onFinishAndGoHome's onExit callback
        // pops the back stack, which delivers GameScreen's ON_PAUSE to *this* ViewModel before it
        // is torn down -> onPause() -> flushSaveNow(). That is deterministic, not a rare race, and
        // it re-saved the just-completed (and just-cleared) snapshot as the "current game" every
        // single time. canPersist must still be false when that flush runs.
        val saveGame = mockk<SaveGame>(relaxed = true)
        val complete = mockk<CompletePuzzle>(relaxed = true)
        val vm = build(saveGame = saveGame, completePuzzle = complete)
        for (i in 0 until 81) {
            if (vm.uiState.value.cells[i].value == 0) {
                vm.onCellTap(i)
                vm.onNumberInput(puzzle.solution.valueAt(i / 9, i % 9))
            }
        }

        vm.onFinishAndGoHome {}
        advanceUntilIdle()
        vm.onPause() // simulates the ON_PAUSE the exit navigation triggers
        advanceUntilIdle()

        coVerify(exactly = 1) { complete(any()) }
        coVerify(exactly = 0) { saveGame(any()) }
    }

    @Test fun `advancing to the next puzzle resumes normal saving`() = runTest {
        val saveGame = mockk<SaveGame>(relaxed = true)
        val getNext = mockk<GetNextCampaignPuzzle>()
        val startGame = mockk<StartGame>()
        coEvery { getNext() } returns puzzle
        coEvery { startGame(puzzle, any()) } returns freshSnapshot()
        val vm = build(saveGame = saveGame, getNext = getNext, startGame = startGame)
        for (i in 0 until 81) {
            if (vm.uiState.value.cells[i].value == 0) {
                vm.onCellTap(i)
                vm.onNumberInput(puzzle.solution.valueAt(i / 9, i % 9))
            }
        }
        vm.onAdvance {}
        advanceUntilIdle()

        // the new puzzle is a fresh in-progress game; pausing now must still persist it
        vm.onPause()
        advanceUntilIdle()

        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.IN_PROGRESS)
        coVerify(atLeast = 1) { saveGame(any()) }
    }

    @Test fun `a second tap on Volver a Home while one is in flight is a no-op`() = runTest {
        val complete = mockk<CompletePuzzle>()
        // a real suspend point (not an instantly-returning relaxed mock) so the first call is
        // still in flight when the second one lands, exercising the re-entrancy guard.
        coEvery { complete(any()) } coAnswers { kotlinx.coroutines.delay(100) }
        val vm = build(completePuzzle = complete)
        for (i in 0 until 81) {
            if (vm.uiState.value.cells[i].value == 0) {
                vm.onCellTap(i)
                vm.onNumberInput(puzzle.solution.valueAt(i / 9, i % 9))
            }
        }
        vm.onFinishAndGoHome {}
        vm.onFinishAndGoHome {}
        advanceUntilIdle()

        coVerify(exactly = 1) { complete(any()) }
    }

    @Test fun `leaving mid-game does not record a completion`() = runTest {
        val complete = mockk<CompletePuzzle>(relaxed = true)
        val vm = build(completePuzzle = complete)
        vm.onCellTap(2)
        vm.onNumberInput(4)

        var exited = false
        vm.onExitRequested { exited = true }

        coVerify(exactly = 0) { complete(any()) }
        assertThat(exited).isTrue()
    }
}
