package com.example.financeapp.feature_transaction.data.repository

import com.example.financeapp.feature_transaction.data.local.BudgetDao
import com.example.financeapp.feature_transaction.data.local.toDomain
import com.example.financeapp.feature_transaction.data.local.toEntity
import com.example.financeapp.feature_transaction.data.remote.BudgetRemoteDataSource
import com.example.financeapp.feature_transaction.domain.model.Budget
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BudgetRepositoryImpl(
    private val dao: BudgetDao,
    private val remote: BudgetRemoteDataSource,
    private val auth: FirebaseAuth
) : BudgetRepository {

    private var listener: ListenerRegistration? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getBudgets(): Flow<List<Budget>> =
        auth.currentUser?.uid?.let { uid ->
            dao.getBudgets(uid).map { list -> list.map { it.toDomain() } }
        } ?: flowOf(emptyList())

    override suspend fun upsert(budget: Budget) {
        val uid = auth.currentUser?.uid ?: return   // Not signed in, do nothing
        val existingRemoteId = when {
            budget.remoteId != null -> budget.remoteId
            budget.id != null -> dao.getById(budget.id!!)?.remoteId
            else -> null
        }
        val entity = budget.copy(userId = uid, remoteId = existingRemoteId).toEntity()
        val assignedRemoteId = remote.upsert(uid, entity)
        dao.upsert(entity.copy(remoteId = assignedRemoteId))
    }

    override suspend fun delete(budget: Budget) {
        val uid = auth.currentUser?.uid ?: return
        val rid = budget.remoteId ?: budget.id?.let { dao.getById(it)?.remoteId }
        rid?.let { remote.delete(uid, it) }
        dao.delete(budget.copy(userId = uid).toEntity())
    }

    override fun startSync(uid: String) {
        listener?.remove()
        listener = remote.observe(uid) { remoteList ->
            ioScope.launch {
                dao.clearForUser(uid)
                remoteList.forEach { dao.upsert(it.copy(userId = uid)) }
            }
        }
    }

    override fun stopSync() {
        listener?.remove()
        listener = null
    }

    override suspend fun clearLocal(uid: String) {
        dao.clearForUser(uid)
    }
}

