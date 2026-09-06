package com.freesudoku.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrentGameDaoTest : DaoTestBase() {

    private fun entity(json: String) = CurrentGameEntity(0, puzzleNumber = 1, snapshotJson = json, updatedAt = 0)

    @Test fun upsert_replaces_single_row_and_clear_empties() = runTest {
        val dao = db.currentGameDao()
        dao.upsert(entity("""{"a":1}"""))
        assertThat(dao.observe().first()?.snapshotJson).isEqualTo("""{"a":1}""")

        dao.upsert(entity("""{"a":2}"""))
        assertThat(dao.get()?.snapshotJson).isEqualTo("""{"a":2}""")

        dao.clear()
        assertThat(dao.get()).isNull()
    }
}
