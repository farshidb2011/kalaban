package com.kalaban.farshidb2011.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.DollarRateHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.SaleEntity
import com.kalaban.farshidb2011.data.local.entity.SaleLineEntity
import com.kalaban.farshidb2011.data.local.entity.SalePaymentEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY nameFa")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units ORDER BY nameFa")
    suspend fun getAll(): List<UnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: UnitEntity): Long

    @Update
    suspend fun update(unit: UnitEntity)

    @Query("DELETE FROM units WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY nameFa")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY nameFa")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ItemDao {
    /** Active products matching title, SKU, barcode, brand or tags (display panel and sales). */
    @Query(
        """
        SELECT * FROM items
        WHERE status = 'ACTIVE' AND (:query = ''
            OR title LIKE '%' || :query || '%'
            OR IFNULL(sku,'') LIKE '%' || :query || '%'
            OR IFNULL(barcode,'') LIKE '%' || :query || '%'
            OR IFNULL(brand,'') LIKE '%' || :query || '%'
            OR IFNULL(tags,'') LIKE '%' || :query || '%')
        ORDER BY title
        """,
    )
    fun observeSearch(query: String): Flow<List<ItemEntity>>

    /** Catalog view for product management; an empty [status] matches every status. */
    @Query(
        """
        SELECT * FROM items
        WHERE (:status = '' OR status = :status) AND (:query = ''
            OR title LIKE '%' || :query || '%'
            OR IFNULL(sku,'') LIKE '%' || :query || '%'
            OR IFNULL(barcode,'') LIKE '%' || :query || '%'
            OR IFNULL(brand,'') LIKE '%' || :query || '%'
            OR IFNULL(supplier,'') LIKE '%' || :query || '%'
            OR IFNULL(tags,'') LIKE '%' || :query || '%')
        ORDER BY title
        """,
    )
    fun observeCatalog(query: String, status: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ItemEntity>

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ItemEntity?

    @Query("SELECT * FROM items WHERE sku = :sku")
    suspend fun findBySku(sku: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE status = 'ACTIVE' AND stockQty <= lowStockThreshold AND lowStockThreshold > 0")
    fun observeLowStock(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT DISTINCT brand FROM items WHERE IFNULL(brand,'') != '' ORDER BY brand")
    fun observeBrands(): Flow<List<String>>

    @Query("SELECT DISTINCT supplier FROM items WHERE IFNULL(supplier,'') != '' ORDER BY supplier")
    fun observeSuppliers(): Flow<List<String>>

    /** Plain INSERT: REPLACE would delete referenced rows, which sale_lines' ON DELETE RESTRICT rejects. */
    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET sellPrice = :sellPrice, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateSellPrice(id: Long, sellPrice: Double, updatedAt: Long)

    @Query(
        "UPDATE items SET sellPrice = :sellPrice, purchaseDollarRate = :purchaseDollarRate, " +
            "updatedAtEpochMs = :updatedAt WHERE id = :id",
    )
    suspend fun updateSellPriceAndPurchaseRate(
        id: Long,
        sellPrice: Double,
        purchaseDollarRate: Double,
        updatedAt: Long,
    )

    @Query("UPDATE items SET status = :status, updatedAtEpochMs = :updatedAt WHERE id IN (:ids)")
    suspend fun setStatus(ids: List<Long>, status: String, updatedAt: Long)

    @Query("UPDATE items SET categoryId = :categoryId, updatedAtEpochMs = :updatedAt WHERE id IN (:ids)")
    suspend fun setCategory(ids: List<Long>, categoryId: Long?, updatedAt: Long)

    @Query("UPDATE items SET formulaId = NULL, pricingMode = 'AUTO' WHERE formulaId = :formulaId")
    suspend fun detachFormula(formulaId: Long)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}

@Dao
interface DollarRateHistoryDao {
    @Query("SELECT * FROM dollar_rate_history ORDER BY recordedAtEpochMs DESC")
    fun observeAll(): Flow<List<DollarRateHistoryEntity>>

    @Insert
    suspend fun insert(row: DollarRateHistoryEntity): Long

    @Query("SELECT * FROM dollar_rate_history ORDER BY recordedAtEpochMs DESC")
    suspend fun getAll(): List<DollarRateHistoryEntity>

    @Query("DELETE FROM dollar_rate_history")
    suspend fun deleteAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Query(
        """
        SELECT * FROM sales
        WHERE createdAtEpochMs BETWEEN :fromEpochMs AND :toEpochMs
        ORDER BY createdAtEpochMs DESC
        """,
    )
    suspend fun getInRange(fromEpochMs: Long, toEpochMs: Long): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE paymentType = 'CREDIT' AND openBalance > 0 AND manuallyZeroed = 0")
    fun observeOpenCredits(): Flow<List<SaleEntity>>

    @Query(
        """
        SELECT * FROM sales
        WHERE paymentType = 'CREDIT'
          AND openBalance > 0
          AND manuallyZeroed = 0
          AND dueAtEpochMs IS NOT NULL
          AND dueAtEpochMs BETWEEN :fromEpochMs AND :toEpochMs
        """,
    )
    suspend fun getDueBetween(fromEpochMs: Long, toEpochMs: Long): List<SaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sale: SaleEntity): Long

    @Update
    suspend fun update(sale: SaleEntity)

    @Query("SELECT * FROM sales")
    suspend fun getAll(): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE createdAtEpochMs BETWEEN :fromEpochMs AND :toEpochMs ORDER BY createdAtEpochMs")
    fun observeInRange(fromEpochMs: Long, toEpochMs: Long): Flow<List<SaleEntity>>
}

@Dao
interface SaleLineDao {
    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId")
    suspend fun getForSale(saleId: Long): List<SaleLineEntity>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId")
    fun observeForSale(saleId: Long): Flow<List<SaleLineEntity>>

    @Insert
    suspend fun insertAll(lines: List<SaleLineEntity>)

    @Query("SELECT * FROM sale_lines")
    suspend fun getAll(): List<SaleLineEntity>

    @Query(
        """
        SELECT sale_lines.* FROM sale_lines
        INNER JOIN sales ON sales.id = sale_lines.saleId
        WHERE sales.createdAtEpochMs BETWEEN :fromEpochMs AND :toEpochMs
        """,
    )
    suspend fun getInRange(fromEpochMs: Long, toEpochMs: Long): List<SaleLineEntity>

    @Query("SELECT COUNT(*) FROM sale_lines WHERE itemId = :itemId")
    suspend fun countForItem(itemId: Long): Int
}

@Dao
interface SalePaymentDao {
    @Query("SELECT * FROM sale_payments WHERE saleId = :saleId ORDER BY paidAtEpochMs")
    fun observeForSale(saleId: Long): Flow<List<SalePaymentEntity>>

    @Insert
    suspend fun insert(payment: SalePaymentEntity): Long

    @Query("SELECT * FROM sale_payments")
    suspend fun getAll(): List<SalePaymentEntity>
}
