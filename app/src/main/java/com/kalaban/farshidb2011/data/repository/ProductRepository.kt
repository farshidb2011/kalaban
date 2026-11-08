package com.kalaban.farshidb2011.data.repository

import androidx.room.withTransaction
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.PricingEngine
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.core.pricing.PriceResult
import com.kalaban.farshidb2011.core.pricing.PricingModes
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.AttributeType
import com.kalaban.farshidb2011.data.local.entity.ItemAttributeValueEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.ItemStatus
import com.kalaban.farshidb2011.data.local.entity.PriceSource
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.domain.Actor
import java.math.BigDecimal

/** Everything the product editor submits; amounts are already parsed and money values are per unit/pack. */
data class ProductDraft(
    val id: Long = 0,
    val title: String,
    val sku: String? = null,
    val barcode: String? = null,
    val unitId: Long?,
    val categoryId: Long? = null,
    val brand: String? = null,
    val supplier: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val tags: String? = null,
    val imageUri: String? = null,
    val piecesPerUnit: BigDecimal = BigDecimal.ONE,
    val packPurchasePrice: BigDecimal = BigDecimal.ZERO,
    val transportCost: BigDecimal = BigDecimal.ZERO,
    val packagingCost: BigDecimal = BigDecimal.ZERO,
    val otherCost: BigDecimal = BigDecimal.ZERO,
    val dollarLinked: Boolean = true,
    val pricingMode: String = PricingModes.AUTO,
    val pricingValue: BigDecimal? = null,
    val formulaId: Long? = null,
    val manualPrice: BigDecimal? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val stockUnits: BigDecimal = BigDecimal.ZERO,
    val lowStockThreshold: BigDecimal = BigDecimal.ONE,
    val maxStock: BigDecimal? = null,
    val status: String = ItemStatus.ACTIVE,
    /** Raw values keyed by attribute definition id. */
    val attributes: Map<Long, String> = emptyMap(),
)

enum class ProductError {
    TITLE_REQUIRED,
    UNIT_REQUIRED,
    NEGATIVE_VALUE,
    VALUE_TOO_LARGE,
    PIECES_INVALID,
    BARCODE_DUPLICATE,
    SKU_DUPLICATE,
    PRICING_VALUE_REQUIRED,
    MARGIN_INVALID,
    FORMULA_REQUIRED,
    MANUAL_PRICE_REQUIRED,
    MIN_ABOVE_MAX,
    ATTRIBUTE_REQUIRED,
    ATTRIBUTE_INVALID,
}

sealed class SaveProductResult {
    data class Saved(val id: Long, val price: BigDecimal, val result: PriceResult) : SaveProductResult()
    data class Invalid(val errors: Set<ProductError>) : SaveProductResult()
}

data class DeleteProductsResult(val deleted: Int, val blocked: List<String>)

