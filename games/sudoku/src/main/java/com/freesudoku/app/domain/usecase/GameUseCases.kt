package com.freesudoku.app.domain.usecase

import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import com.freesudoku.app.data.repository.CampaignRepository
import com.freesudoku.app.data.repository.GameRepository
import com.freesudoku.app.data.repository.PuzzleRepository
import com.freesudoku.app.data.repository.StatsRepository
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.domain.campaign.CampaignProgress
import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.Puzzle
import com.freesudoku.app.domain.stats.PlayerStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Picks the puzzle for the player's current campaign position. */
class GetNextCampaignPuzzle @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val puzzleRepository: PuzzleRepository,
) {
    suspend operator fun invoke(): Puzzle {
        campaignRepository.ensureInitialized()
        return puzzleRepository.puzzleForCampaign(campaignRepository.currentNumber())
    }
}

/** Generates a single puzzle at [band] for "Partida rápida" — no campaign number attached. */
class GetPuzzleForBand @Inject constructor(
    private val puzzleRepository: PuzzleRepository,
) {
    suspend operator fun invoke(band: DifficultyBand): Puzzle = puzzleRepository.puzzleForBand(band)
}

/** Builds a fresh game from a puzzle, honouring the current settings, and persists it. */
class StartGame @Inject constructor(
    private val gameRepository: GameRepository,
) {
    suspend operator fun invoke(puzzle: Puzzle, settings: GameSettings): GameSnapshot {
        val snapshot = GameSnapshot(
            puzzle = puzzle,
            board = Board.fromGivens(puzzle.givens),
            undoStack = emptyList(),
            redoStack = emptyList(),
            elapsedMs = 0,
            mistakes = 0,
            hintsUsed = 0,
            status = GameStatus.IN_PROGRESS,
            mistakeLimitEnabled = settings.mistakeLimitEnabled,
        )
        gameRepository.save(snapshot)
        return snapshot
    }
}

class ResumeGame @Inject constructor(private val gameRepository: GameRepository) {
    suspend operator fun invoke(): GameSnapshot? = gameRepository.currentGame()
}

class ObserveCurrentGame @Inject constructor(private val gameRepository: GameRepository) {
    operator fun invoke(): Flow<GameSnapshot?> = gameRepository.observeCurrentGame()
}

class SaveGame @Inject constructor(private val gameRepository: GameRepository) {
    suspend operator fun invoke(snapshot: GameSnapshot) = gameRepository.save(snapshot)
}

/**
 * Records a finished puzzle and clears the in-progress game. A campaign puzzle
 * ([Puzzle.number] non-null) also advances the campaign; a "Partida rápida" puzzle
 * (`number == null`) is recorded for stats only — same [QUICK_PLAY_PUZZLE_NUMBER] sentinel
 * [GameRepository] already uses for the in-progress row.
 */
class CompletePuzzle @Inject constructor(
    private val gameRepository: GameRepository,
    private val campaignRepository: CampaignRepository,
    private val completedPuzzleDao: CompletedPuzzleDao,
) {
    suspend operator fun invoke(snapshot: GameSnapshot) {
        require(snapshot.status == GameStatus.COMPLETED) { "puzzle is not completed" }
        val number = snapshot.puzzle.number
        completedPuzzleDao.insert(
            CompletedPuzzleEntity(
                puzzleNumber = number ?: QUICK_PLAY_PUZZLE_NUMBER,
                difficultyScore = snapshot.puzzle.difficultyScore,
                band = snapshot.puzzle.band.name,
                durationMs = snapshot.elapsedMs,
                mistakes = snapshot.mistakes,
                hintsUsed = snapshot.hintsUsed,
                completedAt = System.currentTimeMillis(),
            )
        )
        if (number != null) campaignRepository.advanceAfterCompleting(number)
        gameRepository.clear()
    }

    private companion object {
        const val QUICK_PLAY_PUZZLE_NUMBER = -1
    }
}

/** Drops the in-progress game without touching the campaign. */
class AbandonGame @Inject constructor(private val gameRepository: GameRepository) {
    suspend operator fun invoke() = gameRepository.clear()
}

class ObserveCampaignProgress @Inject constructor(private val campaignRepository: CampaignRepository) {
    operator fun invoke(): Flow<CampaignProgress> = campaignRepository.observeProgress()
}

class ObservePlayerStats @Inject constructor(private val statsRepository: StatsRepository) {
    operator fun invoke(): Flow<PlayerStats> = statsRepository.observeStats()
}
