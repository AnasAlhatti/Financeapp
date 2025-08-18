package com.example.financeapp.feature_transaction.domain.use_case.transaction

import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository
import jakarta.inject.Inject

class AddTransaction @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        // Add validation logic if needed
        repository.insertTransaction(transaction)
    }
}
