package com.freesudoku.app.di

import android.content.Context
import androidx.room.Room
import com.freesudoku.app.data.db.FreeSudokuDatabase
import com.freesudoku.app.data.db.dao.CampaignProgressDao
import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.data.db.dao.CurrentGameDao
import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FreeSudokuDatabase =
        Room.databaseBuilder(context, FreeSudokuDatabase::class.java, "freesudoku.db")
            .build()

    @Provides
    fun puzzleBufferDao(db: FreeSudokuDatabase): PuzzleBufferDao = db.puzzleBufferDao()

    @Provides
    fun currentGameDao(db: FreeSudokuDatabase): CurrentGameDao = db.currentGameDao()

    @Provides
    fun completedPuzzleDao(db: FreeSudokuDatabase): CompletedPuzzleDao = db.completedPuzzleDao()

    @Provides
    fun campaignProgressDao(db: FreeSudokuDatabase): CampaignProgressDao = db.campaignProgressDao()
}
