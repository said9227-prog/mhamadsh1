package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.FormatUtils

class InstallmentAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val installmentId = intent.getIntExtra("installmentId", 0)
        val clientName = intent.getStringExtra("clientName") ?: "العميل"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val currency = intent.getStringExtra("currency") ?: "الريال اليمني"

        showNotification(context, installmentId, clientName, amount, currency)
    }

    private fun showNotification(
        context: Context,
        installmentId: Int,
        clientName: String,
        amount: Double,
        currency: String
    ) {
        val channelId = "installment_reminders_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تذكيرات الأقساط المستحقة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة إشعارات للتنبيه بمواعيد استحقاق أقساط العملاء"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openInstallments", true)
            putExtra("installmentId", installmentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            installmentId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = FormatUtils.formatAmount(amount)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ موعد قسط مستحق: $clientName")
            .setContentText("تذكرة بموعد قسط مبلغ $formattedAmount $currency للعميل $clientName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(installmentId, notification)
    }
}
