package com.example.financeapp.feature_transaction.presentation.transaction_list

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilterBar(
    // inputs
    dateRange: DateRange,
    selectedMonth: YearMonth?,
    amountFilter: AmountFilter,
    categories: List<String>,
    selectedCategory: String?,
    // callbacks
    onDateRangeSelected: (DateRange) -> Unit,
    onPickMonth: () -> Unit,
    onTypeSelected: (AmountFilter) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ymFormatter = remember { DateTimeFormatter.ofPattern("MMM uuuu") }
    val dateLabel = when (dateRange) {
        DateRange.ALL -> "All time"
        DateRange.THIS_WEEK -> "This week"
        DateRange.THIS_MONTH -> "This month"
        DateRange.LAST_30D -> "Last 30d"
        DateRange.MONTH -> selectedMonth?.format(ymFormatter) ?: "Pick month"
    }

    val typeLabel = when (amountFilter) {
        AmountFilter.ALL -> "All types"
        AmountFilter.INCOME -> "Income"
        AmountFilter.EXPENSE -> "Expense"
    }

    val categoryLabel = selectedCategory ?: "All categories"
    val hasActiveFilters = dateRange != DateRange.ALL ||
            amountFilter != AmountFilter.ALL ||
            selectedCategory != null

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date chip (dropdown)
            DateMenuChip(
                label = dateLabel,
                onAll = { onDateRangeSelected(DateRange.ALL) },
                onThisWeek = { onDateRangeSelected(DateRange.THIS_WEEK) },
                onThisMonth = { onDateRangeSelected(DateRange.THIS_MONTH) },
                onLast30d = { onDateRangeSelected(DateRange.LAST_30D) },
                onPickMonth = onPickMonth
            )

            // Type chip (dropdown)
            TypeMenuChip(
                label = typeLabel,
                onAll = { onTypeSelected(AmountFilter.ALL) },
                onIncome = { onTypeSelected(AmountFilter.INCOME) },
                onExpense = { onTypeSelected(AmountFilter.EXPENSE) }
            )

            // Category chip (dropdown)
            CategoryMenuChip(
                label = categoryLabel,
                categories = categories,
                onAll = { onCategorySelected(null) },
                onSelect = { onCategorySelected(it) }
            )

            if (hasActiveFilters) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear filters",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DateMenuChip(
    label: String,
    onAll: () -> Unit,
    onThisWeek: () -> Unit,
    onThisMonth: () -> Unit,
    onLast30d: () -> Unit,
    onPickMonth: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = null
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All time") }, onClick = { expanded = false; onAll() })
            DropdownMenuItem(text = { Text("This week") }, onClick = { expanded = false; onThisWeek() })
            DropdownMenuItem(text = { Text("This month") }, onClick = { expanded = false; onThisMonth() })
            DropdownMenuItem(text = { Text("Last 30d") }, onClick = { expanded = false; onLast30d() })
            DropdownMenuItem(text = { Text("Pick month…") }, onClick = { expanded = false; onPickMonth() })
        }
    }
}

@Composable
private fun TypeMenuChip(
    label: String,
    onAll: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = null
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All types") }, onClick = { expanded = false; onAll() })
            DropdownMenuItem(text = { Text("Income") }, onClick = { expanded = false; onIncome() })
            DropdownMenuItem(text = { Text("Expense") }, onClick = { expanded = false; onExpense() })
        }
    }
}

@Composable
private fun CategoryMenuChip(
    label: String,
    categories: List<String>,
    onAll: () -> Unit,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Label,
                    contentDescription = null
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(text = { Text("All categories") }, onClick = { expanded = false; onAll() })
            categories.forEach { cat ->
                DropdownMenuItem(text = { Text(cat) }, onClick = { expanded = false; onSelect(cat) })
            }
        }
    }
}
