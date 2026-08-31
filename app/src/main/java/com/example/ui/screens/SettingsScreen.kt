package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.StoreSettings
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()

    val isDriveSyncEnabled by viewModel.isDriveSyncEnabled.collectAsState()
    val driveAccountEmail by viewModel.driveAccountEmail.collectAsState()
    val lastBackupTimeStr by viewModel.lastBackupTimeStr.collectAsState()
    val driveBackupStatus by viewModel.driveBackupStatus.collectAsState()
    val availableBackupFiles by viewModel.availableBackupFiles.collectAsState()

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val biometricUserName by viewModel.biometricUserName.collectAsState()

    var showBiometricSetupDialog by remember { mutableStateOf(false) }
    var showEditBiometricUserDialog by remember { mutableStateOf(false) }

    var storeName by remember(settings) { mutableStateOf(settings.storeName) }
    var storePhone by remember(settings) { mutableStateOf(settings.storePhone) }
    var storeAddress by remember(settings) { mutableStateOf(settings.storeAddress) }
    var storeEmail by remember(settings) { mutableStateOf(settings.storeEmail) }
    var currency by remember(settings) { mutableStateOf(settings.currency) }
    var invoicePrefix by remember(settings) { mutableStateOf(settings.invoicePrefix) }
    var lastInvoiceNumber by remember(settings) { mutableStateOf(settings.lastInvoiceNumber.toString()) }
    var isAutoPdfBackupEnabled by remember(settings) { mutableStateOf(settings.isAutoPdfBackupEnabled) }
    var autoPdfBackupHour by remember(settings) { mutableStateOf(settings.autoPdfBackupHour) }

    var overdueDaysThresholdText by remember(settings) { mutableStateOf(settings.overdueDaysThreshold.toString()) }
    var fastPayerDaysThresholdText by remember(settings) { mutableStateOf(settings.fastPayerDaysThreshold.toString()) }
    var loyaltyMinInvoicesCountText by remember(settings) { mutableStateOf(settings.loyaltyMinInvoicesCount.toString()) }
    var overdueNoticeTemplate by remember(settings) { mutableStateOf(settings.overdueNoticeTemplate) }
    var loyaltyAppreciationTemplate by remember(settings) { mutableStateOf(settings.loyaltyAppreciationTemplate) }

    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showBackupFilesListDialog by remember { mutableStateOf(false) }
    var isRestoringData by remember { mutableStateOf(false) }
    var userDriveEmailInput by remember { mutableStateOf(driveAccountEmail ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Theme & Appearance Section
        item {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.NightsStay else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("مظهر التطبيق (الوضع النهاري / الليلي)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDarkMode) "الوضع الحالي: ليلي 🌙" else "الوضع الحالي: نهاري (رسمي أبيض) ☀️",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (isDarkMode) "ألوان داكنة مريحة للعين" else "ألوان نهارية رسمية بيضاء وواضحة بخطوط غامقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // 1. Store Identity Section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("معلومات وهوية المتجر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Divider()

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("اسم المتجر / العلامة التجارية *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storePhone,
                        onValueChange = { storePhone = it },
                        label = { Text("رقم هاتف المتجر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storeAddress,
                        onValueChange = { storeAddress = it },
                        label = { Text("عنوان المقر") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storeEmail,
                        onValueChange = { storeEmail = it },
                        label = { Text("البريد الإلكتروني التجاري") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Financial & Invoicing Settings
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PriceChange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("الإعدادات المالية والترقيم", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Divider()

                    // Currency chooser row
                    Text("العملات المستخدمة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val currencyOptions = listOf(
                        "الريال اليمني" to "ر.ي",
                        "الريال السعودي" to "ر.س",
                        "الدولار الأمريكي" to "$"
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currencyOptions.forEach { (name, code) ->
                            val selected = currency == name || currency == code
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { currency = name },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(code, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it },
                            label = { Text("بادئة الفواتير") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lastInvoiceNumber,
                            onValueChange = { lastInvoiceNumber = it },
                            label = { Text("رقم آخر فاتورة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. App Security Settings
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("أمان وحماية التطبيق", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("قفل التطبيق برقم PIN سري", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (securityPin.isNullOrBlank()) "غير مفعل حالياً" else "مفعل ومحمي",
                                fontSize = 11.sp,
                                color = if (securityPin.isNullOrBlank()) Color.Gray else Color(0xFF059669)
                            )
                        }
                        Switch(
                            checked = !securityPin.isNullOrBlank(),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showPinSetupDialog = true
                                } else {
                                    viewModel.setSecurityPin(null)
                                    Toast.makeText(context, "تم إيقاف قفل الحماية السري", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("قفل التطبيق بالبصمة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (isBiometricEnabled) "مفعل للمستخدم: ${biometricUserName.ifBlank { "غير محدد" }}" else "غير مفعل حالياً",
                                fontSize = 11.sp,
                                color = if (isBiometricEnabled) Color(0xFF059669) else Color.Gray
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isBiometricEnabled) {
                                IconButton(onClick = { showEditBiometricUserDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الاسم", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (biometricUserName.isBlank()) {
                                            showBiometricSetupDialog = true
                                        } else {
                                            viewModel.setBiometricSettings(true, biometricUserName)
                                            Toast.makeText(context, "تم تفعيل قفل البصمة", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.setBiometricSettings(false, biometricUserName)
                                        Toast.makeText(context, "تم إيقاف قفل البصمة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    Divider()

                    // Operations Audit Log shortcut
                    Button(
                        onClick = { showAuditLogsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.HistoryToggleOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض سجل عمليات النظام (Audit Log)")
                    }
                }
            }
        }

        // 4. Google Drive Sync & Backup (Google Drive Auto Sync & JSON File Restore)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDriveSyncEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isDriveSyncEnabled) Color(0xFF4285F4) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header with Google Drive Branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4).copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text("المزامنة التلقائية مع Google Drive", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (isDriveSyncEnabled) "المزامنة مفعلة 🟢 ($driveAccountEmail)" else "المزامنة متوقفة ⚪ (اضغط للتفعيل وتوصيل الحساب)",
                                    fontSize = 11.sp,
                                    color = if (isDriveSyncEnabled) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Toggle Switch for Google Drive Sync
                        Switch(
                            checked = isDriveSyncEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showGoogleSignInDialog = true
                                } else {
                                    viewModel.toggleDriveSync(false)
                                    Toast.makeText(context, "تم إيقاف المزامنة التلقائية مع Google Drive", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4285F4)
                            )
                        )
                    }

                    Divider()

                    // Detailed JSON format explanation
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.DataObject, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Text("حفظ النسخة بصيغة JSON واضحة ومقروءة", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF059669))
                            }
                            Text(
                                text = "تتم حفظ بياناتك بملف 'debt_app_clients_backup.json' بداخل حسابك في Google Drive، مما يتيح لك فتح الملف وتصفح كشوفات وحسابات جميع العملاء في أي وقت دون خوف من ضياع البيانات.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("آخر تاريخ مزامنة:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(lastBackupTimeStr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    // Primary Actions: Manual Sync & Restore from Google Drive
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 1. Manual Sync Button
                        OutlinedButton(
                            onClick = {
                                if (!isDriveSyncEnabled) {
                                    showGoogleSignInDialog = true
                                } else {
                                    viewModel.performGoogleDriveBackup()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("رفع ونشر نسخة احتياطية (JSON) الآن لـ Google Drive", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF4285F4))
                        }

                        // 2. RESTORE CLIENT DATA FROM GOOGLE DRIVE BUTTON (Explicit user request)
                        Button(
                            onClick = { showRestoreConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استعادة بيانات العملاء من Google Drive", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 3. SHOW ALL AVAILABLE BACKUP JSON FILES BUTTON
                        OutlinedButton(
                            onClick = {
                                viewModel.loadAvailableBackupFiles()
                                showBackupFilesListDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF059669))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استعراض وتحديد ملف نسخة احتياطية من Accountant Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF059669))
                        }

                        // Local file backup alternative row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.exportDatabase(
                                        onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() },
                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SdCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير لمجلد Download/backup", fontSize = 10.sp)
                            }

                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "يمكنك استخدام زر استعادة بيانات العملاء من Google Drive أعلاه لاسترجاع بياناتك كاملة.", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعلم المزيد عن الاستعادة", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Automatic Daily PDF Backup Section for All Clients
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAutoPdfBackupEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAutoPdfBackupEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626))
                            Column {
                                Text("تصدير كشوفات العملاء التلقائي (PDF)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("تصدير تقرير شامل لجميع العملاء إلى مجلد Downloads", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isAutoPdfBackupEnabled,
                            onCheckedChange = { isAutoPdfBackupEnabled = it }
                        )
                    }

                    Divider()

                    // Time Picker Hour Selection
                    if (isAutoPdfBackupEnabled) {
                        Text("ميعاد التصدير والحفظ التلقائي اليومي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val hourOptions = listOf(
                            0 to "الساعة 12:00 ليلاً (منتصف الليل)",
                            1 to "الساعة 01:00 صباحاً",
                            2 to "الساعة 02:00 صباحاً",
                            8 to "الساعة 08:00 صباحاً",
                            12 to "الساعة 12:00 ظهراً",
                            20 to "الساعة 08:00 مساءً",
                            22 to "الساعة 10:00 مساءً"
                        )

                        var expandedHourDropdown by remember { mutableStateOf(false) }
                        val currentHourLabel = hourOptions.find { it.first == autoPdfBackupHour }?.second ?: "الساعة ${autoPdfBackupHour}:00"

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedHourDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(currentHourLabel, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = expandedHourDropdown,
                                onDismissRequest = { expandedHourDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                hourOptions.forEach { (hr, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontWeight = if (autoPdfBackupHour == hr) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            autoPdfBackupHour = hr
                                            expandedHourDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    "عند التفعيل، يحفظ التطبيق ملف 'نسخة كشوفات العملاء.pdf' تلقائياً يومياً في الساعة المحددة بذاكرة الهاتف الداخلية بمجلد Download.",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Immediate Manual Save Button
                    Button(
                        onClick = {
                            val savedFile = viewModel.generateAndSaveAllClientsPdfNow()
                            if (savedFile != null) {
                                Toast.makeText(
                                    context,
                                    "تم حفظ التقرير الشامل بنجاح!\nاسم الملف: نسخة كشوفات العملاء.pdf\nالمسار: Downloads",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "فشل حفظ ملف PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ وتصدير ملف PDF لجميع العملاء الآن يدوياً", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 6. Loyalty and Payment Management Settings Section (ميزة إدارة السداد والعملاء الأوفياء)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                        Text(
                            text = "إدارة السداد والعملاء الأوفياء ⭐",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "نظام ذكي لمتابعة وتصنيف سلوك السداد تلقائياً، إرسال تذكيرات التأخر، ورسائل الشكر والتقدير للعملاء الأوفياء.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider()

                    // 1. Overdue Threshold
                    OutlinedTextField(
                        value = overdueDaysThresholdText,
                        onValueChange = { overdueDaysThresholdText = it },
                        label = { Text("مهلة تأخر السداد (بالأيام)") },
                        supportingText = { Text("عدد الأيام بعد تاريخ الاستحقاق لتصنيف العميل '⚠️ متأخر بالسداد'") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Fast Payer Threshold
                    OutlinedTextField(
                        value = fastPayerDaysThresholdText,
                        onValueChange = { fastPayerDaysThresholdText = it },
                        label = { Text("مهلة السداد السريع (بالأيام)") },
                        supportingText = { Text("السداد خلال هذه المدة يمنح العميل شارة '⚡ سريع السداد'") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Loyalty Min Invoices Count
                    OutlinedTextField(
                        value = loyaltyMinInvoicesCountText,
                        onValueChange = { loyaltyMinInvoicesCountText = it },
                        label = { Text("الحد الأدنى لعدد الفواتير للوفاء") },
                        supportingText = { Text("عدد الفواتير المنتظمة المطلوبة لنيل تصنيف '⭐ عميل وفي'") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. Overdue Notice WhatsApp Template
                    OutlinedTextField(
                        value = overdueNoticeTemplate,
                        onValueChange = { overdueNoticeTemplate = it },
                        label = { Text("قالب رسالة تذكير المتأخرين بالسداد (واتساب)") },
                        supportingText = { Text("المتغيرات: {اسم_العميل}، {اسم_المتجر}، {المبلغ}، {العملة}، {أيام_التأخير}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    // 5. Loyalty Appreciation WhatsApp Template
                    OutlinedTextField(
                        value = loyaltyAppreciationTemplate,
                        onValueChange = { loyaltyAppreciationTemplate = it },
                        label = { Text("قالب رسالة شكر وتقدير للعميل الوفي (واتساب)") },
                        supportingText = { Text("المتغيرات: {اسم_العميل}، {اسم_المتجر}، {عدد_الفواتير}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                }
            }
        }

        // 7. Submit Changes Action
        item {
            Button(
                onClick = {
                    if (storeName.isBlank()) {
                        Toast.makeText(context, "اسم المتجر مطلوب!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateStoreSettings(
                            settings.copy(
                                storeName = storeName,
                                storePhone = storePhone,
                                storeAddress = storeAddress,
                                storeEmail = storeEmail,
                                currency = currency,
                                invoicePrefix = invoicePrefix,
                                lastInvoiceNumber = lastInvoiceNumber.toIntOrNull() ?: settings.lastInvoiceNumber,
                                isAutoPdfBackupEnabled = isAutoPdfBackupEnabled,
                                autoPdfBackupHour = autoPdfBackupHour,
                                overdueDaysThreshold = overdueDaysThresholdText.toIntOrNull() ?: settings.overdueDaysThreshold,
                                fastPayerDaysThreshold = fastPayerDaysThresholdText.toIntOrNull() ?: settings.fastPayerDaysThreshold,
                                loyaltyMinInvoicesCount = loyaltyMinInvoicesCountText.toIntOrNull() ?: settings.loyaltyMinInvoicesCount,
                                overdueNoticeTemplate = overdueNoticeTemplate.ifBlank { settings.overdueNoticeTemplate },
                                loyaltyAppreciationTemplate = loyaltyAppreciationTemplate.ifBlank { settings.loyaltyAppreciationTemplate }
                            )
                        )
                        Toast.makeText(context, "تم حفظ وتحديث جميع الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ جميع الإعدادات", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Passcode/PIN Dialog Setup
    if (showPinSetupDialog) {
        var pinCode by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("إعداد رقم PIN سري للحماية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمزاً مكوناً من 4 أرقام لتأمين تطبيق حسابات العملاء برو:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinCode = it
                                pinError = false
                            }
                        },
                        label = { Text("رمز القفل PIN") },
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinCode.length == 4) {
                            viewModel.setSecurityPin(pinCode)
                            showPinSetupDialog = false
                            Toast.makeText(context, "تم تفعيل القفل برمز PIN بنجاح!", Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("تفعيل الحماية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Security Audit Logs Dialog Display
    if (showAuditLogsDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = { Text("سجل العمليات وحماية النظام (Audit Log)", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.sizeIn(maxHeight = 400.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text("لا يوجد سجل نشاطات حالياً.")
                    } else {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(auditLogs) { log ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.operationType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("الجدول: ${log.tableName}", fontWeight = FontWeight.Medium, fontSize = 10.sp, color = Color.DarkGray)
                                    Text(log.details, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAuditLogsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Google Sign In & Drive Permissions Dialog
    if (showGoogleSignInDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSignInDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF4285F4))
                    Text("تسجيل الدخول وإذونات Google Drive", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "لتفعيل المزامنة التلقائية بملفات JSON الحية، يرجى تسجيل الدخول إلى حساب Google الخاص بك ومنح التطبيق إذن قراءة وحفظ النسخة الاحتياطية بـ Google Drive:",
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = userDriveEmailInput,
                        onValueChange = { userDriveEmailInput = it },
                        label = { Text("البريد الإلكتروني لحساب Google") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Surface(
                        color = Color(0xFF4285F4).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(16.dp))
                            Text("سيتم إنشاء وتحديث ملف JSON باسم 'debt_app_clients_backup.json' تلقائياً.", fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userDriveEmailInput.isNotBlank()) {
                            viewModel.toggleDriveSync(true, userDriveEmailInput.trim())
                            showGoogleSignInDialog = false
                            Toast.makeText(context, "تم تسجيل الدخول وتفعيل المزامنة مع Google Drive بنجاح!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "يرجى إدخال بريد إلكتروني صالح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("موافقة وتسجيل الدخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleSignInDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Google Drive Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoringData) showRestoreConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF059669))
                    Text("استعادة بيانات العملاء من Google Drive", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "هل ترغب في استعادة جميع بيانات العملاء، الفواتير، المقبوضات والأقساط من ملف JSON المحفوظ في Google Drive؟\n\nسيتم قراءة جميع الملفات واستخراج البيانات وتنسيقها داخل التطبيق لاستعادة حساباتك بالكامل.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    if (isRestoringData) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF059669))
                            Text("جاري استعادة وقراءة البيانات من ملفات JSON...", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoringData = true
                        viewModel.restoreGoogleDriveBackup(
                            onSuccess = { msg ->
                                isRestoringData = false
                                showRestoreConfirmDialog = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isRestoringData = false
                                Toast.makeText(context, "خطأ في الاستعادة: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isRestoringData,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استعادة البيانات الآن")
                }
            },
            dismissButton = {
                if (!isRestoringData) {
                    TextButton(onClick = { showRestoreConfirmDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    // Modal List of JSON Backup Files in Accountant Backup Folder
    if (showBackupFilesListDialog) {
        AlertDialog(
            onDismissRequest = { showBackupFilesListDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF4285F4))
                    Text("ملفات النسخ بـ Accountant Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(modifier = Modifier.sizeIn(maxHeight = 380.dp)) {
                    if (availableBackupFiles.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Text("لم يتم العثور على أية ملفات نسخ احتياطية في مجلد Accountant Backup حالياً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableBackupFiles) { fileInfo ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.DataObject, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                                Text(fileInfo.fileName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Text(fileInfo.fileSizeFormatted, fontSize = 10.sp, color = Color.Gray)
                                        }

                                        Text("التاريخ: ${fileInfo.backupDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            "المحتوى: ${fileInfo.clientCount} عميل | ${fileInfo.invoiceCount} فاتورة | ${fileInfo.paymentCount} دفعة",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Button(
                                            onClick = {
                                                viewModel.restoreBackupFromFile(
                                                    fileInfo,
                                                    onSuccess = { msg ->
                                                        showBackupFilesListDialog = false
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, "خطأ: $err", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("استعادة البيانات من هذا الملف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBackupFilesListDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showBiometricSetupDialog) {
        var newUserName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBiometricSetupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("تفعيل البصمة وتسجيل المستخدم", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "لإتمام تفعيل البصمة لأول مرة، الرجاء إدخال اسمك لربطه بالمصادقة (سيظهر عند قفل التطبيق):",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("اسم المستخدم (مثال: محمد، المدير)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            viewModel.setBiometricSettings(true, newUserName)
                            Toast.makeText(context, "تم تفعيل قفل البصمة بنجاح!", Toast.LENGTH_SHORT).show()
                            showBiometricSetupDialog = false
                        } else {
                            Toast.makeText(context, "الرجاء إدخال الاسم أولاً", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("حفظ وتفعيل البصمة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricSetupDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEditBiometricUserDialog) {
        var editUserName by remember { mutableStateOf(biometricUserName) }
        AlertDialog(
            onDismissRequest = { showEditBiometricUserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("تعديل اسم مستخدم البصمة", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("قم بتحديث اسم المستخدم المرتبط ببصمة الأمان:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = editUserName,
                        onValueChange = { editUserName = it },
                        label = { Text("الاسم الجديد") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editUserName.isNotBlank()) {
                            viewModel.updateBiometricUserName(editUserName)
                            Toast.makeText(context, "تم تحديث الاسم بنجاح", Toast.LENGTH_SHORT).show()
                            showEditBiometricUserDialog = false
                        }
                    }
                ) {
                    Text("تحديث")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBiometricUserDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
