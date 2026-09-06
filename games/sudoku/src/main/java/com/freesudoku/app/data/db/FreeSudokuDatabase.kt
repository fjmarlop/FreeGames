package com.freesudoku.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freesudoku.app.data.db.dao.CampaignProgressDao
import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.data.db.dao.CurrentGameDao
import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import com.freesudoku.app.data.db.entity.CampaignProgressEntity
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import com.freesudoku.app.data.db.entity.PuzzleBufferEntity

@Database(
    entities = [
        PuzzleBufferEntity::class,
        CurrentGameEntity::class,
        CompletedPuzzleEntity::class,
        CampaignProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FreeSudokuDatabase : RoomDatabase() {
    abstract fun puzzleBufferDao(): PuzzleBufferDao
    abstract fun currentGameDao(): CurrentGameDao
    abstract fun completedPuzzleDao(): CompletedPuzzleDao
    abstract fun campaignProgressDao(): CampaignProgressDao
}
