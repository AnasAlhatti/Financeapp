package com.example.financeapp.feature_transaction.domain.repository

import com.example.financeapp.feature_transaction.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>
    suspend fun upsert(budget: Budget)
    suspend fun delete(budget: Budget)
    suspend fun getById(id: Int): Budget?
}