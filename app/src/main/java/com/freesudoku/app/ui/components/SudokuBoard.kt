package com.freesudoku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freesudoku.app.ui.game.CellUi
import com.freesudoku.app.ui.theme.JetBrainsMono

@Composable
fun SudokuBoard(
    cells: List<CellUi>,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cells.size != 81) return
    val boxLine = MaterialTheme.colorScheme.outline
    val cellLine = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            // Drawn *after* the cells (drawWithContent, not drawBehind) so the grid stays crisp
            // and unbroken even where a cell underneath is tinted for selection/highlighting.
            .drawWithContent {
                drawContent()
                val step = size.width / 9f
                for (i in 0..9) {
                    val strokePx = if (i % 3 == 0) 2.5.dp.toPx() else 1.dp.toPx()
                    val color = if (i % 3 == 0) boxLine else cellLine
                    drawLine(color, Offset(i * step, 0f), Offset(i * step, size.height), strokePx)
                    drawLine(color, Offset(0f, i * step), Offset(size.width, i * step), strokePx)
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    for (col in 0 until 9) {
                        val index = row * 9 + col
                        CellView(
                            cell = cells[index],
                            index = index,
                            onClick = { onCellClick(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CellView(
    cell: CellUi,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // One hue (primary), graduated by alpha, so selection reads as a soft tint rather than a
    // block of flat color — and low enough that it never fights the grid lines drawn on top.
    val background = when {
        cell.selected -> scheme.primary.copy(alpha = 0.28f)
        cell.sameValueAsSelection -> scheme.primary.copy(alpha = 0.14f)
        cell.inSelectionScope -> scheme.primary.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    val textColor = when {
        cell.error -> scheme.error
        cell.given -> scheme.onSurface
        else -> scheme.primary
    }

    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick)
            .testTag("cell_$index")
            .semantics {
                contentDescription = if (cell.value != 0) {
                    "Celda ${index / 9 + 1}, ${index % 9 + 1}: ${cell.value}"
                } else {
                    "Celda ${index / 9 + 1}, ${index % 9 + 1}: vacía"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.value != 0 -> Text(
                text = cell.value.toString(),
                color = textColor,
                fontFamily = JetBrainsMono,
                fontWeight = if (cell.given) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 19.sp,
            )
            cell.notes.isNotEmpty() -> NotesGrid(cell.notes)
        }
    }
}

@Composable
private fun NotesGrid(notes: Set<Int>) {
    Column {
        for (r in 0 until 3) {
            Row {
                for (c in 0 until 3) {
                    val d = r * 3 + c + 1
                    Text(
                        text = if (d in notes) d.toString() else " ",
                        fontFamily = JetBrainsMono,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 1.dp),
                    )
                }
            }
        }
    }
}
