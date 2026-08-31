package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Installment
import com.example.receiver.InstallmentAlarmReceiver
import java.util.Calendar

object InstallmentManager {

    fun scheduleExactAlarm(context: Context, installment: Installment) {
        if (installment.isPaid || installment.dueDate <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, InstallmentAlarmReceiver::class.java).apply {
            putExtra("installmentId", installment.id)
            putExtra("clientName", installment.clientName)
            putExtra("amount", installment.amount)
            putExtra("currency", installment.currency)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            installment.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        installment.dueDate,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        installment.dueDate,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    installment.dueDate,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                installment.dueDate,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, installmentId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, InstallmentAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            installmentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextDueDate(currentDueDate: Long, recurrence: String): Long? {
        if (recurrence == "بدون تكرار") return null

        val cal = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when (recurrence) {
            "يومي" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "أسبوعي" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "شهري" -> cal.add(Calendar.MONTH, 1)
            "سنوي" -> cal.add(Calendar.YEAR, 1)
            else -> return null
        }
        return cal.timeInMillis
    }
}
