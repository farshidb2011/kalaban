package com.kalaban.farshidb2011.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Who performed an action; [OWNER_NAME] and [SYSTEM_NAME] are sentinels localized at display time. */
data class Actor(val id: Long?, val name: String) {
    companion object {
        const val OWNER_ID = 0L
        const val OWNER_NAME = "owner"
        const val SYSTEM_NAME = "system"
        val SYSTEM = Actor(null, SYSTEM_NAME)
        val OWNER = Actor(OWNER_ID, OWNER_NAME)
    }
}

data class SessionUser(
    val id: Long,
    val name: String,
    val roleKey: String,
    val permissions: Set<Permission>,
) {
    val isOwner: Boolean get() = id == Actor.OWNER_ID
    val actor: Actor get() = Actor(id, name)

    companion object {
        val OWNER = SessionUser(Actor.OWNER_ID, Actor.OWNER_NAME, BuiltInRoles.ADMIN, Permission.entries.toSet())
    }
}

/**
 * In-memory login state: unlocking must not survive process death. Also throttles PIN guessing
 * (escalating lockout after repeated failures).
 */
class SessionManager(private val clock: () -> Long = System::currentTimeMillis) {
    private val _user = MutableStateFlow<SessionUser?>(null)
    val user: StateFlow<SessionUser?> = _user.asStateFlow()

    private var failures = 0
    private var lockedUntil = 0L

    val actor: Actor get() = _user.value?.actor ?: Actor.SYSTEM

    fun has(permission: Permission): Boolean = _user.value?.permissions?.contains(permission) == true

    fun signIn(user: SessionUser) {
        failures = 0
        lockedUntil = 0L
        _user.value = user
    }

    fun signOut() {
        _user.value = null
    }

    /** Remaining lockout in milliseconds, or 0 when a new attempt is allowed. */
    fun lockoutRemaining(): Long = (lockedUntil - clock()).coerceAtLeast(0L)

    fun recordFailure() {
        failures++
        if (failures >= MAX_ATTEMPTS) {
            val step = failures - MAX_ATTEMPTS
            lockedUntil = clock() + (BASE_LOCKOUT_MS shl step.coerceAtMost(5))
        }
    }

    /** Replaces the cached permissions after the signed-in user's role was edited. */
    fun refresh(user: SessionUser) {
        if (_user.value?.id == user.id) _user.value = user
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val BASE_LOCKOUT_MS = 30_000L
    }
}
