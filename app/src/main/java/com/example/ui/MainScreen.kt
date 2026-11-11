package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.StringsHelper
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.*
import com.example.ui.theme.NavyPrimary

enum class AppScreen {
    MAIN_NAV,
    SETTINGS
}

enum class NavTab(
    val titleKey: String,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD("nav_dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    POS("nav_pos", Icons.Default.PointOfSale, "tab_pos"),
    PRODUCTS("nav_products", Icons.Default.Inventory2, "tab_products"),
    REPORTS("nav_reports", Icons.Default.Assessment, "tab_reports"),
    MULTI_DEVICE("nav_multidevice", Icons.Default.Phonelink, "tab_multidevice")
}

@Composable
fun MainScreen(
    viewModel: KasirViewModel,
    onRequireLogin: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_NAV) }
    var selectedTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    val activeReceipt by viewModel.activeReceipt.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    // Global Receipt Modal
    if (activeReceipt != null) {
        ReceiptDialog(
            transaction = activeReceipt!!,
            storeProfile = storeProfile,
            onDismiss = { viewModel.closeReceipt() },
            onPrintClick = { viewModel.printActiveReceipt() },
            isPrinting = isPrinting
        )
    }

    when (currentScreen) {
        AppScreen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MAIN_NAV },
                onRequireLogin = onRequireLogin
            )
        }
        AppScreen.MAIN_NAV -> {
            BackHandler(enabled = selectedTab != NavTab.DASHBOARD) {
                selectedTab = NavTab.DASHBOARD
            }

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        NavTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = StringsHelper.get(tab.titleKey, language)
                                    )
                                },
                                label = {
                                    Text(
                                        text = StringsHelper.get(tab.titleKey, language),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavyPrimary,
                                    selectedTextColor = NavyPrimary,
                                    indicatorColor = NavyPrimary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    when (selectedTab) {
                        NavTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToPos = { selectedTab = NavTab.POS },
                            onNavigateToProducts = { selectedTab = NavTab.PRODUCTS },
                            onNavigateToReports = { selectedTab = NavTab.REPORTS },
                            onNavigateToMultiDevice = { selectedTab = NavTab.MULTI_DEVICE },
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                        NavTab.POS -> PosScreen(
                            viewModel = viewModel,
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                        NavTab.PRODUCTS -> ProductScreen(
                            viewModel = viewModel,
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                        NavTab.REPORTS -> ReportScreen(
                            viewModel = viewModel,
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                        NavTab.MULTI_DEVICE -> MultiDeviceScreen(
                            viewModel = viewModel,
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                            onRequireLogin = onRequireLogin
                        )
                    }
                }
            }
        }
    }
}
