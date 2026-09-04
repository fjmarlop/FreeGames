package com.freesudoku.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.data.settings.GameSettings
import com.freesudoku.app.data.settings.ThemeMode
import com.freesudoku.app.ui.components.ApexBottomNav
import com.freesudoku.app.ui.components.ApexHeader
import com.freesudoku.app.ui.components.BottomTab

@Composable
fun SettingsScreen(
    onOpenHome: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings,
        onOpenHome = onOpenHome,
        onOpenStats = onOpenStats,
        onMistakeLimit = viewModel::setMistakeLimit,
        onHighlightErrors = viewModel::setHighlightErrors,
        onHighlightSame = viewModel::setHighlightSame,
        onAutoRemoveNotes = viewModel::setAutoRemoveNotes,
        onThemeMode = viewModel::setThemeMode,
    )
}

@Composable
fun SettingsContent(
    settings: GameSettings,
    onOpenHome: () -> Unit,
    onOpenStats: () -> Unit,
    onMistakeLimit: (Boolean) -> Unit,
    onHighlightErrors: (Boolean) -> Unit,
    onHighlightSame: (Boolean) -> Unit,
    onAutoRemoveNotes: (Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
) {
    Scaffold(
        topBar = { ApexHeader(section = "Ajustes", onBack = onOpenHome) },
        bottomBar = {
            ApexBottomNav(current = BottomTab.SETTINGS) { tab ->
                when (tab) {
                    BottomTab.HOME -> onOpenHome()
                    BottomTab.STATS -> onOpenStats()
                    BottomTab.SETTINGS -> Unit
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeading("Configuración de juego")
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    SettingRow(
                        icon = Icons.Filled.Favorite,
                        title = "Límite de errores",
                        description = "3 fallos y la partida termina",
                        checked = settings.mistakeLimitEnabled,
                        onCheckedChange = onMistakeLimit,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Filled.WarningAmber,
                        title = "Resaltar errores",
                        description = "Marca la celda al fallar",
                        checked = settings.highlightErrors,
                        onCheckedChange = onHighlightErrors,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Filled.MyLocation,
                        title = "Resaltar números iguales",
                        description = "Ilumina las coincidencias en el tablero",
                        checked = settings.highlightSameNumbers,
                        onCheckedChange = onHighlightSame,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Filled.AutoFixHigh,
                        title = "Borrar notas automáticamente",
                        description = "Limpia fila, columna y caja al colocar un número",
                        checked = settings.autoRemoveNotes,
                        onCheckedChange = onAutoRemoveNotes,
                    )
                }
            }

            SectionHeading("Tema visual")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeOption(
                    label = "Oscuro",
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { onThemeMode(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                )
                ThemeOption(
                    label = "Sistema",
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeMode(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f),
                )
                ThemeOption(
                    label = "Claro",
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeMode(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(6.dp)
                    .size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                checkedThumbColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingDivider() {
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.selectable(selected = selected, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
