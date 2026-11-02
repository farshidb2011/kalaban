package com.kalaban.farshidb2011.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kalaban.farshidb2011.data.local.entity.AttributeDefinitionEntity
import com.kalaban.farshidb2011.data.local.entity.DiscountRuleEntity
import com.kalaban.farshidb2011.data.local.entity.FormulaVersionEntity
import com.kalaban.farshidb2011.data.local.entity.ItemAttributeValueEntity
import com.kalaban.farshidb2011.data.local.entity.PriceBatchEntity
import com.kalaban.farshidb2011.data.local.entity.PriceHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.PricingFormulaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormulaDao {
    @Query("SELECT * FROM pricing_formulas ORDER BY active DESC, priority DESC, name")
    fun observeAll(): Flow<List<PricingFormulaEntity>>

    @Query("SELECT * FROM pricing_formulas")
    suspend fun getAll(): List<PricingFormulaEntity>

    @Query("SELECT * FROM pricing_formulas WHERE id = :id")
    suspend fun getById(id: Long): PricingFormulaEntity?

    @Insert
    suspend fun insert(formula: PricingFormulaEntity): Long

    @Update
    suspend fun update(formula: PricingFormulaEntity)

    @Query("DELETE FROM pricing_formulas WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertVersion(version: FormulaVersionEntity): Long

    @Query("SELECT * FROM formula_versions WHERE formulaId = :formulaId ORDER BY version DESC")
    fun observeVersions(formulaId: Long): Flow<List<FormulaVersionEntity>>

    @Query("SELECT * FROM formula_versions WHERE id = :id")
    suspend fun getVersion(id: Long): FormulaVersionEntity?

    @Query("SELECT * FROM formula_versions")
    suspend fun getAllVersions(): List<FormulaVersionEntity>
}

@Dao
interface PriceHistoryDao {
    @Insert
    suspend fun insert(row: PriceHistoryEntity): Long

    @Insert
    suspend fun insertAll(rows: List<PriceHistoryEntity>)

    @Query("SELECT * FROM price_history WHERE itemId = :itemId ORDER BY changedAtEpochMs DESC, id DESC")
    fun observeForItem(itemId: Long): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE batchId = :batchId ORDER BY id")
    suspend fun getForBatch(batchId: Long): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history WHERE changedAtEpochMs BETWEEN :fromEpochMs AND :toEpochMs ORDER BY changedAtEpochMs DESC, id DESC")
    suspend fun getInRange(fromEpochMs: Long, toEpochMs: Long): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history")
    suspend fun getAll(): List<PriceHistoryEntity>
}

@Dao
interface PriceBatchDao {
    @Insert
    suspend fun insert(batch: PriceBatchEntity): Long

    @Update
    suspend fun update(batch: PriceBatchEntity)

    @Query("SELECT * FROM price_batches ORDER BY createdAtEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<PriceBatchEntity>>

    @Query("SELECT * FROM price_batches WHERE id = :id")
    suspend fun getById(id: Long): PriceBatchEntity?

    @Query("SELECT * FROM price_batches WHERE status = 'SCHEDULED' ORDER BY scheduledAtEpochMs")
    suspend fun getScheduled(): List<PriceBatchEntity>

    @Query("SELECT * FROM price_batches")
    suspend fun getAll(): List<PriceBatchEntity>
}

@Dao
interface DiscountRuleDao {
    @Query("SELECT * FROM discount_rules ORDER BY IFNULL(categoryId, 0), minQty")
    fun observeAll(): Flow<List<DiscountRuleEntity>>

    @Query("SELECT * FROM discount_rules")
    suspend fun getAll(): List<DiscountRuleEntity>

    @Insert
    suspend fun insert(rule: DiscountRuleEntity): Long

    @Update
    suspend fun update(rule: DiscountRuleEntity)

    @Query("DELETE FROM discount_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AttributeDao {
    @Query("SELECT * FROM attribute_definitions ORDER BY sortOrder, id")
    fun observeDefinitions(): Flow<List<AttributeDefinitionEntity>>

    @Query("SELECT * FROM attribute_definitions ORDER BY sortOrder, id")
    suspend fun getDefinitions(): List<AttributeDefinitionEntity>

    @Insert
    suspend fun insertDefinition(definition: AttributeDefinitionEntity): Long

    /** Never REPLACE definitions: that deletes the row and cascades to every stored value. */
    @Update
    suspend fun updateDefinition(definition: AttributeDefinitionEntity)

    @Query("DELETE FROM attribute_definitions WHERE id = :id")
    suspend fun deleteDefinition(id: Long)

    @Query("SELECT * FROM item_attribute_values WHERE itemId = :itemId")
    suspend fun valuesForItem(itemId: Long): List<ItemAttributeValueEntity>

    @Query("SELECT * FROM item_attribute_values WHERE itemId = :itemId")
    fun observeValuesForItem(itemId: Long): Flow<List<ItemAttributeValueEntity>>

    @Query("SELECT * FROM item_attribute_values")
    suspend fun allValues(): List<ItemAttributeValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertValues(values: List<ItemAttributeValueEntity>)

    @Query("DELETE FROM item_attribute_values WHERE itemId = :itemId")
    suspend fun clearForItem(itemId: Long)
}
