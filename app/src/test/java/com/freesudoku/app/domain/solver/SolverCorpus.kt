package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.generator.FullGridGenerator
import com.freesudoku.app.domain.generator.PuzzleCarver
import com.freesudoku.app.domain.model.Grid
import kotlin.random.Random

/** A shared bag of realistic (givens, solution) pairs for solver/rater tests. */
object SolverCorpus {

    data class Case(val givens: Grid, val solution: Grid)

    /** [count] deterministically carved puzzles. */
    fun generate(count: Int, seed: Long = 20260904L): List<Case> {
        val rng = Random(seed)
        return (0 until count).map {
            val full = FullGridGenerator(rng).generate()
            val carved = PuzzleCarver(rng).carve(full)
            Case(carved.givens, carved.solution)
        }
    }

    val EASY_GIVENS: Grid = Grid.of(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
            .map { it - '0' }.toIntArray()
    )
    val EASY_SOLUTION: Grid = Grid.of(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
            .map { it - '0' }.toIntArray()
    )
}
