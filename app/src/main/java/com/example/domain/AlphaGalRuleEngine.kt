package com.example.domain

import com.example.domain.model.AllergenCategory
import com.example.domain.model.IngredientInsight
import com.example.domain.model.ProductAnalysisResult
import com.example.domain.model.SafetyVerdict
import com.example.domain.model.UserAllergenPreferences

object AlphaGalRuleEngine {

    private data class KeywordRule(
        val pattern: Regex,
        val simpleName: String,
        val explanation: String,
        val sourceOrigin: String,
        val whyMatters: String
    )

    private val dairyFalsePositives = listOf(
        "cocoa butter", "cacao butter", "shea butter", "peanut butter", "almond butter",
        "cashew butter", "sunflower butter", "apple butter", "coconut butter",
        "coconut milk", "almond milk", "oat milk", "soy milk", "soymilk", "rice milk",
        "cashew milk", "hemp milk", "flax milk", "macadamia milk", "pea milk",
        "coconut cream", "cream of tartar", "lactic acid", "calcium lactate",
        "sodium lactate", "potassium lactate", "vegan cheese", "dairy-free cheese",
        "non-dairy creamer", "plant butter", "vegan butter", "butter flavor"
    )

    private val mammalFalsePositives = listOf(
        "vegetable gelatin", "agar", "pectin", "plant collagen",
        "poultry", "chicken", "turkey", "duck", "fish", "salmon", "tuna", "shrimp",
        "vegetable glycerin", "vegetable glycerine", "plant-based", "vegan",
        "microbial rennet", "vegetable rennet"
    )

    private val glutenFalsePositives = listOf(
        "buckwheat", "gluten-free", "gluten free", "certified gluten-free",
        "certified gluten free", "maltodextrin", "tamari"
    )

    private val mammalRules = listOf(
        KeywordRule(
            Regex("\\b(beef|steak|brisket|hamburger|veal|oxtail|tallow|beef fat|beef broth|beef stock|beef extract)\\b", RegexOption.IGNORE_CASE),
            "Beef (Mammal Meat)",
            "Derived from cattle. Contains the alpha-gal carbohydrate present in all red/mammal meats.",
            "Cattle / Mammal",
            "Direct Alpha-Gal trigger — avoid completely."
        ),
        KeywordRule(
            Regex("\\b(pork|bacon|ham|prosciutto|pepperoni|salami|sausage|chorizo|lard|pork fat|pork broth|pork skin|pork rinds)\\b", RegexOption.IGNORE_CASE),
            "Pork (Mammal Meat)",
            "Derived from pigs, including cured cuts, bacon, and rendered pork fat (lard).",
            "Pig / Mammal",
            "Direct Alpha-Gal trigger — avoid completely."
        ),
        KeywordRule(
            Regex("\\b(lamb|mutton|venison|bison|buffalo meat|elk|boar|goat meat|rabbit|meat extract|bone broth|meat)\\b", RegexOption.IGNORE_CASE),
            "Red / Mammal Meat",
            "Meat or broth from a non-poultry mammal (lamb, venison, bison, or unspecified meat).",
            "Mammal Meat",
            "Direct Alpha-Gal trigger — avoid completely."
        ),
        KeywordRule(
            Regex("\\b(gelatin|gelatine|hydrolyzed gelatin|collagen|collagen peptides)\\b", RegexOption.IGNORE_CASE),
            "Animal Gelatin / Collagen",
            "Protein boiled from mammal bones, skin, and connective tissue (usually pork or beef).",
            "Pork / Beef By-product",
            "High Alpha-Gal trigger — commonly hides in gummies, marshmallows, and capsules."
        ),
        KeywordRule(
            Regex("\\b(suet|animal fat|animal shortening|dripping|oleo|oleostearin|bone char)\\b", RegexOption.IGNORE_CASE),
            "Rendered Mammal Fat / Bone",
            "Solid fat or processing aid rendered from beef or sheep fat tissue or charred bones.",
            "Mammal Fat / Bone",
            "Direct Alpha-Gal trigger — animal-derived fat or bone processing."
        ),
        KeywordRule(
            Regex("\\b(rennet|animal rennet|pepsin)\\b", RegexOption.IGNORE_CASE),
            "Animal Enzyme (Rennet/Pepsin)",
            "Digestive enzyme traditionally extracted from calf or pig stomachs, used in cheese and supplements.",
            "Mammal Stomach Enzyme",
            "Alpha-Gal risk unless explicitly labeled microbial or vegetarian."
        ),
        KeywordRule(
            Regex("\\b(lanolin)\\b", RegexOption.IGNORE_CASE),
            "Sheep Wool Wax (Lanolin)",
            "Waxy oil extracted from sheep's wool, often used to make Vitamin D3 or coatings.",
            "Sheep / Mammal By-product",
            "Mammal-derived ingredient that can trigger sensitive Alpha-Gal reactions."
        ),
        KeywordRule(
            Regex("\\b(carrageenan)\\b", RegexOption.IGNORE_CASE),
            "Carrageenan (Seaweed Thickener)",
            "A gelling thickener from red seaweed that naturally carries the alpha-gal sugar structure.",
            "Red Seaweed (Alpha-Gal Epitope)",
            "Known Alpha-Gal trigger even though it is plant-based — many Alpha-Gal patients react to it."
        )
    )

