package com.example.financeapp.feature_transaction.domain.use_case.budget

import javax.inject.Inject

data class BudgetUseCases @Inject constructor(
    val getBudgets: GetBudgets,
    val upsertBudget: UpsertBudget,
    val deleteBudget: DeleteBudget
)
