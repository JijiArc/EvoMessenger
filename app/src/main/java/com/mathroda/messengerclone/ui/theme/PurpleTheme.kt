package com.mathroda.messengerclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPurple = lightColors(
    primary = BottomSelected,
    primaryVariant = PurpleDark,
    secondary = BottomSelected,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkPurple = darkColors(
    primary = Color(0xFFD0BCFF),
    primaryVariant = Color(0xFFB69DF8),
    secondary = Color(0xFFD0BCFF),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2B2930),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF381E72),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

/** Plain Material theme in the app's purple, used by screens that do not use the chat theme. */
@Composable
fun PurpleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) DarkPurple else LightPurple,
        content = content
    )
}

// Status colours. Lighter shades are used in dark mode so text keeps enough contrast.
@Composable
fun resultDanger(): Color = if (isSystemInDarkTheme()) Color(0xFFF2B8B5) else ResultDanger

@Composable
fun resultWarning(): Color = if (isSystemInDarkTheme()) Color(0xFFFFD27F) else ResultWarning

@Composable
fun resultOk(): Color = if (isSystemInDarkTheme()) Color(0xFF9AD7AE) else ResultOk
