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
import com.kalaban.farshidb2011.ui.SaleCartLineDraft
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailDivider
import com.kalaban.farshidb2011.ui.components.RetailMenuRow
import com.kalaban.farshidb2011.ui.components.RetailTextField
import com.kalaban.farshidb2011.ui.components.RetailOutlinedButton
import com.kalaban.farshidb2011.ui.components.RetailPrimaryButton
import com.kalaban.farshidb2011.ui.components.RetailContent
import com.kalaban.farshidb2011.ui.components.retailContentWindowInsets
import com.kalaban.farshidb2011.ui.components.JalaliDateField
import com.kalaban.farshidb2011.ui.components.SearchField
import com.kalaban.farshidb2011.ui.theme.semantic
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.toAmountOrNull
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.data.repository.AuthResult
import com.kalaban.farshidb2011.data.repository.DisplayPriceMode
import com.kalaban.farshidb2011.domain.Permission
import androidx.compose.material3.ListItemDefaults
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

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
    var lockedSeconds by rememberSaveable { mutableStateOf(0L) }
    var resetAfterBiometric by rememberSaveable { mutableStateOf(false) }
    fun handle(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> onUnlocked()
            is AuthResult.LockedOut -> {
                lockedSeconds = (result.remainingMs + 999) / 1000
                error = false
            }
            AuthResult.Invalid -> error = true
        }
    }
    LaunchedEffect(lockedSeconds) {
        if (lockedSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            lockedSeconds -= 1
        }
    }
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
                        vm.unlockWithBiometric()
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
                        if (lockedSeconds > 0) {
                            Text(
                                stringResource(R.string.pin_locked_out, lockedSeconds),
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
                                        vm.unlockWithPin(pin, ::handle)
                                    }
                                } else {
                                    if (pin.isBlank()) {
                                        error = true
                                        return@RetailPrimaryButton
                                    }
                                    vm.unlockWithPin(pin, ::handle)
                                }
                            },
                            enabled = lockedSeconds <= 0L,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DisplayPanel(vm: AppViewModel, onOpenAdmin: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    val pricingContext by vm.pricingContext.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val showCash = config.displayPriceMode == DisplayPriceMode.CASH
    LaunchedEffect(showCash, settings) { if (showCash) vm.refreshPricingContext() }
    fun shownPrice(item: ItemEntity): java.math.BigDecimal {
        val ctx = pricingContext
        return if (showCash && ctx != null) {
            PriceCalculator.salePrice(Money.of(item.sellPrice), ctx.policy).cashPrice
        } else {
            Money.of(item.sellPrice)
        }
    }
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
                title = {
                    Text(
                        config.businessName.ifBlank { stringResource(R.string.display_panel) },
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                },
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
                            price = shownPrice(item),
                            currencyCode = settings.currencyCode,
                            unitName = unitName,
                            categoryName = displayName,
                            showStock = config.displayShowStock,
                            showImage = config.displayShowImages,
                            onImageClick = { uri -> previewUri = uri },
                        )
                    }
                }
                if (config.businessNote.isNotBlank() || config.businessPhone.isNotBlank()) {
                    item(key = "business_footer") {
                        Text(
                            listOf(config.businessNote, config.businessPhone, config.businessAddress).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
    price: java.math.BigDecimal,
    currencyCode: String,
    unitName: String?,
    categoryName: String?,
    showStock: Boolean,
    showImage: Boolean,
    onImageClick: (String) -> Unit,
) {
    val density = LocalDensity.current
    val cardImageMaxPx = with(density) { 720.dp.roundToPx() }
    RetailCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            if (showImage) {
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
            }
            if (showImage && item.imageUri != null) {
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
                    "${Money.format(price)} $currencyCode",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                val details = buildList {
                    if (showStock) add("${stringResource(R.string.stock)}: ${Money.input(item.stockQty)}")
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
internal fun ProductImage(
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
internal fun ItemThumb(uri: String?) {
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
                val taxTotal = lines.sumOf { it.taxAmount }
                val discountTotal = lines.sumOf { it.discountAmount }
                if (taxTotal > 0.0) {
                    Text("${stringResource(R.string.tax_collected)}: ${Money.format(taxTotal)}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                if (discountTotal > 0.0) {
                    Text("${stringResource(R.string.discounts_given)}: ${Money.format(discountTotal)}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                if (vm.can(Permission.VIEW_COSTS) && lines.isNotEmpty() && lines.all { it.unitCost != null }) {
                    val profit = lines.sumOf { it.lineTotal - it.taxAmount - (it.unitCost ?: 0.0) * it.quantity }
                    Text(
                        "${stringResource(R.string.net_profit)}: ${Money.format(profit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (profit < 0) scheme.error else MaterialTheme.semantic.success,
                    )
                }
                sale.createdBy?.let {
                    Text("${stringResource(R.string.user)}: ${actorLabel(it)}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
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
    val pricingContext by vm.pricingContext.collectAsStateWithLifecycle()
    val canEditPrice = vm.can(Permission.EDIT_SALE_PRICE)
    var itemSearch by rememberSaveable { mutableStateOf("") }
    val quantity = qty.toAmountOrNull()
    val quote = remember(selectedItem, quantity, credit, pricingContext) {
        selectedItem?.let { vm.quote(it, quantity ?: 1.0, credit) }
    }

    // Ensure the full catalog is searchable even if another screen left a filter; load current pricing rules.
    LaunchedEffect(Unit) {
        vm.setQuery("")
        vm.refreshPricingContext()
    }

    // Re-quote when the product, quantity tier or cash/credit mode changes.
    LaunchedEffect(selectedItemId, credit, quantity, pricingContext) {
        val q = quote ?: return@LaunchedEffect
        val key = "$selectedItemId:$credit:$quantity"
        if (lastPricedKey == key) return@LaunchedEffect
        vm.updateNewSaleDraft { it.copy(unitPriceText = Money.input(q.unitPrice)) }
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
                            JalaliDateField(
                                label = stringResource(R.string.sale_date),
                                value = JalaliDate.parse(jalaliDate),
                                onPick = { picked ->
                                    vm.updateNewSaleDraft { it.copy(jalaliDate = picked.format()) }
                                    dateError = false
                                },
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
                            SearchField(itemSearch, { itemSearch = it }, stringResource(R.string.search_product_sale))
                            val matches = remember(items, itemSearch) {
                                val term = itemSearch.trim()
                                if (term.isEmpty()) emptyList() else items.filter {
                                    it.title.contains(term, true) || it.sku.orEmpty().contains(term, true) || it.barcode.orEmpty() == term
                                }.take(8)
                            }
                            matches.forEach { item ->
                                ListItem(
                                    headlineContent = { Text(item.title, color = MaterialTheme.colorScheme.onSurface) },
                                    supportingContent = {
                                        Text(
                                            listOfNotNull(
                                                vm.quote(item, 1.0, credit)?.let { Money.format(it.unitPrice) },
                                                item.sku?.takeIf { it.isNotBlank() },
                                                "${stringResource(R.string.stock)}: ${Money.input(item.stockQty)}",
                                            ).joinToString(" · "),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        lastPricedKey = null
                                        vm.updateNewSaleDraft { it.copy(selectedItemId = item.id) }
                                        itemSearch = ""
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                )
                            }
                            if (itemSearch.isNotBlank() && matches.isEmpty()) {
                                Text(stringResource(R.string.no_search_results), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            selectedItem?.let { picked ->
                                Text(
                                    "${stringResource(R.string.selected_item)}: ${picked.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
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
                                readOnly = !canEditPrice,
                                supportingText = if (!canEditPrice) stringResource(R.string.price_edit_not_allowed) else null,
                            )
                            quote?.let { qt ->
                                if (qt.discountPct.signum() > 0) {
                                    Text(
                                        stringResource(R.string.tier_discount_applied, Money.input(qt.discountPct), Money.format(qt.listPrice)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.semantic.success,
                                    )
                                }
                                val typed = unitPriceText.toAmountOrNull()?.let(Money::of)
                                if (typed != null && qt.minimum != null && typed < qt.minimum) {
                                    Text(stringResource(R.string.below_minimum_price, Money.format(qt.minimum)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                } else if (typed != null && qt.unitCost.signum() > 0 && typed < qt.breakEven && vm.can(Permission.VIEW_COSTS)) {
                                    Text(stringResource(R.string.below_break_even, Money.format(qt.breakEven)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            RetailPrimaryButton(
                                onClick = {
                                    val item = selectedItem ?: return@RetailPrimaryButton
                                    val q = qty.toAmountOrNull() ?: return@RetailPrimaryButton
                                    if (q <= 0.0) return@RetailPrimaryButton
                                    val lineQuote = vm.quote(item, q, credit)
                                    val typed = if (canEditPrice) unitPriceText.toAmountOrNull() else null
                                    val price = Money.round(typed ?: lineQuote?.unitPrice?.toDouble() ?: item.sellPrice)
                                    val list = lineQuote?.listPrice?.toDouble() ?: price
                                    vm.updateNewSaleDraft { d ->
                                        d.copy(
                                            cart = d.cart + SaleCartLineDraft(
                                                itemId = item.id,
                                                title = item.title,
                                                quantity = q,
                                                unitPrice = price,
                                                unitCost = lineQuote?.unitCost?.toDouble(),
                                                taxPerUnit = vm.taxPortion(price),
                                                discountPerUnit = (list - price).coerceAtLeast(0.0),
                                            ),
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
                            val dueDays = (Numbers.parseInt(dueDaysText)?.toLong() ?: 7L).coerceAtLeast(1L)
                            val due = if (credit) createdAt + dueDays * 24L * 60 * 60 * 1000 else null
                            val lines = cart.mapNotNull { line ->
                                val item = items.firstOrNull { it.id == line.itemId } ?: return@mapNotNull null
                                ShopRepository.CartLine(item, line.quantity, line.unitPrice, line.unitCost, line.taxPerUnit, line.discountPerUnit)
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
                    val amt = payAmount.toAmountOrNull() ?: 0.0
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
fun BackupScreen(vm: AppViewModel, onBack: () -> Unit, onOpenImport: () -> Unit) {
    val context = LocalContext.current
    val msg by vm.backupMessage.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    var pendingRestore by remember { mutableStateOf<(() -> Unit)?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) vm.exportToUri(uri)
    }
    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingRestore = { vm.importFromUri(uri) }
    }
    pendingRestore?.let { restore ->
        com.kalaban.farshidb2011.ui.components.ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            text = stringResource(R.string.restore_confirm_text),
            confirmLabel = stringResource(R.string.restore),
            destructive = true,
            onConfirm = {
                pendingRestore = null
                restore()
            },
            onDismiss = { pendingRestore = null },
        )
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
            com.kalaban.farshidb2011.ui.components.InfoBanner(
                stringResource(
                    R.string.last_backup_info,
                    if (config.lastBackupAt > 0) JalaliDate.fromEpochMs(config.lastBackupAt).format() else stringResource(R.string.never),
                ),
                com.kalaban.farshidb2011.ui.components.Tone.INFO,
            )
            RetailCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                kalb.exists() -> pendingRestore = { vm.importFrom(kalb) }
                                legacyJson.exists() -> pendingRestore = { vm.importFrom(legacyJson) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(stringResource(R.string.quick_local_restore))
                    }
                }
            }
            RetailCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.import_export), color = MaterialTheme.colorScheme.onSurface) },
                    supportingContent = { Text(stringResource(R.string.import_export_sub), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Outlined.FileDownload, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable(onClick = onOpenImport),
                )
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
