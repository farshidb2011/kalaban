package com.kalaban.farshidb2011.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.formula.FormulaError
import com.kalaban.farshidb2011.core.formula.FormulaErrorCode
import com.kalaban.farshidb2011.data.local.entity.BatchStatus
import com.kalaban.farshidb2011.data.local.entity.PriceSource
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.domain.Actor
import com.kalaban.farshidb2011.domain.Permission

@Composable
fun actorLabel(name: String): String = when (name) {
    Actor.OWNER_NAME -> stringResource(R.string.actor_owner)
    Actor.SYSTEM_NAME -> stringResource(R.string.actor_system)
    else -> name
}

@Composable
fun priceSourceLabel(source: String): String = stringResource(
    when (source) {
        PriceSource.CREATE -> R.string.source_create
        PriceSource.EDIT -> R.string.source_edit
        PriceSource.BULK -> R.string.source_bulk
        PriceSource.RECALCULATION -> R.string.source_recalculation
        PriceSource.DOLLAR_RATE -> R.string.source_dollar_rate
        PriceSource.IMPORT -> R.string.source_import
        PriceSource.ROLLBACK -> R.string.source_rollback
        PriceSource.SCHEDULED -> R.string.source_scheduled
        else -> R.string.source_edit
    },
)

@Composable
fun batchStatusLabel(status: String): String = stringResource(
    when (status) {
        BatchStatus.ROLLED_BACK -> R.string.batch_rolled_back
        BatchStatus.SCHEDULED -> R.string.batch_scheduled
        BatchStatus.CANCELLED -> R.string.batch_cancelled
        BatchStatus.FAILED -> R.string.batch_failed
        else -> R.string.batch_applied
    },
)

@Composable
fun formulaErrorText(error: FormulaError): String {
    val base = stringResource(
        when (error.code) {
            FormulaErrorCode.EMPTY -> R.string.ferr_empty
            FormulaErrorCode.TOO_LONG -> R.string.ferr_too_long
            FormulaErrorCode.TOO_COMPLEX -> R.string.ferr_too_complex
            FormulaErrorCode.UNEXPECTED_CHARACTER -> R.string.ferr_unexpected_char
            FormulaErrorCode.UNEXPECTED_TOKEN -> R.string.ferr_unexpected_token
            FormulaErrorCode.UNEXPECTED_END -> R.string.ferr_unexpected_end
            FormulaErrorCode.UNBALANCED_PARENTHESES -> R.string.ferr_parentheses
            FormulaErrorCode.UNKNOWN_VARIABLE -> R.string.ferr_unknown_variable
            FormulaErrorCode.MISSING_VALUE -> R.string.ferr_missing_value
            FormulaErrorCode.UNKNOWN_FUNCTION -> R.string.ferr_unknown_function
            FormulaErrorCode.WRONG_ARGUMENT_COUNT -> R.string.ferr_argument_count
            FormulaErrorCode.EXPECTED_NUMBER -> R.string.ferr_expected_number
            FormulaErrorCode.EXPECTED_CONDITION -> R.string.ferr_expected_condition
            FormulaErrorCode.DIVISION_BY_ZERO -> R.string.ferr_division_by_zero
            FormulaErrorCode.VALUE_TOO_LARGE -> R.string.ferr_too_large
            FormulaErrorCode.INVALID_ARGUMENT -> R.string.ferr_invalid_argument
        },
    )
    val detail = error.detail?.takeIf { it.isNotBlank() }?.let { " «$it»" }.orEmpty()
    return stringResource(R.string.ferr_format, base + detail, error.position + 1)
}

@Composable
fun auditActionLabel(action: String): String = stringResource(
    when (action) {
        AuditAction.LOGIN -> R.string.audit_login
        AuditAction.LOGIN_FAILED -> R.string.audit_login_failed
        AuditAction.LOGOUT -> R.string.audit_logout
        AuditAction.PIN_CHANGE -> R.string.audit_pin_change
        AuditAction.PRODUCT_CREATE -> R.string.audit_product_create
        AuditAction.PRODUCT_UPDATE -> R.string.audit_product_update
        AuditAction.PRODUCT_DUPLICATE -> R.string.audit_product_duplicate
        AuditAction.PRODUCT_ARCHIVE -> R.string.audit_product_archive
        AuditAction.PRODUCT_RESTORE -> R.string.audit_product_restore
        AuditAction.PRODUCT_DELETE -> R.string.audit_product_delete
        AuditAction.PRODUCT_BULK_CATEGORY -> R.string.audit_product_category
        AuditAction.PRICE_BATCH -> R.string.audit_price_batch
        AuditAction.PRICE_ROLLBACK -> R.string.audit_price_rollback
        AuditAction.PRICE_SCHEDULE -> R.string.audit_price_schedule
        AuditAction.PRICE_SCHEDULE_CANCEL -> R.string.audit_price_schedule_cancel
        AuditAction.FORMULA_SAVE -> R.string.audit_formula_save
        AuditAction.FORMULA_DELETE -> R.string.audit_formula_delete
        AuditAction.FORMULA_ACTIVATE -> R.string.audit_formula_activate
        AuditAction.FORMULA_REVERT -> R.string.audit_formula_revert
        AuditAction.SETTINGS_UPDATE -> R.string.audit_settings
        AuditAction.CONFIG_UPDATE -> R.string.audit_config
        AuditAction.CATALOG_UPDATE -> R.string.audit_catalog
        AuditAction.SALE_CREATE -> R.string.audit_sale
        AuditAction.PAYMENT_ADD -> R.string.audit_payment
        AuditAction.BALANCE_ZERO -> R.string.audit_balance_zero
        AuditAction.DATA_EXPORT -> R.string.audit_export
        AuditAction.DATA_RESTORE -> R.string.audit_restore
        AuditAction.DATA_IMPORT -> R.string.audit_import
        AuditAction.USER_SAVE -> R.string.audit_user_save
        AuditAction.USER_DELETE -> R.string.audit_user_delete
        AuditAction.ROLE_SAVE -> R.string.audit_role_save
        AuditAction.ROLE_DELETE -> R.string.audit_role_delete
        AuditAction.HISTORY_CLEAR -> R.string.audit_history_clear
        else -> R.string.audit_other
    },
)

@Composable
fun permissionLabel(p: Permission): String = stringResource(
    when (p) {
        Permission.VIEW_PRODUCTS -> R.string.perm_view_products
        Permission.EDIT_PRODUCTS -> R.string.perm_edit_products
        Permission.DELETE_PRODUCTS -> R.string.perm_delete_products
        Permission.VIEW_COSTS -> R.string.perm_view_costs
        Permission.MANAGE_PRICES -> R.string.perm_manage_prices
        Permission.MANAGE_FORMULAS -> R.string.perm_manage_formulas
        Permission.MANAGE_CATALOG -> R.string.perm_manage_catalog
        Permission.RECORD_SALES -> R.string.perm_record_sales
        Permission.EDIT_SALE_PRICE -> R.string.perm_edit_sale_price
        Permission.MANAGE_CREDITS -> R.string.perm_manage_credits
        Permission.VIEW_REPORTS -> R.string.perm_view_reports
        Permission.EXPORT_DATA -> R.string.perm_export
        Permission.IMPORT_DATA -> R.string.perm_import
        Permission.MANAGE_SETTINGS -> R.string.perm_settings
        Permission.MANAGE_USERS -> R.string.perm_users
        Permission.VIEW_AUDIT -> R.string.perm_audit
    },
)
