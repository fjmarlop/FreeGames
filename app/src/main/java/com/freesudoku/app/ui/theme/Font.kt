package com.freesudoku.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.freesudoku.app.R

/** One weight instance of a variable font, degrading gracefully to its default weight below API 26. */
private fun instance(resId: Int, weight: Int): Font = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Headlines and display text — angular, technical. */
val SpaceGrotesk = FontFamily(
    instance(R.font.space_grotesk_variable, 500),
    instance(R.font.space_grotesk_variable, 600),
    instance(R.font.space_grotesk_variable, 700),
)

/** Body copy — neutral, highly legible. */
val Geist = FontFamily(
    instance(R.font.geist_variable, 400),
    instance(R.font.geist_variable, 500),
    instance(R.font.geist_variable, 600),
    instance(R.font.geist_variable, 700),
)

/** Labels, tags, and tabular numerals — monospaced telemetry look. */
val JetBrainsMono = FontFamily(
    instance(R.font.jetbrains_mono_variable, 500),
    instance(R.font.jetbrains_mono_variable, 600),
    instance(R.font.jetbrains_mono_variable, 700),
)
