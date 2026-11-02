package com.kalaban.farshidb2011.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.csv.Csv
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.core.pricing.PricingModes
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.BatchStatus
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.PriceBatchEntity
import com.kalaban.farshidb2011.data.local.entity.PriceHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.PriceSource
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UnitEntity
import com.kalaban.farshidb2011.domain.Actor
import java.math.BigDecimal

/** Importable product columns; [key] is the canonical CSV header, [aliases] are matched case-insensitively. */
enum class ImportField(val key: String, val required: Boolean, val numeric: Boolean, val aliases: List<String>) {
    TITLE("title", true, false, listOf("name", "product", "عنوان", "نام", "نام کالا", "کالا")),
    SKU("sku", false, false, listOf("code", "کد", "کد کالا")),
    BARCODE("barcode", false, false, listOf("ean", "upc", "بارکد")),
    CATEGORY("category", false, false, listOf("دسته", "دسته‌بندی", "دسته بندی")),
    UNIT("unit", false, false, listOf("واحد")),
    PIECES("pieces_per_unit", false, true, listOf("pieces", "تعداد در واحد")),
    PURCHASE_PRICE("purchase_price", false, true, listOf("cost", "قیمت خرید", "قیمت کل واحد")),
    PURCHASE_RATE("purchase_rate", false, true, listOf("dollar_rate", "نرخ دلار خرید", "نرخ خرید")),
    SELL_PRICE("sell_price", false, true, listOf("price", "قیمت فروش", "قیمت")),
    STOCK("stock", false, true, listOf("quantity", "qty", "موجودی")),
    LOW_STOCK("low_stock_threshold", false, true, listOf("min_stock", "آستانه")),
    MIN_PRICE("min_price", false, true, listOf("حداقل قیمت")),
    MAX_PRICE("max_price", false, true, listOf("حداکثر قیمت")),
    TRANSPORT("transport_cost", false, true, listOf("هزینه حمل")),
    PACKAGING("packaging_cost", false, true, listOf("هزینه بسته‌بندی")),
    OTHER_COST("other_cost", false, true, listOf("سایر هزینه‌ها")),
    PRICING_MODE("pricing_mode", false, false, listOf("روش قیمت‌گذاری")),
    PRICING_VALUE("pricing_value", false, true, listOf("مقدار قیمت‌گذاری")),
    BRAND("brand", false, false, listOf("برند")),
    SUPPLIER("supplier", false, false, listOf("تأمین‌کننده", "تامین کننده")),
    TAGS("tags", false, false, listOf("برچسب‌ها", "برچسب")),
    DESCRIPTION("description", false, false, listOf("توضیحات")),
    NOTES("notes", false, false, listOf("یادداشت")),
}

enum class DuplicatePolicy { SKIP, UPDATE, CREATE }

enum class ImportIssue {
    TITLE_MISSING,
    INVALID_NUMBER,
    NEGATIVE_VALUE,
    UNKNOWN_CATEGORY,
    UNKNOWN_UNIT,
    DUPLICATE_IN_FILE,
    BARCODE_CONFLICT,
    INVALID_PRICING_MODE,
    MARGIN_INVALID,
    VALUE_TOO_LARGE,
}

enum class ImportRowStatus { NEW, UPDATE, SKIP, ERROR }

data class ParsedCsv(val header: List<String>, val rows: List<List<String>>, val delimiter: Char)

data class ImportOptions(
    val mapping: Map<ImportField, Int>,
    val duplicates: DuplicatePolicy = DuplicatePolicy.SKIP,
    val createMissingCategories: Boolean = true,
    val createMissingUnits: Boolean = true,
    val defaultUnitId: Long? = null,
)

data class ImportRow(
    val line: Int,
    val title: String,
    val values: Map<ImportField, String>,
    val status: ImportRowStatus,
    val issues: List<Pair<ImportIssue, ImportField?>>,
    val existingId: Long?,
)

