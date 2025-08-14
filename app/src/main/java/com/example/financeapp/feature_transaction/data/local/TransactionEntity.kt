package com.example.financeapp.feature_transaction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.feature_transaction.domain.model.Transaction

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long
)

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(id, title, amount, category, date)
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(id, title, amount, category, date)
}
