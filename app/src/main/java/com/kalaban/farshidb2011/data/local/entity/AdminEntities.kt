package com.kalaban.farshidb2011.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Append-only record of significant operations; the DAO deliberately exposes no update or delete. */
@Entity(
    tableName = "audit_log",
    indices = [Index("atEpochMs"), Index("action"), Index("entityType")],
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atEpochMs: Long = System.currentTimeMillis(),
    /** 0 for the device owner, null for system jobs, otherwise a `users.id`. */
    val actorId: Long? = null,
    val actorName: String,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val summary: String,
    val details: String? = null,
    val success: Boolean = true,
)

/** Key-value runtime configuration, so new preferences do not require schema migrations. */
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val configKey: String,
    val value: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "roles",
    indices = [Index(value = ["roleKey"], unique = true)],
)
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleKey: String,
    val nameFa: String,
    val nameEn: String,
    /** Comma-separated permission keys. */
    val permissions: String,
    /** Built-in roles cannot be deleted; the administrator role cannot be edited. */
    val builtIn: Boolean = false,
)

/** Additional staff accounts; the device owner authenticates with the PIN kept in SecurePrefs. */
@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("roleId")],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roleId: Long,
    val pinHash: String,
    val pinSalt: String,
    val active: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val lastLoginAtEpochMs: Long? = null,
)
