package com.kalaban.farshidb2011.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.core.pricing.PriceResult
import com.kalaban.farshidb2011.core.pricing.PricingModes
import com.kalaban.farshidb2011.core.pricing.ProfitAnalysis
import com.kalaban.farshidb2011.core.pricing.SalePrice
import com.kalaban.farshidb2011.data.local.entity.AttributeType
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.ItemStatus
import com.kalaban.farshidb2011.data.repository.PricingContext
import com.kalaban.farshidb2011.data.repository.ProductDraft
import com.kalaban.farshidb2011.data.repository.ProductError
import com.kalaban.farshidb2011.data.repository.ProductPrice
import com.kalaban.farshidb2011.data.repository.SaveProductResult
import com.kalaban.farshidb2011.domain.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigDecimal

enum class ProductSort { TITLE, PRICE_ASC, PRICE_DESC, STOCK_ASC, RECENT }

data class CatalogFilter(
    val query: String = "",
    val status: String = ItemStatus.ACTIVE,
    val categoryId: Long? = null,
    val lowStockOnly: Boolean = false,
    val sort: ProductSort = ProductSort.TITLE,
)

/** Text-field state of the product editor; parsed and validated only on save. */
data class ProductEditorState(
    val id: Long = 0,
    val title: String = "",
    val sku: String = "",
    val barcode: String = "",
    val unitId: Long? = null,
    val categoryId: Long? = null,
    val brand: String = "",
    val supplier: String = "",
    val description: String = "",
    val notes: String = "",
    val tags: String = "",
    val imageUri: String? = null,
    val pendingCameraUri: String? = null,
    val pieces: String = "1",
    val packPrice: String = "",
    val transport: String = "",
    val packaging: String = "",
    val other: String = "",
    val dollarLinked: Boolean = true,
    /** New products default to manual sell price entered next to purchase cost. */
    val pricingMode: String = PricingModes.MANUAL,
    val pricingValue: String = "",
    val formulaId: Long? = null,
    val manualPrice: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val stockUnits: String = "",
    val lowStock: String = "1",
    val maxStock: String = "",
    val archived: Boolean = false,
    val attributes: Map<Long, String> = emptyMap(),
    val errors: Set<ProductError> = emptySet(),
    val invalidNumbers: Set<String> = emptySet(),
    val loaded: Boolean = false,
)

data class PricePreview(val result: PriceResult, val sale: SalePrice, val profit: ProfitAnalysis)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ProductsViewModel(application: Application) : GuardedViewModel(application) {
    private val products get() = app.products
    private val keepAlive = SharingStarted.Eagerly

    private val _filter = MutableStateFlow(CatalogFilter())
    val filter: StateFlow<CatalogFilter> = _filter.asStateFlow()

    val catalog: StateFlow<List<ItemEntity>> = _filter
        .flatMapLatest { f -> products.observeCatalog(f.query, f.status).map { list -> list to f } }
        .map { (list, f) ->
            list.asSequence()
                .filter { f.categoryId == null || it.categoryId == f.categoryId }
                .filter { !f.lowStockOnly || (it.lowStockThreshold > 0 && it.stockQty <= it.lowStockThreshold) }
                .let { seq ->
                    when (f.sort) {
                        ProductSort.TITLE -> seq.sortedBy { it.title.lowercase() }
                        ProductSort.PRICE_ASC -> seq.sortedBy { it.sellPrice }
                        ProductSort.PRICE_DESC -> seq.sortedByDescending { it.sellPrice }
                        ProductSort.STOCK_ASC -> seq.sortedBy { it.stockQty }
                        ProductSort.RECENT -> seq.sortedByDescending { it.updatedAtEpochMs }
                    }
                }
                .toList()
        }
        .stateIn(viewModelScope, keepAlive, emptyList())

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    val attributeDefinitions = app.database.attributeDao().observeDefinitions().stateIn(viewModelScope, keepAlive, emptyList())
    val formulas = app.pricing.observeFormulas().stateIn(viewModelScope, keepAlive, emptyList())
    val brands = products.observeBrands().stateIn(viewModelScope, keepAlive, emptyList())
    val suppliers = products.observeSuppliers().stateIn(viewModelScope, keepAlive, emptyList())
    val recentSearches = app.config.config.map { it.recentSearches }.stateIn(viewModelScope, keepAlive, emptyList())

    private val _editor = MutableStateFlow(ProductEditorState())
    val editor: StateFlow<ProductEditorState> = _editor.asStateFlow()

    private val _preview = MutableStateFlow<PricePreview?>(null)
    val preview: StateFlow<PricePreview?> = _preview.asStateFlow()

    private val _detail = MutableStateFlow<ProductPrice?>(null)
    val detail: StateFlow<ProductPrice?> = _detail.asStateFlow()

    private var editorContext: PricingContext? = null

    init {
        // Live price preview while editing; debounced so typing stays smooth.
        combine(_editor, attributeDefinitions) { state, _ -> state }
            .debounce(250)
            .onEach { state -> if (state.loaded) _preview.value = withContext(Dispatchers.Default) { computePreview(state) } }
            .launchIn(viewModelScope)
    }

