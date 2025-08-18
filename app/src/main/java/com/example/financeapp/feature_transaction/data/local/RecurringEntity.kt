package com.example.financeapp.feature_transaction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.feature_transaction.domain.model.Frequency
import com.example.financeapp.feature_transaction.domain.model.RecurringRule

@Entity(tableName = "recurring_rules")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val title: String,
    val amount: Double,
    val category: String,
    val startAt: Long,
    val frequency: String,      // store enum name
    val dayOfMonth: Int?,       // nullable
    val dayOfWeek: Int?,        // nullable (1..7)
    val endAt: Long?,           // nullable
    val nextAt: Long
)

fun RecurringEntity.toDomain() = RecurringRule(
    id = id,
    title = title,
    amount = amount,
    category = category,
    startAt = startAt,
    frequency = Frequency.valueOf(frequency),
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek,
    endAt = endAt,
    nextAt = nextAt
)

fun RecurringRule.toEntity() = RecurringEntity(
    id = id,
    title = title,
    amount = amount,
    category = category,
    startAt = startAt,
    frequency = frequency.name,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek,
    endAt = endAt,
    nextAt = nextAt
)
