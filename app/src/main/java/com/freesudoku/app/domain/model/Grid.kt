package com.freesudoku.app.domain.model

/**
 * Immutable 9x9 Sudoku grid. 0 = empty, 1..9 = a placed digit.
 * Row-major storage: index = row * 9 + col.
 */
class Grid private constructor(val cells: IntArray) {

    init { require(cells.size == 81) { "Grid needs 81 cells, got ${cells.size}" } }

    fun valueAt(row: Int, col: Int): Int = cells[row * 9 + col]

    fun withValue(row: Int, col: Int, value: Int): Grid {
        require(value in 0..9) { "value must be 0..9, got $value" }
        val copy = cells.copyOf()
        copy[row * 9 + col] = value
        return Grid(copy)
    }

    val filledCount: Int get() = cells.count { it != 0 }

    val isFull: Boolean get() = cells.none { it == 0 }

    val isComplete: Boolean
        get() {
            if (!isFull) return false
            for (i in 0 until 9) {
                if (rowDigits(i) != FULL_SET) return false
                if (colDigits(i) != FULL_SET) return false
                if (boxDigits(i) != FULL_SET) return false
            }
            return true
        }

    /** True if placing [value] at ([row], [col]) breaks no row/column/box constraint. 0 is always valid. */
    fun isValidPlacement(row: Int, col: Int, value: Int): Boolean {
        if (value == 0) return true
        for (c in 0 until 9) if (c != col && valueAt(row, c) == value) return false
        for (r in 0 until 9) if (r != row && valueAt(r, col) == value) return false
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if ((r != row || c != col) && valueAt(r, c) == value) return false
        }
        return true
    }

    private fun rowDigits(row: Int): Set<Int> = (0 until 9).map { valueAt(row, it) }.toSet()

    private fun colDigits(col: Int): Set<Int> = (0 until 9).map { valueAt(it, col) }.toSet()

    private fun boxDigits(box: Int): Set<Int> {
        val br = (box / 3) * 3
        val bc = (box % 3) * 3
        val out = HashSet<Int>()
        for (r in br until br + 3) for (c in bc until bc + 3) out += valueAt(r, c)
        return out
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Grid && cells.contentEquals(other.cells))

    override fun hashCode(): Int = cells.contentHashCode()

    override fun toString(): String = GridCodec.encode(this)

    companion object {
        private val FULL_SET = (1..9).toSet()

        fun empty(): Grid = Grid(IntArray(81))

        fun of(cells: IntArray): Grid = Grid(cells.copyOf())

        fun boxIndex(row: Int, col: Int): Int = (row / 3) * 3 + (col / 3)
    }
}
