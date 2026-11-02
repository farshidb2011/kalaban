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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
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

/** Status roles Material 3 lacks; profit/loss and alerts use these instead of ad-hoc colors. */
@Immutable
data class SemanticColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

private val LightSemantic = SemanticColors(
    success = Color(0xFF2E7D4F),
    successContainer = Color(0xFFD7F0DF),
    onSuccessContainer = Color(0xFF0F3B22),
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFE7B8),
    onWarningContainer = Color(0xFF3D2700),
    info = Color(0xFF3A5A78),
    infoContainer = Color(0xFFDCE6F0),
    onInfoContainer = Color(0xFF13283B),
)

private val DarkSemantic = SemanticColors(
    success = Color(0xFF7BD39A),
    successContainer = Color(0xFF1E4A30),
    onSuccessContainer = Color(0xFFC9F2D6),
    warning = Color(0xFFF2C26B),
    warningContainer = Color(0xFF4A3810),
    onWarningContainer = Color(0xFFFFE7B8),
    info = Color(0xFFA7C3DD),
    infoContainer = Color(0xFF24384B),
    onInfoContainer = Color(0xFFDCE6F0),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

val MaterialTheme.semantic: SemanticColors
    @Composable @ReadOnlyComposable
    get() = LocalSemanticColors.current

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
                LocalSemanticColors provides if (darkTheme) DarkSemantic else LightSemantic,
            ) {
                content()
            }
        },
    )
}
