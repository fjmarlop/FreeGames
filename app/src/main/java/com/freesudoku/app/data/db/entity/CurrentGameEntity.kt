package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single row (id is always 0): the serialized in-progress game. */
@Entity(tableName = "current_game")
data class CurrentGameEntity(
    @PrimaryKey val id: Int = 0,
    val puzzleNumber: Int,
    val snapshotJson: String,
    val updatedAt: Long,
)