    fun setFilter(transform: (CatalogFilter) -> CatalogFilter) = _filter.update(transform)

    fun rememberSearch(query: String) = launchSafely {
        val q = query.trim()
        if (q.length < 2) return@launchSafely
        app.config.update { cfg -> cfg.copy(recentSearches = (listOf(q) + cfg.recentSearches.filterNot { it == q }).take(6)) }
    }

    fun toggleSelection(id: Long) = _selection.update { if (id in it) it - id else it + id }
    fun selectAll(ids: Collection<Long>) = _selection.update { ids.toSet() }
    fun clearSelection() = _selection.update { emptySet() }

    // ---- Detail ----

    fun observeItem(id: Long) = products.observeItem(id)
    fun observeHistory(id: Long) = app.pricing.observeHistory(id)
    fun observeAttributeValues(id: Long) = products.observeAttributeValues(id)

    fun loadDetail(item: ItemEntity) = launchSafely {
        _detail.value = withContext(Dispatchers.IO) { app.pricing.priceOf(item) }
    }

    // ---- Editor ----

    private fun String.dec(): BigDecimal? = Numbers.parseDecimal(this)

    fun startNew(defaultUnitId: Long?, categoryId: Long? = null) = launchSafely {
        editorContext = app.pricing.loadContext()
        _editor.value = ProductEditorState(unitId = defaultUnitId, categoryId = categoryId, loaded = true)
    }

    fun startEdit(id: Long) = launchSafely {
        editorContext = app.pricing.loadContext()
        val item = products.item(id) ?: return@launchSafely
        val pieces = Money.of(item.piecesPerUnit).max(BigDecimal.ONE)
        fun amount(v: Double?) = v?.takeIf { it != 0.0 }?.let(Money::input).orEmpty()
        _editor.value = ProductEditorState(
            id = item.id,
            title = item.title,
            sku = item.sku.orEmpty(),
            barcode = item.barcode.orEmpty(),
            unitId = item.unitId,
            categoryId = item.categoryId,
            brand = item.brand.orEmpty(),
            supplier = item.supplier.orEmpty(),
            description = item.description.orEmpty(),
            notes = item.notes.orEmpty(),
            tags = item.tags.orEmpty(),
            imageUri = item.imageUri,
            pieces = Money.input(pieces),
            packPrice = amount(Money.of(item.purchasePrice).multiply(pieces, Money.MC).toDouble()),
            transport = amount(item.transportCost),
            packaging = amount(item.packagingCost),
            other = amount(item.otherCost),
            dollarLinked = item.dollarLinked,
            pricingMode = item.pricingMode,
            pricingValue = item.pricingValue?.let(Money::input).orEmpty(),
            formulaId = item.formulaId,
            manualPrice = if (item.pricingMode == PricingModes.MANUAL) Money.input(item.sellPrice) else "",
            minPrice = item.minPrice?.let(Money::input).orEmpty(),
            maxPrice = item.maxPrice?.let(Money::input).orEmpty(),
            stockUnits = amount(Money.of(item.stockQty).divide(pieces, Money.MC).toDouble()),
            lowStock = Money.input(item.lowStockThreshold),
            maxStock = item.maxStock?.let(Money::input).orEmpty(),
            archived = item.status == ItemStatus.ARCHIVED,
            attributes = products.attributeValues(id).associate { it.attributeId to it.value },
            loaded = true,
        )
    }

    fun updateEditor(transform: (ProductEditorState) -> ProductEditorState) = _editor.update { transform(it).copy(errors = emptySet()) }

    fun importImage(sourceUri: String) = launchSafely {
        val durable = withContext(Dispatchers.IO) { app.repository.importImageToInternalStorage(sourceUri) }
        _editor.update { it.copy(imageUri = durable, pendingCameraUri = null) }
    }

    private fun invalidNumbers(s: ProductEditorState): Set<String> = buildSet {
        listOf(
            "pieces" to s.pieces, "packPrice" to s.packPrice, "transport" to s.transport, "packaging" to s.packaging,
            "other" to s.other, "pricingValue" to s.pricingValue, "manualPrice" to s.manualPrice, "minPrice" to s.minPrice,
            "maxPrice" to s.maxPrice, "stockUnits" to s.stockUnits, "lowStock" to s.lowStock, "maxStock" to s.maxStock,
        ).forEach { (key, text) -> if (text.isNotBlank() && text.dec() == null) add(key) }
    }

