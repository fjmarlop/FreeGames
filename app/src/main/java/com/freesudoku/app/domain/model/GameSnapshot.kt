package com.freesudoku.app.domain.model

enum class GameStatus { IN_PROGRESS, COMPLETED, FAILED }

/**
 * The single source of truth for one in-progress game. Pure data — persistence is the data
 * layer's job (Plan 2), gameplay rules are [com.freesudoku.app.domain.game.GameEngine]'s (Plan 1).
 */
data class GameSnapshot(
    val puzzle: Puzzle,
    val board: Board,
    val undoStack: List<Move>,
    val redoStack: List<Move>,
    val elapsedMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val status: GameStatus,
    val mistakeLimitEnabled: Boolean,
) {
    companion object {
        const val MISTAKE_LIMIT = 3
    }
}
