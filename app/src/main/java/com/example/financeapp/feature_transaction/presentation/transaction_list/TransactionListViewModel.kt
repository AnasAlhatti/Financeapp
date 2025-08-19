package com.example.financeapp.feature_transaction.presentation.transaction_list

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.domain.repository.RecurringRepository
import com.example.financeapp.feature_transaction.domain.use_case.transaction.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import javax.inject.Inject

enum class DateRange { ALL, THIS_WEEK, THIS_MONTH, LAST_30D, MONTH }
enum class AmountFilter { ALL, INCOME, EXPENSE }

data class TransactionListUiState(
    val allTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val dateRange: DateRange = DateRange.ALL,
    val selectedMonth: YearMonth? = null,
    val amountFilter: AmountFilter = AmountFilter.ALL,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val nextByRuleId: Map<Int, Long> = emptyMap()
)

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val useCases: TransactionUseCases,
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedDateRange = MutableStateFlow(DateRange.ALL)
    private val selectedMonth = MutableStateFlow<YearMonth?>(null)
    private val selectedType = MutableStateFlow(AmountFilter.ALL)
    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private var recentlyDeleted: Transaction? = null

    init { observe() }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observe() {
        val zone = ZoneId.systemDefault()

        combine(
            useCases.getTransactions(),
            recurringRepository.getAll(),
            selectedCategory,
            selectedDateRange,
            selectedMonth,
            selectedType
        ) { arr: Array<Any?> ->
            val list = arr[0] as List<Transaction>
            val rules = arr[1] as List<RecurringRule>
            val category = arr[2] as String?
            val range = arr[3] as DateRange
            val month = arr[4] as YearMonth?
            val type = arr[5] as AmountFilter

            val nextMap = rules.mapNotNull { r -> r.id?.let { it to r.nextAt } }.toMap()

            val zone = ZoneId.systemDefault()
            val (startMillis, endMillis) = rangeMillisFor(range, month, zone)

            var filtered = list.filter { tx ->
                val t = tx.date
                (startMillis == null || t >= startMillis) &&
                        (endMillis == null || t < endMillis)
            }

            if (!category.isNullOrBlank()) {
                filtered = if (category == "Recurring") filtered.filter { it.isRecurring }
                else filtered.filter { it.category == category }
            }

            filtered = when (type) {
                AmountFilter.ALL -> filtered
                AmountFilter.INCOME -> filtered.filter { it.amount > 0 }
                AmountFilter.EXPENSE -> filtered.filter { it.amount < 0 }
            }

            val income = filtered.filter { it.amount > 0 }.sumOf { it.amount }
            val expense = filtered.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            val balance = income - expense

            TransactionListUiState(
                allTransactions = list,
                filteredTransactions = filtered.sortedByDescending { it.date },
                categories = (list.map { it.category }.distinct().sorted() + "Recurring"),
                selectedCategory = category,
                dateRange = range,
                selectedMonth = month,
                amountFilter = type,
                totalIncome = income,
                totalExpense = expense,
                balance = balance,
                nextByRuleId = nextMap
            )
        }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }
    /** Computes [start, end) in epoch millis for the chosen range, in given zone. */
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
                val end = nowZdt.toInstant().toEpochMilli() + 1 // exclude future strictly
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
                val firstDow = wf.firstDayOfWeek
                val today = nowZdt.toLocalDate()
                val startLocal = today.with(TemporalAdjusters.previousOrSame(firstDow)).atStartOfDay(zone)
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

    fun onCategorySelected(category: String?) { selectedCategory.value = category }
    fun onDateRangeSelected(range: DateRange) {
        selectedDateRange.value = range
        if (range != DateRange.MONTH) selectedMonth.value = null
    }
    fun onMonthPicked(month: YearMonth) {
        selectedMonth.value = month
        selectedDateRange.value = DateRange.MONTH
    }
    fun onTypeSelected(type: AmountFilter) { selectedType.value = type }

    fun deleteTransaction(item: Transaction) {
        viewModelScope.launch {
            recentlyDeleted = item
            useCases.deleteTransaction(item)
        }
    }
    fun restoreLastDeleted() {
        recentlyDeleted?.let { tx ->
            viewModelScope.launch {
                useCases.addTransaction(tx)
                recentlyDeleted = null
            }
        }
    }
    fun addTransactions(items: List<Transaction>) {
        viewModelScope.launch {
            items.forEach { useCases.addTransaction(it) }
        }
    }
}
