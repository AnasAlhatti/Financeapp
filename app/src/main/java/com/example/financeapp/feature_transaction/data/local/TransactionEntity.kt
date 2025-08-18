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
    val date: Long,
    val isRecurring: Boolean = false,
    val recurringRuleId: Int? = null
)

fun TransactionEntity.toDomain() = Transaction(
    id, title, amount, category, date,
    isRecurring = isRecurring,
    recurringRuleId = recurringRuleId
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    title = title,
    amount = amount,
    category = category,
    date = date,
    isRecurring = isRecurring,
    recurringRuleId = recurringRuleId
)
