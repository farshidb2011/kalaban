package com.kalaban.farshidb2011.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import com.kalaban.farshidb2011.ui.findFragmentActivity
import com.kalaban.farshidb2011.ui.components.SuccessDialog
import com.kalaban.farshidb2011.ui.components.HelpIconButton
import com.kalaban.farshidb2011.ui.components.RetailPeriodChip
import com.kalaban.farshidb2011.ui.components.RetailFilterChip
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import com.kalaban.farshidb2011.ui.components.retailTextFieldColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.BuildConfig
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.PaymentType
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.repository.ShopRepository
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.ItemFormDraft
import com.kalaban.farshidb2011.ui.SaleCartLineDraft
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailDivider
import com.kalaban.farshidb2011.ui.components.RetailMenuRow
import com.kalaban.farshidb2011.ui.components.RetailTextField
import com.kalaban.farshidb2011.ui.components.RetailOutlinedButton
import com.kalaban.farshidb2011.ui.components.RetailPrimaryButton
import com.kalaban.farshidb2011.ui.components.RetailContent
import com.kalaban.farshidb2011.ui.components.retailContentWindowInsets
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onAdmin: () -> Unit, onDisplay: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.choose_mode),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Store, null)
            Text(stringResource(R.string.admin_panel), modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDisplay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Visibility, null)
            Text(stringResource(R.string.display_panel), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    vm: AppViewModel,
    onBack: (() -> Unit)? = null,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hasPin by vm.hasPin.collectAsStateWithLifecycle()
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var mismatch by rememberSaveable { mutableStateOf(false) }
    var tooShort by rememberSaveable { mutableStateOf(false) }
    var resetAfterBiometric by rememberSaveable { mutableStateOf(false) }
    val biometricAvailable = remember {
        androidx.biometric.BiometricManager.from(context)
            .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    val showBiometricUnlock = hasPin && !resetAfterBiometric &&
        settings.biometricUnlockEnabled && biometricAvailable
    val settingPin = !hasPin || resetAfterBiometric
    val title = when {
        resetAfterBiometric -> R.string.set_new_pin
        hasPin -> R.string.unlock_admin
        else -> R.string.set_pin_once
    }

    val biometricResetTitle = stringResource(R.string.forgot_pin_biometric_title)
    val biometricUnlockTitle = stringResource(R.string.use_fingerprint)
    val cancelLabel = stringResource(R.string.cancel)

    fun launchBiometric(forReset: Boolean) {
        val activity = context.findFragmentActivity() ?: return
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (forReset) {
                        pin = ""
                        confirm = ""
                        error = false
                        mismatch = false
                        tooShort = false
                        resetAfterBiometric = true
                    } else {
                        vm.unlockSession()
                        onUnlocked()
                    }
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(
                    if (forReset) biometricResetTitle else biometricUnlockTitle,
                )
                .setNegativeButtonText(cancelLabel)
                .setAllowedAuthenticators(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK,
                )
                .build(),
        )
    }

    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        RetailContent(Modifier.padding(pad).padding(24.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RetailCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RetailTextField(
                            value = pin,
                            onValueChange = {
                                pin = it
                                error = false
                                mismatch = false
                                tooShort = false
                            },
                            label = {
                                Text(
                                    stringResource(
                                        if (settingPin) R.string.set_new_pin else R.string.enter_pin,
                                    ),
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            numericLtr = true,
                        )
                        if (settingPin) {
                            RetailTextField(
                                value = confirm,
                                onValueChange = {
                                    confirm = it
                                    mismatch = false
                                    tooShort = false
                                },
                                label = { Text(stringResource(R.string.confirm_pin)) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                numericLtr = true,
                            )
                        }
                        if (tooShort) {
                            Text(
                                stringResource(R.string.pin_too_short),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (error) {
                            Text(
                                stringResource(R.string.invalid_pin),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (mismatch) {
                            Text(
                                stringResource(R.string.pin_mismatch),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        RetailPrimaryButton(
                            onClick = {
                                if (settingPin) {
                                    if (pin.length < 4) {
                                        tooShort = true
                                        return@RetailPrimaryButton
                                    }
                                    if (pin != confirm) {
                                        mismatch = true
                                        return@RetailPrimaryButton
                                    }
                                    if (resetAfterBiometric) {
                                        vm.resetPinAfterBiometric(pin) {
                                            resetAfterBiometric = false
                                            onUnlocked()
                                        }
                                    } else {
                                        vm.unlockWithPin(pin) { ok ->
                                            if (ok) onUnlocked() else error = true
                                        }
                                    }
                                } else {
                                    if (pin.isBlank()) {
                                        error = true
                                        return@RetailPrimaryButton
                                    }
                                    vm.unlockWithPin(pin) { ok ->
                                        if (ok) onUnlocked() else error = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        resetAfterBiometric -> R.string.set_new_pin
                                        hasPin -> R.string.unlock_admin
                                        else -> R.string.set_pin
                                    },
                                ),
                            )
                        }
                        if (showBiometricUnlock) {
                            RetailOutlinedButton(
                                onClick = { launchBiometric(forReset = false) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.use_fingerprint)) }
                        }
                    }
                }
                if (hasPin && !resetAfterBiometric) {
                    if (biometricAvailable) {
                        TextButton(
                            onClick = { launchBiometric(forReset = true) },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                stringResource(R.string.forgot_pin),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.biometric_required_for_reset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (hasPin && settings.biometricUnlockEnabled && !biometricAvailable) {
                    Text(
                        stringResource(R.string.biometric_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHub(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLock: () -> Unit,
) {
    val links = listOf(
        Triple(R.string.units, "units", Icons.Outlined.Straighten),
        Triple(R.string.categories, "categories", Icons.Outlined.Category),
        Triple(R.string.credits, "credits", Icons.Outlined.Payments),
        Triple(R.string.low_stock, "low_stock", Icons.Outlined.WarningAmber),
        Triple(R.string.reminders, "reminders", Icons.Outlined.Notifications),
        Triple(R.string.dollar_history, "dollar_history", Icons.Outlined.History),
        Triple(R.string.backup, "backup", Icons.Outlined.Backup),
        Triple(R.string.settings, "settings", Icons.Outlined.Settings),
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_panel)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onLock) { Icon(Icons.Default.Lock, null) }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        RetailCard(Modifier.padding(pad).padding(16.dp)) {
            links.forEachIndexed { index, (label, route, icon) ->
                RetailMenuRow(
                    label = stringResource(label),
                    icon = icon,
                    onClick = { onNavigate(route) },
                )
                if (index < links.lastIndex) {
                    RetailDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DisplayPanel(vm: AppViewModel, onOpenAdmin: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var q by rememberSaveable { mutableStateOf("") }
    // "all" | "none" | category id as string
    var categoryFilter by rememberSaveable { mutableStateOf("all") }
    var previewUri by rememberSaveable { mutableStateOf<String?>(null) }

    val uncategorizedLabel = stringResource(R.string.uncategorized)
    val filteredItems = remember(items, categoryFilter) {
        when (categoryFilter) {
            "all" -> items
            "none" -> items.filter { it.categoryId == null }
            else -> {
                val id = categoryFilter.toLongOrNull()
                items.filter { it.categoryId == id }
            }
        }
    }
    val groupedItems = remember(filteredItems, categories, fa, uncategorizedLabel) {
        filteredItems
            .groupBy { item ->
                val cat = item.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
                when {
                    cat == null -> Triple(null as Long?, uncategorizedLabel, uncategorizedLabel)
                    fa -> Triple(cat.id, cat.nameFa, cat.nameEn)
                    else -> Triple(cat.id, cat.nameEn, cat.nameFa)
                }
            }
            .entries
            .sortedWith(
                compareBy<Map.Entry<Triple<Long?, String, String>, List<ItemEntity>>> {
                    if (it.key.first == null) 1 else 0
                }.thenBy { it.key.second },
            )
    }
    val hasUncategorized = items.any { it.categoryId == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.display_panel), color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = onOpenAdmin) {
                        Icon(Icons.Outlined.AdminPanelSettings, contentDescription = stringResource(R.string.open_admin))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            RetailTextField(
                value = q,
                onValueChange = { q = it; vm.setQuery(it) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                stringResource(R.string.categories),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "all") {
                    RetailFilterChip(
                        selected = categoryFilter == "all",
                        label = stringResource(R.string.all_categories),
                        onClick = { categoryFilter = "all" },
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    val label = if (fa) cat.nameFa else cat.nameEn
                    RetailFilterChip(
                        selected = categoryFilter == cat.id.toString(),
                        label = label,
                        onClick = { categoryFilter = cat.id.toString() },
                    )
                }
                if (hasUncategorized) {
                    item(key = "none") {
                        RetailFilterChip(
                            selected = categoryFilter == "none",
                            label = uncategorizedLabel,
                            onClick = { categoryFilter = "none" },
                        )
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = true),
            ) {
                if (groupedItems.isEmpty()) {
                    item(key = "empty_display") {
                        com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                            icon = Icons.Default.Store,
                            title = stringResource(R.string.no_items),
                        )
                    }
                }
                groupedItems.forEach { (catKey, catItems) ->
                    val (catId, displayName) = catKey.first to catKey.second
                    item(key = "header_${catId ?: "none"}") {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(catItems, key = { it.id }) { item ->
                        val unitName = units.firstOrNull { it.id == item.unitId }?.let {
                            if (fa) it.nameFa else it.nameEn
                        }
                        DisplayProductCard(
                            item = item,
                            currencyCode = settings.currencyCode,
                            unitName = unitName,
                            categoryName = displayName,
                            onImageClick = { uri -> previewUri = uri },
                        )
                    }
                }
            }
        }
    }
    previewUri?.let { uri ->
        ProductImageViewerDialog(
            uri = uri,
            onDismiss = { previewUri = null },
        )
    }
}

@Composable
private fun DisplayProductCard(
    item: ItemEntity,
    currencyCode: String,
    unitName: String?,
    categoryName: String?,
    onImageClick: (String) -> Unit,
) {
    val density = LocalDensity.current
    val cardImageMaxPx = with(density) { 720.dp.roundToPx() }
    RetailCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            ProductImage(
                uri = item.imageUri,
                maxSidePx = cardImageMaxPx,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .then(
                        item.imageUri?.let { uri ->
                            Modifier.clickable { onImageClick(uri) }
                        } ?: Modifier,
                    ),
                contentScale = ContentScale.Fit,
                contentDescription = item.title,
            )
            if (item.imageUri != null) {
                Text(
                    stringResource(R.string.tap_to_enlarge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${Money.format(item.sellPrice)} $currencyCode",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                val details = buildList {
                    add("${stringResource(R.string.stock)}: ${item.stockQty}")
                    if (!unitName.isNullOrBlank()) add("${stringResource(R.string.unit)}: $unitName")
                    if (!categoryName.isNullOrBlank()) {
                        add("${stringResource(R.string.categories)}: $categoryName")
                    }
                    if (!item.sku.isNullOrBlank()) {
                        add("${stringResource(R.string.sku)}: ${item.sku}")
                    }
                }.joinToString(" · ")
                Text(
                    details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductImageViewerDialog(uri: String, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val maxSidePx = with(density) { 1600.dp.roundToPx() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ProductImage(
                uri = uri,
                maxSidePx = maxSidePx,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
                contentScale = ContentScale.Fit,
                contentDescription = stringResource(R.string.enlarge_image),
                placeholderColor = Color.Black,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_image),
                    tint = Color.White,
                )
            }
        }
    }
}

/** Decode a product image at high quality, capped by [maxSidePx] to avoid OOM. */
private fun loadProductBitmap(context: Context, uri: String, maxSidePx: Int): Bitmap? {
    return runCatching {
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(parsed)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val width = bounds.outWidth.coerceAtLeast(1)
        val height = bounds.outHeight.coerceAtLeast(1)
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > maxSidePx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(parsed)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }.getOrNull()
}

@Composable
private fun ProductImage(
    uri: String?,
    maxSidePx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val context = LocalContext.current
    var bitmap by remember(uri, maxSidePx) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, maxSidePx) {
        bitmap = if (uri.isNullOrBlank()) {
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                loadProductBitmap(context, uri, maxSidePx)
            }
        }
    }
    Box(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    stringResource(R.string.no_product_image),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemThumb(uri: String?) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val maxSidePx = with(density) { 96.dp.roundToPx() }
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, maxSidePx) {
        bitmap = if (uri.isNullOrBlank()) {
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                loadProductBitmap(context, uri, maxSidePx)
            }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemsScreen(
    vm: AppViewModel,
    onBack: (() -> Unit)? = null,
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val form by vm.itemForm.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var categoryMenuOpen by rememberSaveable { mutableStateOf(false) }
    var unitMenuOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteError by rememberSaveable { mutableStateOf<String?>(null) }
    val itemInUseMsg = stringResource(R.string.item_in_use)

    val showForm = form.showForm
    val editingId = form.editingId
    val title = form.title
    val packTotal = form.packTotal
    val piecesPerUnitText = form.piecesPerUnitText
    val stockUnits = form.stockUnits
    val lowStockThresholdText = form.lowStockThresholdText
    val sku = form.sku
    val selectedUnitId = form.selectedUnitId
    val selectedCategoryId = form.selectedCategoryId
    val imageUri = form.imageUri
    val q = form.listQuery

    val piecesPerUnitValue = (piecesPerUnitText.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
    val packTotalValue = packTotal.toDoubleOrNull()
    val piecePurchase = if (packTotalValue != null) packTotalValue / piecesPerUnitValue else null
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val selectedUnit = units.firstOrNull { it.id == selectedUnitId }
    val categoryLabel = when {
        selectedCategory == null -> stringResource(R.string.no_category)
        fa -> selectedCategory.nameFa
        else -> selectedCategory.nameEn
    }
    val unitLabel = when {
        selectedUnit == null -> ""
        fa -> selectedUnit.nameFa
        else -> selectedUnit.nameEn
    }

    LaunchedEffect(units, selectedUnitId) {
        if (selectedUnitId == null) {
            val first = units.firstOrNull()?.id ?: return@LaunchedEffect
            vm.updateItemForm { it.copy(selectedUnitId = first) }
        }
    }
    // Keep the shared items query in sync after process/config restore.
    LaunchedEffect(form.listQuery) {
        vm.setQuery(form.listQuery)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importAndSetItemImage(uri.toString())
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            val uri = form.pendingCameraUri
            if (uri != null) vm.importAndSetItemImage(uri)
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val dir = File(context.cacheDir, "item_images").apply { mkdirs() }
        val file = File(dir, "item_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        vm.updateItemForm { it.copy(pendingCameraUri = uri.toString()) }
        cameraLauncher.launch(uri)
    }
    Scaffold(
        topBar = {
            if (!embedded) {
                com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                    title = stringResource(R.string.items),
                    onBack = onBack,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (showForm) {
                        vm.resetItemForm(defaultUnitId = units.firstOrNull()?.id)
                    } else {
                        vm.updateItemForm {
                            ItemFormDraft(
                                showForm = true,
                                selectedUnitId = units.firstOrNull()?.id,
                            )
                        }
                        vm.setQuery("")
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, null) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = embedded),
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            if (!showForm) {
                RetailTextField(
                    value = q,
                    onValueChange = {
                        vm.updateItemForm { draft -> draft.copy(listQuery = it) }
                        vm.setQuery(it)
                    },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text(stringResource(R.string.search)) },
                )
                deleteError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (showForm) {
                Column(
                    Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(if (editingId == 0L) R.string.add else R.string.edit_item),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RetailTextField(
                    value = title,
                    onValueChange = { value -> vm.updateItemForm { it.copy(title = value) } },
                    label = { Text(stringResource(R.string.title)) },
                )
                RetailTextField(
                    value = sku,
                    onValueChange = { value -> vm.updateItemForm { it.copy(sku = value) } },
                    label = { Text(stringResource(R.string.sku)) },
                    numericLtr = true,
                )
                ExposedDropdownMenuBox(
                    expanded = categoryMenuOpen,
                    onExpandedChange = { categoryMenuOpen = !categoryMenuOpen },
                ) {
                    OutlinedTextField(
                        value = categoryLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuOpen) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = retailTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.no_category),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                vm.updateItemForm { it.copy(selectedCategoryId = null) }
                                categoryMenuOpen = false
                            },
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (fa) cat.nameFa else cat.nameEn,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    vm.updateItemForm { it.copy(selectedCategoryId = cat.id) }
                                    categoryMenuOpen = false
                                },
                            )
                        }
                    }
                }
                if (units.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = unitMenuOpen,
                        onExpandedChange = { unitMenuOpen = !unitMenuOpen },
                    ) {
                        OutlinedTextField(
                            value = unitLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.unit)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuOpen) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = retailTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        )
                        ExposedDropdownMenu(
                            expanded = unitMenuOpen,
                            onDismissRequest = { unitMenuOpen = false },
                        ) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (fa) unit.nameFa else unit.nameEn,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        vm.updateItemForm { it.copy(selectedUnitId = unit.id) }
                                        unitMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
                RetailTextField(
                    value = piecesPerUnitText,
                    onValueChange = { value -> vm.updateItemForm { it.copy(piecesPerUnitText = value) } },
                    label = { Text(stringResource(R.string.pieces_per_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    numericLtr = true,
                )
                RetailTextField(
                    value = packTotal,
                    onValueChange = { value -> vm.updateItemForm { it.copy(packTotal = value) } },
                    label = { Text(stringResource(R.string.pack_total_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    numericLtr = true,
                )
                if (piecePurchase != null) {
                    Text(
                        "${stringResource(R.string.unit_piece_price)}: ${Money.format(piecePurchase)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                    )
                }
                RetailTextField(
                    value = stockUnits,
                    onValueChange = { value -> vm.updateItemForm { it.copy(stockUnits = value) } },
                    label = { Text(stringResource(R.string.stock_in_units)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    numericLtr = true,
                )
                RetailTextField(
                    value = lowStockThresholdText,
                    onValueChange = { value -> vm.updateItemForm { it.copy(lowStockThresholdText = value) } },
                    label = { Text(stringResource(R.string.low_stock_threshold)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    numericLtr = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetailOutlinedButton(
                        onClick = {
                            val startCamera = {
                                val dir = File(context.cacheDir, "item_images").apply { mkdirs() }
                                val file = File(dir, "item_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                                    file,
                                )
                                vm.updateItemForm { it.copy(pendingCameraUri = uri.toString()) }
                                cameraLauncher.launch(uri)
                            }
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                startCamera()
                            } else {
                                cameraPermission.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.take_photo)) }
                    RetailOutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.pick_image))
                    }
                }
                ItemThumb(imageUri)
                RetailPrimaryButton(
                    onClick = {
                        val unitId = selectedUnitId ?: units.firstOrNull()?.id ?: return@RetailPrimaryButton
                        if (title.isBlank()) return@RetailPrimaryButton
                        val pieces = piecesPerUnitValue
                        val pack = packTotal.toDoubleOrNull() ?: 0.0
                        val unitsQty = stockUnits.toDoubleOrNull() ?: 0.0
                        val threshold = (lowStockThresholdText.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0)
                        vm.saveItem(
                            id = editingId,
                            title = title.trim(),
                            unitId = unitId,
                            categoryId = selectedCategoryId,
                            purchasePrice = pack / pieces,
                            purchaseDollarRate = settings.dollarRate,
                            piecesPerUnit = pieces,
                            stockQty = unitsQty * pieces,
                            lowStockThreshold = threshold,
                            sku = sku,
                            imageUri = imageUri,
                        )
                        vm.resetItemForm(defaultUnitId = units.firstOrNull()?.id)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text(stringResource(R.string.save)) }
                TextButton(
                    onClick = { vm.resetItemForm(defaultUnitId = units.firstOrNull()?.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(88.dp))
                }
            } else {
                if (items.isEmpty()) {
                    com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                        icon = Icons.Default.Store,
                        title = stringResource(R.string.no_items),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = true),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            val catName = categories.firstOrNull { it.id == item.categoryId }?.let {
                                if (fa) it.nameFa else it.nameEn
                            }
                            val unitName = units.firstOrNull { it.id == item.unitId }?.let {
                                if (fa) it.nameFa else it.nameEn
                            }
                            val pieces = item.piecesPerUnit.coerceAtLeast(1.0)
                            RetailCard {
                                ListItem(
                                    headlineContent = {
                                        Text(item.title, color = MaterialTheme.colorScheme.onSurface)
                                    },
                                    supportingContent = {
                                        Text(
                                            buildString {
                                                append("${stringResource(R.string.unit_piece_price)}: ${Money.format(item.sellPrice)}")
                                                if (pieces > 1.0) {
                                                    append(" · ${stringResource(R.string.pieces_per_unit)}: $pieces")
                                                }
                                                if (unitName != null) {
                                                    append(" · ${stringResource(R.string.unit)}: $unitName")
                                                }
                                                append(
                                                    " · ${stringResource(R.string.stock)}: ${item.stockQty} " +
                                                        stringResource(R.string.stock_pieces),
                                                )
                                                if (catName != null) {
                                                    append(" · ${stringResource(R.string.categories)}: $catName")
                                                }
                                                if (!item.sku.isNullOrBlank()) {
                                                    append(" · ${stringResource(R.string.sku)}: ${item.sku}")
                                                }
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    leadingContent = { ItemThumb(item.imageUri) },
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { vm.openItemForm(item) }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.edit_item),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            IconButton(onClick = { pendingDeleteId = item.id; deleteError = null }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDeleteId?.let { deleteId ->
        val scheme = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurface,
            title = {
                Text(stringResource(R.string.confirm_delete), color = scheme.onSurface)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteItem(deleteId) {
                            deleteError = itemInUseMsg
                        }
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.delete), color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.cancel), color = scheme.primary)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleNameListScreen(
    titleRes: Int,
    names: List<Pair<Long, String>>,
    labelRes: Int,
    externalError: String? = null,
    onClearExternalError: (() -> Unit)? = null,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: ((Long, String) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    val displayError = externalError ?: errorMsg
    val editing = editingId != null && onUpdate != null
    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(titleRes),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            RetailTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMsg = null
                    onClearExternalError?.invoke()
                },
                label = { Text(stringResource(labelRes)) },
            )
            Spacer(Modifier.height(8.dp))
            RetailPrimaryButton(
                onClick = {
                    if (name.isBlank()) return@RetailPrimaryButton
                    val id = editingId
                    if (id != null && onUpdate != null) {
                        onUpdate(id, name.trim())
                    } else {
                        onAdd(name.trim())
                    }
                    name = ""
                    editingId = null
                    errorMsg = null
                    onClearExternalError?.invoke()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (editing) R.string.save else R.string.add))
            }
            if (editing) {
                TextButton(
                    onClick = {
                        editingId = null
                        name = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
            }
            displayError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (onUpdate != null) {
                Text(
                    stringResource(R.string.tap_to_edit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            RetailCard {
                if (names.isEmpty()) {
                    Text(
                        text = "—",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    names.forEachIndexed { index, (id, n) ->
                        val selected = editingId == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = onUpdate != null) {
                                    editingId = id
                                    name = n
                                    errorMsg = null
                                    onClearExternalError?.invoke()
                                }
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = n,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                            )
                            if (onUpdate != null) {
                                IconButton(
                                    onClick = {
                                        editingId = id
                                        name = n
                                        errorMsg = null
                                        onClearExternalError?.invoke()
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit_item),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (onDelete != null) {
                                IconButton(onClick = { pendingDeleteId = id }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        if (index < names.lastIndex) {
                            RetailDivider(Modifier.padding(horizontal = 12.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    pendingDeleteId?.let { deleteId ->
        val scheme = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurface,
            title = {
                Text(stringResource(R.string.confirm_delete), color = scheme.onSurface)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke(deleteId)
                        if (editingId == deleteId) {
                            editingId = null
                            name = ""
                        }
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.delete), color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.cancel), color = scheme.primary)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val themeModePref by vm.themeMode.collectAsStateWithLifecycle()
    var dollar by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("") }
    var profit by rememberSaveable { mutableStateOf("") }
    var manager by rememberSaveable { mutableStateOf("") }
    var dollarChange by rememberSaveable { mutableStateOf("") }
    var installment by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("IRR") }
    var dark by rememberSaveable { mutableStateOf(false) }
    var biometric by rememberSaveable { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf("fa") }
    var factor by rememberSaveable { mutableStateOf("") }
    var successMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var formDirty by rememberSaveable { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember {
        androidx.biometric.BiometricManager.from(context)
            .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    val savedOkMessage = stringResource(R.string.saved_ok)
    val batchOkMessage = stringResource(R.string.batch_ok)
    val effectiveThemeMode = themeModePref ?: if (dark) "dark" else "light"

    // Hydrate from Room whenever settings change and the user is not mid-edit.
    // Previously remember(s.id) stuck on the first default SettingsEntity() and never
    // reloaded persisted values (id is always 1), so Save could overwrite real data.
    LaunchedEffect(s) {
        if (formDirty) return@LaunchedEffect
        dollar = if (s.dollarRate == 0.0) "" else Money.format(s.dollarRate)
        tax = emptyOr(s.taxPercent)
        profit = emptyOr(s.profitPercent)
        manager = emptyOr(s.managerPercent)
        dollarChange = emptyOr(s.dollarChangePercent)
        installment = emptyOr(s.installmentDiffPercent)
        currency = s.currencyCode
        dark = s.displayDarkMode
        biometric = s.biometricUnlockEnabled
        language = s.languageTag.ifBlank { "fa" }
    }
    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.settings),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        LazyColumn(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                RetailCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SettingsNumRow(stringResource(R.string.dollar_rate), stringResource(R.string.help_dollar_rate), dollar) {
                            formDirty = true
                            dollar = it
                        }
                        SettingsNumRow(stringResource(R.string.tax_percent), stringResource(R.string.help_tax_percent), tax) {
                            formDirty = true
                            tax = it
                        }
                        SettingsNumRow(stringResource(R.string.profit_percent), stringResource(R.string.help_profit_percent), profit) {
                            formDirty = true
                            profit = it
                        }
                        SettingsNumRow(stringResource(R.string.manager_percent), stringResource(R.string.help_manager_percent), manager) {
                            formDirty = true
                            manager = it
                        }
                        SettingsNumRow(stringResource(R.string.dollar_change_percent), stringResource(R.string.help_dollar_change_percent), dollarChange) {
                            formDirty = true
                            dollarChange = it
                        }
                        SettingsNumRow(stringResource(R.string.installment_diff_percent), stringResource(R.string.help_installment_diff_percent), installment) {
                            formDirty = true
                            installment = it
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RetailTextField(
                                value = currency,
                                onValueChange = {
                                    formDirty = true
                                    currency = it
                                },
                                label = { Text(stringResource(R.string.currency)) },
                                modifier = Modifier.weight(1f),
                            )
                            HelpIconButton(stringResource(R.string.help_currency))
                        }
                        RetailPrimaryButton(
                            onClick = {
                                val parsedDollar = (dollar.toDoubleOrNull() ?: s.dollarRate).coerceAtLeast(0.01)
                                val toSave = SettingsEntity(
                                    id = 1,
                                    dollarRate = parsedDollar,
                                    taxPercent = (tax.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    profitPercent = (profit.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    managerPercent = (manager.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    dollarChangePercent = (dollarChange.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    installmentDiffPercent = (installment.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    currencyCode = currency.ifBlank { "IRR" },
                                    displayDarkMode = dark,
                                    biometricUnlockEnabled = biometric && biometricAvailable,
                                    languageTag = language.ifBlank { "fa" },
                                )
                                formDirty = false
                                vm.saveSettings(toSave)
                                successMsg = savedOkMessage
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) { Text(stringResource(R.string.save)) }
                        RetailOutlinedButton(
                            onClick = {
                                vm.recalculate()
                                successMsg = savedOkMessage
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.recalculate_prices)) }
                    }
                }
            }
            item {
                RetailCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.theme_mode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RetailFilterChip(
                                selected = effectiveThemeMode == "system",
                                label = stringResource(R.string.theme_system),
                                onClick = { vm.setThemeMode("system") },
                            )
                            RetailFilterChip(
                                selected = effectiveThemeMode == "light",
                                label = stringResource(R.string.theme_light),
                                onClick = {
                                    dark = false
                                    vm.setThemeMode("light")
                                },
                            )
                            RetailFilterChip(
                                selected = effectiveThemeMode == "dark",
                                label = stringResource(R.string.theme_dark),
                                onClick = {
                                    dark = true
                                    vm.setThemeMode("dark")
                                },
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.display_dark), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = effectiveThemeMode == "dark" || (effectiveThemeMode != "light" && dark),
                                colors = com.kalaban.farshidb2011.ui.components.retailSwitchColors(),
                                onCheckedChange = {
                                    dark = it
                                    vm.setThemeMode(if (it) "dark" else "light")
                                },
                            )
                        }
                        RetailDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.biometric), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = biometric && biometricAvailable,
                                colors = com.kalaban.farshidb2011.ui.components.retailSwitchColors(),
                                onCheckedChange = {
                                    if (biometricAvailable) {
                                        biometric = it
                                        vm.patchSettings { cur -> cur.copy(biometricUnlockEnabled = it) }
                                    }
                                },
                                enabled = biometricAvailable,
                            )
                        }
                        if (!biometricAvailable) {
                            Text(
                                stringResource(R.string.biometric_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RetailDivider()
                        Text(
                            stringResource(R.string.language),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RetailFilterChip(
                                selected = language.startsWith("fa"),
                                label = stringResource(R.string.lang_fa),
                                onClick = {
                                    language = "fa"
                                    vm.patchSettings { cur -> cur.copy(languageTag = "fa") }
                                },
                            )
                            RetailFilterChip(
                                selected = language.startsWith("en"),
                                label = stringResource(R.string.lang_en),
                                onClick = {
                                    language = "en"
                                    vm.patchSettings { cur -> cur.copy(languageTag = "en") }
                                },
                            )
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RetailTextField(
                                value = factor,
                                onValueChange = { factor = it },
                                label = { Text(stringResource(R.string.price_factor)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                numericLtr = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(bringIntoViewRequester)
                                    .onFocusEvent { state ->
                                        if (state.isFocused) scope.launch { bringIntoViewRequester.bringIntoView() }
                                    },
                            )
                            HelpIconButton(stringResource(R.string.help_price_factor))
                        }
                        RetailPrimaryButton(
                            onClick = {
                                val f = factor.toDoubleOrNull() ?: 1.0
                                if (f > 0.0) {
                                    vm.batchFactor(f)
                                    successMsg = batchOkMessage
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.batch_pricing)) }
                    }
                }
                Spacer(Modifier.height(120.dp))
            }
        }
        } // CompositionLocalProvider
    }
    successMsg?.let { msg ->
        SuccessDialog(message = msg, onDismiss = { successMsg = null })
    }
}

private fun emptyOr(v: Double): String = if (v == 0.0) "" else Money.format(v)

@Composable
private fun SettingsNumRow(label: String, help: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RetailTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            numericLtr = true,
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        )
        HelpIconButton(help)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    vm: AppViewModel,
    onBack: (() -> Unit)? = null,
    onNew: () -> Unit,
    embedded: Boolean = false,
) {
    val sales by vm.sales.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var selectedSaleId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            if (!embedded) {
                com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                    title = stringResource(R.string.sales),
                    onBack = onBack,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) { Icon(Icons.Default.Add, null) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = embedded),
    ) { pad ->
        if (sales.isEmpty()) {
            com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                icon = Icons.Outlined.Payments,
                title = stringResource(R.string.no_sales_yet),
                modifier = Modifier.padding(pad),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            ) {
                items(sales, key = { it.id }) { sale ->
                    val typeLabel = if (sale.paymentType == PaymentType.CREDIT) {
                        stringResource(R.string.credit)
                    } else {
                        stringResource(R.string.cash)
                    }
                    RetailCard(
                        modifier = Modifier.clickable { selectedSaleId = sale.id },
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(sale.buyerName, color = MaterialTheme.colorScheme.onSurface)
                            },
                            supportingContent = {
                                Text(
                                    buildString {
                                        append("${sale.jalaliDate} · $typeLabel · ${Money.format(sale.totalAmount)} ${settings.currencyCode}")
                                        if (sale.paymentType == PaymentType.CREDIT) {
                                            append(" · ${stringResource(R.string.open_balance)}: ${Money.format(sale.openBalance)}")
                                        }
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                Text(
                                    Money.format(sale.totalAmount),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    selectedSaleId?.let { id ->
        val sale = sales.firstOrNull { it.id == id }
        if (sale != null) {
            SaleDetailDialog(
                sale = sale,
                currencyCode = settings.currencyCode,
                fa = fa,
                vm = vm,
                onDismiss = { selectedSaleId = null },
            )
        } else {
            selectedSaleId = null
        }
    }
}

@Composable
private fun SaleDetailDialog(
    sale: com.kalaban.farshidb2011.data.local.entity.SaleEntity,
    currencyCode: String,
    fa: Boolean,
    vm: AppViewModel,
    onDismiss: () -> Unit,
) {
    val lines by vm.observeLinesForSale(sale.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val payments by vm.observePaymentsForSale(sale.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val scheme = MaterialTheme.colorScheme
    val typeLabel = if (sale.paymentType == PaymentType.CREDIT) {
        stringResource(R.string.credit)
    } else {
        stringResource(R.string.cash)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurface,
        title = {
            Text(
                "${stringResource(R.string.sale_details)} — ${sale.buyerName}",
                color = scheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${sale.jalaliDate} · $typeLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                sale.dueAtEpochMs?.let { dueMs ->
                    val dueText = JalaliDate.fromEpochMs(dueMs).format()
                    Text(
                        "${stringResource(R.string.due_date)}: $dueText",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                RetailDivider()
                lines.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${line.itemTitle} (${line.quantity} × ${Money.format(line.unitPrice)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            Money.format(line.lineTotal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.tertiary,
                        )
                    }
                }
                RetailDivider()
                Text(
                    "${stringResource(R.string.total)}: ${Money.format(sale.totalAmount)} $currencyCode",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                if (sale.paymentType == PaymentType.CREDIT) {
                    Text(
                        "${stringResource(R.string.open_balance)}: ${Money.format(sale.openBalance)} $currencyCode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sale.openBalance > 0.0) scheme.error else scheme.primary,
                    )
                    if (sale.manuallyZeroed && !sale.zeroReason.isNullOrBlank()) {
                        Text(
                            "${stringResource(R.string.zeroed_reason)}: ${sale.zeroReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (payments.isNotEmpty()) {
                    RetailDivider()
                    Text(
                        stringResource(R.string.payment_history),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                    )
                    payments.forEach { pay ->
                        val dateStr = if (fa) {
                            JalaliDate.fromEpochMs(pay.paidAtEpochMs).format()
                        } else {
                            DateFormat.getDateInstance(DateFormat.SHORT).format(Date(pay.paidAtEpochMs))
                        }
                        Text(
                            "$dateStr: ${Money.format(pay.amount)} $currencyCode",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok), color = scheme.primary)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(vm: AppViewModel, onBack: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val draft by vm.newSaleDraft.collectAsStateWithLifecycle()
    var dateError by rememberSaveable { mutableStateOf(false) }
    var itemMenuOpen by rememberSaveable { mutableStateOf(false) }
    var lastPricedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val buyer = draft.buyer
    val credit = draft.credit
    val jalaliDate = draft.jalaliDate
    val dueDaysText = draft.dueDaysText
    val selectedItemId = draft.selectedItemId
    val qty = draft.qty
    val unitPriceText = draft.unitPriceText
    val cart = draft.cart
    val cartTotal = cart.sumOf { it.quantity * it.unitPrice }
    val selectedItem = items.firstOrNull { it.id == selectedItemId }
    val canSubmit = cart.isNotEmpty() && buyer.isNotBlank() && !dateError

    // Ensure full item list is visible even if ItemsScreen had an active search filter.
    LaunchedEffect(Unit) {
        vm.setQuery("")
    }

    // Fill unit price when the selected item or cash/credit mode changes.
    LaunchedEffect(selectedItemId, credit, items) {
        val id = selectedItemId ?: return@LaunchedEffect
        val key = "$id:$credit"
        if (lastPricedKey == key) return@LaunchedEffect
        val item = items.firstOrNull { it.id == id } ?: return@LaunchedEffect
        vm.updateNewSaleDraft { it.copy(unitPriceText = Money.format(vm.unitPriceForSale(item, installment = credit))) }
        lastPricedKey = key
    }

    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.record_sale),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        RetailContent(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "sale_header") {
                    RetailCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RetailTextField(
                                value = buyer,
                                onValueChange = { value -> vm.updateNewSaleDraft { it.copy(buyer = value) } },
                                label = { Text(stringResource(R.string.buyer_name)) },
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RetailFilterChip(
                                    selected = !credit,
                                    label = stringResource(R.string.cash),
                                    onClick = { vm.updateNewSaleDraft { it.copy(credit = false) } },
                                )
                                RetailFilterChip(
                                    selected = credit,
                                    label = stringResource(R.string.credit),
                                    onClick = { vm.updateNewSaleDraft { it.copy(credit = true) } },
                                )
                            }
                            RetailTextField(
                                value = jalaliDate,
                                onValueChange = { value ->
                                    vm.updateNewSaleDraft { it.copy(jalaliDate = value) }
                                    dateError = JalaliDate.parse(value) == null
                                },
                                label = { Text(stringResource(R.string.sale_date)) },
                                numericLtr = true,
                            )
                            if (dateError) {
                                Text(
                                    stringResource(R.string.sale_invalid_date),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (credit) {
                                RetailTextField(
                                    value = dueDaysText,
                                    onValueChange = { value -> vm.updateNewSaleDraft { it.copy(dueDaysText = value) } },
                                    label = { Text(stringResource(R.string.due_days)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    numericLtr = true,
                                )
                            }
                        }
                    }
                }

                item(key = "add_item_card") {
                    RetailCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.add_to_cart),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            ExposedDropdownMenuBox(
                                expanded = itemMenuOpen,
                                onExpandedChange = { itemMenuOpen = !itemMenuOpen },
                            ) {
                                OutlinedTextField(
                                    value = selectedItem?.title ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.select_item)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemMenuOpen) },
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = retailTextFieldColors(),
                                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                                )
                                ExposedDropdownMenu(
                                    expanded = itemMenuOpen,
                                    onDismissRequest = { itemMenuOpen = false },
                                ) {
                                    items.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "${item.title} · ${Money.format(vm.unitPriceForSale(item, installment = credit))}",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            },
                                            onClick = {
                                                lastPricedKey = null
                                                vm.updateNewSaleDraft { it.copy(selectedItemId = item.id) }
                                                itemMenuOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                            RetailTextField(
                                value = qty,
                                onValueChange = { value -> vm.updateNewSaleDraft { it.copy(qty = value) } },
                                label = {
                                    Text(
                                        if (selectedItem != null) {
                                            "${stringResource(R.string.qty)} — ${selectedItem.title}"
                                        } else {
                                            stringResource(R.string.qty)
                                        },
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                numericLtr = true,
                            )
                            RetailTextField(
                                value = unitPriceText,
                                onValueChange = { value -> vm.updateNewSaleDraft { it.copy(unitPriceText = value) } },
                                label = { Text(stringResource(R.string.unit_price_editable)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                numericLtr = true,
                            )
                            RetailPrimaryButton(
                                onClick = {
                                    val item = selectedItem ?: return@RetailPrimaryButton
                                    val q = qty.toDoubleOrNull() ?: return@RetailPrimaryButton
                                    val price = Money.round(
                                        unitPriceText.toDoubleOrNull() ?: vm.unitPriceForSale(item, installment = credit),
                                    )
                                    if (q <= 0.0) return@RetailPrimaryButton
                                    vm.updateNewSaleDraft { d ->
                                        d.copy(
                                            cart = d.cart + SaleCartLineDraft(item.id, item.title, q, price),
                                            qty = "1",
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedItem != null,
                            ) { Text(stringResource(R.string.add_line)) }
                            if (selectedItem == null && items.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.sale_need_item),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item(key = "cart_header") {
                    Text(
                        stringResource(R.string.cart),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(cart.size) { index ->
                    val line = cart[index]
                    RetailCard {
                        ListItem(
                            headlineContent = {
                                Text(line.title, color = MaterialTheme.colorScheme.onSurface)
                            },
                            supportingContent = {
                                Text(
                                    "${line.quantity} × ${Money.format(line.unitPrice)} = ${Money.format(line.quantity * line.unitPrice)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        vm.updateNewSaleDraft { d ->
                                            d.copy(cart = d.cart.filterIndexed { i, _ -> i != index })
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                        )
                    }
                }

                item(key = "cart_footer") {
                    Text(
                        "${stringResource(R.string.total)}: ${Money.format(cartTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    if (!canSubmit) {
                        Text(
                            when {
                                dateError -> stringResource(R.string.sale_invalid_date)
                                buyer.isBlank() -> stringResource(R.string.sale_need_buyer)
                                cart.isEmpty() -> stringResource(R.string.sale_need_cart)
                                else -> ""
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    RetailPrimaryButton(
                        onClick = {
                            val parsed = JalaliDate.parse(jalaliDate)
                            if (parsed == null) {
                                dateError = true
                                return@RetailPrimaryButton
                            }
                            if (cart.isEmpty() || buyer.isBlank()) return@RetailPrimaryButton
                            val createdAt = parsed.toGregorianEpochMs()
                            val dueDays = (dueDaysText.toLongOrNull() ?: 7L).coerceAtLeast(1L)
                            val due = if (credit) createdAt + dueDays * 24L * 60 * 60 * 1000 else null
                            val lines = cart.mapNotNull { line ->
                                val item = items.firstOrNull { it.id == line.itemId } ?: return@mapNotNull null
                                ShopRepository.CartLine(item, line.quantity, line.unitPrice)
                            }
                            if (lines.size != cart.size) return@RetailPrimaryButton
                            vm.recordSale(
                                buyer = buyer.trim(),
                                type = if (credit) PaymentType.CREDIT else PaymentType.CASH,
                                lines = lines,
                                dueMs = due,
                                jalaliDate = parsed.format(),
                                createdAtEpochMs = createdAt,
                            )
                            vm.clearNewSaleDraft()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSubmit,
                    ) { Text(stringResource(R.string.record_sale)) }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val credits by vm.openCredits.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var payAmount by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSelectHint by rememberSaveable { mutableStateOf(false) }
    var showAmountHint by rememberSaveable { mutableStateOf(false) }
    var showReasonHint by rememberSaveable { mutableStateOf(false) }
    var confirmZero by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(credits) {
        if (selectedId != null && credits.none { it.id == selectedId }) {
            selectedId = null
        }
    }

    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.credits),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .imePadding(),
        ) {
            if (credits.isEmpty()) {
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.Center) {
                    com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                        icon = Icons.Outlined.Payments,
                        title = stringResource(R.string.no_credits),
                    )
                }
            } else {
                RetailCard(Modifier.weight(1f, fill = true)) {
                    LazyColumn {
                        items(credits, key = { it.id }) { sale ->
                            val selected = selectedId == sale.id
                            val dueStr = sale.dueAtEpochMs?.let { JalaliDate.fromEpochMs(it).format() }
                            ListItem(
                                headlineContent = {
                                    Text(sale.buyerName, color = MaterialTheme.colorScheme.onSurface)
                                },
                                supportingContent = {
                                    Text(
                                        buildString {
                                            append("${Money.format(sale.openBalance)} ${settings.currencyCode}")
                                            append(" · ${sale.jalaliDate}")
                                            if (dueStr != null) {
                                                append(" · ${stringResource(R.string.due_date)}: $dueStr")
                                            }
                                        },
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                },
                                modifier = Modifier
                                    .clickable {
                                        selectedId = sale.id
                                        showSelectHint = false
                                    }
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                        else MaterialTheme.colorScheme.surface,
                                    ),
                            )
                            RetailDivider()
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (showSelectHint) {
                Text(
                    stringResource(R.string.select_credit),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (showAmountHint) {
                Text(
                    stringResource(R.string.amount_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            RetailTextField(
                value = payAmount,
                onValueChange = {
                    payAmount = it
                    showAmountHint = false
                },
                label = { Text(stringResource(R.string.partial_payment)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                numericLtr = true,
            )
            RetailPrimaryButton(
                onClick = {
                    val id = selectedId
                    if (id == null) {
                        showSelectHint = true
                        return@RetailPrimaryButton
                    }
                    val amt = payAmount.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        showAmountHint = true
                        return@RetailPrimaryButton
                    }
                    vm.partialPay(id, amt)
                    payAmount = ""
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(stringResource(R.string.partial_payment)) }
            Spacer(Modifier.height(8.dp))
            if (showReasonHint) {
                Text(
                    stringResource(R.string.reason_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            RetailTextField(
                value = reason,
                onValueChange = {
                    reason = it
                    showReasonHint = false
                },
                label = { Text(stringResource(R.string.reason)) },
            )
            RetailOutlinedButton(
                onClick = {
                    val id = selectedId
                    if (id == null) {
                        showSelectHint = true
                        return@RetailOutlinedButton
                    }
                    if (reason.isBlank()) {
                        showReasonHint = true
                        return@RetailOutlinedButton
                    }
                    confirmZero = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(stringResource(R.string.zero_balance)) }
        }
    }

    if (confirmZero) {
        val scheme = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { confirmZero = false },
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurface,
            title = {
                Text(stringResource(R.string.zero_balance), color = scheme.onSurface)
            },
            text = {
                Text(stringResource(R.string.confirm_delete), color = scheme.onSurface)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = selectedId
                        if (id != null && reason.isNotBlank()) {
                            vm.zeroBalance(id, reason.trim())
                            reason = ""
                        }
                        confirmZero = false
                    },
                ) {
                    Text(stringResource(R.string.ok), color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmZero = false }) {
                    Text(stringResource(R.string.cancel), color = scheme.primary)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    vm: AppViewModel,
    onBack: (() -> Unit)? = null,
    embedded: Boolean = false,
) {
    val report by vm.report.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sales by vm.sales.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var selectedName by rememberSaveable { mutableStateOf(ShopRepository.ReportRange.DAILY.name) }
    val selected = runCatching { ShopRepository.ReportRange.valueOf(selectedName) }
        .getOrDefault(ShopRepository.ReportRange.DAILY)
    // Reload from Room whenever the range changes or sales data updates.
    LaunchedEffect(selected, sales) { vm.loadReport(selected) }
    val ranges = listOf(
        R.string.range_daily to ShopRepository.ReportRange.DAILY,
        R.string.range_weekly to ShopRepository.ReportRange.WEEKLY,
        R.string.range_monthly to ShopRepository.ReportRange.MONTHLY,
        R.string.range_quarterly to ShopRepository.ReportRange.QUARTERLY,
        R.string.range_biannual to ShopRepository.ReportRange.BIANNUAL,
        R.string.range_yearly to ShopRepository.ReportRange.YEARLY,
    )
    Scaffold(
        topBar = {
            if (!embedded) {
                com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                    title = stringResource(R.string.reports),
                    onBack = onBack,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(embedded = embedded),
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            RetailCard {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.reports),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ranges.forEach { (label, range) ->
                            RetailPeriodChip(
                                selected = selected == range,
                                label = stringResource(label),
                                onClick = {
                                    selectedName = range.name
                                    vm.loadReport(range)
                                },
                            )
                        }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f, fill = true)) {
                if (report.groups.isEmpty()) {
                    item(key = "no_sales_report") {
                        com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                            icon = Icons.Outlined.History,
                            title = stringResource(R.string.no_sales_in_period),
                        )
                    }
                }
                report.groups.forEach { group ->
                    val catName = if (fa) group.categoryNameFa else group.categoryNameEn
                    item(key = "cat-${group.categoryId}-$catName") {
                        Text(
                            catName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(group.lines, key = { "line-${group.categoryId}-${it.itemId}-${it.unitPrice}" }) { line ->
                        RetailCard {
                            ListItem(
                                headlineContent = {
                                    Text(line.itemTitle, color = MaterialTheme.colorScheme.onSurface)
                                },
                                supportingContent = {
                                    Text(
                                        "${stringResource(R.string.qty_sold)}: ${line.quantity} · " +
                                            "${stringResource(R.string.sell_price)}: ${Money.format(line.unitPrice)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                trailingContent = {
                                    Text(
                                        Money.format(line.lineTotal),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                },
                            )
                        }
                    }
                    item(key = "sub-${group.categoryId}-$catName") {
                        Text(
                            "${stringResource(R.string.category_subtotal)}: ${Money.format(group.subtotal)} ${settings.currencyCode}",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
            RetailCard {
                Text(
                    "${stringResource(R.string.grand_total)}: ${Money.format(report.grandTotal)} ${settings.currencyCode}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockScreen(vm: AppViewModel, onBack: () -> Unit) {
    val items by vm.lowStock.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.low_stock),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        if (items.isEmpty()) {
            com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                icon = Icons.Outlined.WarningAmber,
                title = stringResource(R.string.no_low_stock),
                modifier = Modifier.padding(pad),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val unitName = units.firstOrNull { it.id == item.unitId }?.let {
                        if (fa) it.nameFa else it.nameEn
                    }
                    RetailCard {
                        ListItem(
                            leadingContent = { ItemThumb(item.imageUri) },
                            headlineContent = {
                                Text(item.title, color = MaterialTheme.colorScheme.onSurface)
                            },
                            supportingContent = {
                                Text(
                                    buildString {
                                        append("${stringResource(R.string.stock)}: ${item.stockQty} / ${item.lowStockThreshold}")
                                        if (!unitName.isNullOrBlank()) append(" ($unitName)")
                                    },
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(vm: AppViewModel, onBack: () -> Unit) {
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadReminders() }
    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.reminders),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        if (reminders.isEmpty()) {
            com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.no_reminders),
                modifier = Modifier.padding(pad),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(reminders, key = { it.id }) { sale ->
                    val dueStr = sale.dueAtEpochMs?.let { JalaliDate.fromEpochMs(it).format() }
                    RetailCard {
                        ListItem(
                            headlineContent = {
                                Text(sale.buyerName, color = MaterialTheme.colorScheme.onSurface)
                            },
                            supportingContent = {
                                Text(
                                    buildString {
                                        append("${stringResource(R.string.open_balance)}: ${Money.format(sale.openBalance)} ${settings.currencyCode}")
                                        if (dueStr != null) {
                                            append(" · ${stringResource(R.string.due_date)}: $dueStr")
                                        }
                                    },
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val msg by vm.backupMessage.collectAsStateWithLifecycle()
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) vm.exportToUri(uri)
    }
    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) vm.importFromUri(uri)
    }

    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.backup),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RetailCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RetailPrimaryButton(
                        onClick = {
                            createDocLauncher.launch(
                                com.kalaban.farshidb2011.data.backup.KalabanBackupArchive.FILE_NAME,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Outlined.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_to_file))
                    }
                    RetailOutlinedButton(
                        onClick = {
                            openDocLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Outlined.FileDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.import_from_file))
                    }
                    RetailDivider()
                    RetailOutlinedButton(
                        onClick = {
                            val file = File(
                                context.filesDir,
                                com.kalaban.farshidb2011.data.backup.KalabanBackupArchive.FILE_NAME,
                            )
                            vm.exportTo(file)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(stringResource(R.string.quick_local_backup))
                    }
                    RetailOutlinedButton(
                        onClick = {
                            val kalb = File(
                                context.filesDir,
                                com.kalaban.farshidb2011.data.backup.KalabanBackupArchive.FILE_NAME,
                            )
                            val legacyJson = File(context.filesDir, "kalaban-backup.json")
                            when {
                                kalb.exists() -> vm.importFrom(kalb)
                                legacyJson.exists() -> vm.importFrom(legacyJson)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(stringResource(R.string.quick_local_restore))
                    }
                }
            }
            msg?.let {
                RetailCard {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DollarHistoryScreen(vm: AppViewModel, onBack: () -> Unit) {
    val history by vm.dollarHistory.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            com.kalaban.farshidb2011.ui.components.RetailTopAppBar(
                title = stringResource(R.string.dollar_history),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { confirmClear = true },
                        enabled = history.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear_dollar_history),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = false),
    ) { pad ->
        if (history.isEmpty()) {
            com.kalaban.farshidb2011.ui.components.RetailEmptyState(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.no_dollar_history),
                modifier = Modifier.padding(pad),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(history, key = { it.id }) { row ->
                    val whenText = if (fa) {
                        JalaliDate.fromEpochMs(row.recordedAtEpochMs).format()
                    } else {
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(row.recordedAtEpochMs))
                    }
                    val noteText = when (row.note?.trim()?.lowercase()) {
                        null, "" -> null
                        "initial" -> stringResource(R.string.dollar_note_initial)
                        "settings_update", "setting_update", "setting update", "settings update" ->
                            stringResource(R.string.dollar_note_settings_update)
                        else -> row.note
                    }
                    RetailCard {
                        ListItem(
                            headlineContent = {
                                Text(
                                    "${Money.format(row.rate)} ${settings.currencyCode}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            supportingContent = {
                                Text(
                                    whenText + (noteText?.let { " · $it" } ?: ""),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
    if (confirmClear) {
        val scheme = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurface,
            title = {
                Text(
                    stringResource(R.string.confirm_clear_history),
                    color = scheme.onSurface,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearDollarHistory()
                        confirmClear = false
                    },
                ) {
                    Text(stringResource(R.string.delete), color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel), color = scheme.primary)
                }
            },
        )
    }
}
