package com.example.financeapp.feature_transaction.domain.model

data class Transaction(
    val id: Int? = null,            // local Room PK
    val remoteId: String? = null,   // Firestore doc id
    val userId: String = "",             // Firebase UID (required when logged-in)
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val isRecurring: Boolean = false,
    val recurringRuleId: Int? = null
)
