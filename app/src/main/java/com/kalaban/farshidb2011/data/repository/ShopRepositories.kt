package com.kalaban.farshidb2011.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.annotations.SerializedName
import com.kalaban.farshidb2011.BuildConfig
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.data.backup.KalabanBackupArchive
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.AppConfigEntity
import com.kalaban.farshidb2011.data.local.entity.AttributeDefinitionEntity
import com.kalaban.farshidb2011.data.local.entity.AuditLogEntity
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.DiscountRuleEntity
import com.kalaban.farshidb2011.data.local.entity.DollarRateHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.FormulaVersionEntity
import com.kalaban.farshidb2011.data.local.entity.ItemAttributeValueEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.PaymentType
import com.kalaban.farshidb2011.data.local.entity.PriceBatchEntity
import com.kalaban.farshidb2011.data.local.entity.PriceHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.PricingFormulaEntity
import com.kalaban.farshidb2011.data.local.entity.RoleEntity
import com.kalaban.farshidb2011.data.local.entity.SaleEntity
import com.kalaban.farshidb2011.data.local.entity.SaleLineEntity
import com.kalaban.farshidb2011.data.local.entity.SalePaymentEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UnitEntity
import com.kalaban.farshidb2011.data.local.entity.UserEntity
import com.kalaban.farshidb2011.data.local.seedIfEmpty
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.nio.charset.StandardCharsets

/** Bumped when the backup payload gains tables; absent in backups made before version 3. */
const val BACKUP_SCHEMA_VERSION = 3

