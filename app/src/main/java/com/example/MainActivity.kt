package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.AppViewModelFactory

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(database)

        // Start Foreground Reminder Service for continuous background alarms
        com.example.service.ReminderForegroundService.startService(applicationContext)

        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(application, repository)
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                val appLocked by viewModel.appLocked.collectAsState()

                // Force Right-to-Left (RTL) direction as the entire application is in Arabic
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (appLocked) {
                        SecurityScreen(
                            viewModel = viewModel,
                            onUnlocked = { viewModel.unlockApp(viewModel.securityPin.value ?: "") }
                        )
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val baseRoute = currentRoute?.substringBefore('?')
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // Define top level navigation destinations
    val navigationItems = listOf(
        NavigationItem(
            route = "dashboard",
            title = "الحكيمي للأدوية والمستلزمات الطبية",
            selectedIcon = Icons.Default.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            testTag = "nav_dashboard"
        ),
        NavigationItem(
            route = "clients",
            title = "العملاء",
            selectedIcon = Icons.Default.People,
            unselectedIcon = Icons.Outlined.People,
            testTag = "nav_clients"
        ),
        NavigationItem(
            route = "invoices",
            title = "الفواتير",
            selectedIcon = Icons.Default.ReceiptLong,
            unselectedIcon = Icons.Outlined.ReceiptLong,
            testTag = "nav_invoices"
        ),
        NavigationItem(
            route = "reports",
            title = "التقارير",
            selectedIcon = Icons.Default.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
            testTag = "nav_reports"
        ),
        NavigationItem(
            route = "settings",
            title = "الإعدادات",
            selectedIcon = Icons.Default.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val currentNavItem = navigationItems.find { it.route == baseRoute }
            if (currentNavItem != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentNavItem.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("theme_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = if (isDarkMode) "تفعيل الوضع النهاري" else "تفعيل الوضع الليلي",
                                tint = if (isDarkMode) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            // Only show bottom navigation on top-level routes
            val shouldShowBottomBar = navigationItems.any { it.route == baseRoute }
            if (shouldShowBottomBar) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    navigationItems.forEach { item ->
                        val selected = baseRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (baseRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("dashboard") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dashboard destination
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCreateInvoice = { navController.navigate("create_invoice") },
                    onNavigateToAddClient = { navController.navigate("clients?openAdd=true") },
                    onNavigateToAddItem = { navController.navigate("items") },
                    onNavigateToInvoices = { navController.navigate("invoices") },
                    onNavigateToClients = { navController.navigate("clients") }
                )
            }

            // Clients portfolio list destination
            composable(
                route = "clients?openAdd={openAdd}",
                arguments = listOf(navArgument("openAdd") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { backStackEntry ->
                val openAdd = backStackEntry.arguments?.getBoolean("openAdd") ?: false
                ClientsScreen(
                    viewModel = viewModel,
                    initialShowAddDialog = openAdd,
                    onNavigateToCreateInvoiceForClient = { clientId -> navController.navigate("create_invoice?clientId=$clientId") },
                    onNavigateToRecordPaymentForClient = { clientId -> navController.navigate("client_statement/$clientId") },
                    onNavigateToClientStatement = { clientId -> navController.navigate("client_statement/$clientId") }
                )
            }

            // Items / Products inventory destination
            composable(
                route = "items?openAdd={openAdd}",
                arguments = listOf(navArgument("openAdd") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { backStackEntry ->
                val openAdd = backStackEntry.arguments?.getBoolean("openAdd") ?: false
                ItemScreen(
                    viewModel = viewModel,
                    initialShowAddDialog = openAdd
                )
            }

            // Invoices index listing
            composable("invoices") {
                InvoicesScreen(
                    viewModel = viewModel,
                    onNavigateToInvoiceDetails = { invoiceId -> navController.navigate("invoice_details/$invoiceId") }
                )
            }

            // Reports screen
            composable("reports") {
                ReportsScreen(viewModel = viewModel)
            }

            // Shop settings
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }

            // Create Invoice screen (Detailed or Quick)
            composable(
                route = "create_invoice?clientId={clientId}",
                arguments = listOf(navArgument("clientId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val clientIdArg = backStackEntry.arguments?.getInt("clientId")
                val clientId = if (clientIdArg == -1) null else clientIdArg
                CreateInvoiceScreen(
                    viewModel = viewModel,
                    initialClientId = clientId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToInvoiceDetails = { invoiceId ->
                        navController.navigate("invoice_details/$invoiceId") {
                            popUpTo("dashboard") { inclusive = false }
                        }
                    }
                )
            }

            // Bill invoice details receipt viewer
            composable(
                route = "invoice_details/{invoiceId}",
                arguments = listOf(navArgument("invoiceId") { type = NavType.IntType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getInt("invoiceId") ?: 0
                InvoiceDetailsScreen(
                    viewModel = viewModel,
                    invoiceId = invoiceId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Client ledger account statement
            composable(
                route = "client_statement/{clientId}",
                arguments = listOf(navArgument("clientId") { type = NavType.IntType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                ClientStatementScreen(
                    viewModel = viewModel,
                    clientId = clientId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)
