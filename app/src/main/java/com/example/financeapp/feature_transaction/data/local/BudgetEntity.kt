package com.example.financeapp.feature_transaction.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.financeapp.feature_transaction.domain.model.Budget

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val category: String,
    val limitAmount: Double,
    val remoteId: String? = null,
    val userId: String? = null
)

fun BudgetEntity.toDomain() = Budget(id, category, limitAmount, remoteId, userId)

fun Budget.toEntity() = BudgetEntity(id, category, limitAmount, remoteId, userId)
