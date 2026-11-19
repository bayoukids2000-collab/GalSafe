package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LookupOutcome
import com.example.data.ProductRepository
import com.example.domain.model.AllergenCategory
import com.example.domain.model.IngredientInsight
import com.example.domain.model.ProductAnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class IngredientFilter(val label: String) {
    ALL("All"),
    FLAGGED("Flagged"),
    MAMMAL_MEAT("Mammal Meat"),
    DAIRY("Dairy"),
    GLUTEN("Gluten"),
    HIDDEN_RISK("Check Source"),
    SAFE("Safe")
}

sealed interface ProductResultUiState {
    data object Loading : ProductResultUiState
    data class Loaded(
        val product: ProductAnalysisResult,
        val isAiAnalyzing: Boolean,
        val selectedFilter: IngredientFilter
    ) : ProductResultUiState {
        val filteredIngredients: List<IngredientInsight>
            get() = when (selectedFilter) {
                IngredientFilter.ALL -> product.ingredients
                IngredientFilter.FLAGGED -> product.ingredients.filter { it.category != AllergenCategory.SAFE }
                IngredientFilter.MAMMAL_MEAT -> product.ingredients.filter { it.category == AllergenCategory.MAMMAL_MEAT }
                IngredientFilter.DAIRY -> product.ingredients.filter { it.category == AllergenCategory.DAIRY }
                IngredientFilter.GLUTEN -> product.ingredients.filter { it.category == AllergenCategory.GLUTEN }
                IngredientFilter.HIDDEN_RISK -> product.ingredients.filter { it.category == AllergenCategory.HIDDEN_RISK }
                IngredientFilter.SAFE -> product.ingredients.filter { it.category == AllergenCategory.SAFE }
            }
    }

    data class NotFound(val barcode: String) : ProductResultUiState
    data class Error(val message: String) : ProductResultUiState
}

class ProductResultViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductResultUiState>(ProductResultUiState.Loading)
    val uiState: StateFlow<ProductResultUiState> = _uiState.asStateFlow()

    private var loadedBarcode: String? = null

    fun loadProduct(barcode: String, forceRefreshAi: Boolean = false) {
        if (loadedBarcode == barcode && !forceRefreshAi && _uiState.value is ProductResultUiState.Loaded) {
            return
        }
        loadedBarcode = barcode

        viewModelScope.launch {
            _uiState.value = ProductResultUiState.Loading
            when (val outcome = repository.fetchProductFast(barcode)) {
                is LookupOutcome.Found -> {
                    val product = outcome.product
                    val needsAi = product.rawIngredientsText.isNotBlank() &&
                        (!product.isAiEnhanced || forceRefreshAi)
                    _uiState.value = ProductResultUiState.Loaded(
                        product = product,
                        isAiAnalyzing = needsAi,
                        selectedFilter = IngredientFilter.ALL
                    )
                    if (needsAi) {
                        runAiEnhancement(product)
                    }
                }
                is LookupOutcome.NotInDatabase -> {
                    _uiState.value = ProductResultUiState.NotFound(outcome.barcode)
                }
                is LookupOutcome.NetworkError -> {
                    _uiState.value = ProductResultUiState.Error(outcome.message)
                }
            }
        }
    }

    private fun runAiEnhancement(baseline: ProductAnalysisResult) {
        viewModelScope.launch {
            val currentFilter = (_uiState.value as? ProductResultUiState.Loaded)?.selectedFilter
                ?: IngredientFilter.ALL
            val enhanced = repository.enhanceWithAi(baseline)
            _uiState.value = ProductResultUiState.Loaded(
                product = enhanced,
                isAiAnalyzing = false,
                selectedFilter = currentFilter
            )
        }
    }

    fun retryAiEnhancement() {
        val current = _uiState.value as? ProductResultUiState.Loaded ?: return
        _uiState.value = current.copy(isAiAnalyzing = true)
        runAiEnhancement(current.product)
    }

    fun submitCustomIngredients(productName: String, rawIngredients: String) {
        val currentBarcode = loadedBarcode ?: "MANUAL-${System.currentTimeMillis()}"
        viewModelScope.launch {
            val baseline = repository.analyzeCustomIngredients(
                barcode = currentBarcode,
                productName = productName,
                rawIngredients = rawIngredients
            )
            _uiState.value = ProductResultUiState.Loaded(
                product = baseline,
                isAiAnalyzing = true,
                selectedFilter = IngredientFilter.ALL
            )
            runAiEnhancement(baseline)
        }
    }

    fun setFilter(filter: IngredientFilter) {
        val current = _uiState.value as? ProductResultUiState.Loaded ?: return
        _uiState.value = current.copy(selectedFilter = filter)
    }

    fun toggleBookmark() {
        val current = _uiState.value as? ProductResultUiState.Loaded ?: return
        val product = current.product
        viewModelScope.launch {
            repository.toggleBookmark(product.barcode, product.isBookmarked)
            _uiState.value = current.copy(
                product = product.copy(isBookmarked = !product.isBookmarked)
            )
        }
    }
}
