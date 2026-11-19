package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.ProductAnalysisResult
import com.example.domain.model.SafetyVerdict
import com.example.ui.theme.LocalAllergenColors
import com.example.ui.viewmodel.HistoryAndGuideViewModel
import com.example.ui.viewmodel.HistoryFilter

@Composable
fun HistoryScreen(
    viewModel: HistoryAndGuideViewModel,
    onOpenProduct: (String) -> Unit,
    onGoToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.historyFilter.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                if (items.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text(stringResource(R.string.clear_history))
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search scanned foods or brands...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryFilter.entries) { entry ->
                    FilterChip(
                        selected = filter == entry,
                        onClick = { viewModel.setHistoryFilter(entry) },
                        label = { Text(entry.label) }
                    )
                }
            }

            if (items.isEmpty()) {
                EmptyHistoryView(
                    hasFilterOrQuery = query.isNotBlank() || filter != HistoryFilter.ALL,
                    onGoToScanner = onGoToScanner,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.barcode }) { item ->
                        HistoryProductCard(
                            item = item,
                            onClick = { onOpenProduct(item.barcode) },
                            onToggleBookmark = { viewModel.toggleBookmark(item) },
                            onDelete = { viewModel.deleteItem(item.barcode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryProductCard(
    item: ProductAnalysisResult,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAllergenColors.current
    val (badgeColor, badgeBg, badgeInk, icon, verdictLabel) = when (item.overallVerdict) {
        SafetyVerdict.UNSAFE -> {
            val parts = buildList {
                if (item.mammalMeatTriggers.isNotEmpty()) add("Mammal")
                if (item.dairyTriggers.isNotEmpty()) add("Dairy")
                if (item.glutenTriggers.isNotEmpty()) add("Gluten")
            }.joinToString(" + ")
            when {
                item.mammalMeatTriggers.isNotEmpty() -> listOf(
                    colors.mammalBadge,
                    colors.mammalContainer,
                    colors.mammalOnContainer,
                    Icons.Filled.Dangerous,
                    parts
                )
                item.dairyTriggers.isNotEmpty() -> listOf(
                    colors.dairyBadge,
                    colors.dairyContainer,
                    colors.dairyOnContainer,
                    Icons.Filled.Dangerous,
                    parts
                )
                else -> listOf(
                    colors.glutenBadge,
                    colors.glutenContainer,
                    colors.glutenOnContainer,
                    Icons.Filled.Dangerous,
                    parts
                )
            }
        }
        SafetyVerdict.CAUTION -> listOf(
            colors.hiddenBadge,
            colors.hiddenContainer,
            colors.hiddenOnContainer,
            Icons.Filled.Warning,
            "Check Source"
        )
        SafetyVerdict.SAFE -> listOf(
            colors.safeBadge,
            colors.safeContainer,
            colors.safeOnContainer,
            Icons.Filled.CheckCircle,
            "Safe"
        )
        SafetyVerdict.UNKNOWN -> listOf(
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.HelpOutline,
            "No List"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${item.barcode}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(badgeBg as Color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                        contentDescription = null,
                        tint = badgeColor as Color
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg as Color
                ) {
                    Text(
                        text = verdictLabel as String,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeInk as Color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.bookmark_cd),
                    tint = if (item.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete from history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(
    hasFilterOrQuery: Boolean,
    onGoToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasFilterOrQuery) {
            Image(
                painter = painterResource(id = R.drawable.img_scanner_hero_1795111019550),
                contentDescription = "Scan food products to build your safe pantry",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
        Text(
            text = if (hasFilterOrQuery) "No matching scanned items" else "No scanned foods yet",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasFilterOrQuery) {
                "Try clearing your search or switching filters."
            } else {
                "Every barcode or label you scan is saved here with its mammal meat & dairy check."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasFilterOrQuery) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGoToScanner,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan First Product")
            }
        }
    }
}
