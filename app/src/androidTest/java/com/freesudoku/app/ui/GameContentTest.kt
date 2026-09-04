package com.freesudoku.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.ui.game.CellUi
import com.freesudoku.app.ui.game.GameCallbacks
import com.freesudoku.app.ui.game.GameContent
import com.freesudoku.app.ui.game.GameUiState
import com.freesudoku.app.ui.theme.FreeSudokuTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class GameContentTest {

    @get:Rule val composeRule = createComposeRule()

    private fun cells() = List(81) { CellUi(0, false, emptySet(), false, false, false, false) }

    private fun noopCallbacks(
        onCellTap: (Int) -> Unit = {},
        onNumberInput: (Int) -> Unit = {},
    ) = GameCallbacks(
        onCellTap = onCellTap,
        onNumberInput = onNumberInput,
        onErase = {}, onToggleNotes = {}, onUndo = {}, onRedo = {}, onHint = {},
        onNext = {}, onHome = {}, onRetry = {}, onQuitAfterFail = {}, onBack = {},
    )

    @Test fun tapping_a_cell_then_a_pad_digit_fires_callbacks() {
        var tapped = -1
        var input = -1
        composeRule.setContent {
            FreeSudokuTheme {
                GameContent(
                    state = GameUiState(
                        loading = false,
                        cells = cells(),
                        remainingPerDigit = (1..9).associateWith { 9 },
                        puzzleNumber = 1,
                    ),
                    callbacks = noopCallbacks(onCellTap = { tapped = it }, onNumberInput = { input = it }),
                )
            }
        }

        composeRule.onNodeWithTag("cell_2").performClick()
        composeRule.onNodeWithTag("pad_4").performClick()

        assertThat(tapped).isEqualTo(2)
        assertThat(input).isEqualTo(4)
    }

    @Test fun completed_status_shows_the_result_sheet() {
        composeRule.setContent {
            FreeSudokuTheme {
                GameContent(
                    state = GameUiState(loading = false, cells = cells(), status = GameStatus.COMPLETED),
                    callbacks = noopCallbacks(),
                )
            }
        }
        composeRule.onNodeWithText("¡Completado!").assertIsDisplayed()
    }

    @Test fun restart_action_asks_for_confirmation_before_calling_back() {
        var retried = false
        composeRule.setContent {
            FreeSudokuTheme {
                GameContent(
                    state = GameUiState(loading = false, cells = cells(), puzzleNumber = 3),
                    callbacks = noopCallbacks().copy(onRetry = { retried = true }),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reiniciar puzzle").performClick()
        composeRule.onNodeWithText("Reiniciar puzzle").assertIsDisplayed()
        assertThat(retried).isFalse() // the dialog must not act until confirmed

        composeRule.onNodeWithText("Cancelar").performClick()
        assertThat(retried).isFalse()

        composeRule.onNodeWithContentDescription("Reiniciar puzzle").performClick()
        composeRule.onNodeWithText("Reiniciar").performClick()
        assertThat(retried).isTrue()
    }

    @Test fun restart_action_is_hidden_once_the_puzzle_is_over() {
        composeRule.setContent {
            FreeSudokuTheme {
                GameContent(
                    state = GameUiState(loading = false, cells = cells(), status = GameStatus.COMPLETED),
                    callbacks = noopCallbacks(),
                )
            }
        }
        composeRule.onAllNodesWithContentDescription("Reiniciar puzzle").assertCountEquals(0)
    }

    @Test fun failed_status_shows_the_dialog() {
        composeRule.setContent {
            FreeSudokuTheme {
                GameContent(
                    state = GameUiState(
                        loading = false, cells = cells(), status = GameStatus.FAILED, mistakes = 3,
                    ),
                    callbacks = noopCallbacks(),
                )
            }
        }
        composeRule.onNodeWithText("Perdiste").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }
}
