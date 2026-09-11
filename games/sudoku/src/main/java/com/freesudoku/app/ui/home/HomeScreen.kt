package com.freesudoku.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.ui.common.label
import com.freesudoku.app.ui.components.ApexBottomNav
import com.freesudoku.app.ui.components.ApexChip
import com.freesudoku.app.ui.components.ApexHeader
import com.freesudoku.app.ui.components.BottomTab
import com.freesudoku.app.ui.components.QuickPlaySheet
import com.freesudoku.app.ui.components.StatTile
import com.freesudoku.app.ui.format.formatDuration

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onPlayOrContinue = { viewModel.onPlayOrContinue(onReady = onPlay) },
        onStartQuickPlay = { band -> viewModel.onStartQuickPlay(band, onReady = onPlay) },
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onPlayOrContinue: () -> Unit,
    onStartQuickPlay: (DifficultyBand) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showQuickPlaySheet by remember { mutableStateOf(false) }
    var pendingQuickPlayBand by remember { mutableStateOf<DifficultyBand?>(null) }

    Scaffold(
        topBar = { ApexHeader(section = "Inicio") },
        bottomBar = {
            ApexBottomNav(current = BottomTab.HOME) { tab ->
                when (tab) {
                    BottomTab.HOME -> Unit
                    BottomTab.STATS -> onOpenStats()
                    BottomTab.SETTINGS -> onOpenSettings()
                }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            ContinuePuzzleCard(state = state, onPlayOrContinue = onPlayOrContinue)
            OutlinedButton(
                onClick = { showQuickPlaySheet = true },
                enabled = !state.preparingPuzzle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_play_entry"),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Partida rápida")
            }
            StatsStrip(state.stats)
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showQuickPlaySheet) {
        QuickPlaySheet(
            onPick = { band ->
                showQuickPlaySheet = false
                if (state.hasResumableGame) pendingQuickPlayBand = band else onStartQuickPlay(band)
            },
            onDismiss = { showQuickPlaySheet = false },
        )
    }

    pendingQuickPlayBand?.let { band ->
        AlertDialog(
            onDismissRequest = { pendingQuickPlayBand = null },
            title = { Text("Empezar partida rápida") },
            text = { Text("Vas a perder el progreso de tu partida actual. ¿Continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingQuickPlayBand = null
                        onStartQuickPlay(band)
                    },
                ) { Text("Sí, empezar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingQuickPlayBand = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ContinuePuzzleCard(state: HomeUiState, onPlayOrContinue: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApexChip(
                    text = if (state.hasResumableGame) "Continuar partida" else "Nuevo puzzle",
                    emphasized = true,
                )
                state.currentBand?.let { band ->
                    ApexChip(text = band.label(), icon = Icons.Filled.Tune)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val number = state.currentNumber
                if (number != null) {
                    Text(
                        "PUZZLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("#$number", style = MaterialTheme.typography.displayLarge)
                } else {
                    // Resumable "Partida rápida": no campaign position to show a number for.
                    Text("PARTIDA RÁPIDA", style = MaterialTheme.typography.displaySmall)
                }
            }
            Button(
                onClick = onPlayOrContinue,
                enabled = !state.preparingPuzzle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.preparingPuzzle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        if (state.hasResumableGame) "Continuar" else "Jugar puzzle ${state.currentNumber}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsStrip(stats: PlayerStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            icon = Icons.Filled.EmojiEvents,
            value = stats.totalCompleted.toString(),
            label = "Completados",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = Icons.Filled.Timer,
            value = stats.bestDurationMs?.let(::formatDuration) ?: "—",
            label = "Mejor tiempo",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = Icons.Filled.LocalFireDepartment,
            value = "${stats.currentStreakDays} d",
            label = "Racha activa",
            modifier = Modifier.weight(1f),
            accent = true,
        )
    }
}
