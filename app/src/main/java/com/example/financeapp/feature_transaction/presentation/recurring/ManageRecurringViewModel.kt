package com.example.financeapp.feature_transaction.presentation.recurring

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_transaction.domain.model.Frequency
import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import com.example.financeapp.feature_transaction.domain.recurring.RecurringProcessor
import com.example.financeapp.feature_transaction.domain.recurring.nextOccurrence
import com.example.financeapp.feature_transaction.domain.use_case.recurring.RecurringUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs

data class RecurringFormState(
    val id: Int? = null,
    val title: String = "",
    val amountInput: String = "",
    val isExpense: Boolean = true,
    val category: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val dayOfWeek: Int? = null,     // 1..7 (MON..SUN)
    val dayOfMonth: Int? = 1,       // 1..31
    val startAt: Long = System.currentTimeMillis(),
    val hasEnd: Boolean = false,
    val endAt: Long? = null,
)

@HiltViewModel
class ManageRecurringViewModel @Inject constructor(
    private val useCases: RecurringUseCases,
    private val processor: RecurringProcessor
) : ViewModel() {

    val rules: StateFlow<List<RecurringRule>> =
        useCases.getRecurring().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    fun upsert(state: RecurringFormState) {
        viewModelScope.launch {
            val amt = state.amountInput.toDoubleOrNull()?.let { if (state.isExpense) -abs(it) else abs(it) } ?: return@launch

            val zone = ZoneId.systemDefault()
            val nextAt = nextOccurrence(
                startAt = state.startAt,
                frequency = state.frequency,
                dayOfMonth = state.dayOfMonth,
                dayOfWeek = state.dayOfWeek,
                fromMillis = System.currentTimeMillis() - 1, // next occurrence >= now
                zone = zone
            )

            val rule = RecurringRule(
                id = state.id,
                title = state.title.trim(),
                amount = amt,
                category = state.category.trim(),
                startAt = state.startAt,
                frequency = state.frequency,
                dayOfMonth = state.dayOfMonth,
                dayOfWeek = state.dayOfWeek,
                endAt = if (state.hasEnd) state.endAt else null,
                nextAt = nextAt
            )
            useCases.upsertRecurring(rule)
            // insert anything due right away
            processor.processDue()
        }
    }

    fun delete(rule: RecurringRule) {
        viewModelScope.launch {
            useCases.deleteRecurring(rule)
        }
    }

    /** Preview the next N occurrences from "now" using the provided form state. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun previewNext(state: RecurringFormState, count: Int = 3): List<Long> {
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<Long>()
        var from = System.currentTimeMillis() - 1
        repeat(count) {
            val next = nextOccurrence(
                startAt = state.startAt,
                frequency = state.frequency,
                dayOfMonth = state.dayOfMonth,
                dayOfWeek = state.dayOfWeek,
                fromMillis = from,
                zone = zone
            )
            // stop if over end
            if (state.hasEnd && state.endAt != null && next > state.endAt) return@repeat
            result += next
            from = next
        }
        return result
    }

    companion object {
        val dayNames = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    }
}
