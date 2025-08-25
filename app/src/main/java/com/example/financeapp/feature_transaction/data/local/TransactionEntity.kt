package com.example.financeapp.feature_transaction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.feature_transaction.domain.model.Transaction

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val remoteId: String? = null,
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: Long = 0L,
    val isRecurring: Boolean = false,
    val recurringRuleId: Int? = null
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    remoteId = remoteId,
    title = title,
    amount = amount,
    category = category,
    date = date,
    isRecurring = isRecurring,
    recurringRuleId = recurringRuleId,
    userId = userId
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    remoteId = remoteId,
    title = title,
    amount = amount,
    category = category,
    date = date,
    isRecurring = isRecurring,
    recurringRuleId = recurringRuleId,
    userId = userId
)
