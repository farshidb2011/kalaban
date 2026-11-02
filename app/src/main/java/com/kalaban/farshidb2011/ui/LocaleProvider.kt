package com.kalaban.farshidb2011.ui

import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

@Composable
fun AppLocaleProvider(
    languageTag: String,
    content: @Composable () -> Unit,
) {
    val tag = languageTag.ifBlank { "fa" }
    val base = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(tag, base, baseConfiguration) {
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(baseConfiguration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val configContext = base.createConfigurationContext(config)
        // Wrap the original context so Activity / registry lookups still unwrap correctly.
        object : ContextWrapper(base) {
            override fun getResources() = configContext.resources
            override fun getAssets() = configContext.assets
        }
    }
    val direction = if (tag.startsWith("fa", ignoreCase = true)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalResources provides localizedContext.resources,
        LocalLayoutDirection provides direction,
    ) {
        content()
    }
}
