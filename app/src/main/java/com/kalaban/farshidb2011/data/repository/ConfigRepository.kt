package com.kalaban.farshidb2011.data.repository

import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.AppConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

/** What the customer-facing display shows: the stored base price or the full cash price incl. tax. */
enum class DisplayPriceMode { BASE, CASH }

object ConfigKeys {
    const val BUSINESS_NAME = "business.name"
    const val BUSINESS_PHONE = "business.phone"
    const val BUSINESS_ADDRESS = "business.address"
    const val BUSINESS_NOTE = "business.note"
    const val MONEY_DECIMALS = "format.moneyDecimals"
    const val GROUPING = "format.grouping"
    const val ROUNDING_MODE = "pricing.roundingMode"
    const val ROUNDING_STEP = "pricing.roundingStep"
    const val OVERHEAD_PCT = "pricing.overheadPct"
    const val MIN_MARGIN_PCT = "pricing.minMarginPct"
    const val RECALC_PROMPT = "pricing.recalcPrompt"
    const val DISPLAY_PRICE_MODE = "display.priceMode"
    const val DISPLAY_SHOW_STOCK = "display.showStock"
    const val DISPLAY_SHOW_IMAGES = "display.showImages"
    const val NOTIFY_LOW_STOCK = "notify.lowStock"
    const val NOTIFY_NEGATIVE_PROFIT = "notify.negativeProfit"
    const val NOTIFY_LOW_MARGIN = "notify.lowMargin"
    const val NOTIFY_CREDIT = "notify.creditReminders"
    const val NOTIFY_SCHEDULED = "notify.scheduledPrices"
    const val BACKUP_REMINDER_DAYS = "backup.reminderDays"
    const val LAST_BACKUP_AT = "backup.lastAt"
    const val CSV_DELIMITER = "csv.delimiter"
    const val IMPORT_MAPPING = "import.mapping"
    const val REPORT_COLUMNS = "report.columns"
    const val RECENT_SEARCHES = "search.recent"
    const val FORMULAS_SEEDED = "seed.formulas"
}

