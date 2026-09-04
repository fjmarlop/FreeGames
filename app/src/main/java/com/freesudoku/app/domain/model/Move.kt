package com.freesudoku.app.domain.model

/** A reversible edit to the board. Each variant carries the state needed to undo it. */
sealed interface Move {
    val row: Int
    val col: Int

    data class SetValue(
        override val row: Int,
        override val col: Int,
        val value: Int,
        val previousValue: Int,
        val previousNotes: Set<Int>,
    ) : Move

    data class ToggleNote(
        override val row: Int,
        override val col: Int,
        val digit: Int,
        val added: Boolean,
    ) : Move

    data class ClearCell(
        override val row: Int,
        override val col: Int,
        val previousValue: Int,
        val previousNotes: Set<Int>,
    ) : Move
}
