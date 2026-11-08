package com.kalaban.farshidb2011.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.PaymentType
import com.kalaban.farshidb2011.data.local.entity.SaleEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UnitEntity
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.data.repository.AuditEntity
import com.kalaban.farshidb2011.data.repository.AuthResult
import com.kalaban.farshidb2011.data.repository.PricingContext
import com.kalaban.farshidb2011.data.repository.ShopRepository
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.notifications.ReminderNotifications
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

data class SaleCartLineDraft(
    val itemId: Long,
    val title: String,
    val quantity: Double,
    val unitPrice: Double,
    val unitCost: Double? = null,
    val taxPerUnit: Double = 0.0,
    val discountPerUnit: Double = 0.0,
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

/** Counter price for one sale line under the current rules and quantity tiers. */
data class LineQuote(
    val unitPrice: BigDecimal,
    val listPrice: BigDecimal,
    val discountPct: BigDecimal,
    val unitCost: BigDecimal,
    val breakEven: BigDecimal,
    val minimum: BigDecimal?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : GuardedViewModel(application) {
    private val repo: ShopRepository get() = app.repository
    private val secure get() = app.securePrefs

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
    val config = app.config.config
    val sessionUser = session.user
    val unlocked = session.user.map { it != null }.stateIn(viewModelScope, keepAlive, false)
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

    private val _newSaleDraft = MutableStateFlow(NewSaleDraft())
    val newSaleDraft: StateFlow<NewSaleDraft> = _newSaleDraft.asStateFlow()

    /** Set after a price-affecting settings change; the UI offers a previewed recalculation. */
    private val _pendingRecalc = MutableStateFlow(false)
    val pendingRecalc: StateFlow<Boolean> = _pendingRecalc.asStateFlow()

    private val _pricingContext = MutableStateFlow<PricingContext?>(null)
    val pricingContext: StateFlow<PricingContext?> = _pricingContext.asStateFlow()

    fun setQuery(value: String) {
        query.value = value
    }

    fun updateNewSaleDraft(transform: (NewSaleDraft) -> NewSaleDraft) {
        _newSaleDraft.update(transform)
    }

    fun clearNewSaleDraft() {
        _newSaleDraft.value = NewSaleDraft()
    }

    fun dismissRecalcPrompt() {
        _pendingRecalc.value = false
    }

    fun setThemeMode(mode: String) = launchSafely {
        secure.setThemeMode(mode)
        when (mode) {
            "dark" -> repo.updateSettings(repo.getSettings().copy(displayDarkMode = true))
            "light" -> repo.updateSettings(repo.getSettings().copy(displayDarkMode = false))
        }
    }

    // ---- Catalog (units & categories) ----

    fun saveUnit(id: Long = 0L, nameFa: String, nameEn: String) = launchSafely {
        if (!allowed(Permission.MANAGE_CATALOG)) return@launchSafely
        repo.upsertUnit(UnitEntity(id = id, nameFa = nameFa, nameEn = nameEn.ifBlank { nameFa }))
        app.audit.log(actor, AuditAction.CATALOG_UPDATE, AuditEntity.CATALOG, id.takeIf { it != 0L }, "unit: $nameFa / $nameEn")
    }

    fun deleteUnit(id: Long, onError: ((String) -> Unit)? = null) = launchSafely {
        if (!allowed(Permission.MANAGE_CATALOG)) return@launchSafely
        try {
            repo.deleteUnit(id)
            app.audit.log(actor, AuditAction.CATALOG_UPDATE, AuditEntity.CATALOG, id, "unit deleted")
        } catch (_: Exception) {
            onError?.invoke("in_use")
        }
    }

    fun saveCategory(id: Long = 0L, nameFa: String, nameEn: String) = launchSafely {
        if (!allowed(Permission.MANAGE_CATALOG)) return@launchSafely
        repo.upsertCategory(CategoryEntity(id = id, nameFa = nameFa, nameEn = nameEn.ifBlank { nameFa }))
        app.audit.log(actor, AuditAction.CATALOG_UPDATE, AuditEntity.CATALOG, id.takeIf { it != 0L }, "category: $nameFa / $nameEn")
    }

    fun deleteCategory(id: Long, onError: ((String) -> Unit)? = null) = launchSafely {
        if (!allowed(Permission.MANAGE_CATALOG)) return@launchSafely
        try {
            repo.deleteCategory(id)
            app.audit.log(actor, AuditAction.CATALOG_UPDATE, AuditEntity.CATALOG, id, "category deleted")
        } catch (_: Exception) {
            onError?.invoke("in_use")
        }
    }

    // ---- Settings ----

    private suspend fun persistSettings(s: SettingsEntity) {
        val normalized = s.copy(id = 1, languageTag = s.languageTag.ifBlank { "fa" })
        val change = repo.updateSettings(normalized)
        change.diff()?.let { app.audit.log(actor, AuditAction.SETTINGS_UPDATE, AuditEntity.SETTINGS, 1, "settings", it) }
        KalabanApp.instance.applyLanguage(normalized.languageTag)
        if (change.pricesAffected && app.config.current().recalcPrompt) _pendingRecalc.value = true
        _pricingContext.value = null
    }

    /**
     * Pricing rules (dollar rate, percentages) — require price management rights.
     * @param offerRecalc when false, only stores the new rate/percents; product prices stay unchanged
     * until the user runs recalculation explicitly.
     */
    fun savePricingRules(s: SettingsEntity, offerRecalc: Boolean = true, onSaved: () -> Unit = {}) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        val normalized = s.copy(id = 1, languageTag = s.languageTag.ifBlank { "fa" })
        val change = repo.updateSettings(normalized)
        change.diff()?.let { app.audit.log(actor, AuditAction.SETTINGS_UPDATE, AuditEntity.SETTINGS, 1, "settings", it) }
        KalabanApp.instance.applyLanguage(normalized.languageTag)
        if (offerRecalc && change.pricesAffected && app.config.current().recalcPrompt) {
            _pendingRecalc.value = true
        }
        _pricingContext.value = null
        UiMessages.post(if (offerRecalc) R.string.saved_ok else R.string.dollar_rate_saved_only)
        onSaved()
    }

    /** Patch persisted settings from the DB row so UI defaults cannot wipe other fields. */
    fun patchSettings(transform: (SettingsEntity) -> SettingsEntity) = launchSafely {
        if (!allowed(Permission.MANAGE_SETTINGS)) return@launchSafely
        persistSettings(transform(repo.getSettings()))
    }

    // ---- Sales ----

    fun refreshPricingContext() = launchSafely {
        _pricingContext.value = app.pricing.loadContext()
    }

    /** Cash or credit unit price with the matching quantity tier, plus floor information for manual edits. */
    fun quote(item: ItemEntity, quantity: Double, credit: Boolean): LineQuote? {
        val ctx = _pricingContext.value ?: return null
        val base = Money.of(item.sellPrice)
        val discount = PriceCalculator.discountFor(Money.of(quantity), item.categoryId, ctx.tiers)
        val sale = PriceCalculator.salePrice(base, ctx.policy, discount)
        val list = if (discount.signum() > 0) PriceCalculator.salePrice(base, ctx.policy) else sale
        val cost = PriceCalculator.cost(app.pricing.input(item).cost, ctx.policy)
        val minimum = item.minPrice?.let { PriceCalculator.salePrice(Money.of(it), ctx.policy) }
        return LineQuote(
            unitPrice = if (credit) sale.creditPrice else sale.cashPrice,
            listPrice = if (credit) list.creditPrice else list.cashPrice,
            discountPct = discount,
            unitCost = cost.fullCost,
            breakEven = PriceCalculator.profit(sale, cost, ctx.policy).breakEvenPayable,
            minimum = minimum?.let { if (credit) it.creditPrice else it.cashPrice },
        )
    }

    /** Tax portion of a tax-inclusive unit price at the current tax rate. */
    fun taxPortion(unitPrice: Double): Double {
        val tax = Money.of(settings.value.taxPercent)
        if (tax.signum() <= 0) return 0.0
        return Money.of(unitPrice).multiply(tax, Money.MC).divide(Money.HUNDRED.add(tax), Money.MC).toDouble()
    }

    fun recordSale(
        buyer: String,
        type: PaymentType,
        lines: List<ShopRepository.CartLine>,
        dueMs: Long?,
        jalaliDate: String = JalaliDate.today().format(),
        createdAtEpochMs: Long = System.currentTimeMillis(),
    ) = launchSafely {
        if (!allowed(Permission.RECORD_SALES)) return@launchSafely
        if (lines.isEmpty()) return@launchSafely
        val saleId = repo.recordSale(
            buyerName = buyer,
            paymentType = type,
            lines = lines,
            dueAtEpochMs = dueMs,
            jalaliDate = jalaliDate,
            createdAtEpochMs = createdAtEpochMs,
            createdBy = actor.name,
        )
        val sale = app.database.saleDao().getById(saleId)
        if (sale != null) {
            if (app.config.current().notifyCreditReminders) ReminderNotifications.scheduleForSale(getApplication(), sale)
            app.audit.log(actor, AuditAction.SALE_CREATE, AuditEntity.SALE, saleId, "$buyer: ${Money.input(sale.totalAmount)}", type.name)
        }
        UiMessages.post(R.string.sale_recorded)
    }

    fun partialPay(saleId: Long, amount: Double) = launchSafely {
        if (!allowed(Permission.MANAGE_CREDITS)) return@launchSafely
        repo.addPartialPayment(saleId, amount)
        app.database.saleDao().getById(saleId)?.let {
            ReminderNotifications.scheduleForSale(getApplication(), it)
            app.audit.log(actor, AuditAction.PAYMENT_ADD, AuditEntity.SALE, saleId, "${it.buyerName}: ${Money.input(amount)}")
        }
        UiMessages.post(R.string.payment_recorded)
    }

    fun zeroBalance(saleId: Long, reason: String) = launchSafely {
        if (!allowed(Permission.MANAGE_CREDITS)) return@launchSafely
        repo.zeroBalance(saleId, reason)
        ReminderNotifications.cancelForSale(getApplication(), saleId)
        app.audit.log(actor, AuditAction.BALANCE_ZERO, AuditEntity.SALE, saleId, reason)
    }

    fun loadReport(fromEpochMs: Long, toEpochMs: Long) = launchSafely {
        _report.value = repo.detailedSalesReport(fromEpochMs, toEpochMs)
    }

    fun loadReminders() = launchSafely {
        _reminders.value = repo.dueReminders()
    }

    // ---- Backup ----

    private suspend fun markBackedUp() {
        app.config.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
    }

    fun exportTo(file: File) = launchSafely {
        if (!allowed(Permission.EXPORT_DATA)) return@launchSafely
        runCatching { withContext(Dispatchers.IO) { repo.writeBackupFile(file) } }
            .onSuccess {
                markBackedUp()
                app.audit.log(actor, AuditAction.DATA_EXPORT, AuditEntity.DATA, summary = file.name)
                _backupMessage.value = "OK: ${file.absolutePath}"
            }
            .onFailure { _backupMessage.value = "Export failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun exportToUri(uri: Uri) = launchSafely {
        if (!allowed(Permission.EXPORT_DATA)) return@launchSafely
        runCatching { withContext(Dispatchers.IO) { repo.exportBackupToUri(uri) } }
            .onSuccess {
                markBackedUp()
                app.audit.log(actor, AuditAction.DATA_EXPORT, AuditEntity.DATA, summary = uri.lastPathSegment ?: "backup")
                _backupMessage.value = "OK: ${uri.lastPathSegment ?: "kalaban-backup.kalb"}"
            }
            .onFailure { _backupMessage.value = "Export failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    private suspend fun afterRestore(name: String) {
        ReminderNotifications.rescheduleAll(getApplication())
        app.audit.log(actor, AuditAction.DATA_RESTORE, AuditEntity.DATA, summary = name)
        _pricingContext.value = null
    }

    fun importFrom(file: File) = launchSafely {
        if (!allowed(Permission.IMPORT_DATA)) return@launchSafely
        runCatching { withContext(Dispatchers.IO) { repo.readBackupFile(file) } }
            .onSuccess {
                afterRestore(file.name)
                _backupMessage.value = "Imported: ${file.name}"
            }
            .onFailure { _backupMessage.value = "Import failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun importFromUri(uri: Uri) = launchSafely {
        if (!allowed(Permission.IMPORT_DATA)) return@launchSafely
        runCatching { withContext(Dispatchers.IO) { repo.importBackupFromUri(uri) } }
            .onSuccess {
                afterRestore(uri.lastPathSegment ?: "backup")
                _backupMessage.value = "Imported: ${uri.lastPathSegment ?: "backup"}"
            }
            .onFailure { _backupMessage.value = "Import failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    fun observeLinesForSale(saleId: Long) = repo.observeLinesForSale(saleId)
    fun observePaymentsForSale(saleId: Long) = repo.observePaymentsForSale(saleId)

    // ---- Authentication ----

    /** First run sets the owner PIN; afterwards the PIN may belong to the owner or a staff user. */
    fun unlockWithPin(pin: String, onResult: (AuthResult) -> Unit) = viewModelScope.launch {
        val result = if (!secure.hasPin()) {
            secure.setPin(pin)
            app.users.authenticate(pin)
        } else {
            app.users.authenticate(pin)
        }
        onResult(result)
    }

    /** After biometric verification for a forgotten PIN: replace the owner PIN and sign in as owner. */
    fun resetPinAfterBiometric(newPin: String, onDone: () -> Unit) = viewModelScope.launch {
        secure.setPin(newPin)
        app.users.signInOwnerWithBiometric()
        onDone()
    }

    fun unlockWithBiometric() = viewModelScope.launch { app.users.signInOwnerWithBiometric() }

    fun lockSession() = viewModelScope.launch { app.users.signOut() }

    fun clearDollarHistory() = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        repo.clearDollarHistory()
        app.audit.log(actor, AuditAction.HISTORY_CLEAR, AuditEntity.PRICE, summary = "dollar_rate_history")
    }

    companion object {
        fun factory() = appViewModelFactory { AppViewModel(KalabanApp.instance) }
    }
}