    private val dairyRules = listOf(
        KeywordRule(
            Regex("\\b(whole milk|skim milk|nonfat milk|dried milk|milk powder|milk solids|evaporated milk|condensed milk|buttermilk|milk chocolate|cultured milk|milk)\\b", RegexOption.IGNORE_CASE),
            "Cow's Milk",
            "Liquid, powdered, or concentrated milk from dairy cows.",
            "Cow's Milk (Dairy)",
            "Restricted dairy ingredient — avoid for dairy & Alpha-Gal sensitivity."
        ),
        KeywordRule(
            Regex("\\b(whey|whey protein|whey powder|sweet whey|demineralized whey)\\b", RegexOption.IGNORE_CASE),
            "Whey (Milk Liquid Protein)",
            "The watery protein left over after milk curdles during cheese making.",
            "Cow's Milk (Dairy)",
            "Concentrated dairy protein — very common in snacks, breads, and protein bars."
        ),
        KeywordRule(
            Regex("\\b(casein|caseinate|sodium caseinate|calcium caseinate|potassium caseinate|rennet casein)\\b", RegexOption.IGNORE_CASE),
            "Casein (Milk Binding Protein)",
            "The primary protein in cow's milk, often used as a creamy binder even in 'non-dairy' creamers.",
            "Cow's Milk (Dairy)",
            "Hidden dairy protein — avoid even when a label says 'non-dairy'."
        ),
        KeywordRule(
            Regex("\\b(lactose|lactalbumin|lactoglobulin|lactoferrin|milk protein|milk derivative|curds|dairy)\\b", RegexOption.IGNORE_CASE),
            "Milk Sugar / Milk Protein",
            "Sugar or protein fraction isolated from mammal milk.",
            "Dairy Derivative",
            "Restricted dairy ingredient — triggers dairy allergy and Alpha-Gal dairy sensitivity."
        ),
        KeywordRule(
            Regex("\\b(butter|butterfat|milkfat|milk fat|anhydrous milk fat|ghee|clarified butter)\\b", RegexOption.IGNORE_CASE),
            "Butter / Milk Fat",
            "Churned or clarified fat separated from cow's milk or cream.",
            "Cow's Milk Fat (Dairy)",
            "Restricted dairy fat — ghee and butterfat still come from mammal milk."
        ),
        KeywordRule(
            Regex("\\b(cream|heavy cream|whipping cream|sour cream|half and half|creamer)\\b", RegexOption.IGNORE_CASE),
            "Dairy Cream",
            "Rich butterfat layer skimmed from cow's milk.",
            "Cow's Milk (Dairy)",
            "Restricted dairy ingredient unless explicitly plant-based."
        ),
        KeywordRule(
            Regex("\\b(cheese|cheddar|mozzarella|parmesan|romano|ricotta|cream cheese|cottage cheese|yogurt|kefir)\\b", RegexOption.IGNORE_CASE),
            "Cheese / Cultured Dairy",
            "Fermented or aged cow, goat, or sheep milk product.",
            "Mammal Milk (Dairy)",
            "Restricted dairy ingredient (and may also contain animal rennet)."
        )
    )