class ShopRepository(
    private val db: KalabanDatabase,
    private val appContext: Context,
) {
    // Instance creators keep Kotlin defaults for fields missing from older backups (e.g. dollarLinked = true).
    private val gson = GsonBuilder()
        .registerTypeAdapter(
            ItemEntity::class.java,
            InstanceCreator { ItemEntity(title = "", unitId = 0, purchasePrice = 0.0, purchaseDollarRate = 0.0, sellPrice = 0.0) },
        )
        .registerTypeAdapter(
            SaleLineEntity::class.java,
            InstanceCreator { SaleLineEntity(saleId = 0, itemId = 0, itemTitle = "", quantity = 0.0, unitPrice = 0.0, lineTotal = 0.0) },
        )
        .create()
    private val imageDir: File
        get() = File(appContext.filesDir, "item_images").also { it.mkdirs() }

    fun observeUnits() = db.unitDao().observeAll()
    fun observeCategories() = db.categoryDao().observeAll()
    fun observeItems(query: String) = db.itemDao().observeSearch(query.trim())
    fun observeLowStock() = db.itemDao().observeLowStock()
    fun observeSettings(): Flow<SettingsEntity> =
        db.settingsDao().observe().map { it ?: SettingsEntity() }
    fun observeDollarHistory() = db.dollarRateHistoryDao().observeAll()

    suspend fun getSettings(): SettingsEntity =
        db.settingsDao().get() ?: SettingsEntity()

    suspend fun clearDollarHistory() = db.dollarRateHistoryDao().deleteAll()
    fun observeSales() = db.saleDao().observeAll()
    fun observeOpenCredits() = db.saleDao().observeOpenCredits()

    suspend fun openCreditsSnapshot(): List<SaleEntity> =
        db.saleDao().getAll().filter {
            it.paymentType == PaymentType.CREDIT && it.openBalance > 0.0 && !it.manuallyZeroed
        }

    suspend fun upsertUnit(unit: UnitEntity) {
        if (unit.id == 0L) db.unitDao().insert(unit) else db.unitDao().update(unit)
    }

    suspend fun deleteUnit(id: Long) = db.unitDao().delete(id)

    suspend fun upsertCategory(category: CategoryEntity) {
        if (category.id == 0L) db.categoryDao().insert(category) else db.categoryDao().update(category)
    }

    suspend fun deleteCategory(id: Long) = db.categoryDao().delete(id)

    /** Result of saving settings; price-affecting changes are re-priced through a previewed batch, never silently. */
    data class SettingsChange(
        val previous: SettingsEntity?,
        val current: SettingsEntity,
    ) {
        val pricesAffected: Boolean
            get() = previous != null && (
                previous.dollarRate != current.dollarRate || previous.managerPercent != current.managerPercent
                )

        fun diff(): String? = previous?.let { p ->
            listOf(
                "dollarRate" to (p.dollarRate to current.dollarRate),
                "taxPercent" to (p.taxPercent to current.taxPercent),
                "profitPercent" to (p.profitPercent to current.profitPercent),
                "dollarChangePercent" to (p.dollarChangePercent to current.dollarChangePercent),
                "managerPercent" to (p.managerPercent to current.managerPercent),
                "installmentDiffPercent" to (p.installmentDiffPercent to current.installmentDiffPercent),
                "currencyCode" to (p.currencyCode to current.currencyCode),
                "languageTag" to (p.languageTag to current.languageTag),
                "displayDarkMode" to (p.displayDarkMode to current.displayDarkMode),
                "biometricUnlockEnabled" to (p.biometricUnlockEnabled to current.biometricUnlockEnabled),
            ).filter { it.second.first != it.second.second }
                .joinToString("; ") { (k, v) -> "$k: ${v.first} → ${v.second}" }
                .ifEmpty { null }
        }
    }

    suspend fun updateSettings(settings: SettingsEntity): SettingsChange = db.withTransaction {
        val previous = db.settingsDao().get()
        val normalized = settings.copy(
            id = 1,
            dollarRate = Money.round(settings.dollarRate),
        )
        db.settingsDao().upsert(normalized)
        if (previous != null && previous.dollarRate != normalized.dollarRate) {
            db.dollarRateHistoryDao().insert(
                DollarRateHistoryEntity(rate = normalized.dollarRate, note = "settings_update"),
            )
        }
        SettingsChange(previous, normalized)
    }

    data class CartLine(
        val item: ItemEntity,
        val quantity: Double,
        val unitPrice: Double,
        val unitCost: Double? = null,
        val taxPerUnit: Double = 0.0,
        val discountPerUnit: Double = 0.0,
    )

    suspend fun recordSale(
        buyerName: String,
        paymentType: PaymentType,
        lines: List<CartLine>,
        dueAtEpochMs: Long? = null,
        jalaliDate: String = JalaliDate.today().format(),
        createdAtEpochMs: Long = System.currentTimeMillis(),
        createdBy: String? = null,
    ): Long = db.withTransaction {
        val normalizedLines = lines.map {
            it.copy(unitPrice = Money.round(it.unitPrice))
        }
        val total = Money.round(normalizedLines.sumOf { it.quantity * it.unitPrice })
        val open = if (paymentType == PaymentType.CREDIT) total else 0.0
        val saleId = db.saleDao().upsert(
            SaleEntity(
                buyerName = buyerName,
                paymentType = paymentType,
                jalaliDate = jalaliDate,
                createdAtEpochMs = createdAtEpochMs,
                dueAtEpochMs = dueAtEpochMs,
                totalAmount = total,
                openBalance = open,
                createdBy = createdBy,
            ),
        )
        db.saleLineDao().insertAll(
            normalizedLines.map {
                val lineTotal = Money.round(it.quantity * it.unitPrice)
                SaleLineEntity(
                    saleId = saleId,
                    itemId = it.item.id,
                    itemTitle = it.item.title,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    lineTotal = lineTotal,
                    unitCost = it.unitCost?.let(Money::round),
                    taxAmount = Money.round(it.quantity * it.taxPerUnit),
                    discountAmount = Money.round(it.quantity * it.discountPerUnit),
                )
            },
        )
        lines.forEach { line ->
            val item = db.itemDao().getById(line.item.id) ?: return@forEach
            db.itemDao().update(item.copy(stockQty = (item.stockQty - line.quantity).coerceAtLeast(0.0)))
        }
        saleId
    }

    suspend fun addPartialPayment(saleId: Long, amount: Double, note: String? = null) = db.withTransaction {
        val sale = db.saleDao().getById(saleId) ?: return@withTransaction
        val pay = Money.round(amount.coerceAtMost(sale.openBalance).coerceAtLeast(0.0))
        if (pay <= 0.0) return@withTransaction
        db.salePaymentDao().insert(SalePaymentEntity(saleId = saleId, amount = pay, note = note))
        db.saleDao().update(
            sale.copy(openBalance = Money.round((sale.openBalance - pay).coerceAtLeast(0.0))),
        )
    }

    fun observeLinesForSale(saleId: Long) = db.saleLineDao().observeForSale(saleId)
    fun observePaymentsForSale(saleId: Long) = db.salePaymentDao().observeForSale(saleId)

    /**
     * Copy an external URI (gallery or cache camera file) into durable app storage
     * (`filesDir/item_images`) and return a FileProvider URI so images survive cache clears
     * and temporary picker grants.
     */
    fun importImageToInternalStorage(sourceUri: String): String {
        val bytes = readUriBytes(sourceUri) ?: return sourceUri
        if (bytes.isEmpty()) return sourceUri
        val mime = sniffImageMime(bytes)
        val ext = when {
            mime.contains("png", ignoreCase = true) -> "png"
            mime.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val file = File(imageDir, "item_${System.currentTimeMillis()}.$ext")
        return runCatching {
            file.writeBytes(bytes)
            FileProvider.getUriForFile(
                appContext,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            ).toString()
        }.getOrDefault(sourceUri)
    }

    suspend fun exportBackupToUri(uri: Uri) {
        val temp = File(appContext.cacheDir, "export_temp_${System.currentTimeMillis()}.kalb")
        try {
            writeBackupFile(temp)
            appContext.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(temp.readBytes())
            } ?: error("Cannot open output stream")
        } finally {
            temp.delete()
        }
    }

    suspend fun importBackupFromUri(uri: Uri) {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot open input stream")
        val temp = File(appContext.cacheDir, "import_temp_${System.currentTimeMillis()}.kalb")
        try {
            temp.writeBytes(bytes)
            readBackupFile(temp)
        } finally {
            temp.delete()
        }
    }

    suspend fun zeroBalance(saleId: Long, reason: String) {
        val sale = db.saleDao().getById(saleId) ?: return
        require(reason.isNotBlank()) { "reason required" }
        db.saleDao().update(
            sale.copy(openBalance = 0.0, manuallyZeroed = true, zeroReason = reason),
        )
    }

    suspend fun dueReminders(nowEpochMs: Long = System.currentTimeMillis()): List<SaleEntity> {
        val dayMs = 24L * 60 * 60 * 1000
        val from = nowEpochMs
        val to = nowEpochMs + dayMs
        val dueToday = db.saleDao().getDueBetween(nowEpochMs - dayMs / 2, nowEpochMs + dayMs / 2)
        val dueTomorrow = db.saleDao().getDueBetween(from + dayMs / 2, to + dayMs / 2)
        return (dueToday + dueTomorrow).distinctBy { it.id }
    }

    enum class ReportRange { DAILY, WEEKLY, MONTHLY, QUARTERLY, BIANNUAL, YEARLY }

    data class SoldItemLine(
        val itemId: Long,
        val itemTitle: String,
        val categoryId: Long?,
        val categoryNameFa: String,
        val categoryNameEn: String,
        val quantity: Double,
        val unitPrice: Double,
        val lineTotal: Double,
    )

    data class CategorySalesGroup(
        val categoryId: Long?,
        val categoryNameFa: String,
        val categoryNameEn: String,
        val lines: List<SoldItemLine>,
        val subtotal: Double,
    )

    data class DetailedSalesReport(
        val groups: List<CategorySalesGroup> = emptyList(),
        val grandTotal: Double = 0.0,
    )

    suspend fun salesReport(range: ReportRange): List<SaleEntity> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val span = when (range) {
            ReportRange.DAILY -> day
            ReportRange.WEEKLY -> 7 * day
            ReportRange.MONTHLY -> 30 * day
            ReportRange.QUARTERLY -> 90 * day
            ReportRange.BIANNUAL -> 182 * day
            ReportRange.YEARLY -> 365 * day
        }
        // Include future-Hour timestamps on today's Jalali date (toGregorianEpochMs sets noon)
        return db.saleDao().getInRange(now - span, now + day)
    }

    /**
     * Sold lines in [range], aggregated by item, grouped under each category.
     * Category comes from the current item record (or "uncategorized" if missing).
     */
    suspend fun detailedSalesReport(range: ReportRange): DetailedSalesReport = groupSales(salesReport(range))

    suspend fun detailedSalesReport(fromEpochMs: Long, toEpochMs: Long): DetailedSalesReport =
        groupSales(db.saleDao().getInRange(fromEpochMs, toEpochMs))

    private suspend fun groupSales(sales: List<SaleEntity>): DetailedSalesReport {
        if (sales.isEmpty()) return DetailedSalesReport()
        val saleIds = sales.map { it.id }.toSet()
        val lines = db.saleLineDao().getAll().filter { it.saleId in saleIds }
        if (lines.isEmpty()) return DetailedSalesReport()
        val itemsById = db.itemDao().getAll().associateBy { it.id }
        val categoriesById = db.categoryDao().getAll().associateBy { it.id }

        data class AggKey(val itemId: Long, val categoryId: Long?, val title: String, val unitPrice: Double)
        val aggregated = linkedMapOf<AggKey, Double>()
        for (line in lines) {
            val item = itemsById[line.itemId]
            val categoryId = item?.categoryId
            val key = AggKey(
                itemId = line.itemId,
                categoryId = categoryId,
                title = line.itemTitle,
                unitPrice = line.unitPrice,
            )
            aggregated[key] = (aggregated[key] ?: 0.0) + line.quantity
        }

        val soldLines = aggregated.map { (key, qty) ->
            val cat = key.categoryId?.let { categoriesById[it] }
            SoldItemLine(
                itemId = key.itemId,
                itemTitle = key.title,
                categoryId = key.categoryId,
                categoryNameFa = cat?.nameFa ?: "بدون دسته",
                categoryNameEn = cat?.nameEn ?: "Uncategorized",
                quantity = qty,
                unitPrice = Money.round(key.unitPrice),
                lineTotal = Money.round(qty * key.unitPrice),
            )
        }

        val groups = soldLines
            .groupBy { Triple(it.categoryId, it.categoryNameFa, it.categoryNameEn) }
            .map { (key, groupLines) ->
                CategorySalesGroup(
                    categoryId = key.first,
                    categoryNameFa = key.second,
                    categoryNameEn = key.third,
                    lines = groupLines.sortedBy { it.itemTitle },
                    subtotal = Money.round(groupLines.sumOf { it.lineTotal }),
                )
            }
            .sortedBy { it.categoryNameFa }

        return DetailedSalesReport(
            groups = groups,
            grandTotal = Money.round(groups.sumOf { it.subtotal }),
        )
    }

    /** Metadata-only payload stored as UTF-8 JSON inside the `.kalb` binary container. */
    data class BackupPayload(
        @SerializedName("schemaVersion") val schemaVersion: Int? = null,
        @SerializedName("settings") val settings: SettingsEntity? = null,
        @SerializedName("units") val units: List<UnitEntity>? = null,
        @SerializedName("categories") val categories: List<CategoryEntity>? = null,
        @SerializedName("items") val items: List<ItemEntity>? = null,
        @SerializedName("dollarHistory") val dollarHistory: List<DollarRateHistoryEntity>? = null,
        @SerializedName("sales") val sales: List<SaleEntity>? = null,
        @SerializedName("saleLines") val saleLines: List<SaleLineEntity>? = null,
        @SerializedName("salePayments") val salePayments: List<SalePaymentEntity>? = null,
        @SerializedName("formulas") val formulas: List<PricingFormulaEntity>? = null,
        @SerializedName("formulaVersions") val formulaVersions: List<FormulaVersionEntity>? = null,
        @SerializedName("priceBatches") val priceBatches: List<PriceBatchEntity>? = null,
        @SerializedName("priceHistory") val priceHistory: List<PriceHistoryEntity>? = null,
        @SerializedName("discountRules") val discountRules: List<DiscountRuleEntity>? = null,
        @SerializedName("attributeDefinitions") val attributeDefinitions: List<AttributeDefinitionEntity>? = null,
        @SerializedName("attributeValues") val attributeValues: List<ItemAttributeValueEntity>? = null,
        @SerializedName("auditLog") val auditLog: List<AuditLogEntity>? = null,
        @SerializedName("config") val config: List<AppConfigEntity>? = null,
        @SerializedName("roles") val roles: List<RoleEntity>? = null,
        @SerializedName("users") val users: List<UserEntity>? = null,
    )

    suspend fun writeBackupFile(file: File) {
        val items = db.itemDao().getAll()
        val images = items.mapNotNull { item -> collectRawImage(item) }
        // Strip non-portable local URI paths; image bytes live in the binary section.
        val portableItems = items.map { it.copy(imageUri = null) }
        val payload = BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            settings = db.settingsDao().get(),
            units = db.unitDao().getAll(),
            categories = db.categoryDao().getAll(),
            items = portableItems,
            dollarHistory = db.dollarRateHistoryDao().getAll(),
            sales = db.saleDao().getAll(),
            saleLines = db.saleLineDao().getAll(),
            salePayments = db.salePaymentDao().getAll(),
            formulas = db.formulaDao().getAll(),
            formulaVersions = db.formulaDao().getAllVersions(),
            priceBatches = db.priceBatchDao().getAll(),
            priceHistory = db.priceHistoryDao().getAll(),
            discountRules = db.discountRuleDao().getAll(),
            attributeDefinitions = db.attributeDao().getDefinitions(),
            attributeValues = db.attributeDao().allValues(),
            auditLog = db.auditDao().getAll(),
            config = db.configDao().getAll(),
            roles = db.userDao().getRoles(),
            users = db.userDao().getUsers(),
        )
        val archiveBytes = KalabanBackupArchive.pack(
            metaJson = gson.toJson(payload),
            images = images,
        )
        file.writeBytes(archiveBytes)
    }

    suspend fun readBackupFile(file: File) {
        val bytes = file.readBytes()
        when {
            KalabanBackupArchive.isKalabanArchive(bytes) -> {
                val archive = KalabanBackupArchive.unpack(bytes)
                applyBackupPayload(
                    payload = gson.fromJson(archive.metaJson, BackupPayload::class.java) ?: return,
                    images = archive.images,
                )
            }
            // Legacy plain-JSON backups (pre-.kalb): import metadata only; paths may be dead.
            else -> {
                val json = String(bytes, StandardCharsets.UTF_8)
                val payload = gson.fromJson(json, BackupPayload::class.java) ?: return
                applyBackupPayload(payload = payload, images = emptyList())
            }
        }
    }

    private suspend fun applyBackupPayload(
        payload: BackupPayload,
        images: List<KalabanBackupArchive.ImageEntry>,
    ) {
        // Gson can leave Kotlin non-null lists as null at runtime.
        val units = payload.units ?: emptyList()
        val categories = payload.categories ?: emptyList()
        val items = payload.items ?: emptyList()
        val dollarHistory = payload.dollarHistory ?: emptyList()
        val sales = payload.sales ?: emptyList()
        val saleLines = payload.saleLines ?: emptyList()
        val salePayments = payload.salePayments ?: emptyList()
        val imagesByItemId = images.groupBy { it.itemId }.mapValues { it.value.last() }

        // Restore images to durable storage before the DB transaction (IO outside Room).
        val restoredUris = items.associate { item ->
            val restored = imagesByItemId[item.id]?.let { entry ->
                storeRawImage(item.id, entry.mime, entry.bytes)
            }
            val fallback = item.imageUri?.takeIf { canReadUri(it) }
            item.id to (restored ?: fallback)
        }

        db.withTransaction {
            // Item REPLACE would DELETE+INSERT and fail while sale_lines still reference items
            // (FK ON DELETE RESTRICT). Wipe in dependency order, then insert fresh rows.
            db.clearAllTables()

            payload.settings?.let { db.settingsDao().upsert(it.copy(id = 1)) }
                ?: db.settingsDao().upsert(SettingsEntity())

            // After clear, must INSERT (update-only upsertUnit would no-op for missing ids).
            units.forEach { db.unitDao().insert(it) }
            categories.forEach { db.categoryDao().insert(it) }
            items.forEach { item ->
                db.itemDao().insert(item.copy(imageUri = restoredUris[item.id]))
            }
            dollarHistory.forEach { db.dollarRateHistoryDao().insert(it.copy(id = 0)) }
            // Keep ids that other rows reference (formulas, batches, attributes, roles); insert parents first.
            payload.formulas.orEmpty().forEach { db.formulaDao().insert(it) }
            payload.formulaVersions.orEmpty().forEach { db.formulaDao().insertVersion(it.copy(id = 0)) }
            payload.priceBatches.orEmpty().forEach { db.priceBatchDao().insert(it) }
            db.priceHistoryDao().insertAll(payload.priceHistory.orEmpty().map { it.copy(id = 0) })
            payload.discountRules.orEmpty().forEach { db.discountRuleDao().insert(it) }
            payload.attributeDefinitions.orEmpty().forEach { db.attributeDao().insertDefinition(it) }
            db.attributeDao().upsertValues(payload.attributeValues.orEmpty())
            db.auditDao().insertAll(payload.auditLog.orEmpty().map { it.copy(id = 0) })
            db.configDao().upsertAll(payload.config.orEmpty())
            payload.roles.orEmpty().forEach { db.userDao().insertRole(it) }
            payload.users.orEmpty().forEach { db.userDao().insertUser(it) }
            sales.forEach { db.saleDao().upsert(it) }
            if (saleLines.isNotEmpty()) {
                db.saleLineDao().insertAll(saleLines.map { it.copy(id = 0) })
            }
            salePayments.forEach { db.salePaymentDao().insert(it.copy(id = 0)) }
        }
        // Backups made before roles existed restore with none; reseed built-in defaults.
        db.seedIfEmpty()
    }

    private fun collectRawImage(item: ItemEntity): KalabanBackupArchive.ImageEntry? {
        val uri = item.imageUri?.takeIf { it.isNotBlank() } ?: return null
        val bytes = readUriBytes(uri) ?: return null
        if (bytes.isEmpty()) return null
        return KalabanBackupArchive.ImageEntry(
            itemId = item.id,
            mime = sniffImageMime(bytes),
            bytes = bytes,
        )
    }

    private fun storeRawImage(itemId: Long, mime: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val ext = when {
                mime.contains("png", ignoreCase = true) -> "png"
                mime.contains("webp", ignoreCase = true) -> "webp"
                sniffImageMime(bytes).contains("png") -> "png"
                else -> "jpg"
            }
            val file = File(imageDir, "backup_${itemId}_${System.currentTimeMillis()}.$ext")
            file.writeBytes(bytes)
            FileProvider.getUriForFile(
                appContext,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            ).toString()
        }.getOrNull()
    }

    private fun readUriBytes(uriString: String): ByteArray? {
        return runCatching {
            appContext.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
                ?: run {
                    val asFile = when {
                        uriString.startsWith("file:") -> File(Uri.parse(uriString).path ?: return null)
                        uriString.startsWith("/") -> File(uriString)
                        else -> null
                    }
                    asFile?.takeIf { it.isFile }?.readBytes()
                }
        }.getOrNull()
    }

    private fun canReadUri(uriString: String): Boolean {
        return readUriBytes(uriString)?.isNotEmpty() == true
    }

    private fun sniffImageMime(bytes: ByteArray): String {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }
        return "image/jpeg"
    }
}
