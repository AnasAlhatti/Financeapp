package com.example.financeapp.feature_transaction.presentation.budgets

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.use_case.TransactionUseCases
import com.example.financeapp.feature_transaction.domain.use_case.budget.BudgetUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs

data class BudgetWarning(
    val category: String,
    val spent: Double,
    val limit: Double,
    val ratio: Double // 0..inf
)

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class BudgetsSummaryViewModel @Inject constructor(
    private val budgets: BudgetUseCases,
    private val tx: TransactionUseCases
) : ViewModel() {

    private val _warnings = MutableStateFlow<List<BudgetWarning>>(emptyList())
    val warnings: StateFlow<List<BudgetWarning>> = _warnings.asStateFlow()

    init {
        observe()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observe() {
        val zone = ZoneId.systemDefault()
        val ym = YearMonth.now()
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        combine(budgets.getBudgets(), tx.getTransactions()) { bs, txs ->
            val monthTxs = txs.filter { it.date in start until end && it.amount < 0 }
            bs.mapNotNull { b ->
                if (b.limitAmount <= 0) return@mapNotNull null
                val spent = monthTxs.filter { it.category == b.category }.sumOf { abs(it.amount) }
                val r = spent / b.limitAmount
                if (r >= 0.8) BudgetWarning(b.category, spent, b.limitAmount, r) else null
            }.sortedByDescending { it.ratio }
        }.onEach { _warnings.value = it }
            .launchIn(viewModelScope)
    }
}
