package dev.goalocus.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fallback palette for previews and when dynamic colour is off: a calm teal ("focus")
// with a warm amber accent ("streak"). Dynamic colour from the wallpaper is the default.
private val Teal10 = Color(0xFF00201C)
private val Teal20 = Color(0xFF003731)
private val Teal30 = Color(0xFF005048)
private val Teal40 = Color(0xFF006B60)
private val Teal80 = Color(0xFF5CDBC9)
private val Teal90 = Color(0xFF7BF8E5)
private val Amber10 = Color(0xFF2A1800)
private val Amber20 = Color(0xFF462A00)
private val Amber30 = Color(0xFF643F00)
private val Amber40 = Color(0xFF845400)
private val Amber80 = Color(0xFFFFB95C)
private val Amber90 = Color(0xFFFFDDB6)
private val Neutral10 = Color(0xFF191C1B)
private val Neutral90 = Color(0xFFE0E3E1)
private val Neutral99 = Color(0xFFFAFDFB)
private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)

internal val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    error = Red40,
)

internal val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Amber80,
    onSecondary = Amber20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    error = Red80,
)
