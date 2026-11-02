package com.kalaban.farshidb2011.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.BuildConfig
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.data.help.HelpContent
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.components.KeyValueRow
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailEmptyState
import com.kalaban.farshidb2011.ui.components.RetailTopAppBar
import com.kalaban.farshidb2011.ui.components.SearchField
import com.kalaban.farshidb2011.ui.components.SectionHeader
import com.kalaban.farshidb2011.ui.components.Spacing
import com.kalaban.farshidb2011.ui.components.retailContentWindowInsets
import com.kalaban.farshidb2011.ui.components.retailListItemColors
import com.kalaban.farshidb2011.ui.theme.Ink40

@Composable
fun HelpCenterScreen(appVm: AppViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val articles = remember(settings.languageTag) { HelpContent.articles(context, settings.languageTag) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(articles, query) { HelpContent.search(articles, query) }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.help_center), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            item { SearchField(query, { query = it }, stringResource(R.string.search_help)) }
            if (results.isEmpty()) item { RetailEmptyState(Icons.AutoMirrored.Outlined.MenuBook, stringResource(R.string.no_help_results)) }
            items(results, key = { it.id }) { a ->
                RetailCard(Modifier.clickable { onOpen(a.id) }) {
                    ListItem(
                        headlineContent = { Text(a.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(a.summary, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
}

@Composable
fun HelpArticleScreen(id: String, appVm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val article = remember(settings.languageTag, id) { HelpContent.articles(context, settings.languageTag).firstOrNull { it.id == id } }
    Scaffold(
        topBar = { RetailTopAppBar(article?.title ?: stringResource(R.string.help_center), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        if (article == null) {
            RetailEmptyState(Icons.AutoMirrored.Outlined.MenuBook, stringResource(R.string.no_help_results), Modifier.padding(pad))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item { Text(article.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(article.sections.orEmpty()) { s ->
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        s.heading?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) }
                        s.paragraphs.orEmpty().forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
                        s.steps.orEmpty().forEachIndexed { i, step ->
                            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        s.tips.orEmpty().forEach { tip ->
                            ListItem(
                                headlineContent = { Text(tip, style = MaterialTheme.typography.bodyMedium) },
                                leadingContent = { Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                                colors = retailListItemColors(MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** App mark: the launcher foreground on the brand ink background (adaptive icon XML cannot be drawn by Compose). */
@Composable
fun BrandMark(size: Int = 96) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 4).dp)).background(Ink40),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = stringResource(R.string.app_name), modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var license by remember { mutableStateOf<String?>(null) }
    val capabilities = listOf(
        R.string.cap_pricing, R.string.cap_formulas, R.string.cap_bulk, R.string.cap_products,
        R.string.cap_sales, R.string.cap_reports, R.string.cap_data, R.string.cap_security,
    )
    val notices = listOf(
        Triple("Vazirmatn", "SIL Open Font License 1.1", "fonts/OFL-Vazirmatn.txt"),
        Triple("AndroidX, Jetpack Compose, Room, WorkManager, DataStore, Biometric", "Apache License 2.0", null),
        Triple("Material Symbols / Material Icons", "Apache License 2.0", null),
        Triple("Gson", "Apache License 2.0", null),
        Triple("Kotlin & kotlinx.coroutines", "Apache License 2.0", null),
        Triple("shamsi-picker (alirezajavan)", "MIT License", null),
    )
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.about), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark()
                    Spacer(Modifier.height(Spacing.m))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        KeyValueRow(stringResource(R.string.version), BuildConfig.VERSION_NAME)
                        KeyValueRow(stringResource(R.string.build_number), BuildConfig.VERSION_CODE.toString())
                        KeyValueRow(stringResource(R.string.build_type), BuildConfig.BUILD_TYPE)
                        KeyValueRow(stringResource(R.string.database_version), "3")
                        KeyValueRow(stringResource(R.string.package_name), BuildConfig.APPLICATION_ID)
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(Spacing.s))
                        capabilities.forEach { cap ->
                            ListItem(
                                headlineContent = { Text(stringResource(cap), style = MaterialTheme.typography.bodyMedium) },
                                leadingContent = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                                colors = retailListItemColors(Color.Transparent),
                            )
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.privacy_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.licenses)) }
            items(notices) { (name, licenseName, asset) ->
                RetailCard(Modifier.clickable(enabled = asset != null) {
                    license = asset?.let { runCatching { context.assets.open(it).bufferedReader().use { r -> r.readText() } }.getOrNull() }
                }) {
                    ListItem(
                        headlineContent = { Text(name, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Text(licenseName + if (asset != null) " · ${stringResource(R.string.view_license)}" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.copyright_notice, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                )
            }
        }
    }
    license?.let { text ->
        AlertDialog(
            onDismissRequest = { license = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("SIL Open Font License 1.1") },
            text = { Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { license = null }) { Text(stringResource(R.string.close_image)) } },
        )
    }
}
