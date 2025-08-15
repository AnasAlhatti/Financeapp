package com.example.financeapp.feature_transaction.presentation.budgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import com.example.financeapp.R

enum class BudgetLevel { OK, CAUTION, WARNING, OVER }

/** Decide level by ratio (spent/limit). */
fun levelForRatio(ratio: Double): BudgetLevel = when {
    ratio > 1.0 -> BudgetLevel.OVER
    ratio >= 0.8 -> BudgetLevel.WARNING
    ratio >= 0.6 -> BudgetLevel.CAUTION
    else -> BudgetLevel.OK
}

/** Compose colors from XML resources. */
@Composable
fun colorForLevel(level: BudgetLevel): Color = when (level) {
    BudgetLevel.OK -> colorResource(id = R.color.budget_ok)
    BudgetLevel.CAUTION -> colorResource(id = R.color.budget_caution)
    BudgetLevel.WARNING -> colorResource(id = R.color.budget_warning)
    BudgetLevel.OVER -> colorResource(id = R.color.budget_over)
}

/** Slightly translucent container background for chips/cards. */
fun containerFrom(base: Color): Color = base.copy(alpha = 0.15f)

/** Int color for NotificationCompat.setColor(). */
fun notifColorIntForLevel(ctx: Context, level: BudgetLevel): Int = when (level) {
    BudgetLevel.OK -> ContextCompat.getColor(ctx, R.color.budget_ok)
    BudgetLevel.CAUTION -> ContextCompat.getColor(ctx, R.color.budget_caution)
    BudgetLevel.WARNING -> ContextCompat.getColor(ctx, R.color.budget_warning)
    BudgetLevel.OVER -> ContextCompat.getColor(ctx, R.color.budget_over)
}
