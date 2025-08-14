package com.example.financeapp.feature_transaction.domain.use_case.budget

import com.example.financeapp.feature_transaction.domain.model.Budget
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import javax.inject.Inject

class UpsertBudget @Inject constructor(
    private val repo: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) = repo.upsert(budget)
}
