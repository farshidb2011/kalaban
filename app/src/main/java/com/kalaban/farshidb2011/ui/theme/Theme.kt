package com.kalaban.farshidb2011.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Ink40,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = Copper40,
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF7F0),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFF2EAE0),
    surfaceContainerHighest = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

// Dark: copper-forward chrome so buttons/tabs aren't washed gray.
private val DarkColorScheme = darkColorScheme(
    primary = CopperBright,
    onPrimary = Color(0xFF1A120C),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Copper80,
    onSecondary = Color(0xFF1A120C),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = Copper80,
    onTertiary = Color(0xFF1A120C),
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0D1014),
    surfaceContainerLow = Color(0xFF161C22),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF232C35),
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

private fun ContextWrapper.findActivity(): Activity? {
    var ctx: android.content.Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun KalabanTheme(
    themeMode: String = "system",
    darkThemeOverride: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = darkThemeOverride ?: when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val context = LocalContext.current

    SideEffect {
        val activity = (context as? Activity) ?: (context as? ContextWrapper)?.findActivity()
        if (activity != null && !view.isInEditMode) {
            val window = activity.window
            val topBarColor = colorScheme.primary.toArgb()
            val navBarColor = colorScheme.surface.toArgb()
            val bgColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.statusBarColor = topBarColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = navBarColor
            window.decorView.setBackgroundColor(bgColor)
            val insets = WindowCompat.getInsetsController(window, view)
            // In light mode, primary top bar is dark Ink40 (#2F3E46), so status bar icons should be light (false).
            // In dark mode, primary top bar is bright Copper (#D4925A), so status bar icons should be dark (true).
            insets.isAppearanceLightStatusBars = darkTheme
            insets.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            // Vazir via Typography; color must come from LocalContentColor only.
            // Baking color into LocalTextStyle made Text ignore Button onPrimary
            // (black label on dark primary) and delayed theme-switch redraws.
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onBackground,
                LocalTextStyle provides Typography.bodyLarge.copy(color = Color.Unspecified),
            ) {
                content()
            }
        },
    )
}