    private fun draftOf(s: ProductEditorState) = ProductDraft(
        id = s.id,
        title = s.title,
        sku = s.sku,
        barcode = s.barcode,
        unitId = s.unitId,
        categoryId = s.categoryId,
        brand = s.brand,
        supplier = s.supplier,
        description = s.description,
        notes = s.notes,
        tags = s.tags,
        imageUri = s.imageUri,
        piecesPerUnit = s.pieces.dec()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE,
        packPurchasePrice = s.packPrice.dec() ?: BigDecimal.ZERO,
        transportCost = s.transport.dec() ?: BigDecimal.ZERO,
        packagingCost = s.packaging.dec() ?: BigDecimal.ZERO,
        otherCost = s.other.dec() ?: BigDecimal.ZERO,
        dollarLinked = s.dollarLinked,
        pricingMode = s.pricingMode,
        pricingValue = s.pricingValue.dec(),
        formulaId = s.formulaId,
        manualPrice = s.manualPrice.dec(),
        minPrice = s.minPrice.dec(),
        maxPrice = s.maxPrice.dec(),
        stockUnits = s.stockUnits.dec() ?: BigDecimal.ZERO,
        lowStockThreshold = s.lowStock.dec() ?: BigDecimal.ONE,
        maxStock = s.maxStock.dec(),
        status = if (s.archived) ItemStatus.ARCHIVED else ItemStatus.ACTIVE,
        attributes = s.attributes,
    )

    private fun computePreview(s: ProductEditorState): PricePreview? {
        val ctx = editorContext ?: return null
        val d = draftOf(s)
        val perPiece = d.packPurchasePrice.divide(d.piecesPerUnit, Money.MC)
        val existingRate = ctx.policy.dollarRate
        val item = ItemEntity(
            id = s.id,
            title = s.title,
            unitId = s.unitId ?: 0,
            categoryId = s.categoryId,
            purchasePrice = perPiece.toDouble(),
            purchaseDollarRate = existingRate.toDouble(),
            sellPrice = d.manualPrice?.toDouble() ?: 0.0,
            piecesPerUnit = d.piecesPerUnit.toDouble(),
            stockQty = d.stockUnits.multiply(d.piecesPerUnit, Money.MC).toDouble(),
            transportCost = d.transportCost.toDouble(),
            packagingCost = d.packagingCost.toDouble(),
            otherCost = d.otherCost.toDouble(),
            dollarLinked = d.dollarLinked,
            pricingMode = d.pricingMode,
            pricingValue = d.pricingValue?.toDouble(),
            formulaId = d.formulaId,
            minPrice = d.minPrice?.toDouble(),
            maxPrice = d.maxPrice?.toDouble(),
        )
        val numeric = ctx.attributeDefinitions
            .filter { it.active && it.type == AttributeType.NUMBER }
            .mapNotNull { def -> Numbers.parseDecimal(s.attributes[def.id])?.let { def.attrKey to it } }
            .toMap()
        val result = PriceCalculator.calculate(app.pricing.input(item, numeric), ctx.policy, ctx.formulas, ctx.knownVariables)
        val sale = PriceCalculator.salePrice(result.finalPrice, ctx.policy)
        return PricePreview(result, sale, PriceCalculator.profit(sale, result.cost, ctx.policy))
    }

    fun save(onSaved: (Long) -> Unit) = launchSafely {
        if (!allowed(Permission.EDIT_PRODUCTS)) return@launchSafely
        val state = _editor.value
        val invalid = invalidNumbers(state)
        if (invalid.isNotEmpty()) {
            _editor.update { it.copy(invalidNumbers = invalid) }
            UiMessages.post(R.string.fix_highlighted_fields, error = true)
            return@launchSafely
        }
        when (val result = products.save(draftOf(state), actor)) {
            is SaveProductResult.Invalid -> {
                _editor.update { it.copy(errors = result.errors, invalidNumbers = emptySet()) }
                UiMessages.post(R.string.fix_highlighted_fields, error = true)
            }
            is SaveProductResult.Saved -> {
                UiMessages.post(R.string.product_saved, Money.format(result.price))
                onSaved(result.id)
            }
        }
    }

    // ---- Actions ----

    fun duplicate(id: Long, title: String, onDone: (Long) -> Unit = {}) = launchSafely {
        if (!allowed(Permission.EDIT_PRODUCTS)) return@launchSafely
        products.duplicate(id, title, actor)?.let {
            UiMessages.post(R.string.product_duplicated)
            onDone(it)
        }
    }

    fun setArchived(ids: Collection<Long>, archived: Boolean) = launchSafely {
        if (!allowed(Permission.DELETE_PRODUCTS)) return@launchSafely
        products.setArchived(ids, archived, actor)
        clearSelection()
        UiMessages.post(if (archived) R.string.products_archived else R.string.products_restored, ids.size)
    }

    fun setCategory(ids: Collection<Long>, categoryId: Long?) = launchSafely {
        if (!allowed(Permission.EDIT_PRODUCTS)) return@launchSafely
        products.setCategory(ids, categoryId, actor)
        clearSelection()
        UiMessages.post(R.string.products_moved, ids.size)
    }

    fun delete(ids: Collection<Long>, onDone: () -> Unit = {}) = launchSafely {
        if (!allowed(Permission.DELETE_PRODUCTS)) return@launchSafely
        val result = products.delete(ids, actor)
        clearSelection()
        if (result.blocked.isEmpty()) {
            UiMessages.post(R.string.products_deleted, result.deleted)
        } else {
            UiMessages.post(R.string.products_delete_blocked, result.deleted, result.blocked.size, error = true)
        }
        onDone()
    }

    companion object {
        fun factory() = appViewModelFactory { ProductsViewModel(KalabanApp.instance) }
    }
}
