package com.example.financeapp.feature_transaction.data.repository

import com.example.financeapp.feature_transaction.data.local.RecurringDao
import com.example.financeapp.feature_transaction.data.local.toDomain
import com.example.financeapp.feature_transaction.data.local.toEntity
import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import com.example.financeapp.feature_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringRepositoryImpl(
    private val dao: RecurringDao
) : RecurringRepository {
    override fun getAll(): Flow<List<RecurringRule>> =
        dao.getAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getDue(now: Long): List<RecurringRule> =
        dao.getDue(now).map { it.toDomain() }

    override suspend fun upsert(rule: RecurringRule): Int =
        dao.upsert(rule.toEntity()).toInt()

    override suspend fun delete(rule: RecurringRule) =
        dao.delete(rule.toEntity())

    override suspend fun updateNext(id: Int, nextAt: Long) =
        dao.updateNext(id, nextAt)
}