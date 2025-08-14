package com.example.financeapp.feature_transaction.presentation.add_edit

data class AddEditTransactionState(
    val id: Int? = null,
    val title: String = "",
    val amountInput: String = "",
    val category: String = "Other",
    val dateMillis: Long = System.currentTimeMillis(),
    val isExpense: Boolean = true, // ✅ new: default expense
    val isSaving: Boolean = false,
    val error: String? = null
)

