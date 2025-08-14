package com.example.financeapp.feature_transaction.domain.use_case.budget

data class BudgetUseCases(
    val getBudgets: GetBudgets,
    val upsertBudget: UpsertBudget,
    val deleteBudget: DeleteBudget
)
