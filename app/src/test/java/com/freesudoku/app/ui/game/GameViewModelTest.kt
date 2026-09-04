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
}