    private val glutenRules = listOf(
        KeywordRule(
            Regex("\\b(vital wheat gluten|wheat gluten|gluten)\\b", RegexOption.IGNORE_CASE),
            "Wheat Gluten Protein",
            "Concentrated gluten protein from wheat — the primary trigger of autoimmune intestinal damage in Celiac Disease.",
            "Wheat (Gluten)",
            "Celiac danger — avoid completely."
        ),
        KeywordRule(
            Regex("\\b(wheat flour|enriched wheat flour|whole wheat flour|whole wheat|enriched flour|unbleached flour|bread flour|cake flour|pastry flour|wheat starch|wheat germ|wheat bran|graham flour|hydrolyzed wheat protein|textured wheat protein|wheat|durum|semolina|spelt|kamut|farro|einkorn|emmer|bulgur|couscous|seitan|orzo|panko|matzo|matzah)\\b", RegexOption.IGNORE_CASE),
            "Wheat / Wheat Grain",
            "Wheat or an ancient wheat variety (spelt, farro, semolina, durum, couscous) that naturally contains gluten.",
            "Wheat Grain (Gluten)",
            "Celiac danger — contains gluten protein."
        ),
        KeywordRule(
            Regex("\\b(barley|malted barley|barley malt|malt extract|malt syrup|malt flavoring|malt vinegar|malt|brewer'?s yeast)\\b", RegexOption.IGNORE_CASE),
            "Barley / Malt (Gluten)",
            "Barley grain or malt sweetener/flavoring made from sprouted barley.",
            "Barley Grain (Gluten)",
            "Celiac danger — barley and malt contain hordeins (gluten)."
        ),
        KeywordRule(
            Regex("\\b(rye|rye flour|pumpernickel|triticale)\\b", RegexOption.IGNORE_CASE),
            "Rye / Triticale (Gluten)",
            "Rye grain or triticale (a wheat-rye hybrid) containing gluten proteins.",
            "Rye / Hybrid Grain (Gluten)",
            "Celiac danger — avoid completely."
        )
    )

    private val glutenHiddenRiskRules = listOf(
        KeywordRule(
            Regex("\\b(oats|oat flour|rolled oats|steel cut oats|oat bran|oatmeal)\\b", RegexOption.IGNORE_CASE),
            "Oats (Check Certified GF)",
            "Oats are naturally gluten-free, but standard oats are heavily cross-contaminated with wheat and barley during farming and milling.",
            "Grain (Cross-Contact Risk)",
            "For Celiac Disease, only eat oats explicitly labeled 'Certified Gluten-Free'."
        ),
        KeywordRule(
            Regex("\\b(soy sauce|teriyaki)\\b", RegexOption.IGNORE_CASE),
            "Traditional Soy Sauce",
            "Standard soy sauce is fermented with roasted wheat in addition to soybeans.",
            "Soy + Wheat (Usually Gluten)",
            "Unsafe for Celiac Disease unless explicitly made with gluten-free tamari."
        )
    )

    private val hiddenRiskRules = listOf(
        KeywordRule(
            Regex("\\b(natural flavors?|natural flavoring|artificial and natural flavors?)\\b", RegexOption.IGNORE_CASE),
            "Natural Flavors (Ambiguous)",
            "Umbrella term for flavor extracts that may include beef/pork broth, butter, or dairy derivatives.",
            "Unspecified Plant or Animal",
            "Manufacturer verification needed — can hide mammal broth or dairy notes."
        ),
        KeywordRule(
            Regex("\\b(glycerin|glycerine|glycerol)\\b", RegexOption.IGNORE_CASE),
            "Glycerin (Check Source)",
            "Sweet, syrupy humectant that keeps foods moist; made from either vegetable oil or animal fat.",
            "Plant Oil or Mammal Fat",
            "Safe if vegetable-sourced, risky if derived from beef/pork tallow."
        ),
        KeywordRule(
            Regex("\\b(mono-?\\s*and\\s*diglycerides|monoglycerides|diglycerides)\\b", RegexOption.IGNORE_CASE),
            "Mono- & Diglycerides",
            "Emulsifiers that keep oil and water blended in baked goods; sourced from soy/palm oil or animal fat.",
            "Plant Oil or Mammal Fat",
            "Check if vegan/plant-sourced; otherwise may originate from mammal fat."
        ),
        KeywordRule(
            Regex("\\b(stearic acid|magnesium stearate|calcium stearate|sodium stearoyl lactylate)\\b", RegexOption.IGNORE_CASE),
            "Stearate / Emulsifier",
            "Fatty acid binder used in tablets, candies, and bread; can be made from beef/pork fat or plant oils.",
            "Plant Oil, Dairy, or Mammal Fat",
            "Common hidden Alpha-Gal trigger in medications and processed foods."
        ),
        KeywordRule(
            Regex("\\b(vitamin d3|cholecalciferol)\\b", RegexOption.IGNORE_CASE),
            "Vitamin D3",
            "Added Vitamin D nutrient, most commonly produced from sheep's wool lanolin (unless lichen-sourced).",
            "Often Sheep Lanolin",
            "Check source if highly sensitive to mammal by-products."
        ),
        KeywordRule(
            Regex("\\b(polysorbate 60|polysorbate 80)\\b", RegexOption.IGNORE_CASE),
            "Polysorbate Emulsifier",
            "Synthetic-feeling blender made using fatty acids that can come from plant or animal fats.",
            "Plant or Animal Fatty Acids",
            "Usually plant-based in the US, but source is unverified unless labeled vegan."
        ),
        KeywordRule(
            Regex("\\b(enzymes|cultured)\\b", RegexOption.IGNORE_CASE),
            "Unspecified Enzymes / Cultures",
            "Processing proteins or starter cultures that can come from microbes, plants, or animal stomachs/milk.",
            "Microbial, Dairy, or Animal",
            "Verify whether enzymes are microbial or animal-derived."
        )
    )