data class ImportPreview(val rows: List<ImportRow>) {
    fun count(status: ImportRowStatus) = rows.count { it.status == status }
}

data class ImportResult(val created: Int, val updated: Int, val skipped: Int, val failed: Int, val batchId: Long?)

class ImportExportRepository(
    private val db: KalabanDatabase,
    private val appContext: Context,
    private val pricing: PricingRepository,
    private val config: ConfigRepository,
    private val audit: AuditRepository,
) {
    private val gson = Gson()

    suspend fun parse(uri: Uri): ParsedCsv {
        val text = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Cannot open input stream")
        val delimiter = Csv.detectDelimiter(text)
        val all = Csv.parse(text, delimiter)
        require(all.isNotEmpty()) { "empty file" }
        return ParsedCsv(all.first().map { it.trim() }, all.drop(1), delimiter)
    }

    /** Header matching by canonical key or alias, falling back to the mapping saved from the last import. */
    suspend fun autoMap(header: List<String>): Map<ImportField, Int> {
        val saved: Map<String, String> = config.current().importMapping.takeIf { it.isNotBlank() }?.let {
            runCatching { gson.fromJson<Map<String, String>>(it, object : TypeToken<Map<String, String>>() {}.type) }.getOrNull()
        }.orEmpty()
        val normalized = header.map { it.trim().lowercase() }
        val mapping = LinkedHashMap<ImportField, Int>()
        for (field in ImportField.entries) {
            val names = (listOf(field.key) + field.aliases).map { it.lowercase() } + listOfNotNull(saved[field.key]?.lowercase())
            val index = normalized.indexOfFirst { it in names }
            if (index >= 0) mapping[field] = index
        }
        return mapping
    }

    suspend fun saveMapping(header: List<String>, mapping: Map<ImportField, Int>) {
        val byKey = mapping.mapNotNull { (field, index) -> header.getOrNull(index)?.let { field.key to it } }.toMap()
        config.update { it.copy(importMapping = gson.toJson(byKey)) }
    }

    private fun cell(row: List<String>, options: ImportOptions, field: ImportField): String? =
        options.mapping[field]?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }

    suspend fun preview(parsed: ParsedCsv, options: ImportOptions, fa: Boolean): ImportPreview {
        val items = db.itemDao().getAll()
        val byBarcode = items.filter { !it.barcode.isNullOrBlank() }.associateBy { it.barcode!! }
        val bySku = items.filter { !it.sku.isNullOrBlank() }.groupBy { it.sku!! }
        val categories = db.categoryDao().getAll()
        val units = db.unitDao().getAll()
        val seenBarcodes = HashSet<String>()
        val seenSkus = HashSet<String>()
        val rows = parsed.rows.mapIndexed { index, raw ->
            val values = ImportField.entries.mapNotNull { f -> cell(raw, options, f)?.let { f to it } }.toMap()
            val issues = ArrayList<Pair<ImportIssue, ImportField?>>()
            val title = values[ImportField.TITLE].orEmpty()
            if (title.isBlank()) issues += ImportIssue.TITLE_MISSING to ImportField.TITLE
            for ((field, v) in values) {
                if (!field.numeric) continue
                val number = Numbers.parseDecimal(v)
                when {
                    number == null -> issues += ImportIssue.INVALID_NUMBER to field
                    number.signum() < 0 && field != ImportField.PRICING_VALUE -> issues += ImportIssue.NEGATIVE_VALUE to field
                    !Money.isStorable(number) -> issues += ImportIssue.VALUE_TOO_LARGE to field
                }
            }
            val mode = values[ImportField.PRICING_MODE]?.uppercase()
            if (mode != null && mode !in PricingModes.all) issues += ImportIssue.INVALID_PRICING_MODE to ImportField.PRICING_MODE
            if (mode == PricingModes.MARGIN) {
                val m = Numbers.parseDecimal(values[ImportField.PRICING_VALUE])
                if (m == null || m >= Money.HUNDRED) issues += ImportIssue.MARGIN_INVALID to ImportField.PRICING_VALUE
            }
            values[ImportField.CATEGORY]?.let { name ->
                val known = categories.any { it.nameFa.equals(name, true) || it.nameEn.equals(name, true) }
                if (!known && !options.createMissingCategories) issues += ImportIssue.UNKNOWN_CATEGORY to ImportField.CATEGORY
            }
            val unitName = values[ImportField.UNIT]
            if (unitName != null) {
                val known = units.any { it.nameFa.equals(unitName, true) || it.nameEn.equals(unitName, true) }
                if (!known && !options.createMissingUnits) issues += ImportIssue.UNKNOWN_UNIT to ImportField.UNIT
            } else if (options.defaultUnitId == null && units.isEmpty()) {
                issues += ImportIssue.UNKNOWN_UNIT to ImportField.UNIT
            }
            val barcode = values[ImportField.BARCODE]
            val sku = values[ImportField.SKU]
            if ((barcode != null && !seenBarcodes.add(barcode)) || (barcode == null && sku != null && !seenSkus.add(sku))) {
                issues += ImportIssue.DUPLICATE_IN_FILE to (if (barcode != null) ImportField.BARCODE else ImportField.SKU)
            }
            val existing = barcode?.let { byBarcode[it] } ?: sku?.let { bySku[it]?.firstOrNull() }
            if (existing != null && options.duplicates == DuplicatePolicy.CREATE && barcode != null && byBarcode.containsKey(barcode)) {
                issues += ImportIssue.BARCODE_CONFLICT to ImportField.BARCODE
            }
            val status = when {
                issues.isNotEmpty() -> ImportRowStatus.ERROR
                existing == null -> ImportRowStatus.NEW
                options.duplicates == DuplicatePolicy.SKIP -> ImportRowStatus.SKIP
                options.duplicates == DuplicatePolicy.UPDATE -> ImportRowStatus.UPDATE
                else -> ImportRowStatus.NEW
            }
            ImportRow(
                line = index + 2,
                title = title,
                values = values,
                status = status,
                issues = issues,
                existingId = existing?.id.takeIf { status == ImportRowStatus.UPDATE },
            )
        }
        return ImportPreview(rows)
    }

    /** Imports NEW/UPDATE rows in a single transaction; ERROR and SKIP rows are left untouched and reported. */
    suspend fun import(preview: ImportPreview, options: ImportOptions, actor: Actor, title: String): ImportResult =
        db.withTransaction {
            val rows = preview.rows.filter { it.status == ImportRowStatus.NEW || it.status == ImportRowStatus.UPDATE }
            if (rows.isEmpty()) {
                return@withTransaction ImportResult(0, 0, preview.count(ImportRowStatus.SKIP), preview.count(ImportRowStatus.ERROR), null)
            }
            val settings = db.settingsDao().get() ?: SettingsEntity()
            val categories = db.categoryDao().getAll().toMutableList()
            val units = db.unitDao().getAll().toMutableList()
            suspend fun resolveCategory(name: String?): Long? {
                if (name == null) return null
                categories.firstOrNull { it.nameFa.equals(name, true) || it.nameEn.equals(name, true) }?.let { return it.id }
                val id = db.categoryDao().insert(CategoryEntity(nameFa = name, nameEn = name))
                categories += CategoryEntity(id = id, nameFa = name, nameEn = name)
                return id
            }
            suspend fun resolveUnit(name: String?): Long {
                if (name != null) {
                    units.firstOrNull { it.nameFa.equals(name, true) || it.nameEn.equals(name, true) }?.let { return it.id }
                    val id = db.unitDao().insert(UnitEntity(nameFa = name, nameEn = name))
                    units += UnitEntity(id = id, nameFa = name, nameEn = name)
                    return id
                }
                return options.defaultUnitId ?: units.first().id
            }
            val ctx = pricing.loadContext()
            val now = System.currentTimeMillis()
            val batchId = db.priceBatchDao().insert(
                PriceBatchEntity(source = PriceSource.IMPORT, title = title, actor = actor.name, createdAtEpochMs = now),
            )
            val history = ArrayList<PriceHistoryEntity>()
            var created = 0
            var updated = 0
            for (row in rows) {
                val v = row.values
                fun num(f: ImportField) = Numbers.parseDecimal(v[f])
                val existing = row.existingId?.let { db.itemDao().getById(it) }
                val pieces = num(ImportField.PIECES)?.takeIf { it.signum() > 0 } ?: existing?.piecesPerUnit?.let(Money::of) ?: BigDecimal.ONE
                val purchasePerPiece = num(ImportField.PURCHASE_PRICE)?.let { Money.round(it.divide(pieces, Money.MC)) }
                val sell = num(ImportField.SELL_PRICE)
                val mode = v[ImportField.PRICING_MODE]?.uppercase() ?: if (sell != null) PricingModes.MANUAL else existing?.pricingMode ?: PricingModes.AUTO
                val base = existing ?: ItemEntity(title = row.title, unitId = 0, purchasePrice = 0.0, purchaseDollarRate = settings.dollarRate, sellPrice = 0.0)
                val item = base.copy(
                    title = row.title.ifBlank { base.title },
                    sku = v[ImportField.SKU] ?: base.sku,
                    barcode = v[ImportField.BARCODE] ?: base.barcode,
                    categoryId = if (v.containsKey(ImportField.CATEGORY)) resolveCategory(v[ImportField.CATEGORY]) else base.categoryId,
                    unitId = if (existing != null && v[ImportField.UNIT] == null) base.unitId else resolveUnit(v[ImportField.UNIT]),
                    piecesPerUnit = pieces.toDouble(),
                    purchasePrice = purchasePerPiece?.toDouble() ?: base.purchasePrice,
                    purchaseDollarRate = num(ImportField.PURCHASE_RATE)?.toDouble()
                        ?: if (purchasePerPiece != null) settings.dollarRate else base.purchaseDollarRate,
                    stockQty = num(ImportField.STOCK)?.multiply(pieces, Money.MC)?.let(Money::round)?.toDouble() ?: base.stockQty,
                    lowStockThreshold = num(ImportField.LOW_STOCK)?.toDouble() ?: base.lowStockThreshold.takeIf { existing != null } ?: 1.0,
                    minPrice = num(ImportField.MIN_PRICE)?.toDouble() ?: base.minPrice,
                    maxPrice = num(ImportField.MAX_PRICE)?.toDouble() ?: base.maxPrice,
                    transportCost = num(ImportField.TRANSPORT)?.toDouble() ?: base.transportCost,
                    packagingCost = num(ImportField.PACKAGING)?.toDouble() ?: base.packagingCost,
                    otherCost = num(ImportField.OTHER_COST)?.toDouble() ?: base.otherCost,
                    pricingMode = mode,
                    pricingValue = num(ImportField.PRICING_VALUE)?.toDouble() ?: base.pricingValue,
                    sellPrice = sell?.toDouble() ?: base.sellPrice,
                    brand = v[ImportField.BRAND] ?: base.brand,
                    supplier = v[ImportField.SUPPLIER] ?: base.supplier,
                    tags = v[ImportField.TAGS] ?: base.tags,
                    description = v[ImportField.DESCRIPTION] ?: base.description,
                    notes = v[ImportField.NOTES] ?: base.notes,
                    updatedAtEpochMs = now,
                    createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                )
                val result = PriceCalculator.calculate(pricing.input(item), ctx.policy, ctx.formulas, ctx.knownVariables)
                val priced = item.copy(sellPrice = result.finalPrice.toDouble())
                val id = if (existing == null) {
                    created++
                    db.itemDao().insert(priced)
                } else {
                    updated++
                    db.itemDao().update(priced)
                    priced.id
                }
                if (existing == null || existing.sellPrice != priced.sellPrice) {
                    history += PriceHistoryEntity(
                        itemId = id,
                        batchId = batchId,
                        oldSellPrice = existing?.sellPrice,
                        newSellPrice = priced.sellPrice,
                        newCost = result.cost.fullCost.toDouble(),
                        source = PriceSource.IMPORT,
                        actor = actor.name,
                        changedAtEpochMs = now,
                    )
                }
            }
            db.priceHistoryDao().insertAll(history)
            db.priceBatchDao().getById(batchId)?.let {
                db.priceBatchDao().update(it.copy(affectedCount = history.size, status = BatchStatus.APPLIED))
            }
            val skipped = preview.count(ImportRowStatus.SKIP)
            val failed = preview.count(ImportRowStatus.ERROR)
            audit.log(actor, AuditAction.DATA_IMPORT, AuditEntity.DATA, batchId, title, "created: $created; updated: $updated; skipped: $skipped; errors: $failed")
            ImportResult(created, updated, skipped, failed, batchId)
        }

    fun template(delimiter: Char): String = Csv.write(
        listOf(
            ImportField.entries.map { it.key },
            listOf(
                "Silk scarf", "SC-100", "6260000000017", "Fabric", "Piece", "1", "250000", "600000", "", "12", "2",
                "", "", "5000", "2000", "0", "AUTO", "", "Kalaban", "Main supplier", "silk,red", "", "",
            ),
        ),
        delimiter,
    )

    /** Product export in the import format, so an exported file can be edited and imported back. */
    suspend fun exportProducts(delimiter: Char): String {
        val categories = db.categoryDao().getAll().associateBy { it.id }
        val units = db.unitDao().getAll().associateBy { it.id }
        val rows = db.itemDao().getAll().sortedBy { it.title }.map { item ->
            val pieces = Money.of(item.piecesPerUnit).max(BigDecimal.ONE)
            ImportField.entries.map { f ->
                when (f) {
                    ImportField.TITLE -> Csv.text(item.title)
                    ImportField.SKU -> Csv.text(item.sku)
                    ImportField.BARCODE -> Csv.text(item.barcode)
                    ImportField.CATEGORY -> Csv.text(item.categoryId?.let { categories[it]?.nameFa })
                    ImportField.UNIT -> Csv.text(units[item.unitId]?.nameFa)
                    ImportField.PIECES -> Money.input(item.piecesPerUnit)
                    ImportField.PURCHASE_PRICE -> Money.input(Money.of(item.purchasePrice).multiply(pieces, Money.MC))
                    ImportField.PURCHASE_RATE -> Money.input(item.purchaseDollarRate)
                    ImportField.SELL_PRICE -> if (item.pricingMode == PricingModes.MANUAL) Money.input(item.sellPrice) else ""
                    ImportField.STOCK -> Money.input(Money.of(item.stockQty).divide(pieces, Money.MC))
                    ImportField.LOW_STOCK -> Money.input(item.lowStockThreshold)
                    ImportField.MIN_PRICE -> item.minPrice?.let(Money::input).orEmpty()
                    ImportField.MAX_PRICE -> item.maxPrice?.let(Money::input).orEmpty()
                    ImportField.TRANSPORT -> Money.input(item.transportCost)
                    ImportField.PACKAGING -> Money.input(item.packagingCost)
                    ImportField.OTHER_COST -> Money.input(item.otherCost)
                    ImportField.PRICING_MODE -> item.pricingMode
                    ImportField.PRICING_VALUE -> item.pricingValue?.let(Money::input).orEmpty()
                    ImportField.BRAND -> Csv.text(item.brand)
                    ImportField.SUPPLIER -> Csv.text(item.supplier)
                    ImportField.TAGS -> Csv.text(item.tags)
                    ImportField.DESCRIPTION -> Csv.text(item.description)
                    ImportField.NOTES -> Csv.text(item.notes)
                }
            }
        }
        return Csv.write(listOf(ImportField.entries.map { it.key }) + rows, delimiter)
    }

    fun writeText(uri: Uri, text: String) {
        appContext.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: error("Cannot open output stream")
    }
}
