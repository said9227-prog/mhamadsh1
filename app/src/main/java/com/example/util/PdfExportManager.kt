package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.Payment
import com.example.data.model.StoreSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility to format Arabic timestamp with 12h time (ص/م)
 */
fun formatPdfTimestamp(timeMillis: Long): String {
    val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val arabicDayName = when (dayOfWeek) {
        Calendar.SUNDAY -> "الأحد"
        Calendar.MONDAY -> "الإثنين"
        Calendar.TUESDAY -> "الثلاثاء"
        Calendar.WEDNESDAY -> "الأربعاء"
        Calendar.THURSDAY -> "الخميس"
        Calendar.FRIDAY -> "الجمعة"
        Calendar.SATURDAY -> "السبت"
        else -> ""
    }
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minuteStr = String.format("%02d", cal.get(Calendar.MINUTE))
    val amPmStr = if (cal.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"

    return "$arabicDayName ${dateFmt.format(cal.time)} | $hour12:$minuteStr $amPmStr"
}

/**
 * Exports a comprehensive PDF report containing all clients, their debt balances,
 * invoice counts, and payment counts into the device's public Download folder as:
 * `نسخة كشوفات العملاء.pdf`
 */
fun exportAllClientsStatementToPdf(
    context: Context,
    clientsList: List<Client>,
    invoicesList: List<Invoice>,
    paymentsList: List<Payment>,
    settings: StoreSettings
): File? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595 x 842 pt)
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    val subtitlePaint = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
    }

    val textPaint = Paint().apply {
        textSize = 10f
        color = Color.BLACK
        textAlign = Paint.Align.RIGHT
    }

    val headerPaint = Paint().apply {
        textSize = 10f
        isFakeBoldText = true
        color = Color.BLACK
        textAlign = Paint.Align.RIGHT
    }

    val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    var y = 45f

    // Header Title
    canvas.drawText("تقرير شـامل لجميع كشوفات العملاء والديون", 595f / 2f, y, titlePaint)
    y += 22f
    canvas.drawText("نسخة احتياطية رسمية - ${settings.storeName}", 595f / 2f, y, subtitlePaint)
    y += 25f

    val formattedTime = formatPdfTimestamp(System.currentTimeMillis())

    canvas.drawText("تاريخ التقرير والتحديث: $formattedTime", 545f, y, textPaint)
    y += 18f
    if (settings.storePhone.isNotBlank()) {
        canvas.drawText("هاتف المتجر: ${settings.storePhone}", 545f, y, textPaint)
        y += 18f
    }

    val totalDebts = clientsList.filter { it.balance > 0 }.sumOf { it.balance }
    val totalClientsCount = clientsList.size
    val totalInvoicesCount = invoicesList.size
    val totalPaymentsCount = paymentsList.size

    canvas.drawText(
        "عدد العملاء: $totalClientsCount | الفواتير: $totalInvoicesCount | الدفعات: $totalPaymentsCount",
        545f,
        y,
        textPaint
    )
    y += 18f

    val summaryPaint = Paint(textPaint).apply {
        isFakeBoldText = true
        color = Color.RED
    }
    canvas.drawText(
        "إجمالي الديون المستحقة على العملاء: ${FormatUtils.formatAmount(totalDebts)} ${settings.currency}",
        545f,
        y,
        summaryPaint
    )
    y += 22f

    canvas.drawLine(50f, y, 545f, y, linePaint)
    y += 20f

    // Table Column Headers (RTL format)
    val colNameX = 545f
    val colPhoneX = 390f
    val colInvX = 270f
    val colPayX = 180f
    val colBalX = 90f

    canvas.drawText("اسم العميل", colNameX, y, headerPaint)
    canvas.drawText("الهاتف", colPhoneX, y, headerPaint)
    canvas.drawText("الفواتير", colInvX, y, headerPaint)
    canvas.drawText("الدفعات", colPayX, y, headerPaint)
    canvas.drawText("الرصيد المتبقي", colBalX, y, headerPaint)

    y += 10f
    canvas.drawLine(50f, y, 545f, y, linePaint)
    y += 18f

    // Loop through clients
    val sortedClients = clientsList.sortedByDescending { it.balance }
    sortedClients.forEach { client ->
        val cInvoices = invoicesList.filter { it.clientId == client.id }
        val cPayments = paymentsList.filter { it.clientId == client.id }

        val clientDisplayName = if (client.name.length > 22) client.name.take(20) + ".." else client.name
        canvas.drawText(clientDisplayName, colNameX, y, textPaint)
        canvas.drawText(client.phone.ifEmpty { "-" }, colPhoneX, y, textPaint)
        canvas.drawText("${cInvoices.size} فاتورة", colInvX, y, textPaint)
        canvas.drawText("${cPayments.size} دفعة", colPayX, y, textPaint)

        val balPaint = Paint(textPaint).apply {
            isFakeBoldText = true
            color = when {
                client.balance > 0 -> Color.RED
                client.balance < 0 -> Color.parseColor("#059669")
                else -> Color.DKGRAY
            }
        }
        canvas.drawText("${FormatUtils.formatAmount(client.balance)} ${settings.currency}", colBalX, y, balPaint)

        y += 20f
        if (y > 800f) {
            // Canvas limit reached for page 1
            return@forEach
        }
    }

    pdfDocument.finishPage(page)

    // Destination File inside public Download folder
    var destinationFile: File? = null
    try {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        destinationFile = File(downloadDir, "نسخة كشوفات العملاء.pdf")
        val fos = FileOutputStream(destinationFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback to internal app files directory if public storage permission is restricted
        try {
            destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "نسخة كشوفات العملاء.pdf")
            val fos = FileOutputStream(destinationFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    return destinationFile
}