    private val plainDictionary: Map<String, Pair<String, String>> = mapOf(
        "water" to Pair("Water", "Purified water used as the base liquid."),
        "sugar" to Pair("Cane / Beet Sugar", "Standard sweetener from sugarcane or sugar beets. (Some US cane sugar is filtered with bone char.)"),
        "cane sugar" to Pair("Cane Sugar", "Sweetener extracted from sugarcane stalks."),
        "salt" to Pair("Table Salt", "Mineral sodium chloride for flavor and preservation."),
        "sea salt" to Pair("Sea Salt", "Evaporated ocean salt crystals."),
        "wheat flour" to Pair("Wheat Flour", "Milled wheat grain forming the main structure of baked goods."),
        "enriched wheat flour" to Pair("Enriched Wheat Flour", "Refined wheat flour with added B vitamins and iron."),
        "corn starch" to Pair("Cornstarch", "Plant starch from corn kernels used to thicken sauces and fillings."),
        "cornstarch" to Pair("Cornstarch", "Plant starch from corn kernels used to thicken sauces and fillings."),
        "corn syrup" to Pair("Corn Syrup", "Liquid sweetener made from cornstarch."),
        "high fructose corn syrup" to Pair("Corn-Based Sweetener", "Sweet liquid syrup processed from cornstarch."),
        "citric acid" to Pair("Citric Acid", "Natural-style tartness and preservative originally from citrus fruits (typically plant-fermented)."),
        "ascorbic acid" to Pair("Vitamin C", "Antioxidant vitamin added to keep food fresh and prevent browning."),
        "soy lecithin" to Pair("Soy Lecithin", "Plant-based emulsifier from soybeans that helps oil and water mix smoothly."),
        "sunflower lecithin" to Pair("Sunflower Lecithin", "Plant-based emulsifier from sunflower seeds that keeps textures smooth."),
        "xanthan gum" to Pair("Xanthan Gum", "Plant-fermented thickener that gives sauces and gluten-free foods body."),
        "guar gum" to Pair("Guar Gum", "Natural fiber thickener milled from guar beans."),
        "pectin" to Pair("Fruit Pectin", "Plant fiber from apples or citrus peel used to make jams and gummies gel (safe alternative to gelatin)."),
        "agar" to Pair("Agar-Agar (Seaweed Gel)", "Plant-based gelling agent from red algae — safe alternative to animal gelatin."),
        "maltodextrin" to Pair("Maltodextrin", "Lightly sweet starchy powder made from corn, rice, or potato starch."),
        "dextrose" to Pair("Dextrose (Simple Corn Sugar)", "Simple plant sugar usually derived from corn."),
        "baking soda" to Pair("Baking Soda", "Sodium bicarbonate — mineral leavening agent that helps baked goods rise."),
        "sodium bicarbonate" to Pair("Baking Soda", "Mineral leavening agent that helps baked goods rise."),
        "baking powder" to Pair("Baking Powder", "Blend of baking soda and mild acid salts that makes batters fluffy."),
        "potassium sorbate" to Pair("Potassium Sorbate", "Mild synthetic mineral salt that prevents mold growth."),
        "sodium benzoate" to Pair("Sodium Benzoate", "Preservative salt that keeps acidic drinks and sauces fresh."),
        "tocopherols" to Pair("Vitamin E", "Plant-derived antioxidant (often from soy or sunflower) that keeps oils fresh."),
        "niacin" to Pair("Vitamin B3", "Essential B vitamin added to enrich grains."),
        "riboflavin" to Pair("Vitamin B2", "Essential B vitamin (also gives a natural yellow tint)."),
        "thiamine mononitrate" to Pair("Vitamin B1", "Essential B vitamin added to enriched flours."),
        "folic acid" to Pair("Vitamin B9", "Essential B vitamin added to fortified grain products."),
        "reduced iron" to Pair("Iron Mineral", "Dietary iron added to fortify flours and cereals."),
        "olive oil" to Pair("Olive Oil", "Plant oil pressed from olives."),
        "canola oil" to Pair("Canola Oil", "Neutral cooking oil pressed from rapeseed."),
        "sunflower oil" to Pair("Sunflower Oil", "Light plant oil pressed from sunflower seeds."),
        "palm oil" to Pair("Palm Oil", "Plant oil pressed from oil palm fruit."),
        "soybean oil" to Pair("Soybean Oil", "Plant oil extracted from soybeans."),
        "coconut oil" to Pair("Coconut Oil", "Plant oil pressed from coconut meat."),
        "cocoa butter" to Pair("Cocoa Butter (Plant Fat)", "Pure vegetable fat pressed from cacao beans — completely dairy-free."),
        "cocoa" to Pair("Cocoa Powder", "Roasted, ground cacao beans — naturally plant-based."),
        "garlic powder" to Pair("Garlic Powder", "Dehydrated ground garlic cloves."),
        "onion powder" to Pair("Onion Powder", "Dehydrated ground onions."),
        "paprika" to Pair("Paprika Spice", "Ground sweet red peppers for color and warmth."),
        "turmeric" to Pair("Turmeric Spice", "Golden plant root used for flavor and natural color."),
        "vinegar" to Pair("Vinegar", "Fermented grain or fruit acid (distilled vinegar is gluten-free; avoid malt vinegar)."),
        "yeast" to Pair("Baker's Yeast", "Single-celled fungus used to raise bread and add savory flavor (gluten-free; unlike brewer's yeast)."),
        "buckwheat" to Pair("Buckwheat (Gluten-Free Seed)", "Nutty grain-like seed unrelated to wheat — completely gluten-free and safe for Celiac Disease."),
        "quinoa" to Pair("Quinoa (Gluten-Free Seed)", "Protein-rich Andean seed that is naturally gluten-free."),
        "tapioca" to Pair("Tapioca Starch", "Gluten-free starch extracted from cassava root."),
        "rice flour" to Pair("Rice Flour", "Finely milled white or brown rice — naturally gluten-free."),
        "lactic acid" to Pair("Lactic Acid (Plant-Fermented)", "Tangy organic acid usually fermented from corn or beet sugar — despite its name, typically dairy-free and gluten-free.")
    )

