package com.freesudoku.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.ui.components.GameToolbar
import com.freesudoku.app.ui.components.GameTopStatus
import com.freesudoku.app.ui.components.NumberPad
import com.freesudoku.app.ui.components.ResultSheet
import com.freesudoku.app.ui.components.SudokuBoard

@Composable
fun GameScreen(
    onExit: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    GameContent(
        state = state,
        callbacks = GameCallbacks(
            onCellTap = viewModel::onCellTap,
            onNumberInput = viewModel::onNumberInput,
            onErase = viewModel::onErase,
            onToggleNotes = viewModel::onToggleNotesMode,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onHint = viewModel::onHint,
            onNext = { viewModel.onAdvance(onReady = {}) },
            onHome = { viewModel.onExitRequested(onExit) },
            onRetry = viewModel::onRetry,
            onQuitAfterFail = { viewModel.onQuitAfterFail(onExit) },
            onBack = { viewModel.onExitRequested(onExit) },
        ),
    )
}

data class GameCallbacks(
    val onCellTap: (Int) -> Unit,
    val onNumberInput: (Int) -> Unit,
    val onErase: () -> Unit,
    val onToggleNotes: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onHint: () -> Unit,
    val onNext: () -> Unit,
    val onHome: () -> Unit,
    val onRetry: () -> Unit,
    val onQuitAfterFail: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContent(
    state: GameUiState,
    callbacks: GameCallbacks,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FreeSudoku") },
                navigationIcon = {
                    TextButton(onClick = callbacks.onBack) { Text("Salir") }
                },
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center,
        ) {
            if (state.loading) {
                CircularProgressIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GameTopStatus(
                        puzzleNumber = state.puzzleNumber,
                        elapsedText = state.elapsedText,
                        mistakes = state.mistakes,
                        mistakeLimitEnabled = state.mistakeLimitEnabled,
                    )
                    SudokuBoard(cells = state.cells, onCellClick = callbacks.onCellTap)
                    GameToolbar(
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        notesMode = state.notesMode,
                        onUndo = callbacks.onUndo,
                        onRedo = callbacks.onRedo,
                        onErase = callbacks.onErase,
                        onToggleNotes = callbacks.onToggleNotes,
                        onHint = callbacks.onHint,
                    )
                    NumberPad(
                        remainingPerDigit = state.remainingPerDigit,
                        onInput = callbacks.onNumberInput,
                    )
                }
            }
        }

        if (state.status == GameStatus.COMPLETED) {
            ResultSheet(
                elapsedMsText = state.elapsedText,
                mistakes = state.mistakes,
                hintsUsed = state.hintsUsed,
                onNext = callbacks.onNext,
                onHome = callbacks.onHome,
            )
        }

        if (state.status == GameStatus.FAILED) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Perdiste") },
                text = { Text("Llegaste a 3 errores en este puzzle.") },
                confirmButton = {
                    TextButton(onClick = callbacks.onRetry) { Text("Reintentar") }
                },
                dismissButton = {
                    TextButton(onClick = callbacks.onQuitAfterFail) { Text("Salir") }
                },
            )
        }
    }
}
