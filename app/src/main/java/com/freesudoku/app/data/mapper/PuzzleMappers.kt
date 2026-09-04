package com.freesudoku.app.data.mapper

import com.freesudoku.app.data.db.entity.PuzzleBufferEntity
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle

fun PuzzleBufferEntity.toDomain(): Puzzle = Puzzle(
    id = id,
    number = null,
    givens = GridCodec.decode(givens),
    solution = GridCodec.decode(solution),
    difficultyScore = difficultyScore,
    band = DifficultyBand.valueOf(band),
)

fun Puzzle.toBufferEntity(createdAt: Long = System.currentTimeMillis()): PuzzleBufferEntity =
    PuzzleBufferEntity(
        id = id,
        givens = GridCodec.encode(givens),
        solution = GridCodec.encode(solution),
        difficultyScore = difficultyScore,
        band = band.name,
        createdAt = createdAt,
    )
