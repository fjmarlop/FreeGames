package com.freesudoku.app.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.data.settings.SettingsRepository
import com.freesudoku.app.di.ApplicationScope
import com.freesudoku.app.domain.game.GameEngine
import com.freesudoku.app.domain.model.Grid
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.usecase.AbandonGame
import com.freesudoku.app.domain.usecase.CompletePuzzle
import com.freesudoku.app.domain.usecase.GetNextCampaignPuzzle
import com.freesudoku.app.domain.usecase.ResumeGame
import com.freesudoku.app.domain.usecase.SaveGame
import com.freesudoku.app.domain.usecase.StartGame
import com.freesudoku.app.ui.format.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val resumeGame: ResumeGame,
    private val getNextCampaignPuzzle: GetNextCampaignPuzzle,
    private val startGame: StartGame,
    private val saveGame: SaveGame,
    private val completePuzzle: CompletePuzzle,
    private val abandonGame: AbandonGame,
    private val settingsRepository: SettingsRepository,
    private val ticker: Ticker,
    @ApplicationScope private val appScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private lateinit var snapshot: GameSnapshot
    private lateinit var settings: GameSettings
    private lateinit var engine: GameEngine

    private var selected: Int?
        get() = savedStateHandle["selected"]
        set(v) { savedStateHandle["selected"] = v }

    private var notesMode: Boolean
        get() = savedStateHandle["notesMode"] ?: false
        set(v) { savedStateHandle["notesMode"] = v }

    private var resumed = false
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            settings = settingsRepository.settings.first()
            engine = GameEngine(autoRemoveNotes = settings.autoRemoveNotes)
            val existing = resumeGame()
            snapshot = existing ?: startGame(getNextCampaignPuzzle(), settings)
            render()
            startTimerLoop()
        }
    }

    fun onResume() { resumed = true }

    fun onPause() {
        resumed = false
        flushSaveNow()
    }

    fun onCellTap(index: Int) {
        selected = if (selected == index) null else index
        render()
    }

    fun onToggleNotesMode() {
        notesMode = !notesMode
        render()
    }

    fun onNumberInput(digit: Int) {
        val cell = selected ?: return
        if (!::snapshot.isInitialized) return
        val r = cell / 9
        val c = cell % 9
        snapshot = if (notesMode) {
            engine.toggleNote(snapshot, r, c, digit)
        } else {
            engine.setValue(snapshot, r, c, digit)
        }
        afterMutation()
    }

    fun onErase() {
        val cell = selected ?: return
        snapshot = engine.clear(snapshot, cell / 9, cell % 9)
        afterMutation()
    }

    fun onUndo() {
        snapshot = engine.undo(snapshot)
        afterMutation()
    }

    fun onRedo() {
        snapshot = engine.redo(snapshot)
        afterMutation()
    }

    fun onHint() {
        val cell = selected
        snapshot = if (cell != null && snapshot.board.cell(cell / 9, cell % 9).value == 0) {
            engine.hint(snapshot, cell / 9, cell % 9)
        } else {
            engine.autoHint(snapshot)
        }
        afterMutation()
    }

    /** After the result sheet: record the completion and start the next campaign puzzle. */
    fun onAdvance(onReady: () -> Unit) {
        viewModelScope.launch {
            if (snapshot.status == GameStatus.COMPLETED) completePuzzle(snapshot)
            snapshot = startGame(getNextCampaignPuzzle(), settings)
            selected = null
            render()
            onReady()
        }
    }

    /** After a FAILED game: replay the same puzzle from scratch. */
    fun onRetry() {
        viewModelScope.launch {
            snapshot = startGame(snapshot.puzzle, settings)
            selected = null
            render()
        }
    }

    fun onQuitAfterFail(onExit: () -> Unit) {
        viewModelScope.launch {
            abandonGame()
            onExit()
        }
    }

    fun onExitRequested(onExit: () -> Unit) {
        flushSaveNow()
        onExit()
    }

    private fun afterMutation() {
        render()
        persistDebounced()
    }

    private fun render() {
        if (!::snapshot.isInitialized) return
        val board = snapshot.board
        val solution = snapshot.puzzle.solution
        val sel = selected
        val selValue = sel?.let { board.cell(it / 9, it % 9).value }?.takeIf { it != 0 }

        val cells = (0 until 81).map { i ->
            val bc = board.cell(i / 9, i % 9)
            val wrong = bc.value != 0 && bc.value != solution.valueAt(i / 9, i % 9)
            CellUi(
                value = bc.value,
                given = bc.isGiven,
                notes = bc.notes,
                error = settings.highlightErrors && wrong,
                inSelectionScope = sel != null && sharesUnit(i, sel),
                sameValueAsSelection = settings.highlightSameNumbers &&
                    selValue != null && bc.value == selValue,
                selected = sel == i,
            )
        }

        val counts = IntArray(10)
        for (i in 0 until 81) counts[board.cell(i / 9, i % 9).value]++
        val remaining = (1..9).associateWith { (9 - counts[it]).coerceAtLeast(0) }

        _uiState.value = GameUiState(
            loading = false,
            cells = cells,
            selected = sel,
            notesMode = notesMode,
            mistakes = snapshot.mistakes,
            mistakeLimitEnabled = snapshot.mistakeLimitEnabled,
            elapsedText = formatDuration(snapshot.elapsedMs),
            status = snapshot.status,
            remainingPerDigit = remaining,
            canUndo = snapshot.undoStack.isNotEmpty(),
            canRedo = snapshot.redoStack.isNotEmpty(),
            puzzleNumber = snapshot.puzzle.number ?: 0,
            hintsUsed = snapshot.hintsUsed,
        )
    }

    private fun startTimerLoop() {
        viewModelScope.launch {
            ticker.oneSecondTicks().collect {
                if (resumed && ::snapshot.isInitialized && snapshot.status == GameStatus.IN_PROGRESS) {
                    snapshot = engine.tick(snapshot, 1_000)
                    render()
                }
            }
        }
    }

    private fun persistDebounced() {
        val current = snapshot
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1_000)
            saveGame(current)
        }
    }

    private fun flushSaveNow() {
        if (!::snapshot.isInitialized) return
        saveJob?.cancel()
        val current = snapshot
        appScope.launch { saveGame(current) }
    }

    override fun onCleared() {
        flushSaveNow()
        super.onCleared()
    }

    private companion object {
        fun sharesUnit(a: Int, b: Int): Boolean {
            if (a == b) return true
            val ra = a / 9; val ca = a % 9
            val rb = b / 9; val cb = b % 9
            return ra == rb || ca == cb || Grid.boxIndex(ra, ca) == Grid.boxIndex(rb, cb)
        }
    }
}
