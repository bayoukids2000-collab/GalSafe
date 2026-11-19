package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProductRepository
import com.example.data.local.PreferencesRepository
import com.example.domain.model.ProductAnalysisResult
import com.example.domain.model.SafetyVerdict
import com.example.domain.model.UserAllergenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter(val label: String) {
    ALL("All"),
    SAFE("Safe"),
    FLAGGED("Contains Triggers"),
    SAVED("Saved")
}

class HistoryAndGuideViewModel(
    private val repository: ProductRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    val preferences: StateFlow<UserAllergenPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserAllergenPreferences()
        )

    val filteredHistory: StateFlow<List<ProductAnalysisResult>> = combine(
        repository.scanHistory,
        _searchQuery,
        _historyFilter
    ) { items, query, filter ->
        val q = query.trim().lowercase()
        items.filter { item ->
            val matchesQuery = q.isBlank() ||
                item.productName.lowercase().contains(q) ||
                item.brand.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q)
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.SAFE -> item.overallVerdict == SafetyVerdict.SAFE
                HistoryFilter.FLAGGED -> item.overallVerdict == SafetyVerdict.UNSAFE ||
                    item.overallVerdict == SafetyVerdict.CAUTION
                HistoryFilter.SAVED -> item.isBookmarked
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun toggleBookmark(item: ProductAnalysisResult) {
        viewModelScope.launch {
            repository.toggleBookmark(item.barcode, item.isBookmarked)
        }
    }

    fun deleteItem(barcode: String) {
        viewModelScope.launch {
            repository.deleteFromHistory(barcode)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearUnbookmarkedHistory()
        }
    }

    fun setAvoidMammalMeat(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAvoidMammalMeat(enabled) }
    }

    fun setAvoidDairy(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAvoidDairy(enabled) }
    }

    fun setAvoidGluten(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAvoidGluten(enabled) }
    }

    fun setFlagHiddenDerivatives(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFlagHiddenDerivatives(enabled) }
    }

    fun setFlagCarrageenan(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFlagCarrageenan(enabled) }
    }
}
