package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AppRepository(private val db: AppDatabase) {

    private val clientDao = db.clientDao()
    private val itemDao = db.itemDao()
    private val invoiceDao = db.invoiceDao()
    private val paymentDao = db.paymentDao()
    private val auditLogDao = db.auditLogDao()
    private val storeSettingsDao = db.storeSettingsDao()

    // --- Audit Log Helper ---
    suspend fun insertLog(operationType: String, tableName: String, details: String) {
        withContext(Dispatchers.IO) {
            auditLogDao.insertLog(
                AuditLog(
                    operationType = operationType,
                    tableName = tableName,
                    details = details
                )
            )
        }
    }

    // --- Clients ---
    fun getAllClientsFlow(): Flow<List<Client>> = clientDao.getAllClientsFlow()
    suspend fun getAllClients(): List<Client> = withContext(Dispatchers.IO) { clientDao.getAllClients() }
    fun getClientByIdFlow(id: Int): Flow<Client?> = clientDao.getClientByIdFlow(id)
    suspend fun getClientById(id: Int): Client? = withContext(Dispatchers.IO) { clientDao.getClientById(id) }

    suspend fun insertClient(client: Client): Long = withContext(Dispatchers.IO) {
        val id = clientDao.insertClient(client)
        insertLog("إضافة", "العملاء", "تم إضافة العميل: ${client.name}")
        id
    }

    suspend fun updateClient(client: Client) = withContext(Dispatchers.IO) {
        clientDao.updateClient(client)
        insertLog("تعديل", "العملاء", "تم تعديل بيانات العميل: ${client.name}")
    }

    suspend fun deleteClient(client: Client) = withContext(Dispatchers.IO) {
        clientDao.deleteClient(client)
        insertLog("حذف", "العملاء", "تم حذف العميل: ${client.name}")
    }

    suspend fun recalculateClientBalance(clientId: Int) = withContext(Dispatchers.IO) {
        val client = clientDao.getClientById(clientId) ?: return@withContext
        val invoices = invoiceDao.getInvoicesByClient(clientId).filter { !it.isDraft }
        val payments = paymentDao.getPaymentsByClient(clientId)

        val totalInvoiced = invoices.sumOf { it.totalAmount }
        val totalPaid = payments.sumOf { it.amount }
        val calculatedBalance = totalInvoiced - totalPaid

        val updatedClient = client.copy(balance = calculatedBalance)
        clientDao.updateClient(updatedClient)
    }

    // --- Items ---
    fun getAllItemsFlow(): Flow<List<Item>> = itemDao.getAllItemsFlow()
    suspend fun getAllItems(): List<Item> = withContext(Dispatchers.IO) { itemDao.getAllItems() }
    suspend fun getItemById(id: Int): Item? = withContext(Dispatchers.IO) { itemDao.getItemById(id) }
    suspend fun getItemByBarcode(barcode: String): Item? = withContext(Dispatchers.IO) { itemDao.getItemByBarcode(barcode) }

    suspend fun insertItem(item: Item): Long = withContext(Dispatchers.IO) {
        val id = itemDao.insertItem(item)
        insertLog("إضافة", "الأصناف", "تم إضافة الصنف: ${item.name}")
        id
    }

    suspend fun updateItem(item: Item) = withContext(Dispatchers.IO) {
        itemDao.updateItem(item)
        insertLog("تعديل", "الأصناف", "تم تعديل الصنف: ${item.name}")
    }

    suspend fun deleteItem(item: Item) = withContext(Dispatchers.IO) {
        itemDao.deleteItem(item)
        insertLog("حذف", "الأصناف", "تم حذف الصنف: ${item.name}")
    }

    fun searchItems(query: String): Flow<List<Item>> = itemDao.searchItems("%$query%")

    // --- Invoices ---
    fun getAllInvoicesFlow(): Flow<List<Invoice>> = invoiceDao.getAllInvoicesFlow()
    fun getInvoiceByIdFlow(id: Int): Flow<Invoice?> = invoiceDao.getInvoiceByIdFlow(id)
    suspend fun getInvoiceById(id: Int): Invoice? = withContext(Dispatchers.IO) { invoiceDao.getInvoiceById(id) }
    fun getInvoicesByClientFlow(clientId: Int): Flow<List<Invoice>> = invoiceDao.getInvoicesByClientFlow(clientId)

    suspend fun insertInvoice(invoice: Invoice, items: List<InvoiceItem>): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val invoiceId = invoiceDao.insertInvoice(invoice).toInt()
            
            // Delete old items if updating (in case id is specified, though Room update is usually separate)
            invoiceDao.deleteInvoiceItemsByInvoiceId(invoiceId)
            
            // Insert invoice items
            items.forEach { item ->
                invoiceDao.insertInvoiceItem(item.copy(invoiceId = invoiceId))
                
                // Update stock quantities if item is stored in inventory
                item.itemId?.let { storedItemId ->
                    itemDao.getItemById(storedItemId)?.let { storedItem ->
                        val newQty = (storedItem.quantity - item.quantity).coerceAtLeast(0)
                        itemDao.updateItem(storedItem.copy(quantity = newQty))
                    }
                }
            }

            // Recalculate client's balance
            recalculateClientBalance(invoice.clientId)
            
            val typeStr = if (invoice.isQuickInvoice) "سريعة" else "مفصلة"
            insertLog("إضافة", "الفواتير", "تم إنشاء فاتورة $typeStr رقم: ${invoice.invoiceNumber} للعميل: ${invoice.clientName}")
            if (invoice.isCreditOverride) {
                insertLog(
                    "تجاوز ائتماني",
                    "الائتمان",
                    "⚠️ تم منح تجاوز استثنائي للحد الائتماني بمبلغ ${invoice.overrideAmount} للفاتورة ${invoice.invoiceNumber} للعميل: ${invoice.clientName} بواسطة: ${invoice.overrideAuthorizer ?: "الإدارة"}. السبب: ${invoice.overrideReason ?: "غير محدد"}"
                )
            }
            invoiceId.toLong()
        }
    }

    suspend fun deleteInvoice(invoice: Invoice) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // Restore inventory stock before deletion
            val items = invoiceDao.getInvoiceItems(invoice.id)
            items.forEach { item ->
                item.itemId?.let { storedItemId ->
                    itemDao.getItemById(storedItemId)?.let { storedItem ->
                        val newQty = storedItem.quantity + item.quantity
                        itemDao.updateItem(storedItem.copy(quantity = newQty))
                    }
                }
            }

            invoiceDao.deleteInvoiceItemsByInvoiceId(invoice.id)
            invoiceDao.deleteInvoice(invoice)
            recalculateClientBalance(invoice.clientId)
            insertLog("حذف", "الفواتير", "تم حذف الفاتورة رقم: ${invoice.invoiceNumber} للعميل: ${invoice.clientName}")
        }
    }

    fun getInvoiceItemsFlow(invoiceId: Int): Flow<List<InvoiceItem>> = invoiceDao.getInvoiceItemsFlow(invoiceId)
    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItem> = withContext(Dispatchers.IO) { invoiceDao.getInvoiceItems(invoiceId) }

    // --- Payments ---
    fun getAllPaymentsFlow(): Flow<List<Payment>> = paymentDao.getAllPaymentsFlow()
    fun getPaymentsByClientFlow(clientId: Int): Flow<List<Payment>> = paymentDao.getPaymentsByClientFlow(clientId)
    suspend fun getPaymentsByClient(clientId: Int): List<Payment> = withContext(Dispatchers.IO) { paymentDao.getPaymentsByClient(clientId) }

    suspend fun insertPayment(payment: Payment): Long = withContext(Dispatchers.IO) {
        val id = paymentDao.insertPayment(payment)
        recalculateClientBalance(payment.clientId)
        val client = clientDao.getClientById(payment.clientId)
        insertLog("إضافة", "المدفوعات", "تم تسجيل دفعة بقيمة: ${payment.amount} للعميل: ${client?.name ?: "غير معروف"}")
        id
    }

    suspend fun deletePayment(payment: Payment) = withContext(Dispatchers.IO) {
        paymentDao.deletePayment(payment)
        recalculateClientBalance(payment.clientId)
        val client = clientDao.getClientById(payment.clientId)
        insertLog("حذف", "المدفوعات", "تم حذف دفعة بقيمة: ${payment.amount} للعميل: ${client?.name ?: "غير معروف"}")
    }

    suspend fun updatePayment(payment: Payment) = withContext(Dispatchers.IO) {
        paymentDao.updatePayment(payment)
        recalculateClientBalance(payment.clientId)
        val client = clientDao.getClientById(payment.clientId)
        insertLog("تعديل", "المدفوعات", "تم تعديل دفعة للعميل: ${client?.name ?: "غير معروف"}")
    }

    // --- Audit Logs ---
    fun getAllLogsFlow(): Flow<List<AuditLog>> = auditLogDao.getAllLogsFlow()

    // --- Store Settings ---
    fun getSettingsFlow(): Flow<StoreSettings?> = storeSettingsDao.getSettingsFlow()
    suspend fun getSettings(): StoreSettings? = withContext(Dispatchers.IO) { storeSettingsDao.getSettings() }
    
    suspend fun saveSettings(settings: StoreSettings) = withContext(Dispatchers.IO) {
        storeSettingsDao.insertOrUpdateSettings(settings)
        insertLog("تعديل", "الإعدادات", "تم تحديث إعدادات التطبيق والمتجر")
    }

    // --- Installments ---
    private val installmentDao = db.installmentDao()

    fun getAllInstallmentsFlow(): Flow<List<Installment>> = installmentDao.getAllInstallmentsFlow()
    fun getInstallmentsByClientFlow(clientId: Int): Flow<List<Installment>> = installmentDao.getInstallmentsByClientFlow(clientId)
    suspend fun getAllInstallments(): List<Installment> = withContext(Dispatchers.IO) { installmentDao.getAllInstallments() }

    suspend fun insertInstallment(installment: Installment): Long = withContext(Dispatchers.IO) {
        val id = installmentDao.insertInstallment(installment)
        insertLog("إضافة", "الأقساط", "تم إضافة قسط للعميل: ${installment.clientName} بمبلغ: ${installment.amount}")
        id
    }

    suspend fun updateInstallment(installment: Installment) = withContext(Dispatchers.IO) {
        installmentDao.updateInstallment(installment)
        insertLog("تعديل", "الأقساط", "تم تعديل قسط للعميل: ${installment.clientName}")
    }

    suspend fun deleteInstallment(installment: Installment) = withContext(Dispatchers.IO) {
        installmentDao.deleteInstallment(installment)
        insertLog("حذف", "الأقساط", "تم حذف قسط للعميل: ${installment.clientName}")
    }
}
