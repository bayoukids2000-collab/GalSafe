package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.AllergenCategory
import com.example.ui.theme.LocalAllergenColors
import com.example.ui.viewmodel.HistoryAndGuideViewModel

private data class GuideItem(
    val ingredient: String,
    val plainMeaning: String,
    val category: AllergenCategory,
    val tip: String
)

private val guideReferenceItems = listOf(
    GuideItem(
        "Gelatin / Collagen",
        "Protein boiled from pig or cow skin & bones",
        AllergenCategory.MAMMAL_MEAT,
        "Avoid — hides in marshmallows, gummy candy, frosting, yogurt, and capsules."
    ),
    GuideItem(
        "Lard / Beef Tallow / Suet",
        "Rendered pork or beef fat used for frying or flakiness",
        AllergenCategory.MAMMAL_MEAT,
        "Avoid — common in tortillas, pie crusts, refried beans, and fries."
    ),
    GuideItem(
        "Carrageenan",
        "Red seaweed thickener containing the alpha-gal sugar molecule",
        AllergenCategory.MAMMAL_MEAT,
        "High Alpha-Gal risk — even though plant-based, it frequently triggers Alpha-Gal reactions."
    ),
    GuideItem(
        "Animal Rennet / Pepsin",
        "Stomach enzymes from calves or pigs",
        AllergenCategory.MAMMAL_MEAT,
        "Avoid unless labeled 'microbial' or 'vegetable' rennet."
    ),
    GuideItem(
        "Whey / Casein / Sodium Caseinate",
        "Proteins separated from cow's milk",
        AllergenCategory.DAIRY,
        "Avoid — sodium caseinate often hides in 'non-dairy' coffee creamers."
    ),
    GuideItem(
        "Ghee / Butterfat / Milkfat",
        "Concentrated fat from cow's milk",
        AllergenCategory.DAIRY,
        "Avoid — clarified butter (ghee) still carries dairy & alpha-gal risk."
    ),
    GuideItem(
        "Lactose / Lactalbumin",
        "Milk sugar or milk protein powder",
        AllergenCategory.DAIRY,
        "Avoid — frequently used as a filler in chips, seasoning packets, and pills."
    ),
    GuideItem(
        "Wheat / Spelt / Semolina / Durum",
        "Wheat flour and ancient wheat varieties rich in gluten",
        AllergenCategory.GLUTEN,
        "Celiac danger — avoid completely including enriched flour, farro, couscous, and seitan."
    ),
    GuideItem(
        "Barley / Malt / Brewer's Yeast",
        "Barley grain and sprouted barley malt syrups or extracts",
        AllergenCategory.GLUTEN,
        "Celiac danger — malt flavoring in cereals, chocolates, and vinegar contains gluten."
    ),
    GuideItem(
        "Rye / Triticale",
        "Rye grain and wheat-rye hybrid grain",
        AllergenCategory.GLUTEN,
        "Celiac danger — avoid pumpernickel, rye bread, and triticale."
    ),
    GuideItem(
        "Oats / Traditional Soy Sauce",
        "Regular oats (cross-contact) or wheat-brewed soy sauce",
        AllergenCategory.HIDDEN_RISK,
        "Only eat 'Certified Gluten-Free' oats and switch to gluten-free tamari."
    ),
    GuideItem(
        "Buckwheat / Quinoa / Maltodextrin",
        "Gluten-free seeds or US corn/potato/rice-derived starch",
        AllergenCategory.SAFE,
        "Safe for Celiac Disease — buckwheat is unrelated to wheat and US maltodextrin is gluten-free."
    ),
    GuideItem(
        "Natural Flavors",
        "Umbrella flavor blend with undisclosed sources",
        AllergenCategory.HIDDEN_RISK,
        "Check with brand — can include beef stock, pork extract, or butter notes."
    ),
    GuideItem(
        "Glycerin / Mono- & Diglycerides",
        "Moisturizer or emulsifier from vegetable oil or animal fat",
        AllergenCategory.HIDDEN_RISK,
        "Safe if labeled vegan/plant-based; otherwise confirm source."
    ),
    GuideItem(
        "Magnesium Stearate / Stearic Acid",
        "Fatty acid lubricant used in tablets and candies",
        AllergenCategory.HIDDEN_RISK,
        "Can be made from beef/pork fat or palm oil — verify vegetable source."
    ),
    GuideItem(
        "Vitamin D3 (Cholecalciferol)",
        "Added vitamin often extracted from sheep's wool lanolin",
        AllergenCategory.HIDDEN_RISK,
        "Sensitive patients prefer lichen-derived vegan Vitamin D3."
    ),
    GuideItem(
        "Cocoa Butter / Shea Butter",
        "Pure vegetable fats pressed from cacao beans or shea nuts",
        AllergenCategory.SAFE,
        "Safe — contains zero dairy despite the word 'butter'."
    ),
    GuideItem(
        "Cream of Tartar",
        "Natural acidic mineral salt from winemaking grapes",
        AllergenCategory.SAFE,
        "Safe — completely dairy-free."
    ),
    GuideItem(
        "Lactic Acid",
        "Tangy acid usually fermented from corn or beet sugar",
        AllergenCategory.SAFE,
        "Usually dairy-free — made by fermenting plant sugars, not milk."
    ),
    GuideItem(
        "Agar-Agar / Fruit Pectin",
        "Plant-based gelling agents from algae or fruit peel",
        AllergenCategory.SAFE,
        "Safe — plant alternatives to animal gelatin."
    )
)

