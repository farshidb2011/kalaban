package com.kalaban.farshidb2011.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kalaban.farshidb2011.core.pricing.PricingModes

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameFa: String,
    val nameEn: String,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameFa: String,
    val nameEn: String,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("unitId"),
        Index("categoryId"),
        Index("title"),
        Index("sku"),
        Index(value = ["barcode"], unique = true),
    ],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imageUri: String? = null,
    val unitId: Long,
    val categoryId: Long? = null,
    /** Raw numeric purchase price — not tied to a currency code in storage. */
    val purchasePrice: Double,
    /** Dollar rate at the moment of purchase (basis for recalculation). */
    val purchaseDollarRate: Double,
    /** Current sell price per sellable piece (raw numeric), recalculated when dollar rate changes. */
    val sellPrice: Double,
    /**
     * How many sellable pieces are in one purchased unit of [unitId]
     * (e.g. pack of 5 → 5). Stock is stored in pieces: packs × piecesPerUnit.
     */
    val piecesPerUnit: Double = 1.0,
    /** Stock quantity in sellable pieces. */
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 0.0,
    val sku: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    /** Unique when present; blank values are stored as null. */
    val barcode: String? = null,
    val brand: String? = null,
    val supplier: String? = null,
    val description: String? = null,
    val notes: String? = null,
    /** Comma-separated free-form tags. */
    val tags: String? = null,
    @ColumnInfo(defaultValue = ItemStatus.ACTIVE) val status: String = ItemStatus.ACTIVE,
    /** Per-piece acquisition costs added on top of the dollar-adjusted purchase price. */
    @ColumnInfo(defaultValue = "0") val transportCost: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val packagingCost: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val otherCost: Double = 0.0,
    /** When false the purchase price is not scaled by the dollar-rate ratio (locally sourced goods). */
    @ColumnInfo(defaultValue = "1") val dollarLinked: Boolean = true,
    @ColumnInfo(defaultValue = PricingModes.AUTO) val pricingMode: String = PricingModes.AUTO,
    /** Markup %, margin % or fixed profit amount, depending on [pricingMode]. */
    val pricingValue: Double? = null,
    val formulaId: Long? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val maxStock: Double? = null,
    @ColumnInfo(defaultValue = "0") val createdAtEpochMs: Long = System.currentTimeMillis(),
)

object ItemStatus {
    const val ACTIVE = "ACTIVE"
    const val ARCHIVED = "ARCHIVED"
}

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dollarRate: Double = 1.0,
    val taxPercent: Double = 0.0,
    val profitPercent: Double = 0.0,
    val dollarChangePercent: Double = 0.0,
    val managerPercent: Double = 0.0,
    val installmentDiffPercent: Double = 0.0,
    /** Display-only currency label (e.g. IRR, USD). Prices stay raw numbers. */
    val currencyCode: String = "IRR",
    val languageTag: String = "fa",
    val displayDarkMode: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
)

@Entity(tableName = "dollar_rate_history")
data class DollarRateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rate: Double,
    val recordedAtEpochMs: Long = System.currentTimeMillis(),
    val note: String? = null,
)

enum class PaymentType { CASH, CREDIT }

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buyerName: String,
    val paymentType: PaymentType,
    /** Jalali date as yyyy/MM/dd for display/query convenience. */
    val jalaliDate: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val dueAtEpochMs: Long? = null,
    val totalAmount: Double,
    val openBalance: Double,
    val manuallyZeroed: Boolean = false,
    val zeroReason: String? = null,
    val createdBy: String? = null,
)

@Entity(
    tableName = "sale_lines",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("saleId"), Index("itemId")],
)
data class SaleLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val itemId: Long,
    val itemTitle: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
    /** Full unit cost when sold; null for lines recorded before cost snapshots existed. */
    val unitCost: Double? = null,
    @ColumnInfo(defaultValue = "0") val taxAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val discountAmount: Double = 0.0,
)

@Entity(
    tableName = "sale_payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("saleId")],
)
data class SalePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val amount: Double,
    val paidAtEpochMs: Long = System.currentTimeMillis(),
    val note: String? = null,
)
