package com.example.financeapp.feature_transaction.presentation.reports

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.use_case.transaction.TransactionUseCases
import com.example.financeapp.feature_transaction.presentation.transaction_list.AmountFilter
import com.example.financeapp.feature_transaction.presentation.transaction_list.DateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import javax.inject.Inject
import kotlin.math.abs

data class ReportsUiState(
    val dateRange: DateRange = DateRange.THIS_MONTH,
    val selectedMonth: YearMonth? = null,
    val amountFilter: AmountFilter = AmountFilter.ALL,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val dailyTotals: List<Pair<LocalDate, Double>> = emptyList(),
    val total: Double = 0.0,
)

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val useCases: TransactionUseCases
) : ViewModel() {

    private val dateRange = MutableStateFlow(DateRange.THIS_MONTH)
    private val monthPick = MutableStateFlow<YearMonth?>(null)
    private val type = MutableStateFlow(AmountFilter.ALL)

    private val _ui = MutableStateFlow(ReportsUiState())
    val ui: StateFlow<ReportsUiState> = _ui.asStateFlow()

    init { observe() }

    fun onDateRangeSelected(dr: DateRange) {
        dateRange.value = dr
        if (dr != DateRange.MONTH) monthPick.value = null
    }
    fun onMonthPicked(ym: YearMonth) {
        monthPick.value = ym
        dateRange.value = DateRange.MONTH
    }
    fun onTypeSelected(af: AmountFilter) { type.value = af }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observe() {
        val zone = ZoneId.systemDefault()
        combine(
            useCases.getTransactions(),
            dateRange, monthPick, type
        ) { txs, dr, ym, af ->

            val (startMs, endMs) = rangeMillisFor(dr, ym, zone)
            // Filter by date window
            var filtered = txs.filter { t ->
                (startMs == null || t.date >= startMs) && (endMs == null || t.date < endMs)
            }
            // Filter by type
            filtered = when (af) {
                AmountFilter.ALL -> filtered
                AmountFilter.INCOME -> filtered.filter { it.amount > 0 }
                AmountFilter.EXPENSE -> filtered.filter { it.amount < 0 }
            }

            // Category totals (abs for expense so bars are positive)
            val categoryTotals = filtered
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { if (af == AmountFilter.EXPENSE) abs(it.amount) else it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            // Daily totals (by LocalDate)
            val dailyTotals = filtered
                .groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { if (af == AmountFilter.EXPENSE) abs(it.amount) else it.amount } }
                .toList()
                .sortedBy { it.first }
                .map { it.first to it.second }

            val total = when (af) {
                AmountFilter.ALL -> filtered.sumOf { it.amount } // could be net
                AmountFilter.INCOME -> filtered.sumOf { it.amount }
                AmountFilter.EXPENSE -> filtered.sumOf { abs(it.amount) }
            }

            ReportsUiState(
                dateRange = dr,
                selectedMonth = ym,
                amountFilter = af,
                categoryTotals = categoryTotals,
                dailyTotals = dailyTotals,
                total = total
            )
        }.onEach { _ui.value = it }
            .launchIn(viewModelScope)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun rangeMillisFor(
        range: DateRange,
        month: YearMonth?,
        zone: ZoneId
    ): Pair<Long?, Long?> {
        val nowZdt = ZonedDateTime.now(zone)
        return when (range) {
            DateRange.ALL -> null to null
            DateRange.LAST_30D -> {
                val start = nowZdt.minusDays(30).toInstant().toEpochMilli()
                val end = nowZdt.toInstant().toEpochMilli() + 1
                start to end
            }
            DateRange.THIS_MONTH -> {
                val ym = YearMonth.from(nowZdt)
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
            DateRange.THIS_WEEK -> {
                val wf = WeekFields.of(java.util.Locale.getDefault())
                val startLocal = nowZdt.toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(wf.firstDayOfWeek))
                    .atStartOfDay(zone)
                val endLocal = startLocal.plusDays(7)
                startLocal.toInstant().toEpochMilli() to endLocal.toInstant().toEpochMilli()
            }
            DateRange.MONTH -> {
                val ym = month ?: YearMonth.from(nowZdt)
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
        }
    }
}
