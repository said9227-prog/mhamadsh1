package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.Item
import com.example.ui.viewmodel.AppViewModel
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    viewModel: AppViewModel,
    initialBarcodeToSearch: String = "",
    initialShowAddDialog: Boolean = false
) {
    val itemsList by viewModel.items.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    var searchQuery by remember { mutableStateOf(initialBarcodeToSearch) }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(initialShowAddDialog) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }

    // Dynamic unique categories
    val categories = remember(itemsList) {
        val list = mutableListOf("الكل")
        list.addAll(itemsList.map { it.category }.filter { it.isNotBlank() }.distinct())
        list
    }

    // Filter items
    val filteredItems = remember(itemsList, searchQuery, selectedCategoryFilter, showOnlyLowStock) {
        itemsList.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) || 
                                item.barcode.contains(searchQuery) ||
                                item.category.contains(searchQuery, ignoreCase = true)
            
            val matchesCat = selectedCategoryFilter == "الكل" || 
                             item.category == selectedCategoryFilter
            
            val matchesLowStock = !showOnlyLowStock || 
                                  item.quantity <= item.minQuantityAlert

            matchesSearch && matchesCat && matchesLowStock
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث باسم الصنف، الباركود أو التصنيف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Inventory Filter Switches & Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الأقسام والتصنيفات:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                // Low Stock Toggle
                FilterChip(
                    selected = showOnlyLowStock,
                    onClick = { showOnlyLowStock = !showOnlyLowStock },
                    label = { Text("المخزون المنخفض") },
                    leadingIcon = {
                        if (showOnlyLowStock) Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    }
                )
            }

            // Categories horizontal slider
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategoryFilter).coerceAtLeast(0),
                edgePadding = 0.dp,
                divider = {},
                indicator = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { _, cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inventory Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لم يتم العثور على أصناف تليق بالبحث",
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
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            currency = settings.currency,
                            onEdit = { itemToEdit = item },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }

        // Add Item Floating Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFFFFD700),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_item_fab")
        ) {
            Icon(imageVector = Icons.Default.AddBox, contentDescription = "إضافة صنف")
        }

        // Add Dialog
        if (showAddDialog) {
            ItemFormDialog(
                title = "إضافة صنف جديد للمخازن",
                onDismiss = { showAddDialog = false },
                onSave = { name, barcode, category, purchasePrice, sellingPrice, quantity, minQty ->
                    viewModel.addItem(name, barcode, category, purchasePrice, sellingPrice, quantity, minQty)
                    showAddDialog = false
                }
            )
        }

        // Edit Dialog
        itemToEdit?.let { item ->
            ItemFormDialog(
                title = "تعديل بيانات الصنف",
                item = item,
                onDismiss = { itemToEdit = null },
                onSave = { name, barcode, category, purchasePrice, sellingPrice, quantity, minQty ->
                    viewModel.updateItem(
                        item.copy(
                            name = name,
                            barcode = barcode,
                            category = category,
                            purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice,
                            quantity = quantity,
                            minQuantityAlert = minQty
                        )
                    )
                    itemToEdit = null
                }
            )
        }
    }
}

@Composable
fun ItemCard(
    item: Item,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.quantity <= item.minQuantityAlert

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isLowStock) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (isLowStock) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.category.isNotBlank()) {
                                Text(
                                    text = item.category,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("|", fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (item.barcode.isNotBlank()) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.barcode,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Edit/Delete Dropdown
                Row {
                    if (isLowStock) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "نقص المخزون",
                                color = Color(0xFFDC2626),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    var showItemMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showItemMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(
                        expanded = showItemMenu,
                        onDismissRequest = { showItemMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showItemMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                showItemMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Stock row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("سعر البيع", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${FormatUtils.formatAmount(item.sellingPrice)} $currency",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text("سعر الشراء", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${FormatUtils.formatAmount(item.purchasePrice)} $currency",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("الكمية الحالية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${item.quantity} حبة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) Color(0xFFDC2626) else Color(0xFF059669)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemFormDialog(
    title: String,
    item: Item? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, barcode: String, category: String, purchasePrice: Double, sellingPrice: Double, quantity: Int, minQty: Int) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var barcode by remember { mutableStateOf(item?.barcode ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "عام") }
    var purchasePriceStr by remember { mutableStateOf(item?.purchasePrice?.toString() ?: "0.0") }
    var sellingPriceStr by remember { mutableStateOf(item?.sellingPrice?.toString() ?: "0.0") }
    var quantityStr by remember { mutableStateOf(item?.quantity?.toString() ?: "0") }
    var minQtyStr by remember { mutableStateOf(item?.minQuantityAlert?.toString() ?: "5") }

    var isError by remember { mutableStateOf(false) }

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
                        label = { Text("اسم الصنف *") },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود (رقمي أو يدوي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف / القسم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            label = { Text("سعر الشراء") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it },
                            label = { Text("سعر البيع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minQtyStr,
                            onValueChange = { minQtyStr = it },
                            label = { Text("تنبيه النقص") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onSave(
                            name,
                            barcode,
                            category,
                            purchasePriceStr.toDoubleOrNull() ?: 0.0,
                            sellingPriceStr.toDoubleOrNull() ?: 0.0,
                            quantityStr.toIntOrNull() ?: 0,
                            minQtyStr.toIntOrNull() ?: 5
                        )
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
