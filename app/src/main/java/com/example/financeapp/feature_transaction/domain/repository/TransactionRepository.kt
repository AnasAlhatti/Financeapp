package com.example.financeapp.feature_transaction.domain.repository

import com.example.financeapp.feature_transaction.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Int): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getBetween(start: Long, end: Long): List<Transaction>
    fun startSync(uid: String)
    fun stopSync()
    suspend fun clearLocal()
}
