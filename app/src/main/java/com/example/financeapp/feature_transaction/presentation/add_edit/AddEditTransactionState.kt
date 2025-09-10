package com.example.financeapp.feature_transaction.presentation.add_edit

data class AddEditTransactionState(
    val id: Int? = null,
    val title: String = "",
    val amountInput: String = "",
    val category: String = "Other",
    val dateMillis: Long = System.currentTimeMillis(),
    val isExpense: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isRecurring: Boolean = false,
    val recurringFrequency: com.example.financeapp.feature_transaction.domain.model.Frequency =
        com.example.financeapp.feature_transaction.domain.model.Frequency.MONTHLY,
    val hasEndDate: Boolean = false,
    val endDateMillis: Long? = null
)

