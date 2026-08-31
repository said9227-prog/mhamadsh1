package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.viewmodel.ActivityItem
import com.example.ui.viewmodel.ActivityType
import com.example.ui.viewmodel.AppViewModel
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onNavigateToCreateInvoice: () -> Unit,
    onNavigateToAddClient: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToClients: () -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()
    val overdueList by viewModel.overdueInstallments.collectAsState()
    
    var showQuickAddMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showOverdueDialog by remember { mutableStateOf(true) }

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    if (overdueList.isNotEmpty() && showOverdueDialog) {
        AlertDialog(
            onDismissRequest = { showOverdueDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تنبيه بالأقساط المستحقة!", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يوجد عدد ${overdueList.size} أقساط تجاوزت موعد الاستحقاق:")
                    overdueList.take(3).forEach { inst ->
                        val dateStr = com.example.util.DateTimeUtils.formatDateOnly(inst.dueDate)
                        Text(
                            "• العميل: ${inst.clientName} - ${inst.amount} ${inst.currency} (تاريخ الاستحقاق: $dateStr)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showOverdueDialog = false
                    onNavigateToClients()
                }) {
                    Text("عرض العملاء والأقساط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverdueDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim overlay behind FAB when menu is open
        AnimatedVisibility(
            visible = showQuickAddMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        showQuickAddMenu = false
                    }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Store Welcome Header
            item {
                StoreHeader(
                    storeName = settings.storeName,
                    onAboutClick = { showAboutDialog = true }
                )
            }

            // Financial Summaries Grid
            item {
                FinancialCardsGrid(
                    currency = settings.currency,
                    totalDebt = stats.totalDebt,
                    totalReceived = stats.totalReceived,
                    netBalance = stats.netBalance,
                    totalClients = stats.totalClients,
                    currencyDebts = stats.currencyDebts,
                    currencyPayments = stats.currencyPayments
                )
            }

            // Charts Section
            item {
                DashboardCharts(
                    currency = settings.currency,
                    totalDebt = stats.totalDebt,
                    totalReceived = stats.totalReceived,
                    currencyDebts = stats.currencyDebts,
                    currencyPayments = stats.currencyPayments
                )
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniStatCard(
                        title = "الفواتير",
                        value = "${stats.invoicesCount}",
                        icon = Icons.Default.Receipt,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvoices
                    )
                    MiniStatCard(
                        title = "المدفوعات",
                        value = "${stats.paymentsCount}",
                        icon = Icons.Default.Payments,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvoices
                    )
                    MiniStatCard(
                        title = "الأصناف",
                        value = "${stats.itemsCount}",
                        icon = Icons.Default.Inventory2,
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAddItem
                    )
                }
            }

            // Top Indebted Customers Section
            if (stats.topDebtors.isNotEmpty()) {
                item {
                    SectionHeader(title = "العملاء الأكثر مديونية", onSeeAll = onNavigateToClients)
                }
                items(stats.topDebtors) { debtor ->
                    DebtorListItem(debtor = debtor, currency = settings.currency)
                }
            }

            // Recent Transactions Activity List
            item {
                SectionHeader(title = "آخر العمليات والنشاطات", onSeeAll = onNavigateToInvoices)
            }
            if (stats.recentActivities.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات مسجلة حالياً.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(stats.recentActivities) { activity ->
                    ActivityListItem(activity = activity, currency = settings.currency)
                }
            }
        }

        // Quick Add Floating Menu
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .zIndex(10f)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = showQuickAddMenu,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        QuickAddActionItem(
                            text = "فاتورة جديدة",
                            icon = Icons.Default.PostAdd,
                            color = Color(0xFF4F46E5),
                            onClick = {
                                showQuickAddMenu = false
                                onNavigateToCreateInvoice()
                            }
                        )
                        QuickAddActionItem(
                            text = "إضافة عميل",
                            icon = Icons.Default.PersonAdd,
                            color = Color(0xFF10B981),
                            onClick = {
                                showQuickAddMenu = false
                                onNavigateToAddClient()
                            }
                        )
                        QuickAddActionItem(
                            text = "إضافة صنف",
                            icon = Icons.Default.AddBox,
                            color = Color(0xFFF59E0B),
                            onClick = {
                                showQuickAddMenu = false
                                onNavigateToAddItem()
                            }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showQuickAddMenu = !showQuickAddMenu },
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
                    modifier = Modifier.testTag("dashboard_quick_add_fab")
                ) {
                    Icon(
                        imageVector = if (showQuickAddMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "قائمة الإضافة السريعة"
                    )
                }
            }
        }
    }
}

