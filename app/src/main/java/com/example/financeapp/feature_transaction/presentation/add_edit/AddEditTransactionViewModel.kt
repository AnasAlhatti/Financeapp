package com.example.financeapp.feature_transaction.presentation.add_edit

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.Frequency
import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs


@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val useCases: TransactionUseCases,
    private val budgetUseCases: com.example.financeapp.feature_transaction.domain.use_case.budget.BudgetUseCases,
    private val recurringUse: com.example.financeapp.feature_transaction.domain.use_case.recurring.RecurringUseCases,
    private val recurringProcessor: com.example.financeapp.feature_transaction.domain.recurring.RecurringProcessor,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state = _state.asStateFlow()

    fun loadForEdit(id: Int?) {
        if (id == null) return
        viewModelScope.launch {
            useCases.getTransactionById(id)?.let { tx ->
                _state.update {
                    it.copy(
                        id = tx.id,
                        title = tx.title,
                        amountInput = kotlin.math.abs(tx.amount).toString(),
                        isExpense = tx.amount < 0,
                        category = tx.category,
                        dateMillis = tx.date,
                        // NEW: reflect persisted flags
                        isRecurring = tx.isRecurring,
                        // If you want to also restore frequency/end-date, you can look up rule by id:
                        // (optional) load rule to prefill end date and frequency
                        hasEndDate = false,
                        endDateMillis = null
                    )
                }
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
    fun onRecurringToggle(enabled: Boolean) {
        _state.update { it.copy(isRecurring = enabled) }
    }
    fun onRecurringFrequencyChange(freq: com.example.financeapp.feature_transaction.domain.model.Frequency) {
        _state.update { it.copy(recurringFrequency = freq) }
    }
    fun onHasEndDateChange(has: Boolean) {
        _state.update { it.copy(hasEndDate = has, endDateMillis = if (!has) null else it.endDateMillis) }
    }
    fun onEndDateChange(millis: Long) {
        _state.update { it.copy(endDateMillis = millis) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun save(onSuccess: () -> Unit) {
        val s = state.value
        // ...your validation...
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                // create the single transaction for the chosen date
                val amount = s.amountInput.toDouble() * if (s.isExpense) -1 else 1
                var ruleId: Int? = null
                val tx = Transaction(
                    id = s.id,
                    title = s.title.trim(),
                    amount = amount,
                    category = s.category.trim(),
                    date = s.dateMillis,
                    isRecurring = s.isRecurring,
                    recurringRuleId = ruleId ?: (useCases.getTransactionById(s.id ?: -1)?.recurringRuleId)
                )
                useCases.addTransaction(tx)

                // if recurring, create rule starting from the chosen date
                if (s.isRecurring) {
                    // create/update rule (as before)
                    val localDate = Instant.ofEpochMilli(s.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val dom = if (s.recurringFrequency == Frequency.MONTHLY) localDate.dayOfMonth else null

                    val rule = RecurringRule(
                        id = null,
                        title = s.title.trim(),
                        amount = amount,
                        category = s.category.trim(),
                        startAt = s.dateMillis,
                        frequency = s.recurringFrequency,
                        dayOfMonth = dom,
                        dayOfWeek = null,
                        endAt = if (s.hasEndDate) s.endDateMillis else null,
                        nextAt = when (s.recurringFrequency) {
                            Frequency.DAILY -> localDate.plusDays(1)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                            Frequency.MONTHLY -> {
                                val nextYm = YearMonth.from(localDate).plusMonths(1)
                                val clampedDom = (dom
                                    ?: localDate.dayOfMonth).coerceAtMost(nextYm.lengthOfMonth())
                                nextYm.atDay(clampedDom).atStartOfDay(ZoneId.systemDefault())
                                    .toInstant().toEpochMilli()
                            }

                            Frequency.WEEKLY -> s.dateMillis // not used in our simple UI
                        }
                    )
                    ruleId = recurringUse.upsertRecurring(rule) // returns Int id
                    recurringProcessor.processDue()
                } else {
                    // if previously recurring, and has link -> delete the rule
                    if (s.id != null) {
                        useCases.getTransactionById(s.id)?.recurringRuleId?.let { oldRuleId ->
                            // optional: delete the existing rule so it disappears from Manage Recurring
                            // You'll need a method to get rule by id, or create a dummy rule with id to delete.
                            // If your RecurringRepository doesn't have getById, we can add it — or leave the rule and just unlink.
                            // For now, just unlink; (add getById+delete if you'd like to hard-delete)
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
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
