package com.example.financeapp.feature_transaction.presentation.budgets

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.financeapp.R

object BudgetNotifier {
    private const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyThreshold(context: Context, category: String, percent: Int) {
        ensureChannel(context)

        val level = when {
            percent >= 100 -> BudgetLevel.OVER
            percent >= 80 -> BudgetLevel.WARNING
            percent >= 60 -> BudgetLevel.CAUTION
            else -> BudgetLevel.OK
        }

        val title = when (level) {
            BudgetLevel.OVER -> "Budget exceeded"
            BudgetLevel.WARNING -> "Budget warning"
            BudgetLevel.CAUTION -> "Budget caution"
            BudgetLevel.OK -> "Budget update"
        }
        val text = when (level) {
            BudgetLevel.OVER -> "You’ve exceeded 100% for $category."
            else -> "You’ve reached $percent% of your $category budget."
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(notifColorIntForLevel(context, level))   // ✅ tint by level
            .setColorized(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify((category.hashCode() xor percent).and(0x7fffffff), notif)
    }
}
