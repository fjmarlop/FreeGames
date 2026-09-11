package com.freesudoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
                    onStartQuickPlay = {},
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
                    onStartQuickPlay = {},
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
    }

    @Test fun a_resumable_quick_play_game_shows_no_puzzle_number() {
        composeRule.setContent {
            FreeSudokuTheme {
                HomeContent(
                    state = HomeUiState(
                        loading = false, currentNumber = null,
                        currentBand = DifficultyBand.MEDIO, hasResumableGame = true,
                    ),
                    onPlayOrContinue = {},
                    onStartQuickPlay = {},
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("PARTIDA RÁPIDA").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
    }

    @Test fun picking_a_band_with_no_resumable_game_starts_it_directly() {
        var picked: DifficultyBand? = null
        composeRule.setContent {
            FreeSudokuTheme {
                HomeContent(
                    state = HomeUiState(loading = false, currentNumber = 1, hasResumableGame = false),
                    onPlayOrContinue = {},
                    onStartQuickPlay = { picked = it },
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_play_entry").performClick()
        composeRule.onNodeWithTag("quick_play_${DifficultyBand.DIFICIL.name}").performClick()

        assertThat(picked).isEqualTo(DifficultyBand.DIFICIL)
    }

    @Test fun picking_a_band_with_a_resumable_game_asks_for_confirmation_first() {
        var picked: DifficultyBand? = null
        composeRule.setContent {
            FreeSudokuTheme {
                HomeContent(
                    state = HomeUiState(loading = false, currentNumber = 3, hasResumableGame = true),
                    onPlayOrContinue = {},
                    onStartQuickPlay = { picked = it },
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_play_entry").performClick()
        composeRule.onNodeWithTag("quick_play_${DifficultyBand.FACIL.name}").performClick()

        assertThat(picked).isNull() // not yet — needs confirmation
        composeRule.onNodeWithText("Vas a perder el progreso de tu partida actual. ¿Continuar?").assertIsDisplayed()

        composeRule.onNodeWithText("Sí, empezar").performClick()
        assertThat(picked).isEqualTo(DifficultyBand.FACIL)
    }
}
