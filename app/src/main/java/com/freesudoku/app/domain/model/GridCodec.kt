package com.freesudoku.app.domain.model

/** Serializes a [Grid] to/from an 81-character string. '0' or '.' = empty. */
object GridCodec {

    fun decode(text: String): Grid {
        require(text.length == 81) { "Grid string must be 81 chars, got ${text.length}" }
        val cells = IntArray(81)
        for (i in text.indices) {
            val ch = text[i]
            cells[i] = when {
                ch == '.' || ch == '0' -> 0
                ch in '1'..'9' -> ch - '0'
                else -> throw IllegalArgumentException("Invalid grid char '$ch' at index $i")
            }
        }
        return Grid.of(cells)
    }

    fun encode(grid: Grid): String = buildString(81) {
        grid.cells.forEach { append(if (it == 0) '0' else '0' + it) }
    }
}
