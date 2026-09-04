package com.freesudoku.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val mistakeLimit = booleanPreferencesKey("mistake_limit_enabled")
        val highlightErrors = booleanPreferencesKey("highlight_errors")
        val highlightSame = booleanPreferencesKey("highlight_same_numbers")
        val autoRemoveNotes = booleanPreferencesKey("auto_remove_notes")
        val theme = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<GameSettings> = dataStore.data.map { p ->
        GameSettings(
            mistakeLimitEnabled = p[Keys.mistakeLimit] ?: true,
            highlightErrors = p[Keys.highlightErrors] ?: true,
            highlightSameNumbers = p[Keys.highlightSame] ?: true,
            autoRemoveNotes = p[Keys.autoRemoveNotes] ?: true,
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun setMistakeLimitEnabled(value: Boolean) =
        dataStore.edit { it[Keys.mistakeLimit] = value }

    suspend fun setHighlightErrors(value: Boolean) =
        dataStore.edit { it[Keys.highlightErrors] = value }

    suspend fun setHighlightSameNumbers(value: Boolean) =
        dataStore.edit { it[Keys.highlightSame] = value }

    suspend fun setAutoRemoveNotes(value: Boolean) =
        dataStore.edit { it[Keys.autoRemoveNotes] = value }

    suspend fun setThemeMode(value: ThemeMode) =
        dataStore.edit { it[Keys.theme] = value.name }
}
