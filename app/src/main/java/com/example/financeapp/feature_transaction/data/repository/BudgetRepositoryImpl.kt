package com.example.financeapp.feature_transaction.data.repository

import com.example.financeapp.feature_transaction.data.local.BudgetDao
import com.example.financeapp.feature_transaction.data.local.toDomain
import com.example.financeapp.feature_transaction.data.local.toEntity
import com.example.financeapp.feature_transaction.domain.model.Budget
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val dao: BudgetDao
) : BudgetRepository {
    override fun getBudgets(): Flow<List<Budget>> =
        dao.getBudgets().map { it.map { e -> e.toDomain() } }

    override suspend fun upsert(budget: Budget) = dao.upsert(budget.toEntity())

    override suspend fun delete(budget: Budget) = dao.delete(budget.toEntity())

    override suspend fun getById(id: Int): Budget? = dao.getById(id)?.toDomain()
}
