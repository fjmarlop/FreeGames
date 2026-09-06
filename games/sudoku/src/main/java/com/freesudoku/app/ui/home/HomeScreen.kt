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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.ui.common.label
import com.freesudoku.app.ui.components.ApexBottomNav
import com.freesudoku.app.ui.components.ApexChip
import com.freesudoku.app.ui.components.ApexHeader
import com.freesudoku.app.ui.components.BottomTab
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
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onPlayOrContinue: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
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
            StatsStrip(state.stats)
            Spacer(Modifier.height(4.dp))
        }
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
                Text(
                    "PUZZLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("#${state.currentNumber}", style = MaterialTheme.typography.displayLarge)
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
