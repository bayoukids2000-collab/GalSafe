package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ui.screens.GuideAndSettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProductResultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.viewmodel.AppContainer
import com.example.ui.viewmodel.GalSafeViewModelFactory
import com.example.ui.viewmodel.HistoryAndGuideViewModel
import com.example.ui.viewmodel.ProductResultViewModel
import com.example.ui.viewmodel.ScannerViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ScannerRoute

@Serializable
data object HistoryRoute

@Serializable
data object GuideRoute

@Serializable
data class ProductResultRoute(val barcode: String)

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = ScannerRoute,
        labelRes = R.string.nav_scan,
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_tab_scan"
    ),
    TopLevelDestination(
        route = HistoryRoute,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    ),
    TopLevelDestination(
        route = GuideRoute,
        labelRes = R.string.nav_guide,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_tab_guide"
    )
)

@Composable
fun AppNavigation(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val factory = GalSafeViewModelFactory(appContainer)

    val scannerViewModel: ScannerViewModel = viewModel(factory = factory)
    val historyAndGuideViewModel: HistoryAndGuideViewModel = viewModel(factory = factory)
    val preferences by historyAndGuideViewModel.preferences.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isProductDetail = currentDestination?.hasRoute<ProductResultRoute>() == true

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && !isProductDetail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isProductDetail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        topLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ScannerRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<ScannerRoute> {
                        ScannerScreen(
                            viewModel = scannerViewModel,
                            preferences = preferences,
                            onProductReady = { barcode ->
                                navController.navigate(ProductResultRoute(barcode))
                            }
                        )
                    }

                    composable<HistoryRoute> {
                        HistoryScreen(
                            viewModel = historyAndGuideViewModel,
                            onOpenProduct = { barcode ->
                                navController.navigate(ProductResultRoute(barcode))
                            },
                            onGoToScanner = {
                                navController.navigate(ScannerRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    composable<GuideRoute> {
                        GuideAndSettingsScreen(
                            viewModel = historyAndGuideViewModel
                        )
                    }

                    composable<ProductResultRoute> { entry ->
                        val route = entry.toRoute<ProductResultRoute>()
                        val resultViewModel: ProductResultViewModel = viewModel(factory = factory)
                        ProductResultScreen(
                            barcode = route.barcode,
                            viewModel = resultViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
