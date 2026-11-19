package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.AllergenCategory
import com.example.domain.model.ProductAnalysisResult
import com.example.ui.components.HighlightedIngredientParagraph
import com.example.ui.components.IngredientInsightCard
import com.example.ui.components.SafetyVerdictBanner
import com.example.ui.theme.LocalAllergenColors
import com.example.ui.viewmodel.IngredientFilter
import com.example.ui.viewmodel.ProductResultUiState
import com.example.ui.viewmodel.ProductResultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductResultScreen(
    barcode: String,
    viewModel: ProductResultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(barcode) {
        viewModel.loadProduct(barcode)
    }

    val loadedProduct = (uiState as? ProductResultUiState.Loaded)?.product

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("result_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = loadedProduct?.productName ?: "Product Check",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (loadedProduct != null) {
                            Text(
                                text = "${loadedProduct.brand} • ${loadedProduct.barcode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (loadedProduct != null) {
                        IconButton(
                            onClick = { viewModel.toggleBookmark() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bookmark_product_button")
                        ) {
                            Icon(
                                imageVector = if (loadedProduct.isBookmarked) {
                                    Icons.Filled.Bookmark
                                } else {
                                    Icons.Outlined.BookmarkBorder
                                },
                                contentDescription = stringResource(R.string.bookmark_cd),
                                tint = if (loadedProduct.isBookmarked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is ProductResultUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Checking ingredients for mammal meat & dairy...",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                is ProductResultUiState.NotFound -> {
                    MissingIngredientsEditorCard(
                        title = "Product Not Found ($barcode)",
                        subtitle = "Paste the ingredients from the package below to get instant mammal meat & dairy highlighting plus plain-English translations.",
                        onSubmit = { name, text -> viewModel.submitCustomIngredients(name, text) },
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .padding(20.dp)
                    )
                }

                is ProductResultUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { viewModel.loadProduct(barcode, forceRefreshAi = true) }) {
                            Text("Try Again")
                        }
                    }
                }

                is ProductResultUiState.Loaded -> {
                    LoadedProductContent(
                        state = state,
                        onSelectFilter = { viewModel.setFilter(it) },
                        onRetryAi = { viewModel.retryAiEnhancement() },
                        onSubmitMissingIngredients = { name, text ->
                            viewModel.submitCustomIngredients(name, text)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedProductContent(
    state: ProductResultUiState.Loaded,
    onSelectFilter: (IngredientFilter) -> Unit,
    onRetryAi: () -> Unit,
    onSubmitMissingIngredients: (String, String) -> Unit
) {
    val product = state.product

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Product Header Thumbnail Row (if image exists)
        if (!product.imageUrl.isNullOrBlank()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.productName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = product.brand,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "UPC: ${product.barcode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 1. High-Contrast Verdict Banner
        item {
            SafetyVerdictBanner(product = product)
        }

        // 2. Highlighted Restricted Ingredients Summary Card
        if (product.mammalMeatTriggers.isNotEmpty() ||
            product.dairyTriggers.isNotEmpty() ||
            product.glutenTriggers.isNotEmpty() ||
            product.hiddenRiskTriggers.isNotEmpty()
        ) {
            item {
                RestrictedIngredientsCallout(product = product)
            }
        }

        // 3. AI Plain-English Status / Error Strip
        if (state.isAiAnalyzing) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Translating complex ingredients into simple terms...",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else if (!product.aiErrorMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = product.aiErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryAi) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry AI")
                        }
                    }
                }
            }
        }

        // 4. Missing ingredients fallback editor
        if (product.rawIngredientsText.isBlank()) {
            item {
                MissingIngredientsEditorCard(
                    title = "No Ingredients Listed for This Product",
                    subtitle = "Open Food Facts didn't include the ingredients text yet. Paste the label ingredients below to highlight mammal meat & dairy.",
                    defaultName = product.productName,
                    onSubmit = onSubmitMissingIngredients
                )
            }
        } else {
            // 5. Highlighted Full Package Ingredient Text
            item {
                HighlightedIngredientParagraph(
                    rawIngredientsText = product.rawIngredientsText,
                    insights = product.ingredients
                )
            }

            // 6. Plain-English Breakdown Header + Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ingredients in Simple Terms",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (product.isAiEnhanced) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI Simplified",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(IngredientFilter.entries) { filter ->
                            val count = when (filter) {
                                IngredientFilter.ALL -> product.ingredients.size
                                IngredientFilter.FLAGGED -> product.ingredients.count { it.category != AllergenCategory.SAFE }
                                IngredientFilter.MAMMAL_MEAT -> product.ingredients.count { it.category == AllergenCategory.MAMMAL_MEAT }
                                IngredientFilter.DAIRY -> product.ingredients.count { it.category == AllergenCategory.DAIRY }
                                IngredientFilter.GLUTEN -> product.ingredients.count { it.category == AllergenCategory.GLUTEN }
                                IngredientFilter.HIDDEN_RISK -> product.ingredients.count { it.category == AllergenCategory.HIDDEN_RISK }
                                IngredientFilter.SAFE -> product.ingredients.count { it.category == AllergenCategory.SAFE }
                            }
                            if (filter == IngredientFilter.ALL || count > 0) {
                                FilterChip(
                                    selected = state.selectedFilter == filter,
                                    onClick = { onSelectFilter(filter) },
                                    label = { Text("${filter.label} ($count)") },
                                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // 7. Individual Ingredient Breakdown Cards
            items(
                items = state.filteredIngredients,
                key = { "${it.rawName}_${it.category.name}" }
            ) { insight ->
                IngredientInsightCard(insight = insight)
            }
        }

        // 8. Allergy & Label Verification Disclaimer
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.allergy_disclaimer_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.allergy_disclaimer_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestrictedIngredientsCallout(product: ProductAnalysisResult) {
    val colors = LocalAllergenColors.current

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Restricted Allergens Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (product.mammalMeatTriggers.isNotEmpty()) {
                TriggerGroupRow(
                    title = "Mammal Meat / Alpha-Gal Triggers",
                    items = product.mammalMeatTriggers,
                    badgeColor = colors.mammalBadge,
                    chipBg = colors.mammalContainer,
                    chipInk = colors.mammalOnContainer
                )
            }

            if (product.dairyTriggers.isNotEmpty()) {
                TriggerGroupRow(
                    title = "Dairy / Milk Triggers",
                    items = product.dairyTriggers,
                    badgeColor = colors.dairyBadge,
                    chipBg = colors.dairyContainer,
                    chipInk = colors.dairyOnContainer
                )
            }

            if (product.glutenTriggers.isNotEmpty()) {
                TriggerGroupRow(
                    title = "Gluten / Celiac Triggers",
                    items = product.glutenTriggers,
                    badgeColor = colors.glutenBadge,
                    chipBg = colors.glutenContainer,
                    chipInk = colors.glutenOnContainer
                )
            }

            if (product.hiddenRiskTriggers.isNotEmpty()) {
                TriggerGroupRow(
                    title = "Ambiguous / Verify Source",
                    items = product.hiddenRiskTriggers,
                    badgeColor = colors.hiddenBadge,
                    chipBg = colors.hiddenContainer,
                    chipInk = colors.hiddenOnContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggerGroupRow(
    title: String,
    items: List<String>,
    badgeColor: Color,
    chipBg: Color,
    chipInk: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = badgeColor,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { trigger ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = chipBg,
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = trigger,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = chipInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingIngredientsEditorCard(
    title: String,
    subtitle: String,
    defaultName: String = "",
    onSubmit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(defaultName) }
    var ingredients by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredients list from package") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSubmit(name, ingredients) },
                enabled = ingredients.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
            ) {
                Text("Analyze Ingredients")
            }
        }
    }
}
