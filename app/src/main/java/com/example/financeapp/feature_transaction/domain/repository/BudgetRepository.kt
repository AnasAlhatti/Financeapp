package com.example.financeapp.feature_transaction.domain.repository

import com.example.financeapp.feature_transaction.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>
    suspend fun upsert(budget: Budget)
    suspend fun delete(budget: Budget)
     fun startSync(uid: String)
     fun stopSync()
    suspend fun clearLocal(uid: String)
}