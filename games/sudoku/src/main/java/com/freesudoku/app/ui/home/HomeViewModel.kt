package com.freesudoku.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freesudoku.app.data.repository.GameRepository
import com.freesudoku.app.data.settings.SettingsRepository
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.domain.usecase.GetNextCampaignPuzzle
import com.freesudoku.app.domain.usecase.GetPuzzleForBand
import com.freesudoku.app.domain.usecase.ObserveCampaignProgress
import com.freesudoku.app.domain.usecase.ObservePlayerStats
import com.freesudoku.app.domain.usecase.StartGame
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    /** Null when the resumable game (if any) is a "Partida rápida" — no campaign position. */
    val currentNumber: Int? = 1,
    val currentBand: DifficultyBand? = null,
    val hasResumableGame: Boolean = false,
    val stats: PlayerStats = PlayerStats.EMPTY,
    val preparingPuzzle: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeCampaignProgress: ObserveCampaignProgress,
    observePlayerStats: ObservePlayerStats,
    private val gameRepository: GameRepository,
    private val getNextCampaignPuzzle: GetNextCampaignPuzzle,
    private val getPuzzleForBand: GetPuzzleForBand,
    private val startGame: StartGame,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val preparing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        observeCampaignProgress(),
        observePlayerStats(),
        gameRepository.observeCurrentGame(),
        preparing,
    ) { progress, stats, currentGame, isPreparing ->
        HomeUiState(
            loading = false,
            // null only when there IS a resumable game and it's a quick-play one (no number).
            currentNumber = if (currentGame != null) currentGame.puzzle.number else progress.currentNumber,
            currentBand = currentGame?.puzzle?.band,
            hasResumableGame = currentGame != null,
            stats = stats,
            preparingPuzzle = isPreparing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Continues an in-progress game if one exists, otherwise generates + starts the next one. */
    fun onPlayOrContinue(onReady: () -> Unit) {
        if (preparing.value) return
        viewModelScope.launch {
            val hasGame = gameRepository.currentGame() != null
            if (!hasGame) {
                preparing.value = true
                try {
                    val puzzle = getNextCampaignPuzzle()
                    startGame(puzzle, settingsRepository.settings.first())
                } finally {
                    preparing.value = false
                }
            }
            onReady()
        }
    }

    /**
     * "Partida rápida": generates a single puzzle at [band] (no campaign number) and starts it,
     * replacing whatever is in the current-game slot. Any confirm-you'll-lose-progress prompt
     * lives in the UI — this always proceeds when called.
     */
    fun onStartQuickPlay(band: DifficultyBand, onReady: () -> Unit) {
        if (preparing.value) return
        viewModelScope.launch {
            preparing.value = true
            try {
                val puzzle = getPuzzleForBand(band)
                startGame(puzzle, settingsRepository.settings.first())
            } finally {
                preparing.value = false
            }
            onReady()
        }
    }
}
