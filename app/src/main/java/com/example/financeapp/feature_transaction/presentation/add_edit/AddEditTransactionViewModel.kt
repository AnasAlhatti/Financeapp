package com.example.financeapp.feature_transaction.presentation.add_edit

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs


@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val useCases: TransactionUseCases,
    private val budgetUseCases: com.example.financeapp.feature_transaction.domain.use_case.budget.BudgetUseCases,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state = _state.asStateFlow()

    fun loadForEdit(transactionId: Int?) {
        if (transactionId == null || transactionId == -1) return
        viewModelScope.launch {
            val t = useCases.getTransactionById(transactionId)
            if (t != null) {
                _state.value = _state.value.copy(
                    id = t.id,
                    title = t.title,
                    amountInput = kotlin.math.abs(t.amount).toString(), // show absolute value
                    category = t.category,
                    dateMillis = t.date,
                    isExpense = t.amount < 0
                )
            }
        }
    }

    fun onTitleChange(v: String) { _state.value = _state.value.copy(title = v, error = null) }
    fun onAmountChange(v: String) { _state.value = _state.value.copy(amountInput = v, error = null) }
    fun onCategoryChange(v: String) { _state.value = _state.value.copy(category = v) }
    fun onDateChange(millis: Long) { _state.value = _state.value.copy(dateMillis = millis) }
    fun onTypeChange(isExpense: Boolean) {
        _state.value = _state.value.copy(isExpense = isExpense)
    }
    override fun onCleared() { super.onCleared() }


    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        val amount = s.amountInput.toDoubleOrNull()
        val titleOk = s.title.isNotBlank()
        val amountOk = amount != null

        if (!titleOk || !amountOk) {
            _state.value = s.copy(error =
                if (!titleOk) "Title required" else "Amount must be a number")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val signedAmount = if (s.isExpense) -kotlin.math.abs(amount!!) else kotlin.math.abs(amount!!)
            val tx = com.example.financeapp.feature_transaction.domain.model.Transaction(
                id = s.id,
                title = s.title.trim(),
                amount = signedAmount,
                category = s.category,
                date = s.dateMillis
            )
            useCases.addTransaction(tx)
            _state.value = _state.value.copy(isSaving = false)
            onSuccess()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkBudgetThresholdsAfterSave(tx: com.example.financeapp.feature_transaction.domain.model.Transaction) {
        // Only for expenses within this month
        if (tx.amount >= 0) return
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val ym = YearMonth.now()
            val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val budgets = budgetUseCases.getBudgets().first()
            val budget = budgets.firstOrNull { it.category == tx.category } ?: return@launch

            val txs = useCases.getTransactions().first().filter { it.date in start until end && it.category == tx.category && it.amount < 0 }
            val spentAfter = txs.sumOf { abs(it.amount) }
            val spentBefore = spentAfter - abs(tx.amount)

            val limit = budget.limitAmount
            if (limit <= 0) return@launch

            val beforePct = (spentBefore / limit) * 100.0
            val afterPct  = (spentAfter  / limit) * 100.0

            // fire only on crossing upward
            val crossed100 = beforePct < 100 && afterPct >= 100
            val crossed80  = beforePct < 80  && afterPct >= 80

            if (crossed100) {
                com.example.financeapp.feature_transaction.presentation.budgets.BudgetNotifier.notifyThreshold(appContext, tx.category, 100)
            } else if (crossed80) {
                com.example.financeapp.feature_transaction.presentation.budgets.BudgetNotifier.notifyThreshold(appContext, tx.category, 80)
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val id = _state.value.id ?: return
        viewModelScope.launch {
            // minimal fetch to get full object (or build from state)
            useCases.getTransactionById(id)?.let { useCases.deleteTransaction(it) }
            onSuccess()
        }
    }
}