    fun splitIngredients(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()
        val cleaned = rawText
            .replace(Regex("^(ingredients|contains|may contain)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .replace("\n", ", ")
            .trim()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (ch in cleaned) {
            when (ch) {
                '(', '[' -> {
                    depth++
                    current.append(ch)
                }
                ')', ']' -> {
                    if (depth > 0) depth--
                    current.append(ch)
                }
                ',', ';' -> {
                    if (depth == 0) {
                        val item = current.toString().trim().trimEnd('.')
                        if (item.isNotBlank()) result.add(item)
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }
        val tail = current.toString().trim().trimEnd('.')
        if (tail.isNotBlank()) result.add(tail)
        return result.distinctBy { it.lowercase() }
    }

    fun analyzeLocally(
        barcode: String,
        productName: String,
        brand: String,
        imageUrl: String?,
        rawIngredientsText: String,
        allergenTagsFromLabel: List<String>,
        prefs: UserAllergenPreferences
    ): ProductAnalysisResult {
        val tokens = splitIngredients(rawIngredientsText)
        val insights = tokens.map { token -> classifyIngredient(token, prefs) }

        val mammalTriggers = insights
            .filter { it.category == AllergenCategory.MAMMAL_MEAT }
            .map { it.rawName }
            .toMutableList()

        val dairyTriggers = insights
            .filter { it.category == AllergenCategory.DAIRY }
            .map { it.rawName }
            .toMutableList()

        val glutenTriggers = insights
            .filter { it.category == AllergenCategory.GLUTEN }
            .map { it.rawName }
            .toMutableList()

        val hiddenTriggers = insights
            .filter { it.category == AllergenCategory.HIDDEN_RISK }
            .map { it.rawName }
            .toMutableList()

        // Also inspect structured allergen tags from Open Food Facts (e.g. "en:milk", "en:gluten")
        val hasMilkLabelTag = prefs.avoidDairy && allergenTagsFromLabel.any {
            it.contains("milk", ignoreCase = true) || it.contains("dairy", ignoreCase = true)
        }
        if (hasMilkLabelTag && dairyTriggers.isEmpty()) {
            dairyTriggers.add("Milk (Declared on Allergen Label)")
        }

        val hasGlutenLabelTag = prefs.avoidGluten && allergenTagsFromLabel.any {
            it.contains("gluten", ignoreCase = true) || it.contains("wheat", ignoreCase = true) ||
                it.contains("barley", ignoreCase = true) || it.contains("rye", ignoreCase = true)
        }
        if (hasGlutenLabelTag && glutenTriggers.isEmpty()) {
            glutenTriggers.add("Gluten / Wheat (Declared on Allergen Label)")
        }

        val verdict = when {
            rawIngredientsText.isBlank() && !hasMilkLabelTag && !hasGlutenLabelTag -> SafetyVerdict.UNKNOWN
            mammalTriggers.isNotEmpty() || dairyTriggers.isNotEmpty() || glutenTriggers.isNotEmpty() -> SafetyVerdict.UNSAFE
            hiddenTriggers.isNotEmpty() -> SafetyVerdict.CAUTION
            else -> SafetyVerdict.SAFE
        }

        val headline = when (verdict) {
            SafetyVerdict.UNSAFE -> buildString {
                append("Avoid — ")
                val parts = mutableListOf<String>()
                if (mammalTriggers.isNotEmpty()) parts.add("Mammal")
                if (dairyTriggers.isNotEmpty()) parts.add("Dairy")
                if (glutenTriggers.isNotEmpty()) parts.add("Gluten")
                append(parts.joinToString(" & "))
                append(" Found")
            }
            SafetyVerdict.CAUTION -> "Check Source — Ambiguous Ingredients"
            SafetyVerdict.SAFE -> "No Mammal, Dairy, or Gluten Detected"
            SafetyVerdict.UNKNOWN -> "Ingredient List Missing"
        }

        val summary = when (verdict) {
            SafetyVerdict.UNSAFE -> {
                val all = (mammalTriggers + dairyTriggers + glutenTriggers).take(4).joinToString(", ")
                "Flagged restricted ingredients: $all. Review the highlighted label below before consuming."
            }
            SafetyVerdict.CAUTION -> {
                val hidden = hiddenTriggers.take(3).joinToString(", ")
                "No direct mammal meat, dairy, or gluten listed, but contains ingredients requiring source verification ($hidden)."
            }
            SafetyVerdict.SAFE -> {
                "None of the ${insights.size} parsed ingredients matched mammal meat, carrageenan, dairy, or gluten triggers."
            }
            SafetyVerdict.UNKNOWN -> {
                "No ingredient text was found for this barcode. Snap a photo of the ingredient label or paste it to check."
            }
        }

        return ProductAnalysisResult(
            barcode = barcode,
            productName = productName,
            brand = brand,
            imageUrl = imageUrl,
            rawIngredientsText = rawIngredientsText,
            allergenTagsFromLabel = allergenTagsFromLabel,
            overallVerdict = verdict,
            verdictHeadline = headline,
            verdictSummary = summary,
            mammalMeatTriggers = mammalTriggers,
            dairyTriggers = dairyTriggers,
            glutenTriggers = glutenTriggers,
            hiddenRiskTriggers = hiddenTriggers,
            ingredients = insights,
            isAiEnhanced = false
        )
    }

    fun classifyIngredient(rawIngredient: String, prefs: UserAllergenPreferences): IngredientInsight {
        val lower = rawIngredient.lowercase()

        // 1. Check mammal rules (respecting false-positive exclusions)
        val isMammalFalsePositive = mammalFalsePositives.any { lower.contains(it) }
        if (prefs.avoidMammalMeat && !isMammalFalsePositive) {
            for (rule in mammalRules) {
                if (rule.pattern.containsMatchIn(rawIngredient)) {
                    if (!prefs.flagCarrageenan && rule.simpleName.contains("Carrageenan", ignoreCase = true)) {
                        continue
                    }
                    return IngredientInsight(
                        rawName = rawIngredient,
                        simpleName = rule.simpleName,
                        plainEnglishExplanation = rule.explanation,
                        category = AllergenCategory.MAMMAL_MEAT,
                        sourceOrigin = rule.sourceOrigin,
                        whyItMattersForAlphaGal = rule.whyMatters
                    )
                }
            }
        }

        // 2. Check dairy rules (respecting plant milk / cocoa butter false positives)
        val isDairyFalsePositive = dairyFalsePositives.any { lower.contains(it) }
        if (prefs.avoidDairy && !isDairyFalsePositive) {
            for (rule in dairyRules) {
                if (rule.pattern.containsMatchIn(rawIngredient)) {
                    return IngredientInsight(
                        rawName = rawIngredient,
                        simpleName = rule.simpleName,
                        plainEnglishExplanation = rule.explanation,
                        category = AllergenCategory.DAIRY,
                        sourceOrigin = rule.sourceOrigin,
                        whyItMattersForAlphaGal = rule.whyMatters
                    )
                }
            }
        }

        // 3. Check gluten / Celiac rules (respecting gluten-free false positives)
        val isGlutenFalsePositive = glutenFalsePositives.any { lower.contains(it) }
        if (prefs.avoidGluten && !isGlutenFalsePositive) {
            for (rule in glutenRules) {
                if (rule.pattern.containsMatchIn(rawIngredient)) {
                    return IngredientInsight(
                        rawName = rawIngredient,
                        simpleName = rule.simpleName,
                        plainEnglishExplanation = rule.explanation,
                        category = AllergenCategory.GLUTEN,
                        sourceOrigin = rule.sourceOrigin,
                        whyItMattersForAlphaGal = rule.whyMatters
                    )
                }
            }
            for (rule in glutenHiddenRiskRules) {
                if (rule.pattern.containsMatchIn(rawIngredient)) {
                    return IngredientInsight(
                        rawName = rawIngredient,
                        simpleName = rule.simpleName,
                        plainEnglishExplanation = rule.explanation,
                        category = AllergenCategory.HIDDEN_RISK,
                        sourceOrigin = rule.sourceOrigin,
                        whyItMattersForAlphaGal = rule.whyMatters
                    )
                }
            }
        }

        // 4. Check hidden / ambiguous animal-source ingredients
        if (prefs.flagHiddenMammalDerivatives) {
            val isExplicitlyVeganOrPlant = lower.contains("vegetable") || lower.contains("plant") || lower.contains("vegan") || lower.contains("microbial")
            if (!isExplicitlyVeganOrPlant) {
                for (rule in hiddenRiskRules) {
                    if (rule.pattern.containsMatchIn(rawIngredient)) {
                        return IngredientInsight(
                            rawName = rawIngredient,
                            simpleName = rule.simpleName,
                            plainEnglishExplanation = rule.explanation,
                            category = AllergenCategory.HIDDEN_RISK,
                            sourceOrigin = rule.sourceOrigin,
                            whyItMattersForAlphaGal = rule.whyMatters
                        )
                    }
                }
            }
        }

        // 4. Plain-English dictionary fallback for safe ingredients
        val matchedEntry = plainDictionary.entries.firstOrNull { (key, _) ->
            lower == key || lower.contains(key)
        }
        if (matchedEntry != null) {
            return IngredientInsight(
                rawName = rawIngredient,
                simpleName = matchedEntry.value.first,
                plainEnglishExplanation = matchedEntry.value.second,
                category = AllergenCategory.SAFE,
                sourceOrigin = "Plant / Mineral / Safe",
                whyItMattersForAlphaGal = "Does not contain mammal meat or dairy."
            )
        }

        return IngredientInsight(
            rawName = rawIngredient,
            simpleName = rawIngredient.replaceFirstChar { it.uppercase() },
            plainEnglishExplanation = "Food ingredient listed on the package. No mammal meat or dairy keywords detected.",
            category = AllergenCategory.SAFE,
            sourceOrigin = "Plant / Grain / Seasoning",
            whyItMattersForAlphaGal = "No mammal meat or dairy trigger matched."
        )
    }
}
