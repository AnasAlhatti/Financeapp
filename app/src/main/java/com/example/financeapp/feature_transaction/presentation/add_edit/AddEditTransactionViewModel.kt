package com.example.financeapp.feature_transaction.presentation.add_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val useCases: TransactionUseCases
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

    fun delete(onSuccess: () -> Unit) {
        val id = _state.value.id ?: return
        viewModelScope.launch {
            // minimal fetch to get full object (or build from state)
            useCases.getTransactionById(id)?.let { useCases.deleteTransaction(it) }
            onSuccess()
        }
    }
}
