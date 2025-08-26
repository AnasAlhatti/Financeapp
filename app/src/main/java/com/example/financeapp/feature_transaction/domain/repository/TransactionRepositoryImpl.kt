package com.example.financeapp.feature_transaction.domain.repository

import com.example.financeapp.feature_transaction.data.local.TransactionDao
import com.example.financeapp.feature_transaction.data.local.TransactionEntity
import com.example.financeapp.feature_transaction.data.local.toDomain
import com.example.financeapp.feature_transaction.data.local.toEntity
import com.example.financeapp.feature_transaction.data.remote.TransactionRemoteDataSource
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val remote: TransactionRemoteDataSource,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { it.map(TransactionEntity::toDomain) }

    override suspend fun getTransactionById(id: Int): Transaction? =
        dao.getTransactionById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction) {
        val uid = auth.currentUser?.uid ?: return

        val existingRemoteId: String? = when {
            transaction.remoteId != null -> transaction.remoteId
            transaction.id != null       -> dao.getTransactionById(transaction.id!!)?.remoteId
            else                         -> null
        }
        val base = transaction.copy(userId = uid, remoteId = existingRemoteId)
        val entity = base.toEntity()
        val assignedRemoteId = remote.upsert(uid, entity.copy(remoteId = existingRemoteId))

        dao.insertTransaction(entity.copy(remoteId = assignedRemoteId))
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val uid = auth.currentUser?.uid ?: return
        val rid = transaction.remoteId
            ?: transaction.id?.let { dao.getTransactionById(it)?.remoteId }
        rid?.let { remote.delete(uid, it) }

        dao.deleteTransaction(transaction.copy(userId = uid).toEntity())
    }

    override suspend fun getBetween(start: Long, end: Long): List<Transaction> =
        dao.getBetween(start, end).map { it.toDomain() }

    override fun startSync(uid: String) {
        listenerRegistration?.remove()
        listenerRegistration = remote.observe(uid) { remoteList ->
            ioScope.launch {
                dao.clearAll()
                remoteList.forEach { dao.insertTransaction(it.copy(userId = uid)) }
            }
        }
    }

    override fun stopSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    override suspend fun clearLocal() {
        dao.clearAll()
    }
}
