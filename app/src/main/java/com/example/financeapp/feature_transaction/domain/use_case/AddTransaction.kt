package com.example.financeapp.feature_transaction.domain.use_case

import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository

class AddTransaction(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        // Add validation logic if needed
        repository.insertTransaction(transaction)
    }
}
