package dev.goalocus.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one theme for every screen. Dynamic colour is on by default (minSdk 31 always has it);
 * turn it off for previews and screenshot tests so they don't depend on the wallpaper.
 */
@Composable
fun GoalocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalSpacing provides DefaultSpacing) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/** Spacing scale, so screens don't scatter magic numbers. Read with `GoalocusTheme.spacing`. */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

private val DefaultSpacing = Spacing()

private val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }

/** Theme values that Material doesn't model, mirroring how `MaterialTheme.colorScheme` is read. */
object GoalocusTheme {
    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current
}
