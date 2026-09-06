package com.freesudoku.app.domain.model

data class BoardCell(
    val value: Int,
    val isGiven: Boolean,
    val notes: Set<Int> = emptySet(),
)

/**
 * The mutable-looking (actually copy-on-write) player board: 81 [BoardCell]s.
 * Given cells are fixed and reject edits.
 */
class Board private constructor(private val grid: List<BoardCell>) {

    fun cell(row: Int, col: Int): BoardCell = grid[row * 9 + col]

    fun withValue(row: Int, col: Int, value: Int): Board {
        val idx = row * 9 + col
        if (grid[idx].isGiven) return this
        if (grid[idx].value == value && grid[idx].notes.isEmpty()) return this
        val next = grid.toMutableList()
        next[idx] = next[idx].copy(value = value, notes = emptySet())
        return Board(next)
    }

    fun withNotes(row: Int, col: Int, notes: Set<Int>): Board {
        val idx = row * 9 + col
        if (grid[idx].isGiven || grid[idx].value != 0) return this
        val next = grid.toMutableList()
        next[idx] = next[idx].copy(notes = notes.toSortedSet())
        return Board(next)
    }

    fun withNoteToggled(row: Int, col: Int, digit: Int): Board {
        val current = cell(row, col).notes
        val updated = if (digit in current) current - digit else current + digit
        return withNotes(row, col, updated)
    }

    fun toGrid(): Grid = Grid.of(IntArray(81) { grid[it].value })

    val isComplete: Boolean get() = toGrid().isComplete

    fun emptyCells(): List<Pair<Int, Int>> = buildList {
        for (r in 0 until 9) for (c in 0 until 9) if (cell(r, c).value == 0) add(r to c)
    }

    fun cells(): List<BoardCell> = grid

    companion object {
        fun fromGivens(givens: Grid): Board = Board(
            List(81) { i ->
                val v = givens.cells[i]
                BoardCell(value = v, isGiven = v != 0)
            }
        )

        fun restore(cells: List<BoardCell>): Board {
            require(cells.size == 81) { "Board needs 81 cells, got ${cells.size}" }
            return Board(cells)
        }
    }
}
