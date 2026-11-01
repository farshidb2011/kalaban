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
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = Copper40,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

// Dark: copper-forward chrome so buttons/tabs aren't washed gray.
private val DarkColorScheme = darkColorScheme(
    primary = CopperBright,
    onPrimary = Color(0xFF1A120C),
    secondary = Copper80,
    onSecondary = Color(0xFF1A120C),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = Copper80,
    onTertiary = Color(0xFF1A120C),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val context = LocalContext.current

    SideEffect {
        val activity = (context as? Activity) ?: (context as? ContextWrapper)?.findActivity()
        if (activity != null && !view.isInEditMode) {
            val window = activity.window
            val bg = colorScheme.background.toArgb()
            window.statusBarColor = bg
            window.navigationBarColor = bg
            window.decorView.setBackgroundColor(bg)
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !darkTheme
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
