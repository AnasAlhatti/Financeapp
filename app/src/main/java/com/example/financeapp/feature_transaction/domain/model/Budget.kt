package com.example.financeapp.feature_transaction.domain.model

data class Budget(
    val id: Int? = null,
    val category: String,
    val limitAmount: Double,
    val remoteId: String? = null,
    val userId: String? = null
)
