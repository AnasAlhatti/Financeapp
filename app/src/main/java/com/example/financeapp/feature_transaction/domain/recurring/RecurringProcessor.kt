package com.example.financeapp.feature_transaction.domain.recurring

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.use_case.transaction.AddTransaction
import com.example.financeapp.feature_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.repeat

class RecurringProcessor(
    private val repo: RecurringRepository,
    private val addTransaction: AddTransaction,
    private val txRepo: com.example.financeapp.feature_transaction.domain.repository.TransactionRepository
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun processDue(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val due = repo.getDue(nowMillis)
        for (rule in due) {
            var nextAt = rule.nextAt
            // cap catch-up to avoid huge loops
            repeat(24) {
                if (nextAt > nowMillis) return@repeat

                // ---- DUPLICATE GUARD (same day, same title/category/amount) ----
                val localDate = Instant.ofEpochMilli(nextAt).atZone(zone).toLocalDate()
                val dayStart = localDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = dayStart + java.time.Duration.ofDays(1).toMillis()

                val existsSame = txRepo.getBetween(dayStart, dayEnd).any {
                    it.title == rule.title &&
                            it.category == rule.category &&
                            it.amount == rule.amount
                }

                if (!existsSame) {
                    val tx = Transaction(
                        id = null,
                        title = rule.title,
                        amount = rule.amount,
                        category = rule.category,
                        date = nextAt,
                        isRecurring = true,
                        recurringRuleId = rule.id
                    )
                    addTransaction(tx)
                }
                // ----------------------------------------------------------------

                // compute next (strictly after current nextAt)
                val computed = nextOccurrence(
                    startAt = rule.startAt,
                    frequency = rule.frequency,
                    dayOfMonth = rule.dayOfMonth,
                    dayOfWeek = rule.dayOfWeek,
                    fromMillis = nextAt,
                    zone = zone
                )
                // respect inclusive end date: allow equals, stop if beyond
                val bounded = rule.endAt?.let { end ->
                    if (computed > end) Long.MAX_VALUE else computed
                } ?: computed

                nextAt = bounded
                rule.id?.let { repo.updateNext(it, nextAt) }
                if (nextAt == Long.MAX_VALUE) return@repeat
            }
        }
    }
}
