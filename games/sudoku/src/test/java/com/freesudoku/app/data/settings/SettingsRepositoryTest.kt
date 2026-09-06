package com.freesudoku.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var file: File
    private lateinit var repo: SettingsRepository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = File(ctx.filesDir, "test-${System.nanoTime()}.preferences_pb")
        repo = SettingsRepository(PreferenceDataStoreFactory.create { file })
    }

    @After fun tearDown() {
        file.delete()
    }

    @Test fun `defaults are all-on and system theme`() = runTest {
        assertThat(repo.settings.first()).isEqualTo(GameSettings())
    }

    @Test fun `each setter round trips`() = runTest {
        repo.setMistakeLimitEnabled(false)
        repo.setHighlightErrors(false)
        repo.setHighlightSameNumbers(false)
        repo.setAutoRemoveNotes(false)
        repo.setThemeMode(ThemeMode.DARK)

        assertThat(repo.settings.first()).isEqualTo(
            GameSettings(
                mistakeLimitEnabled = false,
                highlightErrors = false,
                highlightSameNumbers = false,
                autoRemoveNotes = false,
                themeMode = ThemeMode.DARK,
            )
        )
    }
}
