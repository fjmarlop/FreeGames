package com.freesudoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.ui.home.HomeContent
import com.freesudoku.app.ui.home.HomeUiState
import com.freesudoku.app.ui.theme.FreeSudokuTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class HomeContentTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun shows_play_button_for_a_new_puzzle_and_fires_callback() {
        var played = false
        composeRule.setContent {
            FreeSudokuTheme {
                HomeContent(
                    state = HomeUiState(
                        loading = false,
                        currentNumber = 1,
                        currentBand = DifficultyBand.FACIL,
                        hasResumableGame = false,
                    ),
                    onPlayOrContinue = { played = true },
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Jugar puzzle 1").assertIsDisplayed()
        composeRule.onNodeWithText("Jugar puzzle 1").performClick()
        assertThat(played).isTrue()
    }

    @Test fun shows_continue_when_a_game_is_resumable() {
        composeRule.setContent {
            FreeSudokuTheme {
                HomeContent(
                    state = HomeUiState(loading = false, currentNumber = 3, hasResumableGame = true),
                    onPlayOrContinue = {},
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
    }
}
