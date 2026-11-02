package com.kalaban.farshidb2011.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kalaban.farshidb2011.data.local.dao.AttributeDao
import com.kalaban.farshidb2011.data.local.dao.AuditDao
import com.kalaban.farshidb2011.data.local.dao.CategoryDao
import com.kalaban.farshidb2011.data.local.dao.ConfigDao
import com.kalaban.farshidb2011.data.local.dao.DiscountRuleDao
import com.kalaban.farshidb2011.data.local.dao.DollarRateHistoryDao
import com.kalaban.farshidb2011.data.local.dao.FormulaDao
import com.kalaban.farshidb2011.data.local.dao.ItemDao
import com.kalaban.farshidb2011.data.local.dao.PriceBatchDao
import com.kalaban.farshidb2011.data.local.dao.PriceHistoryDao
import com.kalaban.farshidb2011.data.local.dao.SaleDao
import com.kalaban.farshidb2011.data.local.dao.SaleLineDao
import com.kalaban.farshidb2011.data.local.dao.SalePaymentDao
import com.kalaban.farshidb2011.data.local.dao.SettingsDao
import com.kalaban.farshidb2011.data.local.dao.UnitDao
import com.kalaban.farshidb2011.data.local.dao.UserDao
import com.kalaban.farshidb2011.data.local.entity.AppConfigEntity
import com.kalaban.farshidb2011.data.local.entity.AttributeDefinitionEntity
import com.kalaban.farshidb2011.data.local.entity.AuditLogEntity
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.DiscountRuleEntity
import com.kalaban.farshidb2011.data.local.entity.DollarRateHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.FormulaVersionEntity
import com.kalaban.farshidb2011.data.local.entity.ItemAttributeValueEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
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
import com.kalaban.farshidb2011.data.repository.ConfigKeys
import com.kalaban.farshidb2011.domain.BuiltInRoles
import com.kalaban.farshidb2011.domain.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UnitEntity::class,
        CategoryEntity::class,
        ItemEntity::class,
        SettingsEntity::class,
        DollarRateHistoryEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        SalePaymentEntity::class,
        PricingFormulaEntity::class,
        FormulaVersionEntity::class,
        PriceBatchEntity::class,
        PriceHistoryEntity::class,
        DiscountRuleEntity::class,
        AttributeDefinitionEntity::class,
        ItemAttributeValueEntity::class,
        AuditLogEntity::class,
        AppConfigEntity::class,
        RoleEntity::class,
        UserEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KalabanDatabase : RoomDatabase() {
    abstract fun unitDao(): UnitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun settingsDao(): SettingsDao
    abstract fun dollarRateHistoryDao(): DollarRateHistoryDao
    abstract fun saleDao(): SaleDao
    abstract fun saleLineDao(): SaleLineDao
    abstract fun salePaymentDao(): SalePaymentDao
    abstract fun formulaDao(): FormulaDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun priceBatchDao(): PriceBatchDao
    abstract fun discountRuleDao(): DiscountRuleDao
    abstract fun attributeDao(): AttributeDao
    abstract fun auditDao(): AuditDao
    abstract fun configDao(): ConfigDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var instance: KalabanDatabase? = null

        fun get(context: Context): KalabanDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KalabanDatabase::class.java,
                    "kalaban.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(SeedCallback())
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE items ADD COLUMN piecesPerUnit REAL NOT NULL DEFAULT 1.0",
                )
            }
        }

        /** Additive only: existing rows keep their values; new columns are nullable or defaulted. */
        val MIGRATION_2_3_STATEMENTS = listOf(
            "ALTER TABLE `items` ADD COLUMN `barcode` TEXT",
            "ALTER TABLE `items` ADD COLUMN `brand` TEXT",
            "ALTER TABLE `items` ADD COLUMN `supplier` TEXT",
            "ALTER TABLE `items` ADD COLUMN `description` TEXT",
            "ALTER TABLE `items` ADD COLUMN `notes` TEXT",
            "ALTER TABLE `items` ADD COLUMN `tags` TEXT",
            "ALTER TABLE `items` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'",
            "ALTER TABLE `items` ADD COLUMN `transportCost` REAL NOT NULL DEFAULT 0",
            "ALTER TABLE `items` ADD COLUMN `packagingCost` REAL NOT NULL DEFAULT 0",
            "ALTER TABLE `items` ADD COLUMN `otherCost` REAL NOT NULL DEFAULT 0",
            "ALTER TABLE `items` ADD COLUMN `dollarLinked` INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE `items` ADD COLUMN `pricingMode` TEXT NOT NULL DEFAULT 'AUTO'",
            "ALTER TABLE `items` ADD COLUMN `pricingValue` REAL",
            "ALTER TABLE `items` ADD COLUMN `formulaId` INTEGER",
            "ALTER TABLE `items` ADD COLUMN `minPrice` REAL",
            "ALTER TABLE `items` ADD COLUMN `maxPrice` REAL",
            "ALTER TABLE `items` ADD COLUMN `maxStock` REAL",
            "ALTER TABLE `items` ADD COLUMN `createdAtEpochMs` INTEGER NOT NULL DEFAULT 0",
            "UPDATE `items` SET `createdAtEpochMs` = `updatedAtEpochMs`",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_items_barcode` ON `items` (`barcode`)",
            "ALTER TABLE `sales` ADD COLUMN `createdBy` TEXT",
            "ALTER TABLE `sale_lines` ADD COLUMN `unitCost` REAL",
            "ALTER TABLE `sale_lines` ADD COLUMN `taxAmount` REAL NOT NULL DEFAULT 0",
            "ALTER TABLE `sale_lines` ADD COLUMN `discountAmount` REAL NOT NULL DEFAULT 0",
            "CREATE TABLE IF NOT EXISTS `pricing_formulas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `expression` TEXT NOT NULL, `conditionExpr` TEXT, `description` TEXT, `categoryId` INTEGER, `priority` INTEGER NOT NULL, `active` INTEGER NOT NULL, `minPrice` REAL, `maxPrice` REAL, `roundingStep` REAL, `roundingMode` TEXT, `version` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_pricing_formulas_categoryId` ON `pricing_formulas` (`categoryId`)",
            "CREATE TABLE IF NOT EXISTS `formula_versions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `formulaId` INTEGER NOT NULL, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `expression` TEXT NOT NULL, `conditionExpr` TEXT, `categoryId` INTEGER, `priority` INTEGER NOT NULL, `minPrice` REAL, `maxPrice` REAL, `roundingStep` REAL, `roundingMode` TEXT, `savedAtEpochMs` INTEGER NOT NULL, `savedBy` TEXT, FOREIGN KEY(`formulaId`) REFERENCES `pricing_formulas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_formula_versions_formulaId` ON `formula_versions` (`formulaId`)",
            "CREATE TABLE IF NOT EXISTS `price_batches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `source` TEXT NOT NULL, `title` TEXT NOT NULL, `note` TEXT, `actor` TEXT, `affectedCount` INTEGER NOT NULL, `status` TEXT NOT NULL, `rolledBackByBatchId` INTEGER, `payload` TEXT, `scheduledAtEpochMs` INTEGER, `createdAtEpochMs` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `price_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemId` INTEGER NOT NULL, `batchId` INTEGER, `oldSellPrice` REAL, `newSellPrice` REAL NOT NULL, `oldCost` REAL, `newCost` REAL, `source` TEXT NOT NULL, `note` TEXT, `actor` TEXT, `changedAtEpochMs` INTEGER NOT NULL, FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`batchId`) REFERENCES `price_batches`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_price_history_itemId` ON `price_history` (`itemId`)",
            "CREATE INDEX IF NOT EXISTS `index_price_history_batchId` ON `price_history` (`batchId`)",
            "CREATE INDEX IF NOT EXISTS `index_price_history_changedAtEpochMs` ON `price_history` (`changedAtEpochMs`)",
            "CREATE TABLE IF NOT EXISTS `discount_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `categoryId` INTEGER, `minQty` REAL NOT NULL, `discountPercent` REAL NOT NULL, `active` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_discount_rules_categoryId` ON `discount_rules` (`categoryId`)",
            "CREATE TABLE IF NOT EXISTS `attribute_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `attrKey` TEXT NOT NULL, `labelFa` TEXT NOT NULL, `labelEn` TEXT NOT NULL, `type` TEXT NOT NULL, `options` TEXT, `required` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `active` INTEGER NOT NULL)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_attribute_definitions_attrKey` ON `attribute_definitions` (`attrKey`)",
            "CREATE TABLE IF NOT EXISTS `item_attribute_values` (`itemId` INTEGER NOT NULL, `attributeId` INTEGER NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`itemId`, `attributeId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`attributeId`) REFERENCES `attribute_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_item_attribute_values_attributeId` ON `item_attribute_values` (`attributeId`)",
            "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `atEpochMs` INTEGER NOT NULL, `actorId` INTEGER, `actorName` TEXT NOT NULL, `action` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT, `summary` TEXT NOT NULL, `details` TEXT, `success` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_audit_log_atEpochMs` ON `audit_log` (`atEpochMs`)",
            "CREATE INDEX IF NOT EXISTS `index_audit_log_action` ON `audit_log` (`action`)",
            "CREATE INDEX IF NOT EXISTS `index_audit_log_entityType` ON `audit_log` (`entityType`)",
            "CREATE TABLE IF NOT EXISTS `app_config` (`configKey` TEXT NOT NULL, `value` TEXT NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`configKey`))",
            "CREATE TABLE IF NOT EXISTS `roles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `roleKey` TEXT NOT NULL, `nameFa` TEXT NOT NULL, `nameEn` TEXT NOT NULL, `permissions` TEXT NOT NULL, `builtIn` INTEGER NOT NULL)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_roles_roleKey` ON `roles` (`roleKey`)",
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `roleId` INTEGER NOT NULL, `pinHash` TEXT NOT NULL, `pinSalt` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `lastLoginAtEpochMs` INTEGER, FOREIGN KEY(`roleId`) REFERENCES `roles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
            "CREATE INDEX IF NOT EXISTS `index_users_roleId` ON `users` (`roleId`)",
        )

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3_STATEMENTS.forEach(db::execSQL)
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed via DAO after instance is ready — scheduled from AppContainer.
        }
    }
}

suspend fun KalabanDatabase.seedIfEmpty() {
    val settingsDao = settingsDao()
    if (settingsDao.get() == null) {
        settingsDao.upsert(SettingsEntity())
        dollarRateHistoryDao().insert(DollarRateHistoryEntity(rate = 1.0, note = "initial"))
    }
    if (unitDao().getAll().isEmpty()) {
        unitDao().insert(UnitEntity(nameFa = "عدد", nameEn = "Piece"))
        unitDao().insert(UnitEntity(nameFa = "کیلوگرم", nameEn = "Kilogram"))
        unitDao().insert(UnitEntity(nameFa = "متر", nameEn = "Meter"))
        unitDao().insert(UnitEntity(nameFa = "بسته", nameEn = "Pack"))
    }
    if (categoryDao().getAll().isEmpty()) {
        categoryDao().insert(CategoryEntity(nameFa = "عمومی", nameEn = "General"))
    }
    if (userDao().getRoles().isEmpty()) {
        BuiltInRoles.defaults.forEach { (key, names, permissions) ->
            userDao().insertRole(
                RoleEntity(roleKey = key, nameFa = names.first, nameEn = names.second, permissions = Permission.encode(permissions), builtIn = true),
            )
        }
    }
    seedExampleFormulas()
}

/** Inactive starter formulas, created once; deleting them does not bring them back. */
private suspend fun KalabanDatabase.seedExampleFormulas() {
    val seededKey = ConfigKeys.FORMULAS_SEEDED
    if (configDao().getAll().any { it.configKey == seededKey && it.value == "true" }) return
    if (formulaDao().getAll().isEmpty()) {
        val fa = (settingsDao().get()?.languageTag ?: "fa").startsWith("fa")
        val examples = listOf(
            Triple(if (fa) "سود ۳۰٪ روی بهای تمام‌شده" else "30% markup on full cost", "addpct(full_cost, 30)", 10),
            Triple(if (fa) "حاشیه سود ۲۵٪، گرد به بالا تا ۱۰۰۰" else "25% margin, rounded up to 1,000", "ceilto(margin(full_cost, 25), 1000)", 20),
            Triple(
                if (fa) "پلکانی: کالای ارزان سود بیشتر" else "Tiered: cheaper items earn more",
                "if(full_cost < 1000000, addpct(full_cost, 35), addpct(full_cost, 20))",
                30,
            ),
        )
        examples.forEach { (name, expression, priority) ->
            formulaDao().insert(PricingFormulaEntity(name = name, expression = expression, priority = priority, active = false))
        }
    }
    configDao().upsert(AppConfigEntity(seededKey, "true"))
}

fun KalabanDatabase.seedInBackground() {
    CoroutineScope(Dispatchers.IO).launch { seedIfEmpty() }
}
