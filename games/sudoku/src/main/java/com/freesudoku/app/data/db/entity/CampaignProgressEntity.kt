package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single row (id is always 0): where the player is in the campaign. */
@Entity(tableName = "campaign_progress")
data class CampaignProgressEntity(
    @PrimaryKey val id: Int = 0,
    val currentPuzzleNumber: Int,
    val highestCompletedNumber: Int,
    val updatedAt: Long,
)
