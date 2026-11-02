package com.kalaban.farshidb2011.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kalaban.farshidb2011.data.local.entity.AppConfigEntity
import com.kalaban.farshidb2011.data.local.entity.AuditLogEntity
import com.kalaban.farshidb2011.data.local.entity.RoleEntity
import com.kalaban.farshidb2011.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/** Audit entries are append-only through the app: there is intentionally no update or delete. */
@Dao
interface AuditDao {
    @Insert
    suspend fun insert(entry: AuditLogEntity): Long

    @Insert
    suspend fun insertAll(entries: List<AuditLogEntity>)

    @Query(
        """
        SELECT * FROM audit_log
        WHERE (:action = '' OR action = :action)
          AND (:entityType = '' OR entityType = :entityType)
          AND atEpochMs BETWEEN :fromEpochMs AND :toEpochMs
          AND (:query = ''
            OR summary LIKE '%' || :query || '%'
            OR actorName LIKE '%' || :query || '%'
            OR IFNULL(details,'') LIKE '%' || :query || '%')
        ORDER BY atEpochMs DESC, id DESC
        LIMIT :limit
        """,
    )
    fun observe(
        action: String,
        entityType: String,
        query: String,
        fromEpochMs: Long,
        toEpochMs: Long,
        limit: Int,
    ): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log ORDER BY atEpochMs DESC, id DESC")
    suspend fun getAll(): List<AuditLogEntity>
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config")
    fun observeAll(): Flow<List<AppConfigEntity>>

    @Query("SELECT * FROM app_config")
    suspend fun getAll(): List<AppConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<AppConfigEntity>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM roles ORDER BY builtIn DESC, id")
    fun observeRoles(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM roles ORDER BY builtIn DESC, id")
    suspend fun getRoles(): List<RoleEntity>

    @Query("SELECT * FROM roles WHERE id = :id")
    suspend fun getRole(id: Long): RoleEntity?

    @Insert
    suspend fun insertRole(role: RoleEntity): Long

    @Update
    suspend fun updateRole(role: RoleEntity)

    @Query("DELETE FROM roles WHERE id = :id")
    suspend fun deleteRole(id: Long)

    @Query("SELECT COUNT(*) FROM users WHERE roleId = :roleId")
    suspend fun countUsersWithRole(roleId: Long): Int

    @Query("SELECT * FROM users ORDER BY active DESC, name")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE active = 1")
    suspend fun getActiveUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: Long): UserEntity?

    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)

    @Query("UPDATE users SET lastLoginAtEpochMs = :at WHERE id = :id")
    suspend fun touchLogin(id: Long, at: Long)
}
