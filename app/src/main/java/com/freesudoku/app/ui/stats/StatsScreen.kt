package com.freesudoku.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.ui.common.label
import com.freesudoku.app.ui.format.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    StatsContent(stats = stats, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(stats: PlayerStats, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatRow("Puzzles completados", stats.totalCompleted.toString())
            StatRow("Mejor tiempo", stats.bestDurationMs?.let(::formatDuration) ?: "—")
            StatRow("Tiempo promedio", stats.averageDurationMs?.let(::formatDuration) ?: "—")
            StatRow("Racha actual", "${stats.currentStreakDays} días")
            StatRow("Racha máxima", "${stats.longestStreakDays} días")

            if (stats.completedByBand.isNotEmpty()) {
                Text("Por dificultad", style = MaterialTheme.typography.titleMedium)
                stats.completedByBand.entries
                    .sortedBy { it.key.ordinal }
                    .forEach { (band, count) -> StatRow(band.label(), count.toString()) }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
