package com.example.financeapp.feature_transaction.presentation.budgets

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.Budget
import com.example.financeapp.feature_transaction.domain.use_case.transaction.TransactionUseCases
import com.example.financeapp.feature_transaction.domain.use_case.budget.BudgetUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs

data class BudgetUi(
    val budget: Budget,
    val spentThisMonth: Double,
    val progress: Float,
    val over: Boolean
)

data class BudgetsState(
    val items: List<BudgetUi> = emptyList(),
    val categories: List<String> = emptyList()
)

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetUse: BudgetUseCases,
    private val txUse: TransactionUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetsState())
    val state: StateFlow<BudgetsState> = _state.asStateFlow()

    init { observe() }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observe() {
        val zone = ZoneId.systemDefault()
        val ym = YearMonth.now()
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        combine(
            budgetUse.getBudgets(),
            txUse.getTransactions()
        ) { budgets, txs ->
            val monthTxs = txs.filter { it.date in start until end }
            val categories = txs.map { it.category }.distinct().sorted()

            val uiItems = budgets.map { b ->
                val spent = monthTxs
                    .filter { it.category == b.category && it.amount < 0 }
                    .sumOf { abs(it.amount) }
                val progress = if (b.limitAmount <= 0) 0f else (spent / b.limitAmount).toFloat()
                BudgetUi(
                    budget = b,
                    spentThisMonth = spent,
                    progress = progress.coerceAtMost(1f),
                    over = spent > b.limitAmount
                )
            }.sortedBy { it.budget.category }

            BudgetsState(items = uiItems, categories = categories)
        }.onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    fun upsert(category: String, limit: Double, id: Int? = null) {
        viewModelScope.launch {
            budgetUse.upsertBudget(Budget(id, category.trim(), limit))
        }
    }

    fun delete(budget: Budget) {
        viewModelScope.launch { budgetUse.deleteBudget(budget) }
    }
}
