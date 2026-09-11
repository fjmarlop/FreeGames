package com.freesudoku.app.ui.game

import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameStatus

data class CellUi(
    val value: Int,
    val given: Boolean,
    val notes: Set<Int>,
    val error: Boolean,
    val inSelectionScope: Boolean,
    val sameValueAsSelection: Boolean,
    val selected: Boolean,
)

data class GameUiState(
    val loading: Boolean = true,
    val cells: List<CellUi> = emptyList(),
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val mistakeLimitEnabled: Boolean = true,
    val elapsedText: String = "0:00",
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val remainingPerDigit: Map<Int, Int> = emptyMap(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** Null for "Partida rápida" — the puzzle has no campaign position. */
    val puzzleNumber: Int? = null,
    val hintsUsed: Int = 0,
    val band: DifficultyBand? = null,
) {
    companion object {
        const val MISTAKE_LIMIT = 3
    }
}
