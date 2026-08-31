package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.horizontalScroll
import com.example.data.model.Client
import com.example.ui.viewmodel.AppViewModel
import com.example.util.ClientLoyaltyProfile
import com.example.util.CreditStatusLevel
import com.example.util.CreditUtils
import com.example.util.FormatUtils
import com.example.util.LoyaltyBadgeType
import com.example.util.PaymentLoyaltyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: AppViewModel,
    initialShowAddDialog: Boolean = false,
    onNavigateToCreateInvoiceForClient: (Int) -> Unit,
    onNavigateToRecordPaymentForClient: (Int) -> Unit,
    onNavigateToClientStatement: (Int) -> Unit
) {
    val context = LocalContext.current
    val clientsList by viewModel.clients.collectAsState()
    val invoicesList by viewModel.invoices.collectAsState()
    val paymentsList by viewModel.payments.collectAsState()
    val installmentsList by viewModel.installments.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    
    var showAddDialog by remember { mutableStateOf(initialShowAddDialog) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    var clientForInstallment by remember { mutableStateOf<Client?>(null) }
    var clientForCreditAlert by remember { mutableStateOf<Client?>(null) }
    var clientForOverdueReminder by remember { mutableStateOf<Pair<Client, ClientLoyaltyProfile>?>(null) }
    var clientForLoyaltyAppreciation by remember { mutableStateOf<Pair<Client, ClientLoyaltyProfile>?>(null) }
    var clientForBehaviorDetails by remember { mutableStateOf<Pair<Client, ClientLoyaltyProfile>?>(null) }

    // Calculate smart loyalty profiles for all clients
    val profilesMap = remember(clientsList, invoicesList, paymentsList, installmentsList, settings) {
        clientsList.associate { client ->
            client.id to PaymentLoyaltyUtils.analyzeClientBehavior(
                client = client,
                allInvoices = invoicesList,
                allPayments = paymentsList,
                allInstallments = installmentsList,
                settings = settings
            )
        }
    }

    // Filtered client list
    val filteredClients = remember(clientsList, searchQuery, selectedFilter, profilesMap) {
        clientsList.filter { client ->
            val matchesSearch = client.name.contains(searchQuery, ignoreCase = true) || 
                                client.phone.contains(searchQuery) ||
                                client.address.contains(searchQuery, ignoreCase = true)
            
            val profile = profilesMap[client.id]
            val matchesFilter = when (selectedFilter) {
                "الكل" -> true
                "⭐ الأوفياء" -> profile?.isLoyal == true
                "⚡ سريعو السداد" -> profile?.isFastPayer == true
                "✓ منتظمون بالسداد" -> profile?.isRegularPayer == true
                "🔄 سداد ثابت" -> profile?.hasFixedPattern == true
                "⚠️ متأخرون بالسداد" -> profile?.isOverdue == true
                "مميز" -> client.classification == "مميز" || client.classification == "VIP"
                "عادي" -> client.classification == "عادي"
                else -> true
            }
            
            matchesSearch && matchesFilter
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search & Filter Card
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث باسم العميل أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Smart Loyalty & Status Filter Chips (Horizontally scrollable)
            val filterOptions = listOf(
                "الكل",
                "⭐ الأوفياء",
                "⚡ سريعو السداد",
                "✓ منتظمون بالسداد",
                "🔄 سداد ثابت",
                "⚠️ متأخرون بالسداد",
                "مميز"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clients List
            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا يوجد عملاء يطابقون البحث والتصفية",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val clientTotalPaid = paymentsList.filter { it.clientId == client.id }.sumOf { it.amount }
                        val hasActiveReminder = installmentsList.any { it.clientId == client.id && !it.isPaid }
                        val profile = profilesMap[client.id]

                        ClientCard(
                            client = client,
                            currency = settings.currency,
                            totalPaid = clientTotalPaid,
                            hasActiveReminder = hasActiveReminder,
                            loyaltyProfile = profile,
                            onPinToggle = { viewModel.toggleClientPin(client) },
                            onEdit = { clientToEdit = client },
                            onDelete = { clientToDelete = client },
                            onAddInstallment = { clientForInstallment = client },
                            onCreditAlert = { clientForCreditAlert = client },
                            onOverdueReminder = if (profile != null && profile.isOverdue) {
                                { clientForOverdueReminder = Pair(client, profile) }
                            } else null,
                            onLoyaltyAppreciation = if (profile != null && (profile.isLoyal || profile.isFastPayer)) {
                                { clientForLoyaltyAppreciation = Pair(client, profile) }
                            } else null,
                            onShowBehaviorDetails = if (profile != null) {
                                { clientForBehaviorDetails = Pair(client, profile) }
                            } else null,
                            onAddInvoice = { onNavigateToCreateInvoiceForClient(client.id) },
                            onRecordPayment = { onNavigateToRecordPaymentForClient(client.id) },
                            onStatement = { onNavigateToClientStatement(client.id) },
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                context.startActivity(intent)
                            },
                            onWhatsApp = {
                                val msg = if (profile != null && profile.isOverdue) {
                                    profile.overdueNoticeMessage
                                } else {
                                    "مرحباً ${client.name}، نود تذكيركم بخصوص حسابكم التجاري لدى ${settings.storeName}..."
                                }
                                val url = "https://api.whatsapp.com/send?phone=${client.phone}&text=${Uri.encode(msg)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Client
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFFFFD700),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_client_fab")
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "إضافة عميل")
        }

        // Add Client Dialog
        if (showAddDialog) {
            ClientFormDialog(
                title = "إضافة عميل جديد",
                currency = settings.currency,
                onDismiss = { showAddDialog = false },
                onSave = { name, phone, address, email, notes, classification, initialBal, creditLimit, warningThreshold ->
                    viewModel.addClient(
                        name = name,
                        phone = phone,
                        address = address,
                        email = email,
                        notes = notes,
                        classification = classification,
                        initialBalance = initialBal,
                        creditLimit = creditLimit,
                        creditWarningThreshold = warningThreshold
                    )
                    showAddDialog = false
                    Toast.makeText(context, "تم إضافة العميل بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Client Dialog
        clientToEdit?.let { client ->
            ClientFormDialog(
                title = "تعديل بيانات العميل",
                client = client,
                currency = settings.currency,
                onDismiss = { clientToEdit = null },
                onSave = { name, phone, address, email, notes, classification, _, creditLimit, warningThreshold ->
                    viewModel.updateClient(
                        client.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            email = email,
                            notes = notes,
                            classification = classification,
                            creditLimit = creditLimit,
                            creditWarningThreshold = warningThreshold
                        )
                    )
                    clientToEdit = null
                    Toast.makeText(context, "تم تعديل بيانات العميل بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Credit Alert Quick Dialog
        clientForCreditAlert?.let { alertClient ->
            ClientCreditAlertDialog(
                client = alertClient,
                storeName = settings.storeName.ifBlank { "المتجر" },
                currency = settings.currency,
                onDismiss = { clientForCreditAlert = null }
            )
        }

        // Delete Confirmation Dialog
        clientToDelete?.let { client ->
            AlertDialog(
                onDismissRequest = { clientToDelete = null },
                title = { Text("تأكيد حذف العميل", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text("هل أنت متأكد من رغبتك في حذف العميل '${client.name}' نهائياً؟ سيؤدي ذلك إلى حذف سجلاته ولا يمكن التراجع عن هذه العملية.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteClient(client)
                            clientToDelete = null
                            Toast.makeText(context, "تم حذف العميل بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("نعم، احذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clientToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Schedule Installment Dialog
        clientForInstallment?.let { client ->
            AddInstallmentDialog(
                client = client,
                defaultCurrency = settings.currency,
                onDismiss = { clientForInstallment = null },
                onSave = { inst ->
                    viewModel.addInstallment(inst)
                    clientForInstallment = null
                    Toast.makeText(context, "تم جدولة القسط والتذكير بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Overdue Reminder Dialog (WhatsApp Notification)
        clientForOverdueReminder?.let { (client, profile) ->
            ClientOverdueReminderDialog(
                client = client,
                profile = profile,
                currency = settings.currency,
                onDismiss = { clientForOverdueReminder = null }
            )
        }

        // Loyalty Appreciation Dialog (WhatsApp Notification)
        clientForLoyaltyAppreciation?.let { (client, profile) ->
            ClientLoyaltyAppreciationDialog(
                client = client,
                profile = profile,
                currency = settings.currency,
                onDismiss = { clientForLoyaltyAppreciation = null }
            )
        }

        // Client Behavior & Loyalty Profile Analysis Dialog
        clientForBehaviorDetails?.let { (client, profile) ->
            ClientBehaviorProfileDialog(
                client = client,
                profile = profile,
                currency = settings.currency,
                onDismiss = { clientForBehaviorDetails = null },
                onSendOverdueReminder = if (profile.isOverdue) {
                    {
                        clientForBehaviorDetails = null
                        clientForOverdueReminder = Pair(client, profile)
                    }
                } else null,
                onSendAppreciation = if (profile.isLoyal || profile.isFastPayer) {
                    {
                        clientForBehaviorDetails = null
                        clientForLoyaltyAppreciation = Pair(client, profile)
                    }
                } else null
            )
        }
    }
}

@Composable
fun ClientCard(
    client: Client,
    currency: String,
    totalPaid: Double = 0.0,
    hasActiveReminder: Boolean = false,
    loyaltyProfile: ClientLoyaltyProfile? = null,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddInstallment: (() -> Unit)? = null,
    onCreditAlert: (() -> Unit)? = null,
    onOverdueReminder: (() -> Unit)? = null,
    onLoyaltyAppreciation: (() -> Unit)? = null,
    onShowBehaviorDetails: (() -> Unit)? = null,
    onAddInvoice: () -> Unit,
    onRecordPayment: () -> Unit,
    onStatement: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    var expandedActions by remember { mutableStateOf(false) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val balanceBg = if (client.balance > 0) {
        if (isDark) Color(0xFF450A0A) else Color(0xFFFDF2F2)
    } else if (client.balance < 0) {
        if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val balanceText = if (client.balance > 0) {
        if (isDark) Color(0xFFFECACA) else Color(0xFF9B1C1C)
    } else if (client.balance < 0) {
        if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val balanceBorder = if (client.balance > 0) {
        if (isDark) Color(0xFF7F1D1D) else Color(0xFFFDE2E2)
    } else if (client.balance < 0) {
        if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Client main profile row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (loyaltyProfile?.isLoyal == true || client.classification == "VIP" || client.classification == "مميز") Color(0xFFFEF3C7) 
                                else if (loyaltyProfile?.isOverdue == true) Color(0xFFFEE2E2)
                                else MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            )
                            .clickable { onShowBehaviorDetails?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (loyaltyProfile != null && loyaltyProfile.primaryBadge != LoyaltyBadgeType.NORMAL) loyaltyProfile.primaryBadge.iconEmoji else client.name.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (loyaltyProfile != null && loyaltyProfile.primaryBadge != LoyaltyBadgeType.NORMAL) 18.sp else 16.sp,
                            color = if (client.classification == "VIP" || client.classification == "مميز") Color(0xFFD97706) else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = client.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (client.classification == "مميز" || client.classification == "VIP") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = client.classification,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (hasActiveReminder) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(11.dp))
                                        Text("منبه ⏰", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }
                        if (client.phone.isNotEmpty()) {
                            Text(
                                text = client.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Pins, Alarm Bell & Menu Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    onAddInstallment?.let { onAddInst ->
                        IconButton(onClick = onAddInst) {
                            Icon(
                                imageVector = if (hasActiveReminder) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsActive,
                                contentDescription = if (hasActiveReminder) "تم تفعيل المنبه" else "ضبط المنبه والتذكير",
                                tint = if (hasActiveReminder) Color(0xFF059669) else Color(0xFF2563EB)
                            )
                        }
                    }
                    IconButton(onClick = onPinToggle) {
                        Icon(
                            imageVector = if (client.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "تثبيت",
                            tint = if (client.isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        var showDropdown by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "قائمة")
                        }
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            onShowBehaviorDetails?.let {
                                DropdownMenuItem(
                                    text = { Text("سلوك السداد والوفاء 📊") },
                                    leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF2563EB)) },
                                    onClick = {
                                        showDropdown = false
                                        it()
                                    }
                                )
                            }
                            if (onOverdueReminder != null) {
                                DropdownMenuItem(
                                    text = { Text("تنبيه التأخر بالسداد (واتساب) ⚠️") },
                                    leadingIcon = { Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = Color(0xFFDC2626)) },
                                    onClick = {
                                        showDropdown = false
                                        onOverdueReminder()
                                    }
                                )
                            }
                            if (onLoyaltyAppreciation != null) {
                                DropdownMenuItem(
                                    text = { Text("شكر وتقدير للعميل الوفي (واتساب) ⭐") },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706)) },
                                    onClick = {
                                        showDropdown = false
                                        onLoyaltyAppreciation()
                                    }
                                )
                            }
                            if (client.creditLimit > 0 && onCreditAlert != null) {
                                DropdownMenuItem(
                                    text = { Text("إرسال تنبيه الحد الائتماني") },
                                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFDC2626)) },
                                    onClick = {
                                        showDropdown = false
                                        onCreditAlert()
                                    }
                                )
                            }
                            onAddInstallment?.let {
                                DropdownMenuItem(
                                    text = { Text("جدولة قسط / تذكير") },
                                    leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                                    onClick = {
                                        showDropdown = false
                                        it()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("تعديل العميل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showDropdown = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف العميل") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showDropdown = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Smart Loyalty & Behavior Badges Bar
            if (loyaltyProfile != null && loyaltyProfile.allBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    loyaltyProfile.allBadges.forEach { badge ->
                        Surface(
                            color = Color(badge.bgHex),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(badge.colorHex).copy(alpha = 0.35f)),
                            modifier = Modifier.clickable { onShowBehaviorDetails?.invoke() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(badge.iconEmoji, fontSize = 11.sp)
                                Text(
                                    text = badge.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(badge.colorHex)
                                )
                            }
                        }
                    }

                    if (loyaltyProfile.isOverdue && loyaltyProfile.overdueDays > 0) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onOverdueReminder?.invoke() }
                        ) {
                            Text(
                                text = "تأخر ${loyaltyProfile.overdueDays} يوم ⚠️",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Balance Details Panel - Bento Style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(balanceBg, RoundedCornerShape(16.dp))
                    .border(androidx.compose.foundation.BorderStroke(1.dp, balanceBorder), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحالة المالية:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (client.balance > 0) {
                        Surface(
                            color = Color(0xFFDC2626), // Red container for debt
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "مدين",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "مدين بـ: ${FormatUtils.formatAmount(client.balance)} $currency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    } else if (client.balance < 0) {
                        Surface(
                            color = Color(0xFF059669), // Green container for credit
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "دائن",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "دائن بـ: ${FormatUtils.formatAmount(-client.balance)} $currency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFF059669),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "خالص",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "خالص الرصيد (0)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = balanceBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إجمالي المسدد: ${FormatUtils.formatAmount(totalPaid)} $currency",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "إجمالي المتبقي: ${FormatUtils.formatAmount(if (client.balance > 0) client.balance else 0.0)} $currency",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (client.balance > 0) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // --- Integrated Credit Limit System Widget ---
            if (client.creditLimit > 0) {
                val creditInfo = CreditUtils.getCreditStatusInfo(client)
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(creditInfo.containerColor, RoundedCornerShape(16.dp))
                        .border(androidx.compose.foundation.BorderStroke(1.dp, creditInfo.statusColor.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = when (creditInfo.level) {
                                    CreditStatusLevel.LIMIT_REACHED -> Icons.Default.Block
                                    CreditStatusLevel.CRITICAL_WARNING -> Icons.Default.Warning
                                    CreditStatusLevel.EARLY_WARNING -> Icons.Default.Info
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = creditInfo.statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "نظام الحد الائتماني:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = creditInfo.statusColor
                            )
                        }
                        Surface(
                            color = creditInfo.statusColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = creditInfo.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (creditInfo.usagePercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = creditInfo.statusColor,
                        trackColor = creditInfo.statusColor.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الحد: ${FormatUtils.formatAmount(client.creditLimit)} $currency",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (creditInfo.isBlocked && creditInfo.overageAmount > 0)
                                "تجاوز: +${FormatUtils.formatAmount(creditInfo.overageAmount)} $currency"
                            else
                                "المتبقي المتاح: ${FormatUtils.formatAmount(creditInfo.availableCredit)} $currency (${String.format(java.util.Locale.US, "%.0f", (100.0 - creditInfo.usagePercentage).coerceAtLeast(0.0))}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (creditInfo.isBlocked) Color(0xFFDC2626) else creditInfo.statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Quick Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Communications & Smart Alerts Panel
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (client.phone.isNotEmpty()) {
                        IconButton(
                            onClick = onCall,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onWhatsApp,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFDCF8C6), CircleShape)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = "واتساب", tint = Color(0xFF075E54), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onOverdueReminder != null) {
                        IconButton(
                            onClick = onOverdueReminder,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEE2E2), CircleShape)
                        ) {
                            Icon(Icons.Default.NotificationImportant, contentDescription = "تنبيه التأخر بالسداد", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onLoyaltyAppreciation != null && onOverdueReminder == null) {
                        IconButton(
                            onClick = onLoyaltyAppreciation,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEF3C7), CircleShape)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "شكر للعميل الوفي", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (client.creditLimit > 0 && onCreditAlert != null) {
                        IconButton(
                            onClick = onCreditAlert,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEE2E2), CircleShape)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "تنبيه ائتماني", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Transaction shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onStatement) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("كشف الحساب", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                    ) {
                        Text("سداد", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onAddInvoice,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                    ) {
                        Text("+ فاتورة", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ClientFormDialog(
    title: String,
    client: Client? = null,
    currency: String = "ريال",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, email: String, notes: String, classification: String, initialBalance: Double, creditLimit: Double, creditWarningThreshold: Double) -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }
    var classification by remember { mutableStateOf(client?.classification ?: "عادي") }
    var initialBalanceText by remember { mutableStateOf("") }
    var creditLimitText by remember {
        mutableStateOf(
            if (client != null && client.creditLimit > 0) {
                FormatUtils.formatAmount(client.creditLimit)
            } else ""
        )
    }
    var creditWarningThresholdText by remember {
        mutableStateOf(
            if (client != null && client.creditWarningThreshold > 0) {
                String.format(java.util.Locale.US, "%.0f", client.creditWarningThreshold)
            } else "80"
        )
    }

    var isError by remember { mutableStateOf(false) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }

    val parsedCreditLimit = creditLimitText.replace(",", "").toDoubleOrNull() ?: 0.0
    val parsedThreshold = creditWarningThresholdText.toDoubleOrNull() ?: 80.0

    if (showEditConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEditConfirmDialog = false },
            title = { Text("تأكيد تعديل العميل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حفظ التعديلات الجديدة لبيانات العميل؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showEditConfirmDialog = false
                        onSave(name, phone, address, email, notes, classification, 0.0, parsedCreditLimit, parsedThreshold)
                    }
                ) {
                    Text("نعم، حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; isError = false },
                        label = { Text("اسم العميل *") },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (client == null) {
                    item {
                        OutlinedTextField(
                            value = initialBalanceText,
                            onValueChange = { initialBalanceText = it },
                            label = { Text("الرصيد الافتتاحي (اختياري - مديونية سابقة)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // --- Credit Limit Settings ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "نظام الحد الائتماني للعميل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            OutlinedTextField(
                                value = creditLimitText,
                                onValueChange = { creditLimitText = it },
                                label = { Text("الحد الائتماني الأقصى ($currency)") },
                                placeholder = { Text("مثال: 500,000 (0 = بدون حد)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick preset amounts for credit limit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "100k" to "100000",
                                    "250k" to "250000",
                                    "500k" to "500000",
                                    "1M" to "1000000",
                                    "بدون حد" to "0"
                                ).forEach { (label, value) ->
                                    FilterChip(
                                        selected = creditLimitText == value,
                                        onClick = { creditLimitText = if (value == "0") "" else value },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }

                            if (parsedCreditLimit > 0) {
                                OutlinedTextField(
                                    value = creditWarningThresholdText,
                                    onValueChange = { creditWarningThresholdText = it },
                                    label = { Text("نسبة التنبيه المبكر (%)") },
                                    placeholder = { Text("الافتراضي: 80%") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (client != null) {
                                    val simulatedClient = client.copy(
                                        creditLimit = parsedCreditLimit,
                                        creditWarningThreshold = parsedThreshold
                                    )
                                    val status = CreditUtils.getCreditStatusInfo(simulatedClient)
                                    Surface(
                                        color = status.containerColor,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("حالة الائتمان الحالية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(status.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = status.statusColor)
                                            }
                                            Text(
                                                text = "المديونية: ${FormatUtils.formatAmount(client.balance)} $currency | نسبة الاستهلاك: ${String.format(java.util.Locale.US, "%.1f", status.usagePercentage)}%",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("تصنيف العميل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("عادي", "مميز").forEach { opt ->
                            val selected = classification == opt
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { classification = opt },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else if (client != null) {
                        showEditConfirmDialog = true
                    } else {
                        val initialBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                        onSave(name, phone, address, email, notes, classification, initialBal, parsedCreditLimit, parsedThreshold)
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ClientCreditAlertDialog(
    client: Client,
    storeName: String,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val creditInfo = CreditUtils.getCreditStatusInfo(client)
    val alertMessage = remember(client, storeName, currency) {
        CreditUtils.formatCreditAlertMessage(client, storeName, currency)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = creditInfo.statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "تنبيه الحد الائتماني للعميل",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Client Credit Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = creditInfo.containerColor),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, creditInfo.statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(client.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                color = creditInfo.statusColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = creditInfo.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (creditInfo.usagePercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = creditInfo.statusColor,
                            trackColor = creditInfo.statusColor.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "المديونية: ${FormatUtils.formatAmount(client.balance)} $currency",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الحد: ${FormatUtils.formatAmount(client.creditLimit)} $currency",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = creditInfo.description,
                            fontSize = 11.sp,
                            color = creditInfo.statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Message Preview Box
                Text("معاينة نص الرسالة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = alertMessage,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (client.phone.isNotBlank()) {
                        val url = "https://api.whatsapp.com/send?phone=${client.phone}&text=${Uri.encode(alertMessage)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "لا يوجد رقم هاتف مسجل للعميل", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال عبر واتساب")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Credit Alert", alertMessage)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ نص التنبيه إلى الحافظة", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ")
                }
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        }
    )
}

@Composable
fun AddInstallmentDialog(
    client: Client,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.Installment) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Default to EXACT CURRENT DATE AND TIME
    val initialCalendar = remember {
        java.util.Calendar.getInstance()
    }

    var selectedCalendar by remember { mutableStateOf(initialCalendar) }
    var amountText by remember { mutableStateOf(if (client.balance > 0) client.balance.toString() else "") }
    var currency by remember { mutableStateOf(defaultCurrency.ifBlank { "الريال اليمني" }) }
    var notes by remember { mutableStateOf("") }
    var daysCount by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf("NONE") }

    val currencies = listOf("الريال اليمني", "الريال السعودي", "الدولار الأمريكي")
    val recurrenceOptions = listOf(
        "NONE" to "بدون تكرار",
        "DAILY" to "يومياً",
        "WEEKLY" to "أسبوعياً",
        "MONTHLY" to "شهرياً"
    )

    fun updateCalendarDays(days: Long) {
        val cal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, days.toInt())
            set(java.util.Calendar.HOUR_OF_DAY, selectedCalendar.get(java.util.Calendar.HOUR_OF_DAY))
            set(java.util.Calendar.MINUTE, selectedCalendar.get(java.util.Calendar.MINUTE))
        }
        selectedCalendar = cal
    }

    fun openDatePicker() {
        val year = selectedCalendar.get(java.util.Calendar.YEAR)
        val month = selectedCalendar.get(java.util.Calendar.MONTH)
        val day = selectedCalendar.get(java.util.Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(context, { _, y, m, d ->
            val newCal = (selectedCalendar.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.YEAR, y)
                set(java.util.Calendar.MONTH, m)
                set(java.util.Calendar.DAY_OF_MONTH, d)
            }
            selectedCalendar = newCal
        }, year, month, day).show()
    }

    fun openTimePicker() {
        val hour = selectedCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = selectedCalendar.get(java.util.Calendar.MINUTE)

        android.app.TimePickerDialog(context, { _, h, m ->
            val newCal = (selectedCalendar.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, h)
                set(java.util.Calendar.MINUTE, m)
            }
            selectedCalendar = newCal
        }, hour, minute, false).show()
    }

    val formattedDateStr = remember(selectedCalendar) {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale("ar"))
        sdf.format(selectedCalendar.time)
    }

    val formattedTimeStr = remember(selectedCalendar) {
        val h = selectedCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val m = selectedCalendar.get(java.util.Calendar.MINUTE)
        val amPm = if (h >= 12) "مساءً" else "صباحاً"
        val h12 = if (h % 12 == 0) 12 else h % 12
        String.format(java.util.Locale.getDefault(), "%02d:%02d %s", h12, m, amPm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ضبط التذكير والمنبه للعميل", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("العميل: ${client.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                // Display total client balance
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي الدين على العميل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${FormatUtils.formatAmount(client.balance)} $currency",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ القسط (اختياري - يترك فارغاً للتذكير العام)") },
                    placeholder = { Text("المبلغ المتبقي: ${FormatUtils.formatAmount(client.balance)}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("العملة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    currencies.forEach { curr ->
                        FilterChip(
                            selected = currency == curr,
                            onClick = { currency = curr },
                            label = { Text(curr, fontSize = 11.sp) }
                        )
                    }
                }

                // Date and Time picker section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("موعد المنبه (التاريخ والوقت الحالي افتراضياً):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(formattedDateStr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(onClick = { openDatePicker() }) {
                                Text("تغيير التاريخ", fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(formattedTimeStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(onClick = { openTimePicker() }) {
                                Text("تحديد الوقت ⏰", fontSize = 11.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = daysCount,
                    onValueChange = {
                        daysCount = it
                        if (it.isBlank()) {
                            selectedCalendar = java.util.Calendar.getInstance()
                        } else {
                            it.toLongOrNull()?.let { d -> updateCalendarDays(d) }
                        }
                    },
                    label = { Text("أو اكتب عدد الأيام من الآن (اختياري - مثلاً 30)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("التكرار الدوري:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    recurrenceOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = recurrence == key,
                            onClick = { recurrence = key },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التذكير") },
                    placeholder = { Text("تذكير بموعد سداد القسط للعميل ${client.name}") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: if (client.balance > 0) client.balance else 0.0
                    onSave(
                        com.example.data.model.Installment(
                            clientId = client.id,
                            clientName = client.name,
                            amount = amt,
                            currency = currency,
                            dueDate = selectedCalendar.timeInMillis,
                            recurrence = recurrence,
                            notes = notes.ifBlank { "تذكير بقسط للعميل ${client.name}" }
                        )
                    )
                }
            ) {
                Text("تأكيد وحفظ المنبه ⏰")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ClientOverdueReminderDialog(
    client: Client,
    profile: ClientLoyaltyProfile,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editableMessage by remember { mutableStateOf(profile.overdueNoticeMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = Color(0xFFDC2626))
                Text("تنبيه التأخر بالسداد ⚠️", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "العميل: ${client.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "المبلغ المستحق: ${FormatUtils.formatAmount(client.balance)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626)
                        )
                        if (profile.overdueDays > 0) {
                            Text(
                                text = "مدة التأخير: ${profile.overdueDays} يوم عن موعد الاستحقاق",
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }

                Text("نص رسالة التذكير (واتساب):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = editableMessage,
                    onValueChange = { editableMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "سيتم فتح تطبيق واتساب مباشرة مع إدراج الرسالة تلقائياً.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val url = "https://api.whatsapp.com/send?phone=${client.phone}&text=${Uri.encode(editableMessage)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال عبر واتساب", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ClientLoyaltyAppreciationDialog(
    client: Client,
    profile: ClientLoyaltyProfile,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editableMessage by remember { mutableStateOf(profile.loyaltyAppreciationMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706))
                Text("رسالة شكر وتقدير للعميل الوفي ⭐", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "العميل: ${client.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "عدد الفواتير: ${profile.totalInvoices} فاتورة | إجمالي التعامل: ${FormatUtils.formatAmount(profile.totalInvoicedAmount)} $currency",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                        if (profile.isFastPayer) {
                            Text(
                                text = "⚡ عميل سريع السداد (معدل السداد: ${String.format(java.util.Locale.US, "%.1f", profile.averageSettlementDays)} يوم)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                Text("نص رسالة الشكر والتقدير (واتساب):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = editableMessage,
                    onValueChange = { editableMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "تعزيز ولاء العملاء المتميزين برسائل الشكر والتقدير الدورية.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val url = "https://api.whatsapp.com/send?phone=${client.phone}&text=${Uri.encode(editableMessage)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال عبر واتساب", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ClientBehaviorProfileDialog(
    client: Client,
    profile: ClientLoyaltyProfile,
    currency: String,
    onDismiss: () -> Unit,
    onSendOverdueReminder: (() -> Unit)? = null,
    onSendAppreciation: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("تحليل سلوك السداد والولاء 📊", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Client Header Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = client.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "هاتف: ${client.phone.ifBlank { "غير مسجل" }} | العنوان: ${client.address.ifBlank { "غير محدد" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // All Badges
                Text("الشارات والتصنيفات الذكية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profile.allBadges.forEach { badge ->
                        Surface(
                            color = Color(badge.bgHex),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(badge.colorHex).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(badge.iconEmoji, fontSize = 12.sp)
                                Text(
                                    text = badge.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(badge.colorHex)
                                )
                            }
                        }
                    }
                }

                // Behavior Summary Details Grid
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي الفواتير:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${profile.totalInvoices} فاتورة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المشتريات:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${FormatUtils.formatAmount(profile.totalInvoicedAmount)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المسدد:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${FormatUtils.formatAmount(profile.totalPaidAmount)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الرصيد المتبقي:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${FormatUtils.formatAmount(client.balance)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (client.balance > 0) Color(0xFFDC2626) else Color(0xFF059669))
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("متوسط سرعة السداد:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (profile.averageSettlementDays > 0) "${String.format(java.util.Locale.US, "%.1f", profile.averageSettlementDays)} يوم" else "فوري / سداد مباشر",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.isFastPayer) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (profile.isOverdue) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("حالة التأخر:", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                Text("متأخر ${profile.overdueDays} يوم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                        if (profile.hasFixedPattern) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("نمط السداد:", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                Text("سداد ثابت ودوري", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                        }
                    }
                }

                // Action buttons inside dialog
                if (onSendOverdueReminder != null) {
                    Button(
                        onClick = onSendOverdueReminder,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationImportant, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال تذكير تأخر بالسداد")
                    }
                }

                if (onSendAppreciation != null) {
                    Button(
                        onClick = onSendAppreciation,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال شكر وتقدير للعميل")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

