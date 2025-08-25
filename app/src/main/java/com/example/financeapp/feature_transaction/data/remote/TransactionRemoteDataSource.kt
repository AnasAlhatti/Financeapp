package com.example.financeapp.feature_transaction.data.remote

import com.example.financeapp.feature_transaction.data.local.TransactionEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class TransactionRemoteDataSource(
    private val firestore: FirebaseFirestore
) {
    suspend fun upsert(uid: String, tx: TransactionEntity): String {
        val col = firestore.collection("users").document(uid).collection("transactions")
        val doc = if (tx.remoteId != null) col.document(tx.remoteId) else col.document()
        doc.set(tx.copy(remoteId = doc.id, userId = uid)).await()
        return doc.id
    }

    suspend fun delete(uid: String, remoteId: String) {
        firestore.collection("users").document(uid)
            .collection("transactions").document(remoteId)
            .delete().await()
    }

    // Return the ListenerRegistration so callers can remove() it later
    fun observe(uid: String, onChange: (List<TransactionEntity>) -> Unit): ListenerRegistration =
        firestore.collection("users").document(uid)
            .collection("transactions")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    try {
                        d.toObject<TransactionEntity>()?.copy(remoteId = d.id, userId = uid)
                    } catch (_: Exception) {
                        null
                    }
                }
                onChange(list)
            }
}
