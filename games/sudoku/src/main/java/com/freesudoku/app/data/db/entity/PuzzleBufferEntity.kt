package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A pre-generated puzzle waiting to be handed to the campaign. */
@Entity(tableName = "puzzle_buffer")
data class PuzzleBufferEntity(
    @PrimaryKey val id: String,
    val givens: String,
    val solution: String,
    val difficultyScore: Double,
    val band: String,
    val createdAt: Long,
)
