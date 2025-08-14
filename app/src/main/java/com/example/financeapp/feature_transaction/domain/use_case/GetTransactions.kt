package com.example.financeapp.feature_transaction.domain.use_case

import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactions(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getTransactions()
    }
}