data class AppConfig(
    val businessName: String = "",
    val businessPhone: String = "",
    val businessAddress: String = "",
    val businessNote: String = "",
    val moneyDecimals: Int = Money.DEFAULT_SCALE,
    val grouping: Boolean = true,
    val roundingMode: PriceRounding = PriceRounding.NEAREST,
    val roundingStep: BigDecimal? = null,
    val overheadPct: BigDecimal = BigDecimal.ZERO,
    val minMarginPct: BigDecimal = BigDecimal.ZERO,
    val recalcPrompt: Boolean = true,
    val displayPriceMode: DisplayPriceMode = DisplayPriceMode.BASE,
    val displayShowStock: Boolean = true,
    val displayShowImages: Boolean = true,
    val notifyLowStock: Boolean = true,
    val notifyNegativeProfit: Boolean = true,
    val notifyLowMargin: Boolean = false,
    val notifyCreditReminders: Boolean = true,
    val notifyScheduledPrices: Boolean = true,
    val backupReminderDays: Int = 0,
    val lastBackupAt: Long = 0L,
    val csvDelimiter: Char = ',',
    val importMapping: String = "",
    val reportColumns: String = "",
    val recentSearches: List<String> = emptyList(),
    val formulasSeeded: Boolean = false,
) {
    fun toMap(): Map<String, String> = mapOf(
        ConfigKeys.BUSINESS_NAME to businessName,
        ConfigKeys.BUSINESS_PHONE to businessPhone,
        ConfigKeys.BUSINESS_ADDRESS to businessAddress,
        ConfigKeys.BUSINESS_NOTE to businessNote,
        ConfigKeys.MONEY_DECIMALS to moneyDecimals.toString(),
        ConfigKeys.GROUPING to grouping.toString(),
        ConfigKeys.ROUNDING_MODE to roundingMode.name,
        ConfigKeys.ROUNDING_STEP to (roundingStep?.toPlainString() ?: ""),
        ConfigKeys.OVERHEAD_PCT to overheadPct.toPlainString(),
        ConfigKeys.MIN_MARGIN_PCT to minMarginPct.toPlainString(),
        ConfigKeys.RECALC_PROMPT to recalcPrompt.toString(),
        ConfigKeys.DISPLAY_PRICE_MODE to displayPriceMode.name,
        ConfigKeys.DISPLAY_SHOW_STOCK to displayShowStock.toString(),
        ConfigKeys.DISPLAY_SHOW_IMAGES to displayShowImages.toString(),
        ConfigKeys.NOTIFY_LOW_STOCK to notifyLowStock.toString(),
        ConfigKeys.NOTIFY_NEGATIVE_PROFIT to notifyNegativeProfit.toString(),
        ConfigKeys.NOTIFY_LOW_MARGIN to notifyLowMargin.toString(),
        ConfigKeys.NOTIFY_CREDIT to notifyCreditReminders.toString(),
        ConfigKeys.NOTIFY_SCHEDULED to notifyScheduledPrices.toString(),
        ConfigKeys.BACKUP_REMINDER_DAYS to backupReminderDays.toString(),
        ConfigKeys.LAST_BACKUP_AT to lastBackupAt.toString(),
        ConfigKeys.CSV_DELIMITER to csvDelimiter.toString(),
        ConfigKeys.IMPORT_MAPPING to importMapping,
        ConfigKeys.REPORT_COLUMNS to reportColumns,
        ConfigKeys.RECENT_SEARCHES to recentSearches.joinToString("\n"),
        ConfigKeys.FORMULAS_SEEDED to formulasSeeded.toString(),
    )

    companion object {
        /** Missing or malformed values fall back to defaults, so old databases and backups stay readable. */
        fun fromMap(m: Map<String, String>): AppConfig {
            val d = AppConfig()
            fun bool(key: String, default: Boolean) = m[key]?.toBooleanStrictOrNull() ?: default
            fun dec(key: String, default: BigDecimal) = Numbers.parseDecimal(m[key]) ?: default
            return AppConfig(
                businessName = m[ConfigKeys.BUSINESS_NAME] ?: d.businessName,
                businessPhone = m[ConfigKeys.BUSINESS_PHONE] ?: d.businessPhone,
                businessAddress = m[ConfigKeys.BUSINESS_ADDRESS] ?: d.businessAddress,
                businessNote = m[ConfigKeys.BUSINESS_NOTE] ?: d.businessNote,
                moneyDecimals = (m[ConfigKeys.MONEY_DECIMALS]?.toIntOrNull() ?: d.moneyDecimals).coerceIn(0, Money.MAX_SCALE),
                grouping = bool(ConfigKeys.GROUPING, d.grouping),
                roundingMode = PriceRounding.parse(m[ConfigKeys.ROUNDING_MODE]),
                roundingStep = Numbers.parseDecimal(m[ConfigKeys.ROUNDING_STEP])?.takeIf { it.signum() > 0 },
                overheadPct = dec(ConfigKeys.OVERHEAD_PCT, d.overheadPct),
                minMarginPct = dec(ConfigKeys.MIN_MARGIN_PCT, d.minMarginPct),
                recalcPrompt = bool(ConfigKeys.RECALC_PROMPT, d.recalcPrompt),
                displayPriceMode = DisplayPriceMode.entries.firstOrNull { it.name == m[ConfigKeys.DISPLAY_PRICE_MODE] }
                    ?: d.displayPriceMode,
                displayShowStock = bool(ConfigKeys.DISPLAY_SHOW_STOCK, d.displayShowStock),
                displayShowImages = bool(ConfigKeys.DISPLAY_SHOW_IMAGES, d.displayShowImages),
                notifyLowStock = bool(ConfigKeys.NOTIFY_LOW_STOCK, d.notifyLowStock),
                notifyNegativeProfit = bool(ConfigKeys.NOTIFY_NEGATIVE_PROFIT, d.notifyNegativeProfit),
                notifyLowMargin = bool(ConfigKeys.NOTIFY_LOW_MARGIN, d.notifyLowMargin),
                notifyCreditReminders = bool(ConfigKeys.NOTIFY_CREDIT, d.notifyCreditReminders),
                notifyScheduledPrices = bool(ConfigKeys.NOTIFY_SCHEDULED, d.notifyScheduledPrices),
                backupReminderDays = (m[ConfigKeys.BACKUP_REMINDER_DAYS]?.toIntOrNull() ?: d.backupReminderDays).coerceIn(0, 365),
                lastBackupAt = m[ConfigKeys.LAST_BACKUP_AT]?.toLongOrNull() ?: d.lastBackupAt,
                csvDelimiter = m[ConfigKeys.CSV_DELIMITER]?.firstOrNull()?.takeIf { it in listOf(',', ';', '\t') }
                    ?: d.csvDelimiter,
                importMapping = m[ConfigKeys.IMPORT_MAPPING] ?: d.importMapping,
                reportColumns = m[ConfigKeys.REPORT_COLUMNS] ?: d.reportColumns,
                recentSearches = m[ConfigKeys.RECENT_SEARCHES]?.split('\n')?.filter { it.isNotBlank() } ?: d.recentSearches,
                formulasSeeded = bool(ConfigKeys.FORMULAS_SEEDED, d.formulasSeeded),
            )
        }
    }
}

class ConfigRepository(private val db: KalabanDatabase, scope: CoroutineScope) {
    val config: StateFlow<AppConfig> = db.configDao().observeAll()
        .map { rows -> AppConfig.fromMap(rows.associate { it.configKey to it.value }) }
        .onEach { Money.configure(it.moneyDecimals, it.grouping) }
        .stateIn(scope, SharingStarted.Eagerly, AppConfig())

    suspend fun current(): AppConfig =
        AppConfig.fromMap(db.configDao().getAll().associate { it.configKey to it.value })

    /** Persists only keys whose value changed and returns them as `key: old → new` for auditing. */
    suspend fun update(transform: (AppConfig) -> AppConfig): List<String> {
        val before = current()
        val after = transform(before)
        val old = before.toMap()
        val changed = after.toMap().filter { (k, v) -> old[k] != v }
        if (changed.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        db.configDao().upsertAll(changed.map { (k, v) -> AppConfigEntity(k, v, now) })
        Money.configure(after.moneyDecimals, after.grouping)
        return changed.keys.filterNot { it == ConfigKeys.RECENT_SEARCHES || it == ConfigKeys.LAST_BACKUP_AT }
            .map { "$it: ${old[it].orEmpty()} → ${changed[it]}" }
    }
}
