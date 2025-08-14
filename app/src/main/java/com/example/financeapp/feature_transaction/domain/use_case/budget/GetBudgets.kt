package com.example.financeapp.feature_transaction.domain.use_case.budget

import com.example.financeapp.feature_transaction.domain.model.Budget
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgets(private val repo: BudgetRepository) {
    operator fun invoke(): Flow<List<Budget>> = repo.getBudgets()
}
