package com.example.financeapp.feature_transaction.presentation.add_edit

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.Frequency
import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.recurring.RecurringProcessor
import com.example.financeapp.feature_transaction.domain.use_case.recurring.RecurringUseCases
import com.example.financeapp.feature_transaction.domain.use_case.transaction.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject


@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val useCases: TransactionUseCases,
    private val recurringUse: RecurringUseCases,
    private val recurringProcessor: RecurringProcessor,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state = _state.asStateFlow()

    private var loadedOnce = false

    fun loadForEdit(id: Int?) {
        if (loadedOnce) return
        loadedOnce = true

        viewModelScope.launch {
            if (id != null && id != -1) {
                // Editing an existing transaction
                val tx = useCases.getTransactionById(id)
                tx?.let {
                    _state.update { s ->
                        s.copy(
                            id = it.id,
                            title = it.title,
                            amountInput = kotlin.math.abs(it.amount).toString(),
                            isExpense = it.amount < 0,
                            category = it.category,
                            dateMillis = it.date,
                            isRecurring = it.isRecurring,
                            hasEndDate = s.hasEndDate,
                            endDateMillis = s.endDateMillis
                        )
                    }
                }
            } else {
                // New transaction: apply prefill if present (e.g., from Scan Receipt)
                val prefillTitle = savedStateHandle.get<String>("prefillTitle").orEmpty()
                val prefillAmount = savedStateHandle.get<String>("prefillAmount").orEmpty()
                val prefillDate = savedStateHandle.get<Long>("prefillDate") ?: -1L
                val prefillCategory = savedStateHandle.get<String>("prefillCategory").orEmpty()

                _state.update { s ->
                    s.copy(
                        title = if (prefillTitle.isNotBlank()) prefillTitle else s.title,
                        amountInput = if (prefillAmount.isNotBlank()) prefillAmount else s.amountInput,
                        category = if (prefillCategory.isNotBlank()) prefillCategory else s.category,
                        dateMillis = if (prefillDate > 0) prefillDate else s.dateMillis
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

    fun onRecurringToggle(enabled: Boolean) {
        _state.update { it.copy(isRecurring = enabled) }
    }
    fun onRecurringFrequencyChange(freq: Frequency) {
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

                if (s.isRecurring) {
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

                            Frequency.WEEKLY -> s.dateMillis
                        }
                    )
                    ruleId = recurringUse.upsertRecurring(rule)
                    recurringProcessor.processDue()
                } else {
                    if (s.id != null) {
                        useCases.getTransactionById(s.id)?.recurringRuleId?.let { oldRuleId ->
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

    fun delete(onSuccess: () -> Unit) {
        val id = _state.value.id ?: return
        viewModelScope.launch {
            // minimal fetch to get full object (or build from state)
            useCases.getTransactionById(id)?.let { useCases.deleteTransaction(it) }
            onSuccess()
        }
    }
}
