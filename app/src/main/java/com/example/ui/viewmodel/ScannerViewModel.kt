package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LookupOutcome
import com.example.data.ProductRepository
import com.example.domain.model.ProductAnalysisResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data class LookingUp(val barcode: String) : ScannerUiState
    data object AnalyzingLabelPhoto : ScannerUiState
    data class ProductReady(val barcode: String) : ScannerUiState
    data class NotInDatabase(val barcode: String) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class ScannerViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ProductAnalysisResult>>(emptyList())
    val searchResults: StateFlow<List<ProductAnalysisResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var lastHandledBarcode: String? = null
    private var lastHandledTimestampMs: Long = 0L

    fun onBarcodeDetected(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) return
        val now = System.currentTimeMillis()
        if (_uiState.value is ScannerUiState.LookingUp || _uiState.value is ScannerUiState.AnalyzingLabelPhoto) {
            return
        }
        if (code == lastHandledBarcode && (now - lastHandledTimestampMs) < 3500L) {
            return
        }
        lastHandledBarcode = code
        lastHandledTimestampMs = now
        lookupBarcode(code)
    }

    fun lookupBarcode(code: String) {
        val clean = code.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _uiState.value = ScannerUiState.LookingUp(clean)
            when (val outcome = repository.fetchProductFast(clean)) {
                is LookupOutcome.Found -> {
                    _uiState.value = ScannerUiState.ProductReady(outcome.product.barcode)
                }
                is LookupOutcome.NotInDatabase -> {
                    _uiState.value = ScannerUiState.NotInDatabase(outcome.barcode)
                }
                is LookupOutcome.NetworkError -> {
                    _uiState.value = ScannerUiState.Error(outcome.message)
                }
            }
        }
    }

    fun scanBarcodeFromPhotoUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.LookingUp("photo")
            try {
                val barcodeValue = withContext(Dispatchers.IO) {
                    val image = InputImage.fromFilePath(context, uri)
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_ITF
                        )
                        .build()
                    val scanner = BarcodeScanning.getClient(options)
                    suspendCancellableCoroutine<String?> { cont ->
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                cont.resume(barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue)
                            }
                            .addOnFailureListener { e ->
                                cont.resumeWithException(e)
                            }
                    }
                }
                if (!barcodeValue.isNullOrBlank()) {
                    lookupBarcode(barcodeValue)
                } else {
                    _uiState.value = ScannerUiState.Error(
                        "No barcode found in that photo. Try 'Read Label' to scan the ingredient list directly."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(
                    e.localizedMessage ?: "Failed to read barcode from photo."
                )
            }
        }
    }

    fun analyzeIngredientsPhotoUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.AnalyzingLabelPhoto
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeScaledBitmap(context, uri, maxDimension = 1600)
                } ?: throw IllegalStateException("Could not decode the selected image.")

                val result = repository.analyzeLabelPhoto(bitmap)
                result.fold(
                    onSuccess = { product ->
                        _uiState.value = ScannerUiState.ProductReady(product.barcode)
                    },
                    onFailure = { e ->
                        _uiState.value = ScannerUiState.Error(
                            e.localizedMessage ?: "Failed to read ingredients from photo."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(
                    e.localizedMessage ?: "Failed to process label photo."
                )
            }
        }
    }

    fun analyzeCustomText(productName: String, ingredientsText: String) {
        if (ingredientsText.isBlank()) return
        viewModelScope.launch {
            val code = "MANUAL-${System.currentTimeMillis()}"
            val product = repository.analyzeCustomIngredients(code, productName, ingredientsText)
            _uiState.value = ScannerUiState.ProductReady(product.barcode)
        }
    }

    fun searchByName(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            repository.searchProductsByName(query).fold(
                onSuccess = { products -> _searchResults.value = products },
                onFailure = { _searchResults.value = emptyList() }
            )
            _isSearching.value = false
        }
    }

    fun openSearchResult(product: ProductAnalysisResult) {
        viewModelScope.launch {
            repository.saveToHistory(product)
            _uiState.value = ScannerUiState.ProductReady(product.barcode)
        }
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }
}
