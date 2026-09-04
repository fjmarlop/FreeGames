package com.freesudoku.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.data.settings.SettingsRepository
import com.freesudoku.app.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<GameSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameSettings())

    fun setMistakeLimit(v: Boolean) = viewModelScope.launch { settingsRepository.setMistakeLimitEnabled(v) }
    fun setHighlightErrors(v: Boolean) = viewModelScope.launch { settingsRepository.setHighlightErrors(v) }
    fun setHighlightSame(v: Boolean) = viewModelScope.launch { settingsRepository.setHighlightSameNumbers(v) }
    fun setAutoRemoveNotes(v: Boolean) = viewModelScope.launch { settingsRepository.setAutoRemoveNotes(v) }
    fun setThemeMode(v: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(v) }
}
