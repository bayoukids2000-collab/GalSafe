package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.model.AllergenCategory
import com.example.domain.model.IngredientInsight
import com.example.domain.model.ProductAnalysisResult
import com.example.domain.model.SafetyVerdict
import com.example.ui.theme.LocalAllergenColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafetyVerdictBanner(
    product: ProductAnalysisResult,
    modifier: Modifier = Modifier
) {
    val colors = LocalAllergenColors.current
    val (bg, contentColor, accent, icon) = when (product.overallVerdict) {
        SafetyVerdict.UNSAFE -> when {
            product.mammalMeatTriggers.isNotEmpty() -> listOf(
                colors.mammalContainer,
                colors.mammalOnContainer,
                colors.mammalBadge,
                Icons.Filled.Dangerous
            )
            product.dairyTriggers.isNotEmpty() -> listOf(
                colors.dairyContainer,
                colors.dairyOnContainer,
                colors.dairyBadge,
                Icons.Filled.Dangerous
            )
            else -> listOf(
                colors.glutenContainer,
                colors.glutenOnContainer,
                colors.glutenBadge,
                Icons.Filled.Dangerous
            )
        }
        SafetyVerdict.CAUTION -> listOf(
            colors.hiddenContainer,
            colors.hiddenOnContainer,
            colors.hiddenBadge,
            Icons.Filled.Warning
        )
        SafetyVerdict.SAFE -> listOf(
            colors.safeContainer,
            colors.safeOnContainer,
            colors.safeBadge,
            Icons.Filled.CheckCircle
        )
        SafetyVerdict.UNKNOWN -> listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline,
            Icons.Filled.HelpOutline
        )
    }

    val containerBg = bg as Color
    val ink = contentColor as Color
    val badgeAccent = accent as Color
    val statusIcon = icon as ImageVector

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerBg,
        border = BorderStroke(1.5.dp, badgeAccent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = product.verdictHeadline,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.verdictHeadline,
                        style = MaterialTheme.typography.titleLarge,
                        color = ink,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.verdictSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.9f)
                    )
                }
            }

            if (product.ingredients.isNotEmpty() ||
                product.mammalMeatTriggers.isNotEmpty() ||
                product.dairyTriggers.isNotEmpty() ||
                product.glutenTriggers.isNotEmpty()
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product.mammalMeatTriggers.isNotEmpty()) {
                        CountPill(
                            label = "${product.mammalMeatTriggers.size} Mammal / Alpha-Gal",
                            badgeColor = colors.mammalBadge,
                            textColor = Color.White
                        )
                    }
                    if (product.dairyTriggers.isNotEmpty()) {
                        CountPill(
                            label = "${product.dairyTriggers.size} Dairy",
                            badgeColor = colors.dairyBadge,
                            textColor = Color.White
                        )
                    }
                    if (product.glutenTriggers.isNotEmpty()) {
                        CountPill(
                            label = "${product.glutenTriggers.size} Gluten",
                            badgeColor = colors.glutenBadge,
                            textColor = Color.White
                        )
                    }
                    if (product.hiddenRiskTriggers.isNotEmpty()) {
                        CountPill(
                            label = "${product.hiddenRiskTriggers.size} Check Source",
                            badgeColor = colors.hiddenBadge,
                            textColor = Color.White
                        )
                    }
                    val safeCount = product.ingredients.count { it.category == AllergenCategory.SAFE }
                    if (safeCount > 0) {
                        CountPill(
                            label = "$safeCount Safe",
                            badgeColor = colors.safeBadge.copy(alpha = 0.18f),
                            textColor = colors.safeOnContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPill(
    label: String,
    badgeColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = badgeColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun HighlightedIngredientParagraph(
    rawIngredientsText: String,
    insights: List<IngredientInsight>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAllergenColors.current

    val annotated: AnnotatedString = remember(rawIngredientsText, insights, colors) {
        buildAnnotatedString {
            append(rawIngredientsText)
            val flagged = insights.filter { it.category != AllergenCategory.SAFE }
                .sortedByDescending { it.rawName.length }

            val occupied = mutableListOf<IntRange>()
            for (item in flagged) {
                val term = item.rawName.trim()
                if (term.length < 2) continue
                val regex = Regex(Regex.escape(term), RegexOption.IGNORE_CASE)
                regex.findAll(rawIngredientsText).forEach { match ->
                    val range = match.range
                    val overlaps = occupied.any { existing ->
                        range.first <= existing.last && range.last >= existing.first
                    }
                    if (!overlaps) {
                        occupied.add(range)
                        val (bg, fg) = when (item.category) {
                            AllergenCategory.MAMMAL_MEAT -> colors.mammalContainer to colors.mammalOnContainer
                            AllergenCategory.DAIRY -> colors.dairyContainer to colors.dairyOnContainer
                            AllergenCategory.GLUTEN -> colors.glutenContainer to colors.glutenOnContainer
                            AllergenCategory.HIDDEN_RISK -> colors.hiddenContainer to colors.hiddenOnContainer
                            AllergenCategory.SAFE -> Color.Transparent to Color.Unspecified
                        }
                        addStyle(
                            style = SpanStyle(
                                background = bg,
                                color = fg,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = range.first,
                            end = range.last + 1
                        )
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Highlighted Package Ingredients",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HighlightLegendRow()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HighlightLegendRow() {
    val colors = LocalAllergenColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LegendDot(label = "Mammal Meat", color = colors.mammalBadge)
        LegendDot(label = "Dairy", color = colors.dairyBadge)
        LegendDot(label = "Gluten", color = colors.glutenBadge)
        LegendDot(label = "Check Source", color = colors.hiddenBadge)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun IngredientInsightCard(
    insight: IngredientInsight,
    modifier: Modifier = Modifier
) {
    val colors = LocalAllergenColors.current
    val isRestricted = insight.category == AllergenCategory.MAMMAL_MEAT ||
        insight.category == AllergenCategory.DAIRY ||
        insight.category == AllergenCategory.HIDDEN_RISK

    var expanded by remember(insight.rawName) { mutableStateOf(isRestricted) }

    val (cardBg, borderColor, badgeColor, badgeText, catIcon) = when (insight.category) {
        AllergenCategory.MAMMAL_MEAT -> listOf(
            colors.mammalContainer.copy(alpha = 0.55f),
            colors.mammalBadge.copy(alpha = 0.55f),
            colors.mammalBadge,
            "MAMMAL MEAT",
            Icons.Filled.LocalDining
        )
        AllergenCategory.DAIRY -> listOf(
            colors.dairyContainer.copy(alpha = 0.55f),
            colors.dairyBadge.copy(alpha = 0.55f),
            colors.dairyBadge,
            "DAIRY",
            Icons.Filled.WaterDrop
        )
        AllergenCategory.GLUTEN -> listOf(
            colors.glutenContainer.copy(alpha = 0.6f),
            colors.glutenBadge.copy(alpha = 0.55f),
            colors.glutenBadge,
            "GLUTEN",
            Icons.Filled.BakeryDining
        )
        AllergenCategory.HIDDEN_RISK -> listOf(
            colors.hiddenContainer.copy(alpha = 0.55f),
            colors.hiddenBadge.copy(alpha = 0.45f),
            colors.hiddenBadge,
            "CHECK SOURCE",
            Icons.Filled.Warning
        )
        AllergenCategory.SAFE -> listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outlineVariant,
            colors.safeBadge,
            "SAFE",
            Icons.Filled.CheckCircle
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardBg as Color),
        border = BorderStroke(if (isRestricted) 1.5.dp else 1.dp, borderColor as Color)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = catIcon as ImageVector,
                    contentDescription = badgeText as String,
                    tint = badgeColor as Color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.simpleName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!insight.rawName.equals(insight.simpleName, ignoreCase = true)) {
                        Text(
                            text = "Label: ${insight.rawName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse details" else "Expand details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = insight.plainEnglishExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Source: ${insight.sourceOrigin}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (insight.whyItMattersForAlphaGal.isNotBlank() && isRestricted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insight.whyItMattersForAlphaGal,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = when (insight.category) {
                                AllergenCategory.MAMMAL_MEAT -> colors.mammalOnContainer
                                AllergenCategory.DAIRY -> colors.dairyOnContainer
                                AllergenCategory.GLUTEN -> colors.glutenOnContainer
                                AllergenCategory.HIDDEN_RISK -> colors.hiddenOnContainer
                                AllergenCategory.SAFE -> colors.safeOnContainer
                            }
                        )
                    }
                }
            }
        }
    }
}
