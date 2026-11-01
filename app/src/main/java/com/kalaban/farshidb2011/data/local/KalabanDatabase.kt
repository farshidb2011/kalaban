package com.kalaban.farshidb2011.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kalaban.farshidb2011.data.local.dao.CategoryDao
import com.kalaban.farshidb2011.data.local.dao.DollarRateHistoryDao
import com.kalaban.farshidb2011.data.local.dao.ItemDao
import com.kalaban.farshidb2011.data.local.dao.SaleDao
import com.kalaban.farshidb2011.data.local.dao.SaleLineDao
import com.kalaban.farshidb2011.data.local.dao.SalePaymentDao
import com.kalaban.farshidb2011.data.local.dao.SettingsDao
import com.kalaban.farshidb2011.data.local.dao.UnitDao
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.DollarRateHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.SaleEntity
import com.kalaban.farshidb2011.data.local.entity.SaleLineEntity
import com.kalaban.farshidb2011.data.local.entity.SalePaymentEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UnitEntity
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
    ],
    version = 2,
    exportSchema = false,
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

    companion object {
        @Volatile private var instance: KalabanDatabase? = null

        fun get(context: Context): KalabanDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KalabanDatabase::class.java,
                    "kalaban.db",
                )
                    .addMigrations(MIGRATION_1_2)
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
}

fun KalabanDatabase.seedInBackground() {
    CoroutineScope(Dispatchers.IO).launch { seedIfEmpty() }
}
