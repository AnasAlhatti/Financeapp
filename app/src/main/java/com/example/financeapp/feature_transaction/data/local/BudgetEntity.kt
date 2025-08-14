package com.example.financeapp.feature_transaction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.feature_transaction.domain.model.Budget

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val category: String,
    val limitAmount: Double
)

fun BudgetEntity.toDomain() = Budget(id, category, limitAmount)
fun Budget.toEntity() = BudgetEntity(id, category, limitAmount)
