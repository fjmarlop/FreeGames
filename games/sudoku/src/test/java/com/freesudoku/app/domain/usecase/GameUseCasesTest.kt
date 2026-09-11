package com.freesudoku.app.domain.usecase

import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import com.freesudoku.app.data.repository.CampaignRepository
import com.freesudoku.app.data.repository.GameRepository
import com.freesudoku.app.data.repository.PuzzleRepository
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.domain.game.GameEngine
import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GameUseCasesTest {

    private val puzzle = Puzzle(
        id = "p", number = 7,
        givens = GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        solution = GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        difficultyScore = 22.0, band = DifficultyBand.FACIL,
    )

    private val quickPuzzle = puzzle.copy(id = "q", number = null)

    private fun completedSnapshot(p: Puzzle = puzzle): GameSnapshot {
        val engine = GameEngine()
        var s = GameSnapshot(
            p, Board.fromGivens(p.givens), emptyList(), emptyList(),
            elapsedMs = 90_000, mistakes = 1, hintsUsed = 2,
            status = GameStatus.IN_PROGRESS, mistakeLimitEnabled = true,
        )
        for (r in 0 until 9) for (c in 0 until 9) {
            if (s.board.cell(r, c).value == 0) s = engine.setValue(s, r, c, p.solution.valueAt(r, c))
        }
        check(s.status == GameStatus.COMPLETED)
        return s
    }

    @Test fun `StartGame applies the mistake-limit setting and persists`() = runTest {
        val gameRepo = mockk<GameRepository>()
        coEvery { gameRepo.save(any()) } just Runs

        val relaxed = StartGame(gameRepo).invoke(puzzle, GameSettings(mistakeLimitEnabled = false))

        assertThat(relaxed.mistakeLimitEnabled).isFalse()
        assertThat(relaxed.status).isEqualTo(GameStatus.IN_PROGRESS)
        coVerify(exactly = 1) { gameRepo.save(relaxed) }
    }

    @Test fun `CompletePuzzle records history, advances campaign once, clears game`() = runTest {
        val gameRepo = mockk<GameRepository>(relaxed = true)
        val campaignRepo = mockk<CampaignRepository>(relaxed = true)
        val dao = mockk<CompletedPuzzleDao>()
        val entity = slot<CompletedPuzzleEntity>()
        coEvery { dao.insert(capture(entity)) } just Runs

        CompletePuzzle(gameRepo, campaignRepo, dao).invoke(completedSnapshot())

        assertThat(entity.captured.puzzleNumber).isEqualTo(7)
        assertThat(entity.captured.durationMs).isEqualTo(90_000)
        coVerify(exactly = 1) { campaignRepo.advanceAfterCompleting(7) }
        coVerify(exactly = 1) { gameRepo.clear() }
    }

    @Test fun `CompletePuzzle records a quick-play completion without advancing the campaign`() = runTest {
        val gameRepo = mockk<GameRepository>(relaxed = true)
        val campaignRepo = mockk<CampaignRepository>(relaxed = true)
        val dao = mockk<CompletedPuzzleDao>()
        val entity = slot<CompletedPuzzleEntity>()
        coEvery { dao.insert(capture(entity)) } just Runs

        CompletePuzzle(gameRepo, campaignRepo, dao).invoke(completedSnapshot(quickPuzzle))

        assertThat(entity.captured.puzzleNumber).isEqualTo(-1)
        coVerify(exactly = 0) { campaignRepo.advanceAfterCompleting(any()) }
        coVerify(exactly = 1) { gameRepo.clear() }
    }

    @Test fun `CompletePuzzle rejects a non-completed snapshot`() = runTest {
        val open = GameSnapshot(
            puzzle, Board.fromGivens(puzzle.givens), emptyList(), emptyList(),
            0, 0, 0, GameStatus.IN_PROGRESS, mistakeLimitEnabled = true,
        )
        var threw = false
        try {
            CompletePuzzle(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)).invoke(open)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test fun `GetPuzzleForBand asks the puzzle repo for that band`() = runTest {
        val puzzleRepo = mockk<PuzzleRepository>()
        coEvery { puzzleRepo.puzzleForBand(DifficultyBand.DIFICIL) } returns quickPuzzle

        val result = GetPuzzleForBand(puzzleRepo).invoke(DifficultyBand.DIFICIL)

        assertThat(result).isEqualTo(quickPuzzle)
        coVerify(exactly = 1) { puzzleRepo.puzzleForBand(DifficultyBand.DIFICIL) }
    }

    @Test fun `GetNextCampaignPuzzle initializes progress then asks the puzzle repo`() = runTest {
        val campaignRepo = mockk<CampaignRepository>()
        val puzzleRepo = mockk<PuzzleRepository>()
        coEvery { campaignRepo.ensureInitialized() } just Runs
        coEvery { campaignRepo.currentNumber() } returns 7
        coEvery { puzzleRepo.puzzleForCampaign(7) } returns puzzle

        val result = GetNextCampaignPuzzle(campaignRepo, puzzleRepo).invoke()

        assertThat(result).isEqualTo(puzzle)
        coVerify { campaignRepo.ensureInitialized() }
        coVerify { puzzleRepo.puzzleForCampaign(7) }
    }
}
