package com.kalaban.farshidb2011.domain

/** Granular capabilities; names are persisted in `roles.permissions`, so do not rename entries. */
enum class Permission {
    VIEW_PRODUCTS,
    EDIT_PRODUCTS,
    DELETE_PRODUCTS,
    VIEW_COSTS,
    MANAGE_PRICES,
    MANAGE_FORMULAS,
    MANAGE_CATALOG,
    RECORD_SALES,
    EDIT_SALE_PRICE,
    MANAGE_CREDITS,
    VIEW_REPORTS,
    EXPORT_DATA,
    IMPORT_DATA,
    MANAGE_SETTINGS,
    MANAGE_USERS,
    VIEW_AUDIT,
    ;

    companion object {
        fun parse(csv: String): Set<Permission> =
            csv.split(',').mapNotNull { key -> entries.firstOrNull { it.name == key.trim() } }.toSet()

        fun encode(permissions: Set<Permission>): String = permissions.sortedBy { it.ordinal }.joinToString(",") { it.name }
    }
}

/** Built-in roles seeded on first run; [ADMIN] always carries every permission. */
object BuiltInRoles {
    const val ADMIN = "ADMIN"
    const val MANAGER = "MANAGER"
    const val CASHIER = "CASHIER"
    const val VIEWER = "VIEWER"

    val defaults: List<Triple<String, Pair<String, String>, Set<Permission>>> = listOf(
        Triple(ADMIN, "مدیر کل" to "Administrator", Permission.entries.toSet()),
        Triple(
            MANAGER,
            "مدیر فروشگاه" to "Store manager",
            Permission.entries.toSet() - setOf(Permission.MANAGE_USERS, Permission.MANAGE_SETTINGS, Permission.IMPORT_DATA),
        ),
        Triple(CASHIER, "فروشنده" to "Cashier", setOf(Permission.VIEW_PRODUCTS, Permission.RECORD_SALES, Permission.MANAGE_CREDITS)),
        Triple(VIEWER, "فقط مشاهده" to "Read-only", setOf(Permission.VIEW_PRODUCTS, Permission.VIEW_REPORTS)),
    )
}
