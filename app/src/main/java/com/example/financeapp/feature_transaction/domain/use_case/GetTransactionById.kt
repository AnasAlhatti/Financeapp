package com.example.financeapp.feature_transaction.domain.use_case

import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository

class GetTransactionById(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Int): Transaction? {
        return repository.getTransactionById(id)
    }
}
