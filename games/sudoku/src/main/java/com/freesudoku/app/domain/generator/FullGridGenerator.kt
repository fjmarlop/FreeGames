package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.model.Grid
import kotlin.random.Random

/** Produces a complete, valid 9x9 solution grid via randomized backtracking. */
class FullGridGenerator(private val random: Random = Random.Default) {

    fun generate(): Grid {
        val cells = IntArray(81)
        check(fill(cells, 0)) { "backtracking failed to fill a full grid" }
        return Grid.of(cells)
    }

    private fun fill(cells: IntArray, index: Int): Boolean {
        if (index == 81) return true
        val row = index / 9
        val col = index % 9
        for (v in (1..9).shuffled(random)) {
            if (canPlace(cells, row, col, v)) {
                cells[index] = v
                if (fill(cells, index + 1)) return true
                cells[index] = 0
            }
        }
        return false
    }

    private fun canPlace(cells: IntArray, row: Int, col: Int, v: Int): Boolean {
        for (c in 0 until 9) if (cells[row * 9 + c] == v) return false
        for (r in 0 until 9) if (cells[r * 9 + col] == v) return false
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if (cells[r * 9 + c] == v) return false
        }
        return true
    }
}