@Composable
fun GuideAndSettingsScreen(
    viewModel: HistoryAndGuideViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AllergenCategory?>(null) }

    val filteredGuide = remember(search, selectedCategory) {
        val q = search.trim().lowercase()
        guideReferenceItems.filter { item ->
            val matchesCat = selectedCategory == null || item.category == selectedCategory
            val matchesQuery = q.isBlank() ||
                item.ingredient.lowercase().contains(q) ||
                item.plainMeaning.lowercase().contains(q) ||
                item.tip.lowercase().contains(q)
            matchesCat && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.guide_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Customize what GalSafe highlights and browse sneaky ingredients in plain English.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sensitivity Toggles Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Allergen Highlight Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PreferenceSwitchRow(
                            title = "Flag Mammal Meat & By-Products",
                            subtitle = "Beef, pork, lamb, gelatin, lard, tallow, bone broth",
                            checked = prefs.avoidMammalMeat,
                            onCheckedChange = { viewModel.setAvoidMammalMeat(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PreferenceSwitchRow(
                            title = "Flag Dairy & Milk Derivatives",
                            subtitle = "Milk, butter, cream, cheese, whey, casein, lactose",
                            checked = prefs.avoidDairy,
                            onCheckedChange = { viewModel.setAvoidDairy(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PreferenceSwitchRow(
                            title = "Flag Gluten & Celiac Triggers",
                            subtitle = "Wheat, barley, rye, malt, spelt, semolina, brewer's yeast",
                            checked = prefs.avoidGluten,
                            onCheckedChange = { viewModel.setAvoidGluten(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PreferenceSwitchRow(
                            title = "Flag Carrageenan (Alpha-Gal Epitope)",
                            subtitle = "Seaweed thickener that shares the alpha-gal sugar trigger",
                            checked = prefs.flagCarrageenan,
                            onCheckedChange = { viewModel.setFlagCarrageenan(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PreferenceSwitchRow(
                            title = "Flag Hidden / Ambiguous Additives",
                            subtitle = "Natural flavors, glycerin, stearates, Vitamin D3",
                            checked = prefs.flagHiddenMammalDerivatives,
                            onCheckedChange = { viewModel.setFlagHiddenDerivatives(it) }
                        )
                    }
                }
            }

            // Reference Cheat Sheet Search & Filters
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ingredient Decoder Cheat Sheet",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Look up an ingredient (e.g. whey, gelatin)...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == AllergenCategory.MAMMAL_MEAT,
                            onClick = { selectedCategory = AllergenCategory.MAMMAL_MEAT },
                            label = { Text("Mammal Meat") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == AllergenCategory.DAIRY,
                            onClick = { selectedCategory = AllergenCategory.DAIRY },
                            label = { Text("Dairy") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == AllergenCategory.GLUTEN,
                            onClick = { selectedCategory = AllergenCategory.GLUTEN },
                            label = { Text("Gluten / Celiac") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == AllergenCategory.HIDDEN_RISK,
                            onClick = { selectedCategory = AllergenCategory.HIDDEN_RISK },
                            label = { Text("Check Source") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == AllergenCategory.SAFE,
                            onClick = { selectedCategory = AllergenCategory.SAFE },
                            label = { Text("Safe Look-Alikes") }
                        )
                    }
                }
            }

            items(filteredGuide, key = { it.ingredient }) { entry ->
                GuideReferenceCard(entry)
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.allergy_disclaimer_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.allergy_disclaimer_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.privacy_notice_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.privacy_notice_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GuideReferenceCard(entry: GuideItem) {
    val colors = LocalAllergenColors.current
    val (badgeColor, badgeBg, badgeText) = when (entry.category) {
        AllergenCategory.MAMMAL_MEAT -> Triple(colors.mammalBadge, colors.mammalContainer, "MAMMAL / ALPHA-GAL")
        AllergenCategory.DAIRY -> Triple(colors.dairyBadge, colors.dairyContainer, "DAIRY")
        AllergenCategory.GLUTEN -> Triple(colors.glutenBadge, colors.glutenContainer, "GLUTEN / CELIAC")
        AllergenCategory.HIDDEN_RISK -> Triple(colors.hiddenBadge, colors.hiddenContainer, "CHECK SOURCE")
        AllergenCategory.SAFE -> Triple(colors.safeBadge, colors.safeContainer, "SAFE")
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.ingredient,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.plainMeaning,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.tip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
