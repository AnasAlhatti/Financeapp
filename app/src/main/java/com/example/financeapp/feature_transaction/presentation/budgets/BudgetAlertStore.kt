package com.example.financeapp.feature_transaction.presentation.budgets

import android.content.Context
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.YearMonth

/** Stores last notified level per (category + month). Avoids repeated alerts. */
@Singleton
class BudgetAlertStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)

    private fun key(category: String, ym: YearMonth) = "${ym.year}-${ym.monthValue}:$category"

    /** 0=none, 1=CAUTION(60%), 2=WARNING(80%), 3=OVER(100%). */
    fun getLevel(category: String, ym: YearMonth): Int =
        prefs.getInt(key(category, ym), 0)

    fun setLevel(category: String, ym: YearMonth, level: Int) {
        prefs.edit().putInt(key(category, ym), level).apply()
    }

    fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT < 33) true
        else ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
