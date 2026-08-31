package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Client
import com.example.data.model.Item
import com.example.ui.viewmodel.AppViewModel
import com.example.util.InvoiceCreditCheckResult
import com.example.util.CreditStatusLevel
import com.example.util.CreditUtils
import com.example.util.FormatUtils
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateInvoiceScreen(
    viewModel: AppViewModel,
    initialClientId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToInvoiceDetails: (Int) -> Unit
) {
    BackHandler {
        onNavigateBack()
    }
    val context = LocalContext.current
    val clientsList by viewModel.clients.collectAsState()
    val itemsList by viewModel.items.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    // Screen tabs: Detailed vs Quick
    var invoiceTypeTab by remember { mutableStateOf(0) } // 0 = Detailed, 1 = Quick

    // Quick Invoice Fields
    var quickClient by remember { mutableStateOf<Client?>(null) }
    var quickDescription by remember { mutableStateOf("") }
    var quickAmountStr by remember { mutableStateOf("") }
    var quickClientDropdownExpanded by remember { mutableStateOf(false) }
    var quickClientSearchText by remember { mutableStateOf("") }
    var selectedInvoiceCurrency by remember { mutableStateOf(settings.currency.ifBlank { "الريال اليمني" }) }

    // Detailed Invoice States (connected to ViewModel)
    val selectedClient by viewModel.selectedClient.collectAsState()
    val cart by viewModel.invoiceCart.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val notes by viewModel.invoiceNotes.collectAsState()

    // Top-level calculations for detailed invoices
    val subtotal = cart.sumOf { it.customPrice * it.quantity }
    val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmt = afterDiscount * (taxRate / 100.0)
    val grandTotal = afterDiscount + taxAmt

    // SMS integration states
    var sendSmsOnSave by remember { mutableStateOf(false) }
    var showSmsConfirmDialog by remember { mutableStateOf(false) }
    var smsClientPhone by remember { mutableStateOf("") }
    var smsClientName by remember { mutableStateOf("") }
    var smsMessageContent by remember { mutableStateOf("") }
    var navigateBackOnSmsDismiss by remember { mutableStateOf(false) }
    var navigateToDetailsOnSmsDismiss by remember { mutableStateOf<Int?>(null) }

    // Credit Limit Interception State
    var creditLimitDialogData by remember { mutableStateOf<CreditDialogData?>(null) }

    // Autocomplete states for detailed client selection
    var clientSearchText by remember { mutableStateOf("") }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    // Autocomplete states for detailed items selection
    var itemSearchText by remember { mutableStateOf("") }
    var itemDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-select client if initialClientId is provided
    LaunchedEffect(initialClientId, clientsList) {
        if (initialClientId != null && initialClientId > 0) {
            val client = clientsList.find { it.id == initialClientId }
            if (client != null) {
                viewModel.setInvoiceClient(client)
                quickClient = client
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle choosing invoice type
        item {
            TabRow(
                selectedTabIndex = invoiceTypeTab,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(1.dp)
            ) {
                Tab(
                    selected = invoiceTypeTab == 0,
                    onClick = { invoiceTypeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Receipt, contentDescription = null)
                            Text("📦 فاتورة بالأصناف", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = invoiceTypeTab == 1,
                    onClick = { invoiceTypeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Text("⚡ فاتورة سريعة", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        if (invoiceTypeTab == 1) {
            // --- QUICK INVOICE LAYOUT ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("إنشاء فاتورة سريعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Client Select with Dropdown Search
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = quickClient?.name ?: quickClientSearchText,
                                onValueChange = {
                                    quickClientSearchText = it
                                    quickClient = null
                                    quickClientDropdownExpanded = true
                                },
                                label = { Text("اختر العميل *") },
                                trailingIcon = {
                                    IconButton(onClick = { quickClientDropdownExpanded = !quickClientDropdownExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val matchingClients = clientsList.filter {
                                it.name.contains(quickClientSearchText, ignoreCase = true) ||
                                it.phone.contains(quickClientSearchText, ignoreCase = true) ||
                                it.address.contains(quickClientSearchText, ignoreCase = true)
                            }

                            DropdownMenu(
                                expanded = quickClientDropdownExpanded && matchingClients.isNotEmpty(),
                                onDismissRequest = { quickClientDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                matchingClients.forEach { c ->
                                    val creditBadge = if (c.creditLimit > 0) " | حد: ${FormatUtils.formatAmount(c.creditLimit)}" else ""
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (رصيد: ${FormatUtils.formatAmount(c.balance)}$creditBadge)") },
                                        onClick = {
                                            quickClient = c
                                            quickClientSearchText = c.name
                                            quickClientDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Credit Status Mini-Banner for Selected Quick Client
                        quickClient?.let { qc ->
                            if (qc.creditLimit > 0) {
                                InvoiceCreditInfoBanner(client = qc, proposedInvoiceAmount = quickAmountStr.toDoubleOrNull() ?: 0.0)
                            }
                        }

                        // Description Field
                        OutlinedTextField(
                            value = quickDescription,
                            onValueChange = { quickDescription = it },
                            label = { Text("الوصف والخدمات المقدمة (تلقائياً: شراء بضاعة)") },
                            placeholder = { Text("مثال: خدمات تنظيف، شراء بضاعة، إلخ") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Currency Selector for Quick Invoice
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("عملة الفاتورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "الريال اليمني" to "ر.ي",
                                    "الريال السعودي" to "ر.س",
                                    "الدولار الأمريكي" to "$"
                                ).forEach { (curr, sym) ->
                                    val isSel = selectedInvoiceCurrency == curr
                                    Surface(
                                        onClick = { selectedInvoiceCurrency = curr },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(sym, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(curr, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Amount Field
                        OutlinedTextField(
                            value = quickAmountStr,
                            onValueChange = { quickAmountStr = it },
                            label = { Text("المبلغ الإجمالي ($selectedInvoiceCurrency) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // SMS Option Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sendSmsOnSave = !sendSmsOnSave }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = sendSmsOnSave,
                                onCheckedChange = { sendSmsOnSave = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إرسال تفاصيل الفاتورة عبر SMS للعميل تلقائياً بعد الحفظ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Action Save
                        Button(
                            onClick = {
                                val client = quickClient
                                val desc = if (quickDescription.trim().isEmpty()) "شراء بضاعة" else quickDescription.trim()
                                val amt = quickAmountStr.toDoubleOrNull()
                                if (client == null) {
                                    Toast.makeText(context, "الرجاء اختيار عميل!", Toast.LENGTH_SHORT).show()
                                } else if (amt == null || amt <= 0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ صحيح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val checkResult = CreditUtils.checkInvoiceCredit(client, amt)
                                    if (checkResult.isExceeded) {
                                        creditLimitDialogData = CreditDialogData(
                                            client = client,
                                            invoiceAmount = amt,
                                            checkResult = checkResult,
                                            onConfirmOverride = { authorizer, reason ->
                                                viewModel.saveQuickInvoice(
                                                    client = client,
                                                    description = desc,
                                                    amount = amt,
                                                    currency = selectedInvoiceCurrency,
                                                    isCreditOverride = true,
                                                    overrideAuthorizer = authorizer,
                                                    overrideReason = reason,
                                                    overrideAmount = checkResult.overageAmount
                                                ) {
                                                    Toast.makeText(context, "تم حفظ الفاتورة السريعة بتجاوز استثنائي مصرح!", Toast.LENGTH_LONG).show()
                                                    if (sendSmsOnSave) {
                                                        val nextNum = settings.lastInvoiceNumber + 1
                                                        val invoiceNumber = "${settings.invoicePrefix}$nextNum"
                                                        val remBalance = client.balance + amt
                                                        smsClientPhone = client.phone
                                                        smsClientName = client.name
                                                        smsMessageContent = "عميلنا العزيز ${client.name}، تم إضافة فاتورة سريعة رقم: $invoiceNumber بقيمة: ${FormatUtils.formatAmount(amt)} $selectedInvoiceCurrency. الرصيد المتبقي الإجمالي المستحق: ${FormatUtils.formatAmount(remBalance)} $selectedInvoiceCurrency. شكراً لتعاملكم معنا - ${settings.storeName}"
                                                        navigateBackOnSmsDismiss = true
                                                        navigateToDetailsOnSmsDismiss = null
                                                        showSmsConfirmDialog = true
                                                    } else {
                                                        onNavigateBack()
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        viewModel.saveQuickInvoice(client, desc, amt, selectedInvoiceCurrency) {
                                            Toast.makeText(context, "تم حفظ الفاتورة السريعة بنجاح!", Toast.LENGTH_LONG).show()
                                            if (sendSmsOnSave) {
                                                val nextNum = settings.lastInvoiceNumber + 1
                                                val invoiceNumber = "${settings.invoicePrefix}$nextNum"
                                                val remBalance = client.balance + amt
                                                smsClientPhone = client.phone
                                                smsClientName = client.name
                                                smsMessageContent = "عميلنا العزيز ${client.name}، تم إضافة فاتورة سريعة رقم: $invoiceNumber بقيمة: ${FormatUtils.formatAmount(amt)} $selectedInvoiceCurrency. الرصيد المتبقي الإجمالي المستحق: ${FormatUtils.formatAmount(remBalance)} $selectedInvoiceCurrency. شكراً لتعاملكم معنا - ${settings.storeName}"
                                                navigateBackOnSmsDismiss = true
                                                navigateToDetailsOnSmsDismiss = null
                                                showSmsConfirmDialog = true
                                            } else {
                                                onNavigateBack()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("حفظ الفاتورة السريعة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // --- DETAILED INVOICE WITH ITEMS LAYOUT ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("تفاصيل الفاتورة والأصناف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Client Select with Dropdown Search
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedClient?.name ?: clientSearchText,
                                onValueChange = {
                                    clientSearchText = it
                                    viewModel.setInvoiceClient(null)
                                    clientDropdownExpanded = true
                                },
                                label = { Text("اختر العميل لربطه بالفاتورة *") },
                                trailingIcon = {
                                    IconButton(onClick = { clientDropdownExpanded = !clientDropdownExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val matchingClients = clientsList.filter {
                                it.name.contains(clientSearchText, ignoreCase = true) ||
                                it.phone.contains(clientSearchText, ignoreCase = true) ||
                                it.address.contains(clientSearchText, ignoreCase = true)
                            }

                            DropdownMenu(
                                expanded = clientDropdownExpanded && matchingClients.isNotEmpty(),
                                onDismissRequest = { clientDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                matchingClients.forEach { c ->
                                    val creditBadge = if (c.creditLimit > 0) " | حد: ${FormatUtils.formatAmount(c.creditLimit)}" else ""
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (الرصيد: ${FormatUtils.formatAmount(c.balance)}$creditBadge)") },
                                        onClick = {
                                            viewModel.setInvoiceClient(c)
                                            clientSearchText = c.name
                                            clientDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Credit Status Mini-Banner for Selected Detailed Client
                        selectedClient?.let { sc ->
                            if (sc.creditLimit > 0) {
                                InvoiceCreditInfoBanner(client = sc, proposedInvoiceAmount = grandTotal)
                            }
                        }

                        // Currency Selector for Detailed Invoice
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("عملة الفاتورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "الريال اليمني" to "ر.ي",
                                    "الريال السعودي" to "ر.س",
                                    "الدولار الأمريكي" to "$"
                                ).forEach { (curr, sym) ->
                                    val isSel = selectedInvoiceCurrency == curr
                                    Surface(
                                        onClick = { selectedInvoiceCurrency = curr },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(sym, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(curr, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Search & Add Item autocomplete block
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = itemSearchText,
                                onValueChange = {
                                    itemSearchText = it
                                    itemDropdownExpanded = true
                                },
                                label = { Text("ابحث عن صنف لإضافته للفاتورة") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val matchingItems = itemsList.filter {
                                it.name.contains(itemSearchText, ignoreCase = true) || it.barcode.contains(itemSearchText)
                            }

                            DropdownMenu(
                                expanded = itemDropdownExpanded && itemSearchText.isNotEmpty() && matchingItems.isNotEmpty(),
                                onDismissRequest = { itemDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                matchingItems.forEach { i ->
                                    DropdownMenuItem(
                                        text = { Text("${i.name} (السعر: ${i.sellingPrice} | متوفر: ${i.quantity})") },
                                        onClick = {
                                            viewModel.addItemToCart(i)
                                            itemSearchText = ""
                                            itemDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cart Items Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أصناف السلة المختارة:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${cart.size} أصناف", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Items List
            if (cart.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("السلة فارغة. الرجاء البحث وإضافة الأصناف أعلاه.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(cart) { cartItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cartItem.item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("سعر الوحدة:", fontSize = 11.sp, color = Color.Gray)
                                    // Custom price editor
                                    var customPriceText by remember(cartItem.customPrice) { mutableStateOf(cartItem.customPrice.toString()) }
                                    Box(modifier = Modifier.width(70.dp)) {
                                        BasicTextField(
                                            value = customPriceText,
                                            onValueChange = {
                                                customPriceText = it
                                                val amt = it.toDoubleOrNull()
                                                if (amt != null) {
                                                    viewModel.updateCartItemPrice(cartItem, amt)
                                                }
                                            },
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                    Text(selectedInvoiceCurrency, fontSize = 11.sp)
                                }
                            }

                            // Qty buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(cartItem, cartItem.quantity - 1) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                 ) {
                                    Icon(Icons.Default.Remove, contentDescription = "نقص", modifier = Modifier.size(14.dp))
                                }
                                Text("${cartItem.quantity}", fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(cartItem, cartItem.quantity + 1) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(14.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = FormatUtils.formatAmount(cartItem.customPrice * cartItem.quantity),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.removeCartItem(cartItem) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف صنف", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Calculations Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val subtotal = cart.sumOf { it.customPrice * it.quantity }
                        var discountStr by remember { mutableStateOf(discount.toString()) }
                        var taxRateStr by remember { mutableStateOf(taxRate.toString()) }

                        // Subtotal
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع الفرعي:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${FormatUtils.formatAmount(subtotal)} $selectedInvoiceCurrency", fontWeight = FontWeight.Bold)
                        }

                        // Discount Fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الخصم المالي:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = discountStr,
                                onValueChange = {
                                    discountStr = it
                                    val amt = it.toDoubleOrNull() ?: 0.0
                                    viewModel.setDiscount(amt)
                                },
                                modifier = Modifier.width(100.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Tax Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الضريبة (%):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = taxRateStr,
                                onValueChange = {
                                    taxRateStr = it
                                    val rate = it.toDoubleOrNull() ?: 0.0
                                    viewModel.setTaxRate(rate)
                                },
                                modifier = Modifier.width(100.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Divider()

                        // Calculations
                        val discountAmt = discount
                        val afterDiscount = (subtotal - discountAmt).coerceAtLeast(0.0)
                        val taxAmt = afterDiscount * (taxRate / 100.0)
                        val grandTotal = afterDiscount + taxAmt

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ضريبة القيمة المضافة:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("${FormatUtils.formatAmount(taxAmt)} $selectedInvoiceCurrency", fontSize = 12.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع النهائي المطلق:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            Text("${FormatUtils.formatAmount(grandTotal)} $selectedInvoiceCurrency", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Invoice Notes field
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.setInvoiceNotes(it) },
                    label = { Text("ملاحظات الفاتورة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            // SMS Option Checkbox for Detailed Invoices
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sendSmsOnSave = !sendSmsOnSave }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = sendSmsOnSave,
                        onCheckedChange = { sendSmsOnSave = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال تفاصيل الفاتورة عبر SMS للعميل تلقائياً بعد الحفظ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Save actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (selectedClient == null) {
                                Toast.makeText(context, "الرجاء اختيار العميل أولاً!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveDetailedInvoice(isDraft = true, currency = selectedInvoiceCurrency) { id ->
                                    Toast.makeText(context, "تم حفظ الفاتورة كمسودة!", Toast.LENGTH_SHORT).show()
                                    onNavigateToInvoiceDetails(id)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ كمسودة", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (selectedClient == null) {
                                Toast.makeText(context, "الرجاء اختيار العميل أولاً!", Toast.LENGTH_SHORT).show()
                            } else if (cart.isEmpty()) {
                                Toast.makeText(context, "الرجاء إضافة أصناف للسلة!", Toast.LENGTH_SHORT).show()
                            } else {
                                val client = selectedClient!!
                                val checkResult = CreditUtils.checkInvoiceCredit(client, grandTotal)
                                if (checkResult.isExceeded) {
                                    creditLimitDialogData = CreditDialogData(
                                        client = client,
                                        invoiceAmount = grandTotal,
                                        checkResult = checkResult,
                                        onConfirmOverride = { authorizer, reason ->
                                            viewModel.saveDetailedInvoice(
                                                isDraft = false,
                                                currency = selectedInvoiceCurrency,
                                                isCreditOverride = true,
                                                overrideAuthorizer = authorizer,
                                                overrideReason = reason,
                                                overrideAmount = checkResult.overageAmount
                                            ) { id ->
                                                Toast.makeText(context, "تم حفظ الفاتورة بتجاوز استثنائي مصرح!", Toast.LENGTH_LONG).show()
                                                if (sendSmsOnSave) {
                                                    val nextNum = settings.lastInvoiceNumber + 1
                                                    val invoiceNumber = "${settings.invoicePrefix}$nextNum"
                                                    val remBalance = client.balance + grandTotal
                                                    val itemSummary = cart.joinToString(", ") { "${it.item.name} (x${it.quantity})" }
                                                    val detailsText = if (itemSummary.length > 80) itemSummary.take(77) + "..." else itemSummary
                                                    
                                                    smsClientPhone = client.phone
                                                    smsClientName = client.name
                                                    smsMessageContent = "عميلنا العزيز ${client.name}، تم إصدار فاتورة رقم: $invoiceNumber بقيمة: ${FormatUtils.formatAmount(grandTotal)} $selectedInvoiceCurrency. الأصناف: $detailsText. الرصيد المتبقي الإجمالي المستحق: ${FormatUtils.formatAmount(remBalance)} $selectedInvoiceCurrency. شكراً لتعاملكم معنا - ${settings.storeName}"
                                                    
                                                    navigateBackOnSmsDismiss = false
                                                    navigateToDetailsOnSmsDismiss = id
                                                    showSmsConfirmDialog = true
                                                } else {
                                                    onNavigateToInvoiceDetails(id)
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.saveDetailedInvoice(isDraft = false, currency = selectedInvoiceCurrency) { id ->
                                        Toast.makeText(context, "تم إنشاء وحفظ الفاتورة بنجاح!", Toast.LENGTH_SHORT).show()
                                        if (sendSmsOnSave) {
                                            val nextNum = settings.lastInvoiceNumber + 1
                                            val invoiceNumber = "${settings.invoicePrefix}$nextNum"
                                            val remBalance = client.balance + grandTotal
                                            val itemSummary = cart.joinToString(", ") { "${it.item.name} (x${it.quantity})" }
                                            val detailsText = if (itemSummary.length > 80) itemSummary.take(77) + "..." else itemSummary
                                            
                                            smsClientPhone = client.phone
                                            smsClientName = client.name
                                            smsMessageContent = "عميلنا العزيز ${client.name}، تم إصدار فاتورة رقم: $invoiceNumber بقيمة: ${FormatUtils.formatAmount(grandTotal)} $selectedInvoiceCurrency. الأصناف: $detailsText. الرصيد المتبقي الإجمالي المستحق: ${FormatUtils.formatAmount(remBalance)} $selectedInvoiceCurrency. شكراً لتعاملكم معنا - ${settings.storeName}"
                                            
                                            navigateBackOnSmsDismiss = false
                                            navigateToDetailsOnSmsDismiss = id
                                            showSmsConfirmDialog = true
                                        } else {
                                            onNavigateToInvoiceDetails(id)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ نهائي وتحصيل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // SMS Confirmation Dialog with Permission request flow
    if (showSmsConfirmDialog) {
        val phoneNumber = smsClientPhone
        val smsMessage = smsMessageContent

        var permissionGranted by remember {
            mutableStateOf(
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.SEND_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }

        val smsPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionGranted = isGranted
            if (isGranted) {
                val sent = sendDirectSms(context, phoneNumber, smsMessage)
                if (sent) {
                    Toast.makeText(context, "تم إرسال رسالة كشف الفاتورة بنجاح!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل إرسال الرسالة المباشرة، جاري فتح التطبيق الافتراضي...", Toast.LENGTH_LONG).show()
                    sendSmsViaIntent(context, phoneNumber, smsMessage)
                }
            } else {
                Toast.makeText(context, "تم رفض الصلاحية. جاري فتح تطبيق الرسائل لإرسالها يدوياً...", Toast.LENGTH_LONG).show()
                sendSmsViaIntent(context, phoneNumber, smsMessage)
            }
            showSmsConfirmDialog = false
            if (navigateBackOnSmsDismiss) {
                onNavigateBack()
            } else if (navigateToDetailsOnSmsDismiss != null) {
                onNavigateToInvoiceDetails(navigateToDetailsOnSmsDismiss!!)
            }
        }

        AlertDialog(
            onDismissRequest = {
                showSmsConfirmDialog = false
                if (navigateBackOnSmsDismiss) {
                    onNavigateBack()
                } else if (navigateToDetailsOnSmsDismiss != null) {
                    onNavigateToInvoiceDetails(navigateToDetailsOnSmsDismiss!!)
                }
            },
            title = { Text("إرسال تفاصيل الفاتورة للعميل", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تم حفظ الفاتورة بنجاح! يمكنك الآن إرسال تفاصيل الفاتورة وحساب العميل عبر الواتس آب أو الرسائل القصيرة.", fontSize = 13.sp)
                    Text("العميل: ${selectedClient?.name ?: "العميل"} (${phoneNumber.ifEmpty { "لا يوجد رقم مسجل!" }})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    
                    // WhatsApp Main Action Option with Icon
                    Surface(
                        onClick = {
                            val activeClient = if (navigateToDetailsOnSmsDismiss != null) selectedClient else quickClient
                            val clientCurrency = selectedInvoiceCurrency
                            // Format a clean WhatsApp message
                            val whatsAppMsg = if (navigateToDetailsOnSmsDismiss != null) {
                                // Detailed invoice
                                val nextNum = settings.lastInvoiceNumber + 1
                                val invNumber = "${settings.invoicePrefix}$nextNum"
                                val remBal = (selectedClient?.balance ?: 0.0) + grandTotal
                                val itemsSummary = cart.map {
                                    com.example.data.model.InvoiceItem(
                                        invoiceId = 0,
                                        itemId = it.item.id,
                                        itemName = it.item.name,
                                        quantity = it.quantity,
                                        unitPrice = it.customPrice,
                                        totalPrice = it.customPrice * it.quantity
                                    )
                                }
                                val tempInvoice = com.example.data.model.Invoice(
                                    invoiceNumber = invNumber,
                                    clientId = selectedClient?.id ?: 0,
                                    clientName = selectedClient?.name ?: "",
                                    date = System.currentTimeMillis(),
                                    discount = discount,
                                    taxRate = taxRate,
                                    totalAmount = grandTotal,
                                    currency = clientCurrency,
                                    notes = notes
                                )
                                WhatsAppHelper.formatWhatsAppInvoiceMessage(
                                    invoice = tempInvoice,
                                    items = itemsSummary,
                                    storeName = settings.storeName,
                                    storePhone = settings.storePhone,
                                    storeAddress = settings.storeAddress,
                                    currency = clientCurrency,
                                    clientBalance = remBal
                                )
                            } else {
                                // Quick invoice
                                val nextNum = settings.lastInvoiceNumber + 1
                                val invNumber = "${settings.invoicePrefix}$nextNum"
                                val amt = quickAmountStr.toDoubleOrNull() ?: 0.0
                                val remBal = (quickClient?.balance ?: 0.0) + amt
                                val tempInvoice = com.example.data.model.Invoice(
                                    invoiceNumber = invNumber,
                                    clientId = quickClient?.id ?: 0,
                                    clientName = quickClient?.name ?: "",
                                    date = System.currentTimeMillis(),
                                    totalAmount = amt,
                                    isQuickInvoice = true,
                                    description = quickDescription,
                                    currency = clientCurrency
                                )
                                WhatsAppHelper.formatWhatsAppInvoiceMessage(
                                    invoice = tempInvoice,
                                    items = emptyList(),
                                    storeName = settings.storeName,
                                    storePhone = settings.storePhone,
                                    storeAddress = settings.storeAddress,
                                    currency = clientCurrency,
                                    clientBalance = remBal
                                )
                            }
                            WhatsAppHelper.sendWhatsAppMessage(context, phoneNumber, whatsAppMsg)
                            showSmsConfirmDialog = false
                            if (navigateBackOnSmsDismiss) {
                                onNavigateBack()
                            } else if (navigateToDetailsOnSmsDismiss != null) {
                                onNavigateToInvoiceDetails(navigateToDetailsOnSmsDismiss!!)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "إرسال الفاتورة عبر الواتس آب",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            Text(smsMessage, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (phoneNumber.isBlank()) {
                            Toast.makeText(context, "لا يمكن الإرسال، رقم هاتف العميل فارغ!", Toast.LENGTH_SHORT).show()
                        } else {
                            sendSmsViaIntent(context, phoneNumber, smsMessage)
                        }
                        showSmsConfirmDialog = false
                        if (navigateBackOnSmsDismiss) {
                            onNavigateBack()
                        } else if (navigateToDetailsOnSmsDismiss != null) {
                            onNavigateToInvoiceDetails(navigateToDetailsOnSmsDismiss!!)
                        }
                    }
                ) {
                    Text("إرسال عبر الرسائل SMS")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSmsConfirmDialog = false
                        if (navigateBackOnSmsDismiss) {
                            onNavigateBack()
                        } else if (navigateToDetailsOnSmsDismiss != null) {
                            onNavigateToInvoiceDetails(navigateToDetailsOnSmsDismiss!!)
                        }
                    }
                ) {
                    Text("تخطي")
                }
            }
        )
    }

    // Credit Limit Block / Authorization Dialog
    creditLimitDialogData?.let { dialogData ->
        CreditLimitInterceptorDialog(
            data = dialogData,
            currency = selectedInvoiceCurrency,
            onDismiss = { creditLimitDialogData = null }
        )
    }
}

// Data holder for Credit Dialog state
data class CreditDialogData(
    val client: Client,
    val invoiceAmount: Double,
    val checkResult: InvoiceCreditCheckResult,
    val onConfirmOverride: (authorizer: String, reason: String) -> Unit
)

// Mini banner displayed in CreateInvoiceScreen showing client's live credit standing
@Composable
fun InvoiceCreditInfoBanner(
    client: Client,
    proposedInvoiceAmount: Double
) {
    val statusInfo = CreditUtils.getCreditStatusInfo(client)
    val check = CreditUtils.checkInvoiceCredit(client, proposedInvoiceAmount)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (check.isExceeded) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (check.isExceeded) Color(0xFFE53935) else Color(0xFF81C784)
        ),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (check.isExceeded) Icons.Default.Block else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (check.isExceeded) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "نظام الحد الائتماني للعميل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (check.isExceeded) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                }

                Surface(
                    color = statusInfo.statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusInfo.title,
                        color = statusInfo.statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "الحد: ${FormatUtils.formatAmount(client.creditLimit)}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Text(
                    "المديونية الحالية: ${FormatUtils.formatAmount(client.balance)}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Text(
                    "المتبقي: ${FormatUtils.formatAmount(statusInfo.availableCredit)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (statusInfo.availableCredit > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }

            if (proposedInvoiceAmount > 0) {
                val newDebt = client.balance + proposedInvoiceAmount
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "المديونية بعد الفاتورة: ${FormatUtils.formatAmount(newDebt)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (check.isExceeded) Color(0xFFD32F2F) else Color(0xFF1B5E20)
                    )
                    if (check.isExceeded) {
                        Text(
                            "تجاوز بـ: +${FormatUtils.formatAmount(check.overageAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

// Dialog that intercepts invoice creation when credit limit is exceeded, offering management override option
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditLimitInterceptorDialog(
    data: CreditDialogData,
    currency: String,
    onDismiss: () -> Unit
) {
    var allowOverride by remember { mutableStateOf(false) }
    var authorizerName by remember { mutableStateOf("") }
    var overrideReason by remember { mutableStateOf("") }
    var authorizerError by remember { mutableStateOf(false) }
    var reasonError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "تجاوز الحد الائتماني المسموح",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "تم رفض العملية تلقائياً لأن إجمالي مديونية العميل ستتجاوز الحد الائتماني المحدد له.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Comparison Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("العميل:", fontSize = 12.sp, color = Color.DarkGray)
                            Text(data.client.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الحد الائتماني:", fontSize = 12.sp, color = Color.DarkGray)
                            Text("${FormatUtils.formatAmount(data.client.creditLimit)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المديونية الحالية:", fontSize = 12.sp, color = Color.DarkGray)
                            Text("${FormatUtils.formatAmount(data.client.balance)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("قيمة الفاتورة الجديدة:", fontSize = 12.sp, color = Color.DarkGray)
                            Text("${FormatUtils.formatAmount(data.invoiceAmount)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                        Divider(color = Color(0xFFEF9A9A), thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المديونية بعد الفاتورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${FormatUtils.formatAmount(data.checkResult.prospectiveBalance)} $currency", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مبلغ التجاوز الممنوع:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                            Text("+${FormatUtils.formatAmount(data.checkResult.overageAmount)} $currency", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB71C1C))
                        }
                    }
                }

                // Policy notice
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0B2))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Text(
                            "الموظف العادي لا يستطيع تجاوز الحد الائتماني. يتطلب ذلك تفويضاً استثنائياً من الإدارة.",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Management Override Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { allowOverride = !allowOverride }
                        .padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = allowOverride,
                        onCheckedChange = { allowOverride = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "السماح بتجاوز الحد استثنائياً (صلاحية الإدارة)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (allowOverride) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = authorizerName,
                            onValueChange = {
                                authorizerName = it
                                if (it.isNotBlank()) authorizerError = false
                            },
                            label = { Text("اسم المدير / المسؤول المصرح *") },
                            placeholder = { Text("مثال: المدير العام / مدير المبيعات") },
                            isError = authorizerError,
                            supportingText = if (authorizerError) { { Text("الاسم إلزامي للتوثيق والمساءلة") } } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = overrideReason,
                            onValueChange = {
                                overrideReason = it
                                if (it.isNotBlank()) reasonError = false
                            },
                            label = { Text("سبب ومسوغ تجاوز الحد *") },
                            placeholder = { Text("مثال: عميل قديم موثوق بضمانة شخصية") },
                            isError = reasonError,
                            supportingText = if (reasonError) { { Text("السبب إلزامي للتسجيل في سجل الرقابة") } } else null,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (allowOverride) {
                Button(
                    onClick = {
                        var hasErr = false
                        if (authorizerName.trim().isEmpty()) {
                            authorizerError = true
                            hasErr = true
                        }
                        if (overrideReason.trim().isEmpty()) {
                            reasonError = true
                            hasErr = true
                        }
                        if (!hasErr) {
                            onDismiss()
                            data.onConfirmOverride(authorizerName.trim(), overrideReason.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("اعتماد وتجاوز الحد استثنائياً", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (allowOverride) "إلغاء" else "إغلاق والتراجع")
            }
        }
    )
}

// Simple Basic Text field with thin borders inside Box
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}
