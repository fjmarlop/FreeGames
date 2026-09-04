package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.Grid

/**
 * Mutable candidate model used while solving. [values] holds fixed digits (0 = empty).
 * [candidates] is a bitmask per cell; bit (d-1) set means digit d is still possible there.
 */
class SolverState private constructor(
    val values: IntArray,
    val candidates: IntArray,
) {
    fun copy(): SolverState = SolverState(values.copyOf(), candidates.copyOf())

    fun isSolved(): Boolean = values.none { it == 0 }

    fun candidateList(index: Int): List<Int> =
        (1..9).filter { candidates[index] and (1 shl (it - 1)) != 0 }

    fun candidateCount(index: Int): Int = Integer.bitCount(candidates[index])

    /** Assigns [digit] to [index], propagating eliminations. Returns false on contradiction. */
    fun assign(index: Int, digit: Int): Boolean {
        for (d in candidateList(index)) {
            if (d != digit && !eliminate(index, d)) return false
        }
        return true
    }

    /** Removes [digit] from [index]'s candidates, cascading singles to peers. False on contradiction. */
    fun eliminate(index: Int, digit: Int): Boolean {
        val bit = 1 shl (digit - 1)
        if (candidates[index] and bit == 0) return true
        candidates[index] = candidates[index] and bit.inv()
        return when (candidateCount(index)) {
            0 -> false
            1 -> {
                val last = candidateList(index).first()
                place(index, last)
                PEERS[index].all { eliminate(it, last) }
            }
            else -> true
        }
    }

    private fun place(index: Int, digit: Int) {
        values[index] = digit
        candidates[index] = 1 shl (digit - 1)
    }

    fun toGrid(): Grid = Grid.of(values)

    companion object {
        val PEERS: Array<IntArray> = Array(81) { idx ->
            val row = idx / 9
            val col = idx % 9
            val set = LinkedHashSet<Int>()
            for (c in 0 until 9) if (c != col) set += row * 9 + c
            for (r in 0 until 9) if (r != row) set += r * 9 + col
            val br = (row / 3) * 3
            val bc = (col / 3) * 3
            for (r in br until br + 3) for (c in bc until bc + 3) {
                val p = r * 9 + c
                if (p != idx) set += p
            }
            set.toIntArray()
        }

        /** All 27 units (9 rows, 9 columns, 9 boxes). Rows are [0,8], columns [9,17], boxes [18,26]. */
        val UNITS: Array<IntArray> = buildList {
            for (r in 0 until 9) add(IntArray(9) { r * 9 + it })
            for (c in 0 until 9) add(IntArray(9) { it * 9 + c })
            for (b in 0 until 9) {
                val br = (b / 3) * 3
                val bc = (b % 3) * 3
                add(IntArray(9) { k -> (br + k / 3) * 9 + (bc + k % 3) })
            }
        }.toTypedArray()

        fun peers(index: Int): IntArray = PEERS[index]

        fun from(grid: Grid): SolverState? {
            val state = SolverState(IntArray(81), IntArray(81) { 0x1FF })
            for (i in 0 until 81) {
                val v = grid.cells[i]
                if (v != 0) {
                    if (state.candidates[i] and (1 shl (v - 1)) == 0) return null
                    state.place(i, v)
                }
            }
            for (i in 0 until 81) {
                val v = state.values[i]
                if (v != 0 && !PEERS[i].all { state.eliminate(it, v) }) return null
            }
            return state
        }
    }
}
