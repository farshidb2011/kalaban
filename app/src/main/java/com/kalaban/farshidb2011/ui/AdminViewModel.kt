package com.kalaban.farshidb2011.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.csv.Csv
import com.kalaban.farshidb2011.data.local.entity.AttributeDefinitionEntity
import com.kalaban.farshidb2011.data.local.entity.AttributeType
import com.kalaban.farshidb2011.data.repository.AppConfig
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.data.repository.AuditEntity
import com.kalaban.farshidb2011.data.repository.AuditFilter
import com.kalaban.farshidb2011.data.repository.UserSaveError
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.notifications.ShopWorkers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(application: Application) : GuardedViewModel(application) {
    private val keepAlive = SharingStarted.Eagerly

    val config: StateFlow<AppConfig> = app.config.config
    val roles = app.users.observeRoles().stateIn(viewModelScope, keepAlive, emptyList())
    val users = app.users.observeUsers().stateIn(viewModelScope, keepAlive, emptyList())
    val attributes = app.database.attributeDao().observeDefinitions().stateIn(viewModelScope, keepAlive, emptyList())

    private val _auditFilter = MutableStateFlow(AuditFilter())
    val auditFilter: StateFlow<AuditFilter> = _auditFilter.asStateFlow()
    val audit = _auditFilter.flatMapLatest { app.audit.observe(it) }.stateIn(viewModelScope, keepAlive, emptyList())

    private val _userErrors = MutableStateFlow<Set<UserSaveError>>(emptySet())
    val userErrors: StateFlow<Set<UserSaveError>> = _userErrors.asStateFlow()

    fun setAuditFilter(transform: (AuditFilter) -> AuditFilter) = _auditFilter.update(transform)

    /** Persists configuration changes and records them in the audit log. */
    fun updateConfig(permission: Permission = Permission.MANAGE_SETTINGS, message: Boolean = true, transform: (AppConfig) -> AppConfig) =
        launchSafely {
            if (!allowed(permission)) return@launchSafely
            val changes = app.config.update(transform)
            if (changes.isNotEmpty()) {
                app.audit.log(actor, AuditAction.CONFIG_UPDATE, AuditEntity.SETTINGS, summary = "config", details = changes.joinToString("; "))
                ShopWorkers.scheduleHealthCheck(getApplication())
                if (message) UiMessages.post(R.string.saved_ok)
            }
        }

    // ---- Users & roles ----

    fun saveUser(id: Long, name: String, roleId: Long?, pin: String?, active: Boolean, onSaved: () -> Unit) = launchSafely {
        if (!allowed(Permission.MANAGE_USERS)) return@launchSafely
        val errors = app.users.saveUser(id, name, roleId, pin, active, actor)
        _userErrors.value = errors
        if (errors.isEmpty()) {
            UiMessages.post(R.string.saved_ok)
            onSaved()
        }
    }

    fun clearUserErrors() {
        _userErrors.value = emptySet()
    }

    fun deleteUser(id: Long) = launchSafely {
        if (!allowed(Permission.MANAGE_USERS)) return@launchSafely
        app.users.deleteUser(id, actor)
    }

    fun saveRole(id: Long, nameFa: String, nameEn: String, permissions: Set<Permission>, onSaved: () -> Unit) = launchSafely {
        if (!allowed(Permission.MANAGE_USERS)) return@launchSafely
        if (app.users.saveRole(id, nameFa, nameEn, permissions, actor) != null) {
            UiMessages.post(R.string.saved_ok)
            onSaved()
        } else {
            UiMessages.post(R.string.role_not_editable, error = true)
        }
    }

    fun deleteRole(id: Long) = launchSafely {
        if (!allowed(Permission.MANAGE_USERS)) return@launchSafely
        if (!app.users.deleteRole(id, actor)) UiMessages.post(R.string.role_in_use, error = true)
    }

    /** Only the device owner can change the owner PIN, and only with the current PIN. */
    fun changeOwnerPin(current: String, new: String, onDone: () -> Unit) = launchSafely {
        if (session.user.value?.isOwner != true) {
            UiMessages.post(R.string.permission_denied, error = true)
            return@launchSafely
        }
        if (app.users.changeOwnerPin(current, new, actor)) {
            UiMessages.post(R.string.pin_changed)
            onDone()
        } else {
            UiMessages.post(R.string.pin_change_failed, error = true)
        }
    }

    // ---- Custom attributes ----

    private val keyPattern = Regex("^[a-z][a-z0-9_]{0,30}$")

    fun saveAttribute(definition: AttributeDefinitionEntity, onSaved: () -> Unit) = launchSafely {
        if (!allowed(Permission.MANAGE_CATALOG)) return@launchSafely
        val key = definition.attrKey.trim().lowercase()
        val dao = app.database.attributeDao()
        val clash = dao.getDefinitions().any { it.attrKey == key && it.id != definition.id }
        if (!keyPattern.matches(key) || clash || definition.labelFa.isBlank() && definition.labelEn.isBlank() ||
            definition.type !in AttributeType.all ||
            (definition.type == AttributeType.CHOICE && definition.options.orEmpty().split(',').none { it.isNotBlank() })
        ) {
            UiMessages.post(R.string.attribute_invalid, error = true)
            return@launchSafely
        }
        val clean = definition.copy(
            attrKey = key,
            labelFa = definition.labelFa.trim().ifEmpty { definition.labelEn.trim() },
            labelEn = definition.labelEn.trim().ifEmpty { definition.labelFa.trim() },
            options = definition.options?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.joinToString(",")?.ifEmpty { null },
        )
        if (clean.id == 0L) dao.insertDefinition(clean) else dao.updateDefinition(clean)
        app.audit.log(actor, AuditAction.CATALOG_UPDATE, AuditEntity.CATALOG, clean.id.takeIf { it != 0L }, "attribute: $key", clean.type)
        UiMessages.post(R.string.saved_ok)
        onSaved()
    }

    fun deleteAttribute(definition: AttributeDefinitionEntity) = launchSafely {
        if (!allowed(Permission.MANAGE_CATALOG)) return@launchSafely
        app.database.attributeDao().deleteDefinition(definition.id)
        app.audit.log(actor, AuditAction.CATALOG_UPDATE, AuditEntity.CATALOG, definition.id, "attribute deleted: ${definition.attrKey}")
    }

    // ---- Audit export ----

    fun exportAudit(uri: Uri, headers: List<String>) = launchSafely {
        if (!allowed(Permission.VIEW_AUDIT) || !allowed(Permission.EXPORT_DATA)) return@launchSafely
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val rows = audit.value.map {
            listOf(
                formatter.format(Date(it.atEpochMs)), Csv.text(it.actorName), it.action, it.entityType, it.entityId.orEmpty(),
                Csv.text(it.summary), Csv.text(it.details), if (it.success) "1" else "0",
            )
        }
        withContext(Dispatchers.IO) {
            app.importExport.writeText(uri, Csv.write(listOf(headers) + rows, app.config.current().csvDelimiter))
        }
        app.audit.log(actor, AuditAction.DATA_EXPORT, AuditEntity.DATA, summary = "audit_log: ${rows.size}")
        UiMessages.post(R.string.export_done, rows.size)
    }

    companion object {
        fun factory() = appViewModelFactory { AdminViewModel(KalabanApp.instance) }
    }
}
