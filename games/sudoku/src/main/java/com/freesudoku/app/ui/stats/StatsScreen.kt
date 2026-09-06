package com.freesudoku.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.stats.PlayerStats
import com.freesudoku.app.ui.common.label
import com.freesudoku.app.ui.components.ApexBottomNav
import com.freesudoku.app.ui.components.ApexHeader
import com.freesudoku.app.ui.components.BottomTab
import com.freesudoku.app.ui.format.formatDuration

@Composable
fun StatsScreen(
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    StatsContent(stats = stats, onOpenHome = onOpenHome, onOpenSettings = onOpenSettings)
}

@Composable
fun StatsContent(
    stats: PlayerStats,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = { ApexHeader(section = "Estadísticas", onBack = onOpenHome) },
        bottomBar = {
            ApexBottomNav(current = BottomTab.STATS) { tab ->
                when (tab) {
                    BottomTab.HOME -> onOpenHome()
                    BottomTab.STATS -> Unit
                    BottomTab.SETTINGS -> onOpenSettings()
                }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecordsGrid(stats)
            SectionLabel("Por dificultad")
            for (band in DifficultyBand.entries) {
                BandRow(band, stats.completedByBand[band] ?: 0, stats.totalCompleted)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun RecordsGrid(stats: PlayerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RecordCard("Completados", stats.totalCompleted.toString(), Modifier.weight(1f))
            RecordCard(
                "Mejor tiempo",
                stats.bestDurationMs?.let(::formatDuration) ?: "—",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RecordCard("Racha actual", "${stats.currentStreakDays} d", Modifier.weight(1f))
            RecordCard("Racha máxima", "${stats.longestStreakDays} d", Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecordCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun BandRow(band: DifficultyBand, count: Int, total: Int) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    band.label(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$count completados",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
            )
        }
    }
}
