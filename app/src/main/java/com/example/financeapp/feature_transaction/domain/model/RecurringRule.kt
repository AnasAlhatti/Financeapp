package com.example.financeapp.feature_transaction.domain.model

data class RecurringRule(
    val id: Int? = null,
    val title: String,
    val amount: Double,          // negative for expense, positive for income
    val category: String,
    val startAt: Long,           // first occurrence, millis
    val frequency: Frequency,
    val dayOfMonth: Int? = null, // for MONTHLY (1..28/29/30/31 -> we clamp)
    val dayOfWeek: Int? = null,  // for WEEKLY (java.time.DayOfWeek value 1..7)
    val endAt: Long? = null,     // optional end
    val nextAt: Long             // next scheduled run (millis)
)

enum class Frequency { DAILY, WEEKLY, MONTHLY }
