package com.example.financeapp.feature_transaction.domain.recurring

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.financeapp.feature_transaction.domain.model.Frequency
import java.time.*
import java.time.temporal.TemporalAdjusters

/** Compute the next occurrence strictly after 'from' (millis). */
@RequiresApi(Build.VERSION_CODES.O)
fun nextOccurrence(
    startAt: Long,
    frequency: Frequency,
    dayOfMonth: Int?,
    dayOfWeek: Int?,
    fromMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    var zdt = Instant.ofEpochMilli(startAt).atZone(zone)
    val from = Instant.ofEpochMilli(fromMillis).atZone(zone)

    if (zdt.isAfter(from)) return zdt.toInstant().toEpochMilli()

    fun bump(): ZonedDateTime {
        return when (frequency) {
            Frequency.DAILY -> zdt.plusDays(1)
            Frequency.WEEKLY -> {
                val target = DayOfWeek.of(dayOfWeek ?: zdt.dayOfWeek.value)
                var t = zdt.with(TemporalAdjusters.nextOrSame(target))
                if (!t.isAfter(zdt)) t = t.plusWeeks(1)
                t
            }
            Frequency.MONTHLY -> {
                val dom = dayOfMonth ?: zdt.dayOfMonth
                var base = zdt.plusMonths(1)
                val ym = YearMonth.from(base)
                val clamped = dom.coerceAtMost(ym.lengthOfMonth())
                base.withDayOfMonth(clamped)
            }
        }
    }

    while (!zdt.isAfter(from)) {
        zdt = bump()
    }
    return zdt.toInstant().toEpochMilli()
}
