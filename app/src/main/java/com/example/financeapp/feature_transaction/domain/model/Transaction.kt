package com.example.financeapp.feature_transaction.domain.model

data class Transaction(
    val id: Int? = null,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val isRecurring: Boolean = false,
    val recurringRuleId: Int? = null
)
