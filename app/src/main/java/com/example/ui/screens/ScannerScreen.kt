package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.SafetyVerdict
import com.example.domain.model.UserAllergenPreferences
import com.example.ui.theme.LocalAllergenColors
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    preferences: UserAllergenPreferences,
    onProductReady: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var showManualSheet by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val barcodePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.scanBarcodeFromPhotoUri(context, uri)
        }
    }

    val labelPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeIngredientsPhotoUri(context, uri)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ScannerUiState.ProductReady) {
            val code = (uiState as ScannerUiState.ProductReady).barcode
            viewModel.resetState()
            onProductReady(code)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth > 680.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with active allergen guard summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.scan_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.scan_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ActiveGuardPill(preferences = preferences)
                }

                // Live Camera Viewfinder OR Permission Prompt
                val viewfinderHeight = if (isWide) 360.dp else 330.dp
                if (hasCameraPermission) {
                    LiveBarcodeCameraCard(
                        torchEnabled = torchEnabled,
                        onToggleTorch = { torchEnabled = !torchEnabled },
                        onBarcodeDetected = { code -> viewModel.onBarcodeDetected(code) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(viewfinderHeight)
                    )
                } else {
                    CameraPermissionCard(
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Action Strip (works with or without camera!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            barcodePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("scan_photo_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.scan_from_photo),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            labelPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("read_label_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.snap_label_photo),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { showManualSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("enter_barcode_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.enter_barcode),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Helpful Alpha-Gal spotlight card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Instant Plain-English & Allergen Check",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically highlights beef, pork, gelatin, lard, carrageenan, milk, whey, casein, wheat, barley, rye, and hidden additives.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Loading overlay while looking up barcode or reading label photo
        if (uiState is ScannerUiState.LookingUp || uiState is ScannerUiState.AnalyzingLabelPhoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState is ScannerUiState.AnalyzingLabelPhoto) {
                                "Reading ingredients from label photo..."
                            } else {
                                "Looking up product ingredients..."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Not in database dialog -> let user snap label photo or paste ingredients
    if (uiState is ScannerUiState.NotInDatabase) {
        val code = (uiState as ScannerUiState.NotInDatabase).barcode
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            title = { Text("Barcode Not Found ($code)") },
            text = {
                Text(
                    "This barcode isn't in the Open Food Facts database yet. You can pick a photo of the ingredient label or paste the ingredients list to check it right now."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetState()
                        labelPhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Read Label Photo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetState()
                        showManualSheet = true
                    }
                ) {
                    Text("Paste Ingredients")
                }
            }
        )
    }

    if (uiState is ScannerUiState.Error) {
        val msg = (uiState as ScannerUiState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Scan Issue") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { viewModel.resetState() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showManualSheet) {
        ModalBottomSheet(
            onDismissRequest = { showManualSheet = false }
        ) {
            ManualLookupSheetContent(
                searchResults = searchResults,
                isSearching = isSearching,
                onLookupBarcode = { code ->
                    showManualSheet = false
                    viewModel.lookupBarcode(code)
                },
                onSearchByName = { query -> viewModel.searchByName(query) },
                onSelectSearchResult = { product ->
                    showManualSheet = false
                    viewModel.openSearchResult(product)
                },
                onAnalyzeCustomIngredients = { name, text ->
                    showManualSheet = false
                    viewModel.analyzeCustomText(name, text)
                }
            )
        }
    }
}

@Composable
private fun ActiveGuardPill(preferences: UserAllergenPreferences) {
    val activeLabels = buildList {
        if (preferences.avoidMammalMeat) add("No Mammal")
        if (preferences.avoidDairy) add("No Dairy")
        if (preferences.avoidGluten) add("Gluten-Free")
    }.joinToString(" • ").ifBlank { "Custom Guard" }

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = activeLabels,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun LiveBarcodeCameraCard(
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                mainExecutor,
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    mainExecutor
                ) { result ->
                    val detected = result?.getValue(barcodeScanner)
                        ?.firstOrNull { !it.rawValue.isNullOrBlank() }
                        ?.rawValue
                    if (!detected.isNullOrBlank()) {
                        onBarcodeDetected(detected)
                    }
                }
            )
        }
    }

    LaunchedEffect(torchEnabled) {
        cameraController.enableTorch(torchEnabled)
    }

    DisposableEffect(lifecycleOwner, cameraController) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.unbind()
            barcodeScanner.close()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val beamProgress by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_y"
    )

    Card(
        modifier = modifier.clip(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Reticle + animated laser line
            val frameColor = Color(0xFFFAF8F4)
            val beamColor = Color(0xFFE05A47)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width * 0.76f
                val boxHeight = size.height * 0.48f
                val left = (size.width - boxWidth) / 2f
                val top = (size.height - boxHeight) / 2f
                val right = left + boxWidth
                val bottom = top + boxHeight
                val cornerLen = 36f
                val strokeW = 7f

                // Top-left
                drawLine(frameColor, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
                drawLine(frameColor, Offset(left, top), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)
                // Top-right
                drawLine(frameColor, Offset(right - cornerLen, top), Offset(right, top), strokeW, StrokeCap.Round)
                drawLine(frameColor, Offset(right, top), Offset(right, top + cornerLen), strokeW, StrokeCap.Round)
                // Bottom-left
                drawLine(frameColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeW, StrokeCap.Round)
                drawLine(frameColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW, StrokeCap.Round)
                // Bottom-right
                drawLine(frameColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokeW, StrokeCap.Round)
                drawLine(frameColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeW, StrokeCap.Round)

                val beamY = size.height * beamProgress
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, beamColor, Color.Transparent),
                        startX = left,
                        endX = right
                    ),
                    start = Offset(left + 12f, beamY),
                    end = Offset(right - 12f, beamY),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            // Torch toggle button
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .testTag("torch_toggle_button")
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = stringResource(R.string.flash_toggle),
                    tint = Color.White
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.62f)
            ) {
                Text(
                    text = "Center UPC or EAN barcode inside frame",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Image(
                painter = painterResource(id = R.drawable.img_scanner_hero_1795111019550),
                contentDescription = "Scan food barcodes to detect mammal meat and dairy",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.camera_permission_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.camera_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("grant_camera_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.grant_camera_access))
            }
        }
    }
}

@Composable
private fun ManualLookupSheetContent(
    searchResults: List<com.example.domain.model.ProductAnalysisResult>,
    isSearching: Boolean,
    onLookupBarcode: (String) -> Unit,
    onSearchByName: (String) -> Unit,
    onSelectSearchResult: (com.example.domain.model.ProductAnalysisResult) -> Unit,
    onAnalyzeCustomIngredients: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var barcodeInput by remember { mutableStateOf("") }
    var productSearchInput by remember { mutableStateOf("") }
    var customProductName by remember { mutableStateOf("") }
    var customIngredients by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = "Manual Product & Ingredient Check",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Barcode / Search") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Paste Ingredients") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it.filter { ch -> ch.isDigit() } },
                label = { Text("Enter UPC / EAN barcode number") },
                placeholder = { Text("e.g. 0049000042566") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { if (barcodeInput.isNotBlank()) onLookupBarcode(barcodeInput) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_barcode_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onLookupBarcode(barcodeInput) },
                enabled = barcodeInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("lookup_barcode_submit")
            ) {
                Text("Check Barcode")
            }

            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = productSearchInput,
                onValueChange = { productSearchInput = it },
                label = { Text("Or search product by name") },
                placeholder = { Text("e.g. Oreo, Annie's Mac and Cheese") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onSearchByName(productSearchInput) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search products")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearchByName(productSearchInput) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_search_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (searchResults.isNotEmpty()) {
                val colors = LocalAllergenColors.current
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { it.barcode }) { item ->
                        val badgeColor = when (item.overallVerdict) {
                            SafetyVerdict.UNSAFE -> when {
                                item.mammalMeatTriggers.isNotEmpty() -> colors.mammalBadge
                                item.dairyTriggers.isNotEmpty() -> colors.dairyBadge
                                else -> colors.glutenBadge
                            }
                            SafetyVerdict.CAUTION -> colors.hiddenBadge
                            SafetyVerdict.SAFE -> colors.safeBadge
                            SafetyVerdict.UNKNOWN -> MaterialTheme.colorScheme.outline
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSearchResult(item) },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.brand} • ${item.barcode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = customProductName,
                onValueChange = { customProductName = it },
                label = { Text("Product name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = customIngredients,
                onValueChange = { customIngredients = it },
                label = { Text("Paste or type ingredients list") },
                placeholder = { Text("e.g. Enriched wheat flour, sugar, palm oil, whey, gelatin, natural flavors...") },
                minLines = 4,
                maxLines = 7,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_ingredients_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onAnalyzeCustomIngredients(customProductName, customIngredients) },
                enabled = customIngredients.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("analyze_custom_ingredients_button")
            ) {
                Text("Check & Explain Ingredients")
            }
        }
    }
}
