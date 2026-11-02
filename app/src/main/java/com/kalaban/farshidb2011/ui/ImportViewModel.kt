package com.kalaban.farshidb2011.ui

import android.app.Application
import android.net.Uri
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.csv.Csv
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.data.repository.AuditEntity
import com.kalaban.farshidb2011.data.repository.DuplicatePolicy
import com.kalaban.farshidb2011.data.repository.ImportField
import com.kalaban.farshidb2011.data.repository.ImportOptions
import com.kalaban.farshidb2011.data.repository.ImportPreview
import com.kalaban.farshidb2011.data.repository.ImportResult
import com.kalaban.farshidb2011.data.repository.ImportRowStatus
import com.kalaban.farshidb2011.data.repository.ParsedCsv
import com.kalaban.farshidb2011.domain.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

enum class ImportStep { PICK, MAP, PREVIEW, DONE }

class ImportViewModel(application: Application) : GuardedViewModel(application) {
    private val io get() = app.importExport

    private val _step = MutableStateFlow(ImportStep.PICK)
    val step: StateFlow<ImportStep> = _step.asStateFlow()
    private val _parsed = MutableStateFlow<ParsedCsv?>(null)
    val parsed: StateFlow<ParsedCsv?> = _parsed.asStateFlow()
    private val _options = MutableStateFlow(ImportOptions(emptyMap()))
    val options: StateFlow<ImportOptions> = _options.asStateFlow()
    private val _preview = MutableStateFlow<ImportPreview?>(null)
    val preview: StateFlow<ImportPreview?> = _preview.asStateFlow()
    private val _result = MutableStateFlow<ImportResult?>(null)
    val result: StateFlow<ImportResult?> = _result.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private suspend fun isFa() = app.repository.getSettings().languageTag.ifBlank { "fa" }.startsWith("fa")

    fun load(uri: Uri) = launchSafely {
        if (!allowed(Permission.IMPORT_DATA)) return@launchSafely
        _busy.value = true
        try {
            val parsed = withContext(Dispatchers.IO) { io.parse(uri) }
            _parsed.value = parsed
            _options.value = ImportOptions(mapping = io.autoMap(parsed.header), defaultUnitId = app.database.unitDao().getAll().firstOrNull()?.id)
            _preview.value = null
            _result.value = null
            _step.value = ImportStep.MAP
        } finally {
            _busy.value = false
        }
    }

    fun setMapping(field: ImportField, column: Int?) = _options.update { o ->
        o.copy(mapping = if (column == null) o.mapping - field else o.mapping + (field to column))
    }

    fun setOptions(transform: (ImportOptions) -> ImportOptions) = _options.update(transform)

    fun setDuplicates(policy: DuplicatePolicy) = _options.update { it.copy(duplicates = policy) }

    fun buildPreview() = launchSafely {
        val parsed = _parsed.value ?: return@launchSafely
        if (ImportField.TITLE !in _options.value.mapping) {
            UiMessages.post(R.string.import_title_required, error = true)
            return@launchSafely
        }
        _busy.value = true
        try {
            val fa = isFa()
            _preview.value = withContext(Dispatchers.Default) { io.preview(parsed, _options.value, fa) }
            io.saveMapping(parsed.header, _options.value.mapping)
            _step.value = ImportStep.PREVIEW
        } finally {
            _busy.value = false
        }
    }

    fun runImport(title: String) = launchSafely {
        if (!allowed(Permission.IMPORT_DATA)) return@launchSafely
        val preview = _preview.value ?: return@launchSafely
        _busy.value = true
        try {
            _result.value = io.import(preview, _options.value, actor, title)
            _step.value = ImportStep.DONE
        } finally {
            _busy.value = false
        }
    }

    fun back() {
        _step.value = when (_step.value) {
            ImportStep.PREVIEW -> ImportStep.MAP
            ImportStep.MAP -> ImportStep.PICK
            else -> ImportStep.PICK
        }
    }

    fun reset() {
        _step.value = ImportStep.PICK
        _parsed.value = null
        _preview.value = null
        _result.value = null
    }

    /** Rows that could not be imported, with their issues, so they can be fixed and re-imported. */
    fun exportErrors(uri: Uri, issueLabel: (com.kalaban.farshidb2011.data.repository.ImportRow) -> String) = launchSafely {
        val preview = _preview.value ?: return@launchSafely
        val parsed = _parsed.value ?: return@launchSafely
        val failed = preview.rows.filter { it.status == ImportRowStatus.ERROR }
        val rows = listOf(listOf("line", "issues") + parsed.header) + failed.map { row ->
            listOf(row.line.toString(), issueLabel(row)) + parsed.rows.getOrElse(row.line - 2) { emptyList() }
        }
        withContext(Dispatchers.IO) { io.writeText(uri, Csv.write(rows, parsed.delimiter)) }
        UiMessages.post(R.string.export_done, failed.size)
    }

    fun exportProducts(uri: Uri) = launchSafely {
        if (!allowed(Permission.EXPORT_DATA)) return@launchSafely
        val delimiter = app.config.current().csvDelimiter
        val text = withContext(Dispatchers.IO) { io.exportProducts(delimiter) }
        withContext(Dispatchers.IO) { io.writeText(uri, text) }
        app.audit.log(actor, AuditAction.DATA_EXPORT, AuditEntity.DATA, summary = "products.csv")
        UiMessages.post(R.string.export_done, text.count { it == '\n' } - 1)
    }

    fun writeTemplate(uri: Uri) = launchSafely {
        val delimiter = app.config.current().csvDelimiter
        withContext(Dispatchers.IO) { io.writeText(uri, io.template(delimiter)) }
        UiMessages.post(R.string.template_saved)
    }

    companion object {
        fun factory() = appViewModelFactory { ImportViewModel(KalabanApp.instance) }
    }
}
