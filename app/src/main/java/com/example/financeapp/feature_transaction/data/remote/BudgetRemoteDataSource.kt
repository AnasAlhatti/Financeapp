package com.example.financeapp.feature_transaction.data.remote

import com.example.financeapp.feature_transaction.data.local.BudgetEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class BudgetRemote(
    val category: String = "",
    val limitAmount: Double = 0.0,
    val userId: String = ""
)

class BudgetRemoteDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    private fun col(uid: String) =
        db.collection("users").document(uid).collection("budgets")

    fun observe(uid: String, onChange: (List<BudgetEntity>) -> Unit): ListenerRegistration {
        return col(uid).addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(BudgetRemote::class.java)?.let { r ->
                    BudgetEntity(
                        id = null,
                        category = r.category,
                        limitAmount = r.limitAmount,
                        remoteId = doc.id,
                        userId = r.userId
                    )
                }
            }
            onChange(list)
        }
    }

    suspend fun upsert(uid: String, e: BudgetEntity): String {
        val id = e.remoteId ?: col(uid).document().id
        val remote = BudgetRemote(
            category = e.category,
            limitAmount = e.limitAmount,
            userId = uid
        )
        col(uid).document(id).set(remote).await()
        return id
    }

    suspend fun delete(uid: String, remoteId: String) {
        col(uid).document(remoteId).delete().await()
    }
}
