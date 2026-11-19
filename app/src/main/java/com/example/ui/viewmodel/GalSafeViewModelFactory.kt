package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ProductRepository
import com.example.data.ai.AiIngredientAnalyzer
import com.example.data.local.GalSafeDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.remote.OpenFoodFactsService

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val preferencesRepository = PreferencesRepository(appContext)
    private val database = GalSafeDatabase.getInstance(appContext)
    private val offService = OpenFoodFactsService.create()
    private val aiAnalyzer = AiIngredientAnalyzer()

    val productRepository = ProductRepository(
        offService = offService,
        scanHistoryDao = database.scanHistoryDao(),
        preferencesRepository = preferencesRepository,
        aiAnalyzer = aiAnalyzer
    )
}

class GalSafeViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ScannerViewModel::class.java) ->
                ScannerViewModel(container.productRepository) as T
            modelClass.isAssignableFrom(ProductResultViewModel::class.java) ->
                ProductResultViewModel(container.productRepository) as T
            modelClass.isAssignableFrom(HistoryAndGuideViewModel::class.java) ->
                HistoryAndGuideViewModel(container.productRepository, container.preferencesRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
