package com.example.financeapp.feature_transaction.domain.use_case.recurring

import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import com.example.financeapp.feature_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecurring @Inject constructor(private val repo: RecurringRepository) {
    operator fun invoke(): Flow<List<RecurringRule>> = repo.getAll()
}
class UpsertRecurring @Inject constructor(private val repo: RecurringRepository) {
    suspend operator fun invoke(rule: RecurringRule): Int = repo.upsert(rule)
}
class DeleteRecurring @Inject constructor(private val repo: RecurringRepository) {
    suspend operator fun invoke(rule: RecurringRule) = repo.delete(rule)
}
data class RecurringUseCases @Inject constructor(
    val getRecurring: GetRecurring,
    val upsertRecurring: UpsertRecurring,
    val deleteRecurring: DeleteRecurring
)
