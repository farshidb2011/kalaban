package com.kalaban.farshidb2011.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.PricingEngine
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.PaymentType
import com.kalaban.farshidb2011.data.local.entity.SaleEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UnitEntity
import com.kalaban.farshidb2011.data.repository.ShopRepository
import com.kalaban.farshidb2011.notifications.ReminderNotifications
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** In-progress product form — survives configuration changes via ViewModel. */
data class ItemFormDraft(
    val showForm: Boolean = false,
    val editingId: Long = 0L,
    val title: String = "",
    val packTotal: String = "",
    val piecesPerUnitText: String = "1",
    val stockUnits: String = "",
    val lowStockThresholdText: String = "1",
    val sku: String = "",
    val selectedUnitId: Long? = null,
    val selectedCategoryId: Long? = null,
    val imageUri: String? = null,
    val pendingCameraUri: String? = null,
    val listQuery: String = "",
)

data class SaleCartLineDraft(
    val itemId: Long,
    val title: String,
    val quantity: Double,
    val unitPrice: Double,
)

/** In-progress new-sale form — survives configuration changes via ViewModel. */
data class NewSaleDraft(
    val buyer: String = "",
    val credit: Boolean = false,
    val jalaliDate: String = JalaliDate.today().format(),
    val dueDaysText: String = "7",
    val selectedItemId: Long? = null,
    val qty: String = "1",
    val unitPriceText: String = "",
    val cart: List<SaleCartLineDraft> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    application: Application,
    private val repo: ShopRepository = KalabanApp.instance.repository,
    private val secure: com.kalaban.farshidb2011.data.prefs.SecurePrefs = KalabanApp.instance.securePrefs,
) : AndroidViewModel(application) {

    private val query = MutableStateFlow("")

    /**
     * Keep Room/DataStore flows hot for the ViewModel lifetime.
     * [SharingStarted.WhileSubscribed] resets to the initial empty/default value after the
     * stop timeout, which made lists and settings appear wiped after navigation.
     */
    private val keepAlive = SharingStarted.Eagerly

    val items = query.flatMapLatest { repo.observeItems(it) }
        .stateIn(viewModelScope, keepAlive, emptyList())

    val units = repo.observeUnits().stateIn(viewModelScope, keepAlive, emptyList())
    val categories = repo.observeCategories().stateIn(viewModelScope, keepAlive, emptyList())
    val settings = repo.observeSettings().stateIn(viewModelScope, keepAlive, SettingsEntity())
    val sales = repo.observeSales().stateIn(viewModelScope, keepAlive, emptyList())
    val openCredits = repo.observeOpenCredits().stateIn(viewModelScope, keepAlive, emptyList())
    val lowStock = repo.observeLowStock().stateIn(viewModelScope, keepAlive, emptyList())
    val dollarHistory = repo.observeDollarHistory().stateIn(viewModelScope, keepAlive, emptyList())
    val unlocked = secure.sessionUnlocked.stateIn(viewModelScope, keepAlive, false)
    val hasPin = secure.pinHash.map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, keepAlive, false)
    val themeMode: StateFlow<String?> = secure.themeMode
        .stateIn(viewModelScope, keepAlive, null)

    private val _report = MutableStateFlow(ShopRepository.DetailedSalesReport())
    val report: StateFlow<ShopRepository.DetailedSalesReport> = _report
    private val _reminders = MutableStateFlow<List<SaleEntity>>(emptyList())
    val reminders: StateFlow<List<SaleEntity>> = _reminders
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage

    private val _itemForm = MutableStateFlow(ItemFormDraft())
    val itemForm: StateFlow<ItemFormDraft> = _itemForm.asStateFlow()

    private val _newSaleDraft = MutableStateFlow(NewSaleDraft())
    val newSaleDraft: StateFlow<NewSaleDraft> = _newSaleDraft.asStateFlow()

    fun setQuery(value: String) {
        query.value = value
    }

    fun updateItemForm(transform: (ItemFormDraft) -> ItemFormDraft) {
        _itemForm.update(transform)
    }

    fun resetItemForm(defaultUnitId: Long? = null) {
        _itemForm.value = ItemFormDraft(selectedUnitId = defaultUnitId)
        query.value = ""
    }

    fun openItemForm(item: ItemEntity) {
        val pieces = item.piecesPerUnit.coerceAtLeast(1.0)
        val threshold = item.lowStockThreshold.coerceAtLeast(0.0)
        val thresholdStr = if (threshold % 1.0 == 0.0) threshold.toLong().toString() else Money.format(threshold)
        _itemForm.value = ItemFormDraft(
            showForm = true,
            editingId = item.id,
            title = item.title,
            piecesPerUnitText = if (pieces == 1.0) "1" else if (pieces % 1.0 == 0.0) pieces.toLong().toString() else pieces.toString(),
            packTotal = if (item.purchasePrice == 0.0) "" else Money.format(item.purchasePrice * pieces),
            stockUnits = if (item.stockQty == 0.0) "" else Money.format(item.stockQty / pieces),
            lowStockThresholdText = thresholdStr,
            sku = item.sku.orEmpty(),
            selectedUnitId = item.unitId,
            selectedCategoryId = item.categoryId,
            imageUri = item.imageUri,
        )
        query.value = ""
    }

    fun updateNewSaleDraft(transform: (NewSaleDraft) -> NewSaleDraft) {
        _newSaleDraft.update(transform)
    }

    fun clearNewSaleDraft() {
        _newSaleDraft.value = NewSaleDraft()
    }

    fun saveItem(
        id: Long,
        title: String,
        unitId: Long,
        categoryId: Long?,
        purchasePrice: Double,
        purchaseDollarRate: Double,
        piecesPerUnit: Double = 1.0,
        stockQty: Double,
        lowStockThreshold: Double,
        sku: String?,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            val s = settings.value
            val pieces = piecesPerUnit.coerceAtLeast(1.0)
            val rate = repo.getItem(id)?.let {
                PricingEngine.purchaseRateAfterEdit(
                    previousPurchasePrice = it.purchasePrice,
                    previousPurchaseRate = it.purchaseDollarRate,
                    newPurchasePrice = purchasePrice,
                    currentDollarRate = purchaseDollarRate,
                )
            } ?: purchaseDollarRate
            val sell = PricingEngine.recalculateSellPrice(
                purchasePrice, rate, s.dollarRate, s.managerPercent,
            )
            repo.upsertItem(
                ItemEntity(
                    id = id,
                    title = title,
                    unitId = unitId,
                    categoryId = categoryId,
                    purchasePrice = purchasePrice,
                    purchaseDollarRate = rate,
                    sellPrice = sell,
                    piecesPerUnit = pieces,
                    stockQty = stockQty,
                    lowStockThreshold = lowStockThreshold,
                    sku = sku?.ifBlank { null },
                    imageUri = imageUri,
                ),
            )
        }
    }

    fun deleteItem(id: Long, onError: ((String) -> Unit)? = null) = viewModelScope.launch {
        try {
            repo.deleteItem(id)
        } catch (_: Exception) {
            onError?.invoke("in_use")
        }
    }

    fun importAndSetItemImage(sourceUri: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val durableUri = repo.importImageToInternalStorage(sourceUri)
        _itemForm.update { it.copy(imageUri = durableUri, pendingCameraUri = null) }
    }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        secure.setThemeMode(mode)
        when (mode) {
            "dark" -> patchSettings { it.copy(displayDarkMode = true) }
            "light" -> patchSettings { it.copy(displayDarkMode = false) }
        }
    }

    fun saveUnit(id: Long = 0L, nameFa: String, nameEn: String) = viewModelScope.launch {
        repo.upsertUnit(
            UnitEntity(
                id = id,
                nameFa = nameFa,
                nameEn = nameEn.ifBlank { nameFa },
            ),
        )
    }

    fun deleteUnit(id: Long, onError: ((String) -> Unit)? = null) = viewModelScope.launch {
        try {
            repo.deleteUnit(id)
        } catch (_: Exception) {
            onError?.invoke("in_use")
        }
    }

    fun saveCategory(id: Long = 0L, nameFa: String, nameEn: String) = viewModelScope.launch {
        repo.upsertCategory(
            CategoryEntity(
                id = id,
                nameFa = nameFa,
                nameEn = nameEn.ifBlank { nameFa },
            ),
        )
    }

    fun deleteCategory(id: Long, onError: ((String) -> Unit)? = null) = viewModelScope.launch {
        try {
            repo.deleteCategory(id)
        } catch (_: Exception) {
            onError?.invoke("in_use")
        }
    }

    fun saveSettings(s: SettingsEntity) = viewModelScope.launch {
        val normalized = s.copy(id = 1, languageTag = s.languageTag.ifBlank { "fa" })
        repo.updateSettings(normalized)
        // Sync AppCompat locales; Compose picks up language via AppLocaleProvider from settings Flow.
        KalabanApp.instance.applyLanguage(normalized.languageTag)
    }

    /** Patch persisted settings from the DB row so UI defaults cannot wipe other fields. */
    fun patchSettings(transform: (SettingsEntity) -> SettingsEntity) = viewModelScope.launch {
        val current = repo.getSettings()
        val patched = transform(current).copy(id = 1)
        val finalSettings = patched.copy(languageTag = patched.languageTag.ifBlank { "fa" })
        repo.updateSettings(finalSettings)
        KalabanApp.instance.applyLanguage(finalSettings.languageTag)
    }

    fun recalculate() = viewModelScope.launch { repo.recalculateAllPrices() }

    fun batchFactor(factor: Double) = viewModelScope.launch { repo.applyBatchPriceFactor(factor) }

    fun unitPriceForSale(item: ItemEntity, installment: Boolean = false): Double {
        val s = settings.value
        return PricingEngine.saleUnitPrice(
            storedSellPrice = item.sellPrice,
            dollarChangePercent = s.dollarChangePercent,
            taxPercent = s.taxPercent,
            profitPercent = s.profitPercent,
            installment = installment,
            installmentDiffPercent = s.installmentDiffPercent,
        )
    }

    fun recordSale(
        buyer: String,
        type: PaymentType,
        lines: List<ShopRepository.CartLine>,
        dueMs: Long?,
        jalaliDate: String = com.kalaban.farshidb2011.core.JalaliDate.today().format(),
        createdAtEpochMs: Long = System.currentTimeMillis(),
    ) = viewModelScope.launch {
        if (lines.isEmpty()) return@launch
        val saleId = repo.recordSale(
            buyerName = buyer,
            paymentType = type,
            lines = lines,
            dueAtEpochMs = dueMs,
            jalaliDate = jalaliDate,
            createdAtEpochMs = createdAtEpochMs,
        )
        repo.openCreditsSnapshot().find { it.id == saleId }?.let { sale ->
            ReminderNotifications.scheduleForSale(getApplication(), sale)
        } ?: run {
            // Fresh sale may already be in DB with open balance 0 for cash
            val sale = KalabanApp.instance.database.saleDao().getById(saleId)
            if (sale != null) ReminderNotifications.scheduleForSale(getApplication(), sale)
        }
    }

    fun partialPay(saleId: Long, amount: Double) =
        viewModelScope.launch {
            repo.addPartialPayment(saleId, amount)
            KalabanApp.instance.database.saleDao().getById(saleId)?.let {
                ReminderNotifications.scheduleForSale(getApplication(), it)
            }
        }

    fun zeroBalance(saleId: Long, reason: String) =
        viewModelScope.launch {
            repo.zeroBalance(saleId, reason)
            ReminderNotifications.cancelForSale(getApplication(), saleId)
        }

    fun loadReport(range: ShopRepository.ReportRange) = viewModelScope.launch {
        _report.value = repo.detailedSalesReport(range)
    }

    fun loadReminders() = viewModelScope.launch {
        _reminders.value = repo.dueReminders()
    }

    fun exportTo(file: File) = viewModelScope.launch {
        runCatching { repo.writeBackupFile(file) }
            .onSuccess { _backupMessage.value = "OK: ${file.absolutePath}" }
            .onFailure { _backupMessage.value = "Export failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun exportToUri(uri: android.net.Uri) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        runCatching { repo.exportBackupToUri(uri) }
            .onSuccess { _backupMessage.value = "OK: ${uri.lastPathSegment ?: "kalaban-backup.kalb"}" }
            .onFailure { _backupMessage.value = "Export failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun importFrom(file: File) = viewModelScope.launch {
        runCatching {
            repo.readBackupFile(file)
            ReminderNotifications.rescheduleAll(getApplication())
        }
            .onSuccess { _backupMessage.value = "Imported: ${file.name}" }
            .onFailure { _backupMessage.value = "Import failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun importFromUri(uri: android.net.Uri) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            repo.importBackupFromUri(uri)
            ReminderNotifications.rescheduleAll(getApplication())
        }
            .onSuccess { _backupMessage.value = "Imported: ${uri.lastPathSegment ?: "backup"}" }
            .onFailure { _backupMessage.value = "Import failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun observeLinesForSale(saleId: Long) = repo.observeLinesForSale(saleId)
    fun observePaymentsForSale(saleId: Long) = repo.observePaymentsForSale(saleId)

    fun setPin(pin: String) = viewModelScope.launch { secure.setPin(pin) }

    fun unlockWithPin(pin: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = if (!secure.hasPin()) {
            secure.setPin(pin)
            true
        } else {
            secure.checkPin(pin)
        }
        if (ok) secure.setSessionUnlocked(true)
        onResult(ok)
    }

    /** After biometric verification for forgotten PIN: replace PIN and unlock. */
    fun resetPinAfterBiometric(newPin: String, onDone: () -> Unit) = viewModelScope.launch {
        secure.setPin(newPin)
        secure.setSessionUnlocked(true)
        onDone()
    }

    fun unlockSession() = viewModelScope.launch { secure.setSessionUnlocked(true) }
    fun lockSession() = viewModelScope.launch { secure.setSessionUnlocked(false) }

    fun clearDollarHistory() = viewModelScope.launch { repo.clearDollarHistory() }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(KalabanApp.instance) as T
            }
        }
    }
}
