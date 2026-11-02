package com.kalaban.farshidb2011.data.repository

import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.AuditLogEntity
import com.kalaban.farshidb2011.domain.Actor
import kotlinx.coroutines.flow.Flow

object AuditAction {
    const val LOGIN = "LOGIN"
    const val LOGIN_FAILED = "LOGIN_FAILED"
    const val LOGOUT = "LOGOUT"
    const val PIN_CHANGE = "PIN_CHANGE"
    const val PRODUCT_CREATE = "PRODUCT_CREATE"
    const val PRODUCT_UPDATE = "PRODUCT_UPDATE"
    const val PRODUCT_DUPLICATE = "PRODUCT_DUPLICATE"
    const val PRODUCT_ARCHIVE = "PRODUCT_ARCHIVE"
    const val PRODUCT_RESTORE = "PRODUCT_RESTORE"
    const val PRODUCT_DELETE = "PRODUCT_DELETE"
    const val PRODUCT_BULK_CATEGORY = "PRODUCT_BULK_CATEGORY"
    const val PRICE_BATCH = "PRICE_BATCH"
    const val PRICE_ROLLBACK = "PRICE_ROLLBACK"
    const val PRICE_SCHEDULE = "PRICE_SCHEDULE"
    const val PRICE_SCHEDULE_CANCEL = "PRICE_SCHEDULE_CANCEL"
    const val FORMULA_SAVE = "FORMULA_SAVE"
    const val FORMULA_DELETE = "FORMULA_DELETE"
    const val FORMULA_ACTIVATE = "FORMULA_ACTIVATE"
    const val FORMULA_REVERT = "FORMULA_REVERT"
    const val SETTINGS_UPDATE = "SETTINGS_UPDATE"
    const val CONFIG_UPDATE = "CONFIG_UPDATE"
    const val CATALOG_UPDATE = "CATALOG_UPDATE"
    const val SALE_CREATE = "SALE_CREATE"
    const val PAYMENT_ADD = "PAYMENT_ADD"
    const val BALANCE_ZERO = "BALANCE_ZERO"
    const val DATA_EXPORT = "DATA_EXPORT"
    const val DATA_RESTORE = "DATA_RESTORE"
    const val DATA_IMPORT = "DATA_IMPORT"
    const val USER_SAVE = "USER_SAVE"
    const val USER_DELETE = "USER_DELETE"
    const val ROLE_SAVE = "ROLE_SAVE"
    const val ROLE_DELETE = "ROLE_DELETE"
    const val HISTORY_CLEAR = "HISTORY_CLEAR"
}

object AuditEntity {
    const val PRODUCT = "PRODUCT"
    const val PRICE = "PRICE"
    const val FORMULA = "FORMULA"
    const val SETTINGS = "SETTINGS"
    const val CATALOG = "CATALOG"
    const val SALE = "SALE"
    const val DATA = "DATA"
    const val USER = "USER"
    const val SECURITY = "SECURITY"
}

data class AuditFilter(
    val action: String = "",
    val entityType: String = "",
    val query: String = "",
    val fromEpochMs: Long = 0L,
    val toEpochMs: Long = Long.MAX_VALUE,
    val limit: Int = 500,
)

class AuditRepository(private val db: KalabanDatabase) {
    /** Summaries hold data (names, numbers) rather than prose, so entries read correctly in either UI language. */
    suspend fun log(
        actor: Actor,
        action: String,
        entityType: String,
        entityId: Any? = null,
        summary: String,
        details: String? = null,
        success: Boolean = true,
    ) {
        db.auditDao().insert(
            AuditLogEntity(
                actorId = actor.id,
                actorName = actor.name,
                action = action,
                entityType = entityType,
                entityId = entityId?.toString(),
                summary = summary.take(500),
                details = details?.take(4000),
                success = success,
            ),
        )
    }

    fun observe(filter: AuditFilter): Flow<List<AuditLogEntity>> = db.auditDao().observe(
        action = filter.action,
        entityType = filter.entityType,
        query = filter.query.trim(),
        fromEpochMs = filter.fromEpochMs,
        toEpochMs = filter.toEpochMs,
        limit = filter.limit,
    )

    suspend fun all(): List<AuditLogEntity> = db.auditDao().getAll()
}
