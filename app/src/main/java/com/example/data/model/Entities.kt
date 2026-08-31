package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val notes: String = "",
    val classification: String = "عادي", // VIP, عادي, إلخ
    val isPinned: Boolean = false,
    val imageUri: String? = null,
    val balance: Double = 0.0, // positive means they owe money, negative means credit
    val creditLimit: Double = 0.0, // الحد الائتماني (0.0 = بدون حد/غير مقيد)
    val creditWarningThreshold: Double = 80.0 // نسبة التنبيه المبكر (افتراضياً 80%)
)

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val quantity: Int = 0,
    val minQuantityAlert: Int = 5,
    val imageUri: String? = null
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val date: Long = System.currentTimeMillis(),
    val clientId: Int,
    val clientName: String,
    val isQuickInvoice: Boolean = false,
    val description: String? = null, // للفاتورة السريعة
    val discount: Double = 0.0,
    val taxRate: Double = 0.0, // نسبة مئوية
    val notes: String? = null,
    val isDraft: Boolean = false,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val currency: String = "الريال اليمني",
    val isCreditOverride: Boolean = false, // تم اعتمادها بتجاوز استثنائي للحد الائتماني
    val overrideAuthorizer: String? = null, // اسم المستخدم/المدير الذي صرح بالتجاوز
    val overrideReason: String? = null, // سبب التجاوز
    val overrideAmount: Double = 0.0 // مبلغ التجاوز عن الحد الائتماني
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val itemId: Int? = null,
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val invoiceId: Int? = null, // اختياري
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: String = "نقدي", // نقدي، إيداع، تحويل، إلخ
    val notes: String? = null,
    val currency: String = "الريال اليمني",
    val voucherNumber: String? = null, // رقم السند
    val collectorName: String? = null, // اسم المحصل
    val transferNumber: String? = null, // رقم الحوالة
    val receiptImageUri: String? = null // صورة إشعار الحوالة او الإيداع
)

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val clientName: String,
    val invoiceId: Int? = null,
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean = false,
    val paidAmount: Double = 0.0,
    val notes: String = "",
    val recurrence: String = "بدون تكرار", // بدون تكرار، يومي، أسبوعي، شهري، سنوي
    val currency: String = "الريال اليمني",
    val notificationId: Int = 0
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operationType: String, // إضافة، تعديل، حذف
    val tableName: String, // العملاء، الفواتير، الأصناف، المدفوعات
    val details: String
)

@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "الحكيمي للأدوية والمستلزمات الطبية",
    val storePhone: String = "",
    val storeAddress: String = "",
    val storeEmail: String = "",
    val storeLogoUri: String? = null,
    val currency: String = "الريال اليمني",
    val language: String = "ar",
    val isDarkMode: Boolean = false,
    val dateFormat: String = "yyyy-MM-dd",
    val invoicePrefix: String = "INV-",
    val lastInvoiceNumber: Int = 1000,
    val isAutoPdfBackupEnabled: Boolean = false,
    val autoPdfBackupHour: Int = 0, // 0 = 12 AM (منتصف الليل)
    val overdueDaysThreshold: Int = 30, // عدد أيام التأخر لاعتبار العميل متأخر بالسداد
    val overdueNoticeTemplate: String = "عزيزي العميل {اسم_العميل}، نود تذكيركم بوجود مبلغ مستحق على حسابكم بقيمة {المبلغ_المستحق} {العملة}. نرجو سرعة السداد حتى يتم استمرار منحكم مشتريات جديدة. شاكرين لكم حسن تعاونكم.",
    val loyaltyAppreciationTemplate: String = "عزيزي العميل {اسم_العميل}،\nنشكركم على هذا الوفاء والثقة المتبادلة، ونقدّر استمرار تعاملاتكم معنا، ونأمل أن نستمر معًا في هذا التعاون المميز.\nدمتم بألف خير. 🌹",
    val fastPayerDaysThreshold: Int = 7, // مهلة الأيام لاعتبار السداد سريعاً
    val loyaltyMinInvoicesCount: Int = 3 // الحد الأدنى لعدد الفواتير لتأهيل العميل الوفي
)
