package com.kalaban.farshidb2011.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a price change came from; persisted in `price_history.source` and `price_batches.source`. */
object PriceSource {
    const val CREATE = "CREATE"
    const val EDIT = "EDIT"
    const val BULK = "BULK"
    const val RECALCULATION = "RECALCULATION"
    const val DOLLAR_RATE = "DOLLAR_RATE"
    const val IMPORT = "IMPORT"
    const val ROLLBACK = "ROLLBACK"
    const val SCHEDULED = "SCHEDULED"
}

object BatchStatus {
    const val APPLIED = "APPLIED"
    const val ROLLED_BACK = "ROLLED_BACK"
    const val SCHEDULED = "SCHEDULED"
    const val CANCELLED = "CANCELLED"
    const val FAILED = "FAILED"
}

object AttributeType {
    const val TEXT = "TEXT"
    const val NUMBER = "NUMBER"
    const val BOOLEAN = "BOOLEAN"
    const val CHOICE = "CHOICE"
    val all = listOf(TEXT, NUMBER, BOOLEAN, CHOICE)
}

/** Administrator-defined pricing rule. A null [categoryId] applies the formula to every category. */
@Entity(
    tableName = "pricing_formulas",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class PricingFormulaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val expression: String,
    val conditionExpr: String? = null,
    val description: String? = null,
    val categoryId: Long? = null,
    val priority: Int = 0,
    val active: Boolean = false,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val roundingStep: Double? = null,
    val roundingMode: String? = null,
    val version: Int = 1,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

/** Snapshot of a formula before it was changed, so earlier versions can be inspected and restored. */
@Entity(
    tableName = "formula_versions",
    foreignKeys = [
        ForeignKey(
            entity = PricingFormulaEntity::class,
            parentColumns = ["id"],
            childColumns = ["formulaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("formulaId")],
)
data class FormulaVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formulaId: Long,
    val version: Int,
    val name: String,
    val expression: String,
    val conditionExpr: String?,
    val categoryId: Long?,
    val priority: Int,
    val minPrice: Double?,
    val maxPrice: Double?,
    val roundingStep: Double?,
    val roundingMode: String?,
    val savedAtEpochMs: Long = System.currentTimeMillis(),
    val savedBy: String? = null,
)

@Entity(tableName = "price_batches")
data class PriceBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val title: String,
    val note: String? = null,
    val actor: String? = null,
    val affectedCount: Int = 0,
    val status: String = BatchStatus.APPLIED,
    val rolledBackByBatchId: Long? = null,
    /** JSON request for scheduled batches; it is evaluated against the prices current at run time. */
    val payload: String? = null,
    val scheduledAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PriceBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("itemId"), Index("batchId"), Index("changedAtEpochMs")],
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val batchId: Long? = null,
    val oldSellPrice: Double?,
    val newSellPrice: Double,
    val oldCost: Double? = null,
    val newCost: Double? = null,
    val source: String,
    val note: String? = null,
    val actor: String? = null,
    val changedAtEpochMs: Long = System.currentTimeMillis(),
)

/** Quantity tier: lines of at least [minQty] get [discountPercent] off before tax. */
@Entity(
    tableName = "discount_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class DiscountRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val minQty: Double,
    val discountPercent: Double,
    val active: Boolean = true,
)

@Entity(
    tableName = "attribute_definitions",
    indices = [Index(value = ["attrKey"], unique = true)],
)
data class AttributeDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable lowercase identifier; numeric attributes are exposed to formulas as `attr_<attrKey>`. */
    val attrKey: String,
    val labelFa: String,
    val labelEn: String,
    val type: String = AttributeType.TEXT,
    /** Comma-separated options for [AttributeType.CHOICE]. */
    val options: String? = null,
    val required: Boolean = false,
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Entity(
    tableName = "item_attribute_values",
    primaryKeys = ["itemId", "attributeId"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttributeDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["attributeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("attributeId")],
)
data class ItemAttributeValueEntity(
    val itemId: Long,
    val attributeId: Long,
    val value: String,
)