@Composable
fun StoreHeader(
    storeName: String,
    onAboutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "مرحباً بك في",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // About App Icon Button
            Surface(
                onClick = onAboutClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "حول التطبيق",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "حول التطبيق",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialCardsGrid(
    currency: String,
    totalDebt: Double,
    totalReceived: Double,
    netBalance: Double,
    totalClients: Int,
    currencyDebts: Map<String, Double> = emptyMap(),
    currencyPayments: Map<String, Double> = emptyMap()
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val debtBg = if (isDark) Color(0xFF450A0A) else Color(0xFFFDF2F2)
    val debtText = if (isDark) Color(0xFFFECACA) else Color(0xFF9B1C1C)
    val debtBorder = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFDE2E2)

    val recBg = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
    val recText = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
    val recBorder = if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5)

    val balBg = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
    val balText = if (isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3)
    val balBorder = if (isDark) Color(0xFF312E81) else Color(0xFFE0E7FF)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Debts Outstanding Card (Large) - Bento Style with Currency Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = debtBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, debtBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي ديون العملاء",
                        color = debtText.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = debtText.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Breakdown by currencies
                val availableDebts = currencyDebts.filter { it.value > 0 }
                if (availableDebts.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        availableDebts.forEach { (curr, amt) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = curr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = debtText.copy(alpha = 0.9f)
                                )
                                Surface(
                                    color = debtText.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${FormatUtils.formatAmount(amt)} $curr",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = debtText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "${FormatUtils.formatAmount(totalDebt)} $currency",
                        color = debtText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "مبالغ مستحقة الدفع من $totalClients عملاء",
                    color = debtText.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Half width cards for Received and Net
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Received Card with Currency Details
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = recBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, recBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "إجمالي المقبوضات",
                            color = recText.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = recText.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val availablePayments = currencyPayments.filter { it.value > 0 }
                    if (availablePayments.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availablePayments.forEach { (curr, amt) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(curr.take(8), fontSize = 11.sp, color = recText.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                                    Text(FormatUtils.formatAmount(amt), fontSize = 12.sp, color = recText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${FormatUtils.formatAmount(totalReceived)} $currency",
                            color = recText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Net Balance Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = balBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, balBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "الرصيد العام",
                            color = balText.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = balText.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${FormatUtils.formatAmount(netBalance)} $currency",
                        color = balText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (netBalance > 0) "صافي ديون نشطة" else "لا توجد ديون معلقة",
                        color = balText.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCharts(
    currency: String,
    totalDebt: Double,
    totalReceived: Double,
    currencyDebts: Map<String, Double> = emptyMap(),
    currencyPayments: Map<String, Double> = emptyMap()
) {
    // List of active currencies to show
    val allCurrencies = remember(currencyDebts, currencyPayments) {
        val set = mutableSetOf<String>()
        set.addAll(currencyDebts.keys)
        set.addAll(currencyPayments.keys)
        if (set.isEmpty()) {
            listOf("الكل")
        } else {
            listOf("الكل") + set.toList()
        }
    }

    var selectedCurrencyIndex by remember { mutableStateOf(0) }
    val activeCurrency = allCurrencies.getOrElse(selectedCurrencyIndex) { "الكل" }

    val (currentDebt, currentReceived, displayCurr) = if (activeCurrency == "الكل") {
        Triple(totalDebt, totalReceived, currency)
    } else {
        Triple(currencyDebts[activeCurrency] ?: 0.0, currencyPayments[activeCurrency] ?: 0.0, activeCurrency)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "النسبة والتحليل المالي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (allCurrencies.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        allCurrencies.forEachIndexed { idx, currName ->
                            val isSelected = selectedCurrencyIndex == idx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCurrencyIndex = idx }
                            ) {
                                Text(
                                    text = currName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val total = (currentDebt + currentReceived).coerceAtLeast(1.0)
            val debtRatio = (currentDebt / total).toFloat()
            val receivedRatio = (currentReceived / total).toFloat()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart Canvas
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 35f
                        val sizeMin = size.minDimension - strokeWidth
                        
                        // Draw Debt Sweep (Crimson/Coral)
                        drawArc(
                            color = Color(0xFFEF4444),
                            startAngle = -90f,
                            sweepAngle = debtRatio * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            size = Size(sizeMin, sizeMin),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                        // Draw Received Sweep (Emerald)
                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = -90f + (debtRatio * 360f),
                            sweepAngle = receivedRatio * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            size = Size(sizeMin, sizeMin),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                    }
                    Text(
                        text = if (totalReceived + currentDebt > 0) String.format("%.0f%%", receivedRatio * 100) else "0%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Chart Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ChartLegendItem(
                        color = Color(0xFFEF4444),
                        label = "ديون غير محصلة",
                        percentage = String.format(Locale.US, "%.1f%%", debtRatio * 100),
                        value = "${FormatUtils.formatAmount(currentDebt)} $displayCurr"
                    )
                    ChartLegendItem(
                        color = Color(0xFF10B981),
                        label = "مبالغ مستلمة ومسددة",
                        percentage = String.format(Locale.US, "%.1f%%", receivedRatio * 100),
                        value = "${FormatUtils.formatAmount(currentReceived)} $displayCurr"
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String, percentage: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = percentage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MiniStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onSeeAll) {
            Text("عرض الكل", fontSize = 12.sp)
        }
    }
}

@Composable
fun DebtorListItem(debtor: Client, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = debtor.name.take(1),
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = debtor.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الهاتف: ${debtor.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${FormatUtils.formatAmount(debtor.balance)} $currency",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ActivityListItem(activity: ActivityItem, currency: String) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(activity.date))

    val color = if (activity.type == ActivityType.INVOICE) Color(0xFF4F46E5) else Color(0xFF10B981)
    val background = if (activity.type == ActivityType.INVOICE) Color(0xFFEEF2F6) else Color(0xFFECFDF5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activity.type == ActivityType.INVOICE) Icons.Default.Receipt else Icons.Default.Payments,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activity.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = "${if (activity.type == ActivityType.INVOICE) "+" else "-"}${FormatUtils.formatAmount(activity.amount)} $currency",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun QuickAddActionItem(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "تطبيق إدارة الحسابات والديون",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "الإصدار v1.0.0",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // App Purpose
                Text(
                    text = "وظيفة التطبيق الرئيسية:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "نظام مالي متكامل لإدارة حسابات العملاء، تسجيل الفواتير والمدفوعات، التصدير التلقائي لكشوفات الحسابات PDF، والتقارير المالية مع حماية البيانات والنسخ الاحتياطي.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Features List
                Text(
                    text = "أبرز مميزات التطبيق:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                val features = listOf(
                    "📊 متابعة ديون ومدفوعات العملاء بدقة وكشوفات تفصيلية.",
                    "🧾 إنشاء وتفاصيل الفواتير والطباعة بصيغة PDF منسقة.",
                    "🗓️ اختيار التاريخ بنظامين (جدول تقويم + خانات أرقام).",
                    "📂 النسخ الاحتياطي اليومي لكشوفات العملاء في Downloads.",
                    "🔒 أمان عالي برمز PIN وقفل حماية التطبيق."
                )

                features.forEach { feature ->
                    Text(
                        text = feature,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Design Rights Credit
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "حقوق التصميم والتطوير",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "حقوق التصميم من قبل شعيب العوني",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "جميع الحقوق محفوظة © 2026",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}
