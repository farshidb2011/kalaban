package com.kalaban.farshidb2011.data.repository

import androidx.room.withTransaction
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.RoleEntity
import com.kalaban.farshidb2011.data.local.entity.UserEntity
import com.kalaban.farshidb2011.data.prefs.SecurePrefs
import com.kalaban.farshidb2011.domain.Actor
import com.kalaban.farshidb2011.domain.BuiltInRoles
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.domain.SessionManager
import com.kalaban.farshidb2011.domain.SessionUser

sealed class AuthResult {
    data class Success(val user: SessionUser) : AuthResult()
    data object Invalid : AuthResult()
    data class LockedOut(val remainingMs: Long) : AuthResult()
}

enum class UserSaveError { NAME_REQUIRED, NAME_TAKEN, PIN_TOO_SHORT, PIN_IN_USE, ROLE_MISSING }

class UserRepository(
    private val db: KalabanDatabase,
    private val prefs: SecurePrefs,
    private val session: SessionManager,
    private val audit: AuditRepository,
) {
    fun observeRoles() = db.userDao().observeRoles()
    fun observeUsers() = db.userDao().observeUsers()

    private fun normalize(pin: String) = Numbers.normalizeDigits(pin.trim())

    private fun permissionsOf(role: RoleEntity?): Set<Permission> = when {
        role == null -> emptySet()
        role.roleKey == BuiltInRoles.ADMIN -> Permission.entries.toSet()
        else -> Permission.parse(role.permissions)
    }

    private suspend fun ownerMatches(pin: String): Boolean = prefs.checkPin(pin) || prefs.checkPin(normalize(pin))

    suspend fun authenticate(pin: String): AuthResult {
        val remaining = session.lockoutRemaining()
        if (remaining > 0) return AuthResult.LockedOut(remaining)
        if (ownerMatches(pin)) {
            session.signIn(SessionUser.OWNER)
            audit.log(Actor.OWNER, AuditAction.LOGIN, AuditEntity.SECURITY, Actor.OWNER_ID, Actor.OWNER_NAME)
            return AuthResult.Success(SessionUser.OWNER)
        }
        val normalized = normalize(pin)
        val roles = db.userDao().getRoles().associateBy { it.id }
        for (user in db.userDao().getActiveUsers()) {
            if (SecurePrefs.hash(normalized, user.pinSalt) == user.pinHash) {
                val role = roles[user.roleId]
                val signedIn = SessionUser(user.id, user.name, role?.roleKey.orEmpty(), permissionsOf(role))
                db.userDao().touchLogin(user.id, System.currentTimeMillis())
                session.signIn(signedIn)
                audit.log(signedIn.actor, AuditAction.LOGIN, AuditEntity.SECURITY, user.id, user.name)
                return AuthResult.Success(signedIn)
            }
        }
        session.recordFailure()
        audit.log(Actor.SYSTEM, AuditAction.LOGIN_FAILED, AuditEntity.SECURITY, summary = "PIN", success = false)
        val locked = session.lockoutRemaining()
        return if (locked > 0) AuthResult.LockedOut(locked) else AuthResult.Invalid
    }

    suspend fun signInOwnerWithBiometric() {
        session.signIn(SessionUser.OWNER)
        audit.log(Actor.OWNER, AuditAction.LOGIN, AuditEntity.SECURITY, Actor.OWNER_ID, Actor.OWNER_NAME, "biometric")
    }

    suspend fun signOut() {
        val actor = session.actor
        session.signOut()
        if (actor != Actor.SYSTEM) audit.log(actor, AuditAction.LOGOUT, AuditEntity.SECURITY, actor.id, actor.name)
    }

    /** PINs must be unique across the owner and active users, otherwise sign-in would be ambiguous. */
    private suspend fun pinInUse(pin: String, exceptUserId: Long): Boolean {
        if (ownerMatches(pin)) return true
        return db.userDao().getActiveUsers().any { it.id != exceptUserId && SecurePrefs.hash(pin, it.pinSalt) == it.pinHash }
    }

    suspend fun saveUser(id: Long, name: String, roleId: Long?, pin: String?, active: Boolean, actor: Actor): Set<UserSaveError> =
        db.withTransaction {
            val errors = linkedSetOf<UserSaveError>()
            val cleanName = name.trim()
            if (cleanName.isEmpty()) errors += UserSaveError.NAME_REQUIRED
            if (db.userDao().getUsers().any { it.id != id && it.name.equals(cleanName, ignoreCase = true) }) {
                errors += UserSaveError.NAME_TAKEN
            }
            val role = roleId?.let { db.userDao().getRole(it) }
            if (role == null) errors += UserSaveError.ROLE_MISSING
            val newPin = pin?.let(::normalize)?.takeIf { it.isNotEmpty() }
            if (id == 0L && newPin == null) errors += UserSaveError.PIN_TOO_SHORT
            if (newPin != null) {
                if (newPin.length < 4 || !newPin.all { it in '0'..'9' }) errors += UserSaveError.PIN_TOO_SHORT
                else if (pinInUse(newPin, id)) errors += UserSaveError.PIN_IN_USE
            }
            if (errors.isNotEmpty()) return@withTransaction errors
            val existing = if (id != 0L) db.userDao().getUser(id) else null
            val salt = if (newPin != null) SecurePrefs.newSalt() else existing!!.pinSalt
            val hash = if (newPin != null) SecurePrefs.hash(newPin, salt) else existing!!.pinHash
            val user = UserEntity(
                id = id,
                name = cleanName,
                roleId = role!!.id,
                pinHash = hash,
                pinSalt = salt,
                active = active,
                createdAtEpochMs = existing?.createdAtEpochMs ?: System.currentTimeMillis(),
                lastLoginAtEpochMs = existing?.lastLoginAtEpochMs,
            )
            val savedId = if (existing == null) db.userDao().insertUser(user) else {
                db.userDao().updateUser(user)
                id
            }
            audit.log(
                actor, AuditAction.USER_SAVE, AuditEntity.USER, savedId, cleanName,
                "role: ${role.roleKey}; active: $active" + if (newPin != null) "; pin changed" else "",
            )
            if (!active && session.user.value?.id == savedId) session.signOut()
            emptySet()
        }

    suspend fun deleteUser(id: Long, actor: Actor) = db.withTransaction {
        val user = db.userDao().getUser(id) ?: return@withTransaction
        db.userDao().deleteUser(id)
        audit.log(actor, AuditAction.USER_DELETE, AuditEntity.USER, id, user.name)
    }

    /** Creates or edits a custom role; the administrator role keeps every permission. */
    suspend fun saveRole(id: Long, nameFa: String, nameEn: String, permissions: Set<Permission>, actor: Actor): Long? =
        db.withTransaction {
            val existing = if (id != 0L) db.userDao().getRole(id) else null
            if (existing?.roleKey == BuiltInRoles.ADMIN) return@withTransaction null
            val fa = nameFa.trim().ifEmpty { nameEn.trim() }
            val en = nameEn.trim().ifEmpty { fa }
            if (fa.isEmpty()) return@withTransaction null
            val role = RoleEntity(
                id = id,
                roleKey = existing?.roleKey ?: "CUSTOM_${System.currentTimeMillis()}",
                nameFa = fa,
                nameEn = en,
                permissions = Permission.encode(permissions),
                builtIn = existing?.builtIn ?: false,
            )
            val savedId = if (existing == null) db.userDao().insertRole(role) else {
                db.userDao().updateRole(role)
                id
            }
            audit.log(actor, AuditAction.ROLE_SAVE, AuditEntity.USER, savedId, en, role.permissions)
            refreshSession()
            savedId
        }

    suspend fun deleteRole(id: Long, actor: Actor): Boolean = db.withTransaction {
        val role = db.userDao().getRole(id) ?: return@withTransaction false
        if (role.builtIn || db.userDao().countUsersWithRole(id) > 0) return@withTransaction false
        db.userDao().deleteRole(id)
        audit.log(actor, AuditAction.ROLE_DELETE, AuditEntity.USER, id, role.nameEn)
        true
    }

    suspend fun changeOwnerPin(current: String, new: String, actor: Actor): Boolean {
        val pin = normalize(new)
        if (pin.length < 4 || !pin.all { it in '0'..'9' }) return false
        if (db.userDao().getActiveUsers().any { SecurePrefs.hash(pin, it.pinSalt) == it.pinHash }) return false
        val changed = prefs.changePin(current, pin) || prefs.changePin(normalize(current), pin)
        if (changed) audit.log(actor, AuditAction.PIN_CHANGE, AuditEntity.SECURITY, Actor.OWNER_ID, Actor.OWNER_NAME)
        return changed
    }

    /** Re-reads the signed-in staff user's role so permission edits apply immediately. */
    suspend fun refreshSession() {
        val current = session.user.value ?: return
        if (current.isOwner) return
        val user = db.userDao().getUser(current.id)
        if (user == null || !user.active) {
            session.signOut()
            return
        }
        val role = db.userDao().getRole(user.roleId)
        session.refresh(SessionUser(user.id, user.name, role?.roleKey.orEmpty(), permissionsOf(role)))
    }
}
