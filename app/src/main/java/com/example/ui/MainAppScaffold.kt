package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.viewmodel.EVLabViewModel

enum class AppScreen(
    val titleEn: String,
    val titleFa: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", "داشبورد", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    CONFIG("Config", "تنظیمات", Icons.Filled.Tune, Icons.Outlined.Tune),
    INVERTER("Inverter", "اینورتر", Icons.Filled.ElectricBolt, Icons.Outlined.ElectricBolt),
    OPTIMIZATION("Motor & Gear", "موتور و دنده", Icons.Filled.Speed, Icons.Outlined.Speed),
    REPORT("Report", "گزارش", Icons.Filled.Assessment, Icons.Outlined.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: EVLabViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    val isFa = uiState.isFarsiPreferred

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EV Powertrain Lab",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isFa) "شبیه‌ساز پیشرانه و اینورتر خودرو برقی" else "Modular EV Digital Twin & Inverter Suite",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Button
                    OutlinedButton(
                        onClick = { viewModel.toggleLanguage() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_toggle_language")
                    ) {
                        Text(
                            text = if (isFa) "FA / EN" else "EN / FA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.titleEn
                            )
                        },
                        label = {
                            Text(
                                text = if (isFa) screen.titleFa else screen.titleEn,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onRunSimulation = { viewModel.runFullSimulation() },
                    onScrubIndex = { viewModel.setScrubberIndex(it) },
                    onNavigateToInverter = { currentScreen = AppScreen.INVERTER }
                )

                AppScreen.CONFIG -> ConfigurationScreen(
                    uiState = uiState,
                    onUpdateConfig = { updater -> viewModel.updateConfig(updater) },
                    onApplyAndSimulate = {
                        viewModel.runFullSimulation()
                        currentScreen = AppScreen.DASHBOARD
                    },
                    onResetToDefault = { viewModel.selectPreset("emoto_12s14p") }
                )

                AppScreen.INVERTER -> InverterSuiteScreen(
                    uiState = uiState,
                    onUpdateInverterInput = { updater -> viewModel.updateInverterInput(updater) },
                    onUpdateTargetRange = { km -> viewModel.updateTargetRange(km) }
                )

                AppScreen.OPTIMIZATION -> MotorOptimizationScreen(
                    uiState = uiState,
                    onRunOptimization = { viewModel.runGearOptimizationSweep() }
                )

                AppScreen.REPORT -> ReportExportScreen(
                    uiState = uiState,
                    onGenerateReportText = { viewModel.generateAuditReport() },
                    onGenerateCsvText = { viewModel.exportTelemetryCsv() }
                )
            }
        }
    }
}
