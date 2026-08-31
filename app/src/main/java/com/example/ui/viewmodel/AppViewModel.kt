package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(
    private val app: Application,
    private val repository: AppRepository
) : AndroidViewModel(app) {

    // --- State Holders ---
    val clients = repository.getAllClientsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val items = repository.getAllItemsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val invoices = repository.getAllInvoicesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val payments = repository.getAllPaymentsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val auditLogs = repository.getAllLogsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val storeSettings = repository.getSettingsFlow().map { it ?: StoreSettings() }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), StoreSettings()
    )

    val installments = repository.getAllInstallmentsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val overdueInstallments = installments.map { list ->
        val now = System.currentTimeMillis()
        list.filter { !it.isPaid && it.dueDate < now }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val backupManager = com.example.util.GoogleDriveBackupManager(app)
    val hasDriveBackup = MutableStateFlow(false)
    val driveBackupStatus = MutableStateFlow<String?>(null)
    val isDriveSyncEnabled = MutableStateFlow(false)
    val driveAccountEmail = MutableStateFlow<String?>(null)
    val lastBackupTimeStr = MutableStateFlow("لم يتم إنشاء نسخة بعد")
    val availableBackupFiles = MutableStateFlow<List<com.example.util.BackupFileInfo>>(emptyList())

    init {
        val drivePrefs = app.getSharedPreferences("drive_sync_prefs", Context.MODE_PRIVATE)
        isDriveSyncEnabled.value = drivePrefs.getBoolean("is_sync_enabled", false)
        driveAccountEmail.value = drivePrefs.getString("drive_account_email", null)
        checkDriveBackup()
    }

    fun checkDriveBackup() {
        viewModelScope.launch {
            hasDriveBackup.value = backupManager.checkForAvailableBackup()
            lastBackupTimeStr.value = backupManager.getLastBackupTimeFormatted()
            loadAvailableBackupFiles()
        }
    }

    fun loadAvailableBackupFiles() {
        viewModelScope.launch {
            availableBackupFiles.value = backupManager.getAvailableBackupFiles()
        }
    }

    fun toggleDriveSync(enabled: Boolean, email: String = "user.account@gmail.com") {
        isDriveSyncEnabled.value = enabled
        driveAccountEmail.value = if (enabled) email else null
        val drivePrefs = app.getSharedPreferences("drive_sync_prefs", Context.MODE_PRIVATE)
        drivePrefs.edit()
            .putBoolean("is_sync_enabled", enabled)
            .putString("drive_account_email", driveAccountEmail.value)
            .apply()

        if (enabled) {
            performGoogleDriveBackup()
        } else {
            driveBackupStatus.value = "تم إيقاف المزامنة التلقائية مع Google Drive"
        }
    }

    fun performGoogleDriveBackup() {
        viewModelScope.launch {
            driveBackupStatus.value = "جاري إنشاء وتصدير ملف JSON إلى Google Drive..."
            val result = backupManager.createAndUploadBackup(AppDatabase.getDatabase(app))
            result.onSuccess { msg ->
                driveBackupStatus.value = msg
                hasDriveBackup.value = true
                lastBackupTimeStr.value = backupManager.getLastBackupTimeFormatted()
                loadAvailableBackupFiles()
                Toast.makeText(app, "تم رفع وتحديث ملف JSON بـ Google Drive بنجاح!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                driveBackupStatus.value = "خطأ أثناء النسخ الاحتياطي: ${err.message}"
            }
        }
    }

    fun restoreBackupFromFile(fileInfo: com.example.util.BackupFileInfo, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            driveBackupStatus.value = "جاري قراءة ملف JSON (${fileInfo.fileName}) واستعادة البيانات..."
            val result = backupManager.restoreFromSpecificFile(AppDatabase.getDatabase(app), fileInfo.file)
            result.onSuccess { msg ->
                driveBackupStatus.value = msg
                hasDriveBackup.value = true
                lastBackupTimeStr.value = backupManager.getLastBackupTimeFormatted()
                onSuccess(msg)
            }.onFailure { err ->
                driveBackupStatus.value = "خطأ في الاستعادة: ${err.message}"
                onError(err.message ?: "فشلت الاستعادة")
            }
        }
    }

    fun restoreGoogleDriveBackup(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            driveBackupStatus.value = "جاري قراءة ملف JSON واستعادة جميع بيانات العملاء..."
            val result = backupManager.restoreLatestBackup(AppDatabase.getDatabase(app))
            result.onSuccess { msg ->
                driveBackupStatus.value = msg
                hasDriveBackup.value = true
                lastBackupTimeStr.value = backupManager.getLastBackupTimeFormatted()
                onSuccess(msg)
            }.onFailure { err ->
                driveBackupStatus.value = "خطأ في الاستعادة: ${err.message}"
                onError(err.message ?: "فشلت الاستعادة")
            }
        }
    }

    private fun triggerAutoDriveBackup() {
        if (!isDriveSyncEnabled.value) return
        viewModelScope.launch {
            try {
                backupManager.createAndUploadBackup(AppDatabase.getDatabase(app))
                hasDriveBackup.value = true
                lastBackupTimeStr.value = backupManager.getLastBackupTimeFormatted()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Dashboard Statistics State ---
    val dashboardStats = combine(clients, invoices, payments, items) { clientsList, invoicesList, paymentsList, itemsList ->
        val totalClients = clientsList.size
        val totalDebt = clientsList.filter { it.balance > 0 }.sumOf { it.balance }
        val totalReceived = paymentsList.sumOf { it.amount }
        val netBalance = clientsList.sumOf { it.balance } // Overall net debit outstanding
        val invoicesCount = invoicesList.size
        val paymentsCount = paymentsList.size
        val itemsCount = itemsList.size

        // Multi-currency debt & payment breakdown based on actual transactions
        val currencyInvoiced = mutableMapOf<String, Double>()
        val currencyPayments = mutableMapOf<String, Double>()
        val currencyDebts = mutableMapOf<String, Double>()

        invoicesList.filter { !it.isDraft }.forEach { inv ->
            val curr = if (inv.currency.isBlank()) "الريال اليمني" else inv.currency
            currencyInvoiced[curr] = (currencyInvoiced[curr] ?: 0.0) + inv.totalAmount
        }

        paymentsList.forEach { pay ->
            val curr = if (pay.currency.isBlank()) "الريال اليمني" else pay.currency
            currencyPayments[curr] = (currencyPayments[curr] ?: 0.0) + pay.amount
        }

        // Net remaining debts per currency
        val allCurrencies = (currencyInvoiced.keys + currencyPayments.keys).ifEmpty { setOf("الريال اليمني") }
        allCurrencies.forEach { curr ->
            val invTotal = currencyInvoiced[curr] ?: 0.0
            val payTotal = currencyPayments[curr] ?: 0.0
            val rem = invTotal - payTotal
            if (rem > 0 || invTotal > 0) {
                currencyDebts[curr] = rem
            }
        }

        // Recent Activity mapping
        val clientMap = clientsList.associateBy { it.id }
        val activities = mutableListOf<ActivityItem>()
        
        invoicesList.forEach { inv ->
            activities.add(
                ActivityItem(
                    id = inv.id,
                    type = ActivityType.INVOICE,
                    title = if (inv.isQuickInvoice) "فاتورة سريعة" else "فاتورة مفصلة",
                    subtitle = "رقم: ${inv.invoiceNumber} | العميل: ${inv.clientName} (${inv.currency})",
                    amount = inv.totalAmount,
                    date = inv.date,
                    isDraft = inv.isDraft,
                    referenceId = inv.id
                )
            )
        }

        paymentsList.forEach { pay ->
            val cName = clientMap[pay.clientId]?.name ?: "عميل غير معروف"
            activities.add(
                ActivityItem(
                    id = pay.id,
                    type = ActivityType.PAYMENT,
                    title = "دفعة مستلمة (${pay.currency})",
                    subtitle = "العميل: $cName | طريقة الدفع: ${pay.paymentMethod}",
                    amount = pay.amount,
                    date = pay.date,
                    isDraft = false,
                    referenceId = pay.id
                )
            )
        }

        val sortedActivities = activities.sortedByDescending { it.date }.take(20)

        // Most indebted clients
        val topDebtors = clientsList.filter { it.balance > 0 }.sortedByDescending { it.balance }.take(5)

        // Most active clients (by total volume of invoices + payments)
        val activityMap = mutableMapOf<Int, Double>()
        invoicesList.forEach { activityMap[it.clientId] = (activityMap[it.clientId] ?: 0.0) + it.totalAmount }
        paymentsList.forEach { activityMap[it.clientId] = (activityMap[it.clientId] ?: 0.0) + it.amount }
        val topActiveClients = clientsList.map { 
            Pair(it, activityMap[it.id] ?: 0.0) 
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(5)

        DashboardStats(
            totalClients = totalClients,
            totalDebt = totalDebt,
            totalReceived = totalReceived,
            netBalance = netBalance,
            invoicesCount = invoicesCount,
            paymentsCount = paymentsCount,
            itemsCount = itemsCount,
            recentActivities = sortedActivities,
            topDebtors = topDebtors,
            topActiveClients = topActiveClients,
            currencyDebts = currencyDebts,
            currencyPayments = currencyPayments
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardStats()
    )

    // --- Search Queries & Filtering State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Smart Search Results
    val smartSearchResults = combine(searchQuery, clients, items, invoices, payments) { query, cList, iList, invList, pList ->
        if (query.isBlank()) return@combine SmartSearchResult()
        
        val q = query.lowercase().trim()
        val clientsResult = cList.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
        val itemsResult = iList.filter { it.name.lowercase().contains(q) || it.barcode.contains(q) || it.category.lowercase().contains(q) }
        val invoicesResult = invList.filter { it.invoiceNumber.lowercase().contains(q) || it.clientName.lowercase().contains(q) }
        
        SmartSearchResult(
            clients = clientsResult,
            items = itemsResult,
            invoices = invoicesResult
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SmartSearchResult()
    )

    // --- Invoice Creation Draft State ---
    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient = _selectedClient.asStateFlow()

    private val _invoiceCart = MutableStateFlow<List<CartItem>>(emptyList())
    val invoiceCart = _invoiceCart.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount = _discount.asStateFlow()

    private val _taxRate = MutableStateFlow(15.0) // 15% default tax
    val taxRate = _taxRate.asStateFlow()

    private val _invoiceNotes = MutableStateFlow("")
    val invoiceNotes = _invoiceNotes.asStateFlow()

    fun setInvoiceClient(client: Client?) {
        _selectedClient.value = client
    }

    fun addItemToCart(item: Item, qty: Int = 1) {
        val current = _invoiceCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index != -1) {
            val updated = current[index].copy(quantity = current[index].quantity + qty)
            current[index] = updated
        } else {
            current.add(CartItem(item, qty, item.sellingPrice))
        }
        _invoiceCart.value = current
    }

    fun removeCartItem(cartItem: CartItem) {
        _invoiceCart.value = _invoiceCart.value.filter { it.item.id != cartItem.item.id }
    }

    fun updateCartItemQuantity(cartItem: CartItem, newQty: Int) {
        if (newQty <= 0) {
            removeCartItem(cartItem)
            return
        }
        _invoiceCart.value = _invoiceCart.value.map {
            if (it.item.id == cartItem.item.id) it.copy(quantity = newQty) else it
        }
    }

    fun updateCartItemPrice(cartItem: CartItem, newPrice: Double) {
        _invoiceCart.value = _invoiceCart.value.map {
            if (it.item.id == cartItem.item.id) it.copy(customPrice = newPrice) else it
        }
    }

    fun setDiscount(amount: Double) {
        _discount.value = amount
    }

    fun setTaxRate(rate: Double) {
        _taxRate.value = rate
    }

    fun setInvoiceNotes(notes: String) {
        _invoiceNotes.value = notes
    }

    fun clearInvoiceDraft() {
        _selectedClient.value = null
        _invoiceCart.value = emptyList()
        _discount.value = 0.0
        _taxRate.value = 15.0
        _invoiceNotes.value = ""
    }

    // Save Final Detailed Invoice
    fun saveDetailedInvoice(
        isDraft: Boolean = false,
        currency: String = "الريال اليمني",
        isCreditOverride: Boolean = false,
        overrideAuthorizer: String? = null,
        overrideReason: String? = null,
        overrideAmount: Double = 0.0,
        onSuccess: (Int) -> Unit
    ) {
        val client = _selectedClient.value ?: return
        val cart = _invoiceCart.value
        if (cart.isEmpty() && !isDraft) return

        viewModelScope.launch {
            val settings = storeSettings.value
            val nextNum = settings.lastInvoiceNumber + 1
            val invoiceNumber = "${settings.invoicePrefix}$nextNum"

            val subtotal = cart.sumOf { it.customPrice * it.quantity }
            val taxAmount = (subtotal - _discount.value) * (_taxRate.value / 100.0)
            val totalAmount = (subtotal - _discount.value + taxAmount).coerceAtLeast(0.0)

            val invoice = Invoice(
                invoiceNumber = invoiceNumber,
                clientId = client.id,
                clientName = client.name,
                isQuickInvoice = false,
                discount = _discount.value,
                taxRate = _taxRate.value,
                notes = _invoiceNotes.value,
                isDraft = isDraft,
                totalAmount = totalAmount,
                paidAmount = 0.0, // initially zero
                remainingAmount = totalAmount,
                currency = currency.ifBlank { "الريال اليمني" },
                isCreditOverride = isCreditOverride,
                overrideAuthorizer = overrideAuthorizer,
                overrideReason = overrideReason,
                overrideAmount = overrideAmount
            )

            val invoiceItems = cart.map {
                InvoiceItem(
                    invoiceId = 0, // will be overwritten in transaction
                    itemId = it.item.id,
                    itemName = it.item.name,
                    quantity = it.quantity,
                    unitPrice = it.customPrice,
                    totalPrice = it.customPrice * it.quantity
                )
            }

            val id = repository.insertInvoice(invoice, invoiceItems)
            repository.saveSettings(settings.copy(lastInvoiceNumber = nextNum))
            clearInvoiceDraft()
            triggerAutoDriveBackup()
            onSuccess(id.toInt())
        }
    }

    // Save Quick Invoice
    fun saveQuickInvoice(
        client: Client,
        description: String,
        amount: Double,
        currency: String = "الريال اليمني",
        isCreditOverride: Boolean = false,
        overrideAuthorizer: String? = null,
        overrideReason: String? = null,
        overrideAmount: Double = 0.0,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val settings = storeSettings.value
            val nextNum = settings.lastInvoiceNumber + 1
            val invoiceNumber = "${settings.invoicePrefix}$nextNum"

            val invoice = Invoice(
                invoiceNumber = invoiceNumber,
                clientId = client.id,
                clientName = client.name,
                isQuickInvoice = true,
                description = description,
                discount = 0.0,
                taxRate = 0.0,
                isDraft = false,
                totalAmount = amount,
                paidAmount = 0.0,
                remainingAmount = amount,
                currency = currency.ifBlank { "الريال اليمني" },
                isCreditOverride = isCreditOverride,
                overrideAuthorizer = overrideAuthorizer,
                overrideReason = overrideReason,
                overrideAmount = overrideAmount
            )

            repository.insertInvoice(invoice, emptyList())
            repository.saveSettings(settings.copy(lastInvoiceNumber = nextNum))
            triggerAutoDriveBackup()
            onSuccess()
        }
    }

    // --- Client Operations ---
    fun addClient(
        name: String,
        phone: String,
        address: String,
        email: String,
        notes: String,
        classification: String,
        initialBalance: Double = 0.0,
        imageUri: String? = null,
        creditLimit: Double = 0.0,
        creditWarningThreshold: Double = 80.0
    ) {
        viewModelScope.launch {
            val client = Client(
                name = name,
                phone = phone,
                address = address,
                email = email,
                notes = notes,
                classification = classification,
                balance = initialBalance,
                imageUri = imageUri,
                creditLimit = creditLimit,
                creditWarningThreshold = creditWarningThreshold
            )
            repository.insertClient(client)
            triggerAutoDriveBackup()
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client)
            triggerAutoDriveBackup()
        }
    }

    fun toggleClientPin(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client.copy(isPinned = !client.isPinned))
            triggerAutoDriveBackup()
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
            triggerAutoDriveBackup()
        }
    }

    // --- Item Operations ---
    fun addItem(name: String, barcode: String, category: String, purchasePrice: Double, sellingPrice: Double, quantity: Int, minQty: Int, imageUri: String? = null) {
        viewModelScope.launch {
            val item = Item(
                name = name,
                barcode = barcode,
                category = category,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                quantity = quantity,
                minQuantityAlert = minQty,
                imageUri = imageUri
            )
            repository.insertItem(item)
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    // --- Payment Operations ---
    fun addPayment(
        clientId: Int,
        amount: Double,
        paymentMethod: String,
        notes: String,
        invoiceId: Int? = null,
        currency: String? = null,
        voucherNumber: String? = null,
        collectorName: String? = null,
        transferNumber: String? = null,
        receiptImageUri: String? = null
    ) {
        viewModelScope.launch {
            val defaultCurr = storeSettings.value.currency.ifBlank { "الريال اليمني" }
            val finalCurrency = currency ?: defaultCurr
            val payment = Payment(
                clientId = clientId,
                invoiceId = invoiceId,
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes,
                currency = finalCurrency,
                voucherNumber = voucherNumber?.ifBlank { null },
                collectorName = collectorName?.ifBlank { null },
                transferNumber = transferNumber?.ifBlank { null },
                receiptImageUri = receiptImageUri?.ifBlank { null }
            )
            repository.insertPayment(payment)
            triggerAutoDriveBackup()
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            triggerAutoDriveBackup()
        }
    }

    // --- Settings Operations ---
    fun updateStoreSettings(settings: StoreSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    // --- All Clients PDF Backup Export ---
    fun generateAndSaveAllClientsPdfNow(): File? {
        val currentSettings = storeSettings.value
        val file = com.example.util.exportAllClientsStatementToPdf(
            context = app,
            clientsList = clients.value,
            invoicesList = invoices.value,
            paymentsList = payments.value,
            settings = currentSettings
        )
        if (file != null) {
            viewModelScope.launch {
                repository.insertLog("تصدير PDF", "العملاء", "تم حفظ كشوفات العملاء في مجلد Downloads: ${file.name}")
            }
        }
        return file
    }

    // --- Invoice Delete ---
    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
        }
    }

    fun getInvoiceItemsFlow(invoiceId: Int): Flow<List<InvoiceItem>> {
        return repository.getInvoiceItemsFlow(invoiceId)
    }

    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItem> {
        return repository.getInvoiceItems(invoiceId)
    }

    suspend fun getInvoiceById(invoiceId: Int): Invoice? {
        return repository.getInvoiceById(invoiceId)
    }

    // --- App Security PIN & Biometric State ---
    private val _appLocked = MutableStateFlow(false)
    val appLocked = _appLocked.asStateFlow()

    private val _securityPin = MutableStateFlow<String?>(null)
    val securityPin = _securityPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _biometricUserName = MutableStateFlow("")
    val biometricUserName = _biometricUserName.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    init {
        // Read lock PIN and Biometrics from SharedPrefs on startup
        val prefs = app.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        val savedPin = prefs.getString("pin_code", null)
        val savedBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
        val savedBiometricUser = prefs.getString("biometric_user_name", "") ?: ""

        _securityPin.value = savedPin
        _isBiometricEnabled.value = savedBiometricEnabled
        _biometricUserName.value = savedBiometricUser

        if (!savedPin.isNullOrBlank() || savedBiometricEnabled) {
            _appLocked.value = true
        }

        // Read theme mode preference (default false = Light Mode / النهار)
        val themePrefs = app.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        _isDarkMode.value = themePrefs.getBoolean("is_dark_mode", false)
    }

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        val themePrefs = app.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        themePrefs.edit().putBoolean("is_dark_mode", newMode).apply()
    }

    fun setSecurityPin(pin: String?) {
        val prefs = app.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        if (pin.isNullOrBlank()) {
            prefs.edit().remove("pin_code").apply()
            _securityPin.value = null
            if (!_isBiometricEnabled.value) {
                _appLocked.value = false
            }
        } else {
            prefs.edit().putString("pin_code", pin).apply()
            _securityPin.value = pin
            _appLocked.value = true
        }
        viewModelScope.launch {
            repository.insertLog("أمان", "الإعدادات", if (pin.isNullOrBlank()) "تم إيقاف قفل الحماية PIN" else "تم تفعيل قفل الحماية برقم PIN")
        }
    }

    fun setBiometricSettings(enabled: Boolean, userName: String) {
        val cleanName = userName.trim()
        val prefs = app.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("biometric_enabled", enabled)
            .putString("biometric_user_name", cleanName)
            .apply()

        _isBiometricEnabled.value = enabled
        if (cleanName.isNotBlank()) {
            _biometricUserName.value = cleanName
        }

        if (enabled) {
            _appLocked.value = true
        } else if (_securityPin.value.isNullOrBlank()) {
            _appLocked.value = false
        }

        viewModelScope.launch {
            repository.insertLog(
                "أمان",
                "الإعدادات",
                if (enabled) "تم تفعيل قفل البصمة للمستخدم: $cleanName" else "تم إيقاف قفل البصمة"
            )
        }
    }

    fun updateBiometricUserName(userName: String) {
        val cleanName = userName.trim()
        val prefs = app.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("biometric_user_name", cleanName).apply()
        _biometricUserName.value = cleanName
        viewModelScope.launch {
            repository.insertLog("أمان", "الإعدادات", "تم تحديث اسم مستخدم البصمة إلى: $cleanName")
        }
    }

    fun unlockApp(pin: String): Boolean {
        if (_securityPin.value == pin) {
            _appLocked.value = false
            viewModelScope.launch {
                repository.insertLog("أمان", "الدخول", "تم إلغاء قفل التطبيق عبر رمز PIN")
            }
            return true
        }
        return false
    }

    fun unlockAppByBiometric(): Boolean {
        _appLocked.value = false
        viewModelScope.launch {
            val user = _biometricUserName.value.ifBlank { "المستخدم المسجل" }
            repository.insertLog("أمان", "الدخول", "تم إلغاء قفل التطبيق بواسطة البصمة للمستخدم: $user")
        }
        return true
    }

    fun lockApp() {
        if (!_securityPin.value.isNullOrBlank() || _isBiometricEnabled.value) {
            _appLocked.value = true
        }
    }

    // --- EXPORT AND BACKUP OPERATIONS (Real database files operations & CSV Generation) ---
    fun exportDatabase(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = app.getDatabasePath("client_accounts_pro_db")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val exportDir = File(downloadsDir, "backup")
                if (!exportDir.exists()) exportDir.mkdirs()

                val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
                val timestampStr = format.format(Date())
                val backupDbFile = File(exportDir, "backup_accounts_pro_$timestampStr.db")
                val backupJsonFile = File(exportDir, "backup_accounts_pro_$timestampStr.json")

                if (dbFile.exists()) {
                    dbFile.copyTo(backupDbFile, overwrite = true)
                }

                try {
                    backupManager.createAndUploadBackup(AppDatabase.getDatabase(app))
                    if (backupManager.latestBackupJsonFile.exists()) {
                        backupManager.latestBackupJsonFile.copyTo(backupJsonFile, overwrite = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    repository.insertLog("نسخ احتياطي", "النظام", "تصدير قاعدة البيانات بنجاح إلى مجلد Download/backup")
                    onSuccess("تم التصدير بنجاح إلى ذاكرة الهاتف بداخل مجلد Download/backup!\n(تم حفظ الملفين: ${backupDbFile.name} و ${backupJsonFile.name})")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("فشل التصدير: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importDatabase(backupFilePath: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = File(backupFilePath)
                if (backupFile.exists()) {
                    // Close db before overwrite
                    AppDatabase.getDatabase(app).close()
                    val dbFile = app.getDatabasePath("client_accounts_pro_db")
                    backupFile.copyTo(dbFile, overwrite = true)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(app, "تم استعادة قاعدة البيانات بنجاح! الرجاء إعادة تشغيل التطبيق.", Toast.LENGTH_LONG).show()
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("ملف النسخة الاحتياطية غير موجود!")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("فشل الاستيراد: ${e.localizedMessage}")
                }
            }
        }
    }

    // Real CSV/Excel Export for Clients and Statements
    fun exportClientsToCSV(): String {
        val sBuilder = java.lang.StringBuilder()
        sBuilder.append("ID,الاسم,الهاتف,العنوان,البريد الالكتروني,التصنيف,الرصيد\n")
        clients.value.forEach { c ->
            sBuilder.append("${c.id},\"${c.name}\",\"${c.phone}\",\"${c.address}\",\"${c.email}\",\"${c.classification}\",${c.balance}\n")
        }
        return sBuilder.toString()
    }

    fun exportStatementToCSV(client: Client, invoices: List<Invoice>, payments: List<Payment>): String {
        val sBuilder = java.lang.StringBuilder()
        sBuilder.append("كشف حساب العميل: ${client.name}\n")
        sBuilder.append("الهاتف: ${client.phone}\n\n")
        sBuilder.append("التاريخ,العملية,الوصف,المدين,الدائن,الرصيد\n")

        // Chronological sort
        val transactions = mutableListOf<LedgerTransaction>()
        invoices.filter { !it.isDraft }.forEach {
            transactions.add(LedgerTransaction(it.date, "فاتورة", "فاتورة رقم ${it.invoiceNumber}", it.totalAmount, 0.0))
        }
        payments.forEach {
            transactions.add(LedgerTransaction(it.date, "دفعة مستلمة", "طريقة الدفع: ${it.paymentMethod} ${it.notes ?: ""}", 0.0, it.amount))
        }
        transactions.sortBy { it.date }

        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        var runningBalance = 0.0
        transactions.forEach { t ->
            runningBalance += (t.debit - t.credit)
            val dateStr = format.format(Date(t.date))
            sBuilder.append("$dateStr,\"${t.type}\",\"${t.details}\",${t.debit},${t.credit},$runningBalance\n")
        }
        return sBuilder.toString()
    }

    // --- Installment Operations ---
    fun addInstallment(installment: Installment) {
        viewModelScope.launch {
            val id = repository.insertInstallment(installment)
            val insertedInst = installment.copy(id = id.toInt())
            com.example.util.InstallmentManager.scheduleExactAlarm(app, insertedInst)
            triggerAutoDriveBackup()
        }
    }

    fun updateInstallment(installment: Installment) {
        viewModelScope.launch {
            repository.updateInstallment(installment)
            if (installment.isPaid) {
                com.example.util.InstallmentManager.cancelAlarm(app, installment.id)
            } else {
                com.example.util.InstallmentManager.scheduleExactAlarm(app, installment)
            }
            triggerAutoDriveBackup()
        }
    }

    fun deleteInstallment(installment: Installment) {
        viewModelScope.launch {
            repository.deleteInstallment(installment)
            com.example.util.InstallmentManager.cancelAlarm(app, installment.id)
            triggerAutoDriveBackup()
        }
    }

    fun markInstallmentPaid(installment: Installment, paidAmount: Double = installment.amount) {
        viewModelScope.launch {
            val updated = installment.copy(isPaid = true, paidAmount = paidAmount)
            repository.updateInstallment(updated)
            com.example.util.InstallmentManager.cancelAlarm(app, installment.id)

            // Check recurring installment
            val nextDueDate = com.example.util.InstallmentManager.calculateNextDueDate(installment.dueDate, installment.recurrence)
            if (nextDueDate != null) {
                val nextInstallment = installment.copy(
                    id = 0,
                    dueDate = nextDueDate,
                    isPaid = false,
                    paidAmount = 0.0
                )
                val newId = repository.insertInstallment(nextInstallment)
                com.example.util.InstallmentManager.scheduleExactAlarm(app, nextInstallment.copy(id = newId.toInt()))
            }

            // Also record a payment for the client
            repository.insertPayment(
                Payment(
                    clientId = installment.clientId,
                    amount = paidAmount,
                    date = System.currentTimeMillis(),
                    paymentMethod = "نقدي",
                    notes = "سداد قسط للعميل ${installment.clientName}",
                    currency = installment.currency
                )
            )

            triggerAutoDriveBackup()
        }
    }
}

// --- Data classes representing various UI States ---

enum class ActivityType { INVOICE, PAYMENT }

data class ActivityItem(
    val id: Int,
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: Long,
    val isDraft: Boolean,
    val referenceId: Int
)

data class DashboardStats(
    val totalClients: Int = 0,
    val totalDebt: Double = 0.0,
    val totalReceived: Double = 0.0,
    val netBalance: Double = 0.0,
    val invoicesCount: Int = 0,
    val paymentsCount: Int = 0,
    val itemsCount: Int = 0,
    val recentActivities: List<ActivityItem> = emptyList(),
    val topDebtors: List<Client> = emptyList(),
    val topActiveClients: List<Client> = emptyList(),
    val currencyDebts: Map<String, Double> = emptyMap(),
    val currencyPayments: Map<String, Double> = emptyMap()
)

data class SmartSearchResult(
    val clients: List<Client> = emptyList(),
    val items: List<Item> = emptyList(),
    val invoices: List<Invoice> = emptyList()
)

data class CartItem(
    val item: Item,
    val quantity: Int,
    val customPrice: Double
)

data class LedgerTransaction(
    val date: Long,
    val type: String,
    val details: String,
    val debit: Double, // client owes us
    val credit: Double // client paid us
)

// Factory for ViewModel
class AppViewModelFactory(
    private val app: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(app, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
