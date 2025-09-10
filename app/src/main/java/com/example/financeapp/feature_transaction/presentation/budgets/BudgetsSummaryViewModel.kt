package com.example.financeapp.feature_transaction.presentation.budgets

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.use_case.transaction.TransactionUseCases
import com.example.financeapp.feature_transaction.domain.use_case.budget.BudgetUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import android.content.Context
import kotlin.math.abs
import kotlin.math.roundToInt

data class BudgetWarning(
    val category: String,
    val spent: Double,
    val limit: Double,
    val ratio: Double
)

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class BudgetsSummaryViewModel @Inject constructor(
    private val budgets: BudgetUseCases,
    private val tx: TransactionUseCases,
    private val alertStore: BudgetAlertStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _warnings = MutableStateFlow<List<BudgetWarning>>(emptyList())
    val warnings: StateFlow<List<BudgetWarning>> = _warnings.asStateFlow()

    init { observe() }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observe() {
        val zone = ZoneId.systemDefault()

        combine(budgets.getBudgets(), tx.getTransactions()) { bs, txs ->
            val ym = YearMonth.now()
            val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val monthTxs = txs.filter { it.date in start until end && it.amount < 0 }

            val warnings = bs.mapNotNull { b ->
                if (b.limitAmount <= 0) return@mapNotNull null
                val spent = monthTxs.filter { it.category == b.category }.sumOf { abs(it.amount) }
                val r = spent / b.limitAmount
                if (r >= 0.6) BudgetWarning(b.category, spent, b.limitAmount, r) else null
            }.sortedByDescending { it.ratio }

            // 🔔 Try to notify (once per level per month per category)
            maybeNotifyFor(warnings)

            warnings
        }.onEach { _warnings.value = it }
            .launchIn(viewModelScope)
    }

    private fun levelIntForRatio(r: Double): Int {
        // 0=NONE, 1=CAUTION(>=60), 2=WARNING(>=80), 3=OVER(>=100)
        return when {
            r >= 1.0 -> 3
            r >= 0.8 -> 2
            r >= 0.6 -> 1
            else -> 0
        }
    }

    private fun maybeNotifyFor(list: List<BudgetWarning>) {
        val ym = YearMonth.now()
        if (!alertStore.canPostNotifications()) return

        list.forEach { w ->
            val current = levelIntForRatio(w.ratio)
            if (current <= 0) return@forEach

            val last = alertStore.getLevel(w.category, ym)
            if (current > last) {
                // Fire a notification once per step-up (60%→80%→100%)
                val percent = (w.ratio * 100).roundToInt()
                BudgetNotifier.notifyThreshold(appContext, w.category, percent)
                alertStore.setLevel(w.category, ym, current)
            }
        }
    }
}