class ProductRepository(
    private val db: KalabanDatabase,
    private val pricing: PricingRepository,
    private val audit: AuditRepository,
) {
    fun observeCatalog(query: String, status: String) = db.itemDao().observeCatalog(query.trim(), status)
    fun observeItem(id: Long) = db.itemDao().observeById(id)
    fun observeAttributeValues(itemId: Long) = db.attributeDao().observeValuesForItem(itemId)
    fun observeBrands() = db.itemDao().observeBrands()
    fun observeSuppliers() = db.itemDao().observeSuppliers()
    suspend fun item(id: Long) = db.itemDao().getById(id)
    suspend fun attributeValues(itemId: Long) = db.attributeDao().valuesForItem(itemId)

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private suspend fun validate(d: ProductDraft): Set<ProductError> {
        val errors = linkedSetOf<ProductError>()
        if (d.title.isBlank()) errors += ProductError.TITLE_REQUIRED
        if (d.unitId == null) errors += ProductError.UNIT_REQUIRED
        if (d.piecesPerUnit.signum() <= 0) errors += ProductError.PIECES_INVALID
        val amounts = listOfNotNull(
            d.packPurchasePrice, d.transportCost, d.packagingCost, d.otherCost, d.stockUnits,
            d.lowStockThreshold, d.maxStock, d.minPrice, d.maxPrice, d.manualPrice,
        )
        if (amounts.any { it.signum() < 0 }) errors += ProductError.NEGATIVE_VALUE
        if ((amounts + listOfNotNull(d.pricingValue)).any { !Money.isStorable(it) }) errors += ProductError.VALUE_TOO_LARGE
        when (d.pricingMode) {
            PricingModes.MARKUP, PricingModes.FIXED_PROFIT ->
                if (d.pricingValue == null) errors += ProductError.PRICING_VALUE_REQUIRED
            PricingModes.MARGIN ->
                if (d.pricingValue == null) {
                    errors += ProductError.PRICING_VALUE_REQUIRED
                } else if (d.pricingValue.signum() < 0 || d.pricingValue >= Money.HUNDRED) {
                    errors += ProductError.MARGIN_INVALID
                }
            PricingModes.FORMULA -> if (d.formulaId == null) errors += ProductError.FORMULA_REQUIRED
            PricingModes.MANUAL -> if (d.manualPrice == null) errors += ProductError.MANUAL_PRICE_REQUIRED
        }
        if (d.minPrice != null && d.maxPrice != null && d.minPrice > d.maxPrice) errors += ProductError.MIN_ABOVE_MAX
        d.barcode.clean()?.let { code ->
            val other = db.itemDao().findByBarcode(code)
            if (other != null && other.id != d.id) errors += ProductError.BARCODE_DUPLICATE
        }
        d.sku.clean()?.let { sku ->
            if (db.itemDao().findBySku(sku).any { it.id != d.id }) errors += ProductError.SKU_DUPLICATE
        }
        val definitions = db.attributeDao().getDefinitions().filter { it.active }
        for (def in definitions) {
            val raw = d.attributes[def.id].clean()
            if (raw == null) {
                if (def.required) errors += ProductError.ATTRIBUTE_REQUIRED
                continue
            }
            val ok = when (def.type) {
                AttributeType.NUMBER -> Numbers.parseDecimal(raw) != null
                AttributeType.BOOLEAN -> raw == "true" || raw == "false"
                AttributeType.CHOICE -> def.options.orEmpty().split(',').map { it.trim() }.contains(raw)
                else -> true
            }
            if (!ok) errors += ProductError.ATTRIBUTE_INVALID
        }
        return errors
    }

    /** Validates, prices with the active rules, persists, and records price history and audit in one transaction. */
    suspend fun save(d: ProductDraft, actor: Actor): SaveProductResult = db.withTransaction {
        val errors = validate(d)
        if (errors.isNotEmpty()) return@withTransaction SaveProductResult.Invalid(errors)
        val existing = if (d.id != 0L) db.itemDao().getById(d.id) else null
        val settings = db.settingsDao().get() ?: SettingsEntity()
        val pieces = d.piecesPerUnit
        val perPiece = Money.round(d.packPurchasePrice.divide(pieces, Money.MC))
        // MANUAL sell is an absolute shelf price: re-anchor purchaseDollarRate to today's rate so
        // dollar-linked recalculation scales from this save, without rewriting purchasePrice.
        val rate = when {
            d.pricingMode == PricingModes.MANUAL -> settings.dollarRate
            else -> existing?.let {
                PricingEngine.purchaseRateAfterEdit(
                    it.purchasePrice,
                    it.purchaseDollarRate,
                    perPiece.toDouble(),
                    settings.dollarRate,
                )
            } ?: settings.dollarRate
        }
        val now = System.currentTimeMillis()
        val draftItem = ItemEntity(
            id = d.id,
            title = d.title.trim(),
            imageUri = d.imageUri,
            unitId = d.unitId!!,
            categoryId = d.categoryId,
            purchasePrice = perPiece.toDouble(),
            purchaseDollarRate = rate,
            sellPrice = d.manualPrice?.toDouble() ?: existing?.sellPrice ?: 0.0,
            piecesPerUnit = pieces.toDouble(),
            stockQty = Money.round(d.stockUnits.multiply(pieces, Money.MC)).toDouble(),
            lowStockThreshold = d.lowStockThreshold.toDouble(),
            sku = d.sku.clean(),
            updatedAtEpochMs = now,
            barcode = d.barcode.clean(),
            brand = d.brand.clean(),
            supplier = d.supplier.clean(),
            description = d.description.clean(),
            notes = d.notes.clean(),
            tags = d.tags?.split(',')?.mapNotNull { it.clean() }?.distinct()?.joinToString(",")?.takeIf { it.isNotEmpty() },
            status = d.status,
            transportCost = d.transportCost.toDouble(),
            packagingCost = d.packagingCost.toDouble(),
            otherCost = d.otherCost.toDouble(),
            dollarLinked = d.dollarLinked,
            pricingMode = d.pricingMode,
            pricingValue = d.pricingValue?.toDouble(),
            formulaId = if (d.pricingMode == PricingModes.FORMULA) d.formulaId else null,
            minPrice = d.minPrice?.toDouble(),
            maxPrice = d.maxPrice?.toDouble(),
            maxStock = d.maxStock?.toDouble(),
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
        )
        val ctx = pricing.loadContext()
        val numeric = ctx.attributeDefinitions
            .filter { it.active && it.type == AttributeType.NUMBER }
            .mapNotNull { def -> Numbers.parseDecimal(d.attributes[def.id])?.let { def.attrKey to it } }
            .toMap()
        val result = PriceCalculator.calculate(pricing.input(draftItem, numeric), ctx.policy, ctx.formulas, ctx.knownVariables)
        val item = draftItem.copy(sellPrice = result.finalPrice.toDouble())
        val id = if (existing == null) db.itemDao().insert(item) else {
            db.itemDao().update(item)
            item.id
        }
        db.attributeDao().clearForItem(id)
        db.attributeDao().upsertValues(
            d.attributes.mapNotNull { (attrId, raw) -> raw.clean()?.let { ItemAttributeValueEntity(id, attrId, it) } },
        )
        val newCost = result.cost.fullCost.toDouble()
        if (existing == null) {
            pricing.recordChange(id, null, item.sellPrice, null, newCost, PriceSource.CREATE, actor)
            audit.log(actor, AuditAction.PRODUCT_CREATE, AuditEntity.PRODUCT, id, item.title, "price: ${Money.input(item.sellPrice)}")
        } else {
            val oldCost = PriceCalculator.cost(pricing.input(existing).cost, ctx.policy).fullCost.toDouble()
            if (existing.sellPrice != item.sellPrice || existing.purchasePrice != item.purchasePrice) {
                pricing.recordChange(id, existing.sellPrice, item.sellPrice, oldCost, newCost, PriceSource.EDIT, actor)
            }
            audit.log(actor, AuditAction.PRODUCT_UPDATE, AuditEntity.PRODUCT, id, item.title, diff(existing, item))
        }
        SaveProductResult.Saved(id, result.finalPrice, result)
    }

    private fun diff(old: ItemEntity, new: ItemEntity): String? {
        val pairs = listOf(
            "title" to (old.title to new.title),
            "sellPrice" to (Money.input(old.sellPrice) to Money.input(new.sellPrice)),
            "purchasePrice" to (Money.input(old.purchasePrice) to Money.input(new.purchasePrice)),
            "stockQty" to (Money.input(old.stockQty) to Money.input(new.stockQty)),
            "categoryId" to (old.categoryId?.toString() to new.categoryId?.toString()),
            "pricingMode" to (old.pricingMode to new.pricingMode),
            "pricingValue" to (old.pricingValue?.toString() to new.pricingValue?.toString()),
            "barcode" to (old.barcode to new.barcode),
            "sku" to (old.sku to new.sku),
            "minPrice" to (old.minPrice?.toString() to new.minPrice?.toString()),
            "maxPrice" to (old.maxPrice?.toString() to new.maxPrice?.toString()),
            "status" to (old.status to new.status),
        )
        return pairs.filter { (_, v) -> v.first != v.second }
            .joinToString("; ") { (k, v) -> "$k: ${v.first.orEmpty()} → ${v.second.orEmpty()}" }
            .ifEmpty { null }
    }

    /** Copies a product as a new, unstocked item without barcode or SKU (both must stay unique). */
    suspend fun duplicate(id: Long, newTitle: String, actor: Actor): Long? = db.withTransaction {
        val source = db.itemDao().getById(id) ?: return@withTransaction null
        val now = System.currentTimeMillis()
        val copyId = db.itemDao().insert(
            source.copy(
                id = 0,
                title = newTitle,
                barcode = null,
                sku = null,
                stockQty = 0.0,
                status = ItemStatus.ACTIVE,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        db.attributeDao().upsertValues(db.attributeDao().valuesForItem(id).map { it.copy(itemId = copyId) })
        pricing.recordChange(copyId, null, source.sellPrice, null, null, PriceSource.CREATE, actor)
        audit.log(actor, AuditAction.PRODUCT_DUPLICATE, AuditEntity.PRODUCT, copyId, newTitle, "copy of #$id ${source.title}")
        copyId
    }

    suspend fun setArchived(ids: Collection<Long>, archived: Boolean, actor: Actor) = db.withTransaction {
        if (ids.isEmpty()) return@withTransaction
        val status = if (archived) ItemStatus.ARCHIVED else ItemStatus.ACTIVE
        ids.chunked(900).forEach { db.itemDao().setStatus(it, status, System.currentTimeMillis()) }
        audit.log(
            actor,
            if (archived) AuditAction.PRODUCT_ARCHIVE else AuditAction.PRODUCT_RESTORE,
            AuditEntity.PRODUCT,
            ids.singleOrNull(),
            ids.size.toString(),
            ids.joinToString(",", limit = 50),
        )
    }

    suspend fun setCategory(ids: Collection<Long>, categoryId: Long?, actor: Actor) = db.withTransaction {
        if (ids.isEmpty()) return@withTransaction
        ids.chunked(900).forEach { db.itemDao().setCategory(it, categoryId, System.currentTimeMillis()) }
        audit.log(actor, AuditAction.PRODUCT_BULK_CATEGORY, AuditEntity.PRODUCT, categoryId, ids.size.toString(), ids.joinToString(",", limit = 50))
    }

    /** Products that appear on recorded sales cannot be deleted (archive them instead). */
    suspend fun delete(ids: Collection<Long>, actor: Actor): DeleteProductsResult = db.withTransaction {
        val blocked = ArrayList<String>()
        var deleted = 0
        for (id in ids) {
            val item = db.itemDao().getById(id) ?: continue
            if (db.saleLineDao().countForItem(id) > 0) {
                blocked += item.title
                continue
            }
            db.itemDao().delete(id)
            deleted++
            audit.log(actor, AuditAction.PRODUCT_DELETE, AuditEntity.PRODUCT, id, item.title)
        }
        DeleteProductsResult(deleted, blocked)
    }
}
