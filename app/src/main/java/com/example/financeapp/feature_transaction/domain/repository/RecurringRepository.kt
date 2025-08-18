package com.example.financeapp.feature_transaction.domain.repository

import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getAll(): Flow<List<RecurringRule>>
    suspend fun getDue(now: Long): List<RecurringRule>
    suspend fun upsert(rule: RecurringRule): Int
    suspend fun delete(rule: RecurringRule)
    suspend fun updateNext(id: Int, nextAt: Long)
}
