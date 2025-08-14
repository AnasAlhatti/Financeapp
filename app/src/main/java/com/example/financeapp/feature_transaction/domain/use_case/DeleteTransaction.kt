package com.example.financeapp.feature_transaction.domain.use_case

import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository

class DeleteTransaction(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.deleteTransaction(transaction)
    }
}
