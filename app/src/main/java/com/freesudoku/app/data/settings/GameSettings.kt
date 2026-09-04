package com.freesudoku.app.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class GameSettings(
    val mistakeLimitEnabled: Boolean = true,
    val highlightErrors: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
