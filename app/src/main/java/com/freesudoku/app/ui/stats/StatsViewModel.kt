package com.freesudoku.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.domain.usecase.ObservePlayerStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    observePlayerStats: ObservePlayerStats,
) : ViewModel() {

    val stats: StateFlow<PlayerStats> = observePlayerStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerStats.EMPTY)
}
