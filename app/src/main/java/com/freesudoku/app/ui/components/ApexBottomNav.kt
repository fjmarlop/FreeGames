package com.freesudoku.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class BottomTab(val label: String) {
    HOME("Inicio"),
    STATS("Estadísticas"),
    SETTINGS("Ajustes"),
}

@Composable
fun ApexBottomNav(current: BottomTab, onSelect: (BottomTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = current == BottomTab.HOME,
            onClick = { onSelect(BottomTab.HOME) },
            icon = { Icon(Icons.Filled.SportsEsports, contentDescription = null) },
            label = { Text(BottomTab.HOME.label, style = MaterialTheme.typography.labelSmall) },
            colors = apexNavColors(),
        )
        NavigationBarItem(
            selected = current == BottomTab.STATS,
            onClick = { onSelect(BottomTab.STATS) },
            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            label = { Text(BottomTab.STATS.label, style = MaterialTheme.typography.labelSmall) },
            colors = apexNavColors(),
        )
        NavigationBarItem(
            selected = current == BottomTab.SETTINGS,
            onClick = { onSelect(BottomTab.SETTINGS) },
            icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
            label = { Text(BottomTab.SETTINGS.label, style = MaterialTheme.typography.labelSmall) },
            colors = apexNavColors(),
        )
    }
}

@Composable
private fun apexNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)
