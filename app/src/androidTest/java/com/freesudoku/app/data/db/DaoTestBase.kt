package com.freesudoku.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before

abstract class DaoTestBase {

    protected lateinit var db: FreeSudokuDatabase

    @Before fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FreeSudokuDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun closeDb() {
        db.close()
    }
}
