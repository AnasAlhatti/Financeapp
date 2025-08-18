package com.example.financeapp.feature_transaction.presentation.reports

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.feature_transaction.presentation.transaction_list.AmountFilter
import com.example.financeapp.feature_transaction.presentation.transaction_list.DateRange
import com.example.financeapp.feature_transaction.presentation.transaction_list.FilterBar
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.time.YearMonth

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    var showMonthPicker by remember { mutableStateOf(false) }

    val budgetsVm: com.example.financeapp.feature_transaction.presentation.budgets.BudgetsSummaryViewModel = hiltViewModel()
    val warnings by budgetsVm.warnings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Filter Bar (reusing compact bar)
            FilterBar(
                dateRange = ui.dateRange,
                selectedMonth = ui.selectedMonth,
                amountFilter = ui.amountFilter,
                categories = emptyList(),                 // categories filter not needed in reports
                selectedCategory = null,
                onDateRangeSelected = viewModel::onDateRangeSelected,
                onPickMonth = { showMonthPicker = true },
                onTypeSelected = viewModel::onTypeSelected,
                onCategorySelected = {},                  // no-op
                onClearAll = {
                    viewModel.onDateRangeSelected(DateRange.THIS_MONTH)
                    viewModel.onTypeSelected(AmountFilter.ALL)
                },
                modifier = Modifier.padding(16.dp)
            )

            // Totals card
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                val title = when (ui.amountFilter) {
                    AmountFilter.ALL -> "Net Total"
                    AmountFilter.INCOME -> "Total Income"
                    AmountFilter.EXPENSE -> "Total Expense"
                }
                Column(Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(ui.total.formatCurrency(), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (warnings.isNotEmpty()) {
                val nf = remember { java.text.NumberFormat.getCurrencyInstance() }
                Spacer(Modifier.height(12.dp))
                ElevatedCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Budgets at a glance", style = MaterialTheme.typography.titleSmall)

                        warnings.take(3).forEach { w ->
                            val ratio = w.ratio
                            val level = com.example.financeapp.feature_transaction.presentation.budgets.levelForRatio(ratio)
                            val barColor = com.example.financeapp.feature_transaction.presentation.budgets.colorForLevel(level)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(w.category, color = barColor, style = MaterialTheme.typography.bodyMedium)
                                LinearProgressIndicator(
                                    progress = { ratio.toFloat().coerceAtMost(1f) },
                                    color = barColor,
                                    trackColor = barColor.copy(alpha = 0.15f)
                                )
                                Text(
                                    text = "${nf.format(w.spent)} / ${nf.format(w.limit)}  •  ${(ratio * 100).toInt()}%",
                                    color = barColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Optional legend (kept as-is)
                        Spacer(Modifier.height(8.dp))
                        BudgetLegend()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Category totals bar
            SectionTitle("By Category")
            CategoryBarChart(ui)

            Spacer(Modifier.height(16.dp))

            // Daily trend bar
            SectionTitle("Trend")
            DailyBarChart(ui)
        }
    }

    if (showMonthPicker) {
        com.example.financeapp.feature_transaction.presentation.transaction_list.MonthPickerDialog(
            initial = ui.selectedMonth ?: YearMonth.now(),
            onDismiss = { showMonthPicker = false },
            onConfirm = {
                viewModel.onMonthPicked(it)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun BudgetLegend() {
    // Uses Material theme colors to explain the risk mapping.
    // <60% -> primary, 60–80% -> secondary, 80–100% -> tertiary, >100% -> error
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Legend", style = MaterialTheme.typography.labelMedium)
        LegendRow(color = MaterialTheme.colorScheme.primary, label = "< 60% (OK)")
        LegendRow(color = MaterialTheme.colorScheme.secondary, label = "60–80% (Caution)")
        LegendRow(color = MaterialTheme.colorScheme.tertiary, label = "80–100% (Warning)")
        LegendRow(color = MaterialTheme.colorScheme.error, label = "> 100% (Over)")
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = color,
            contentColor = color,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(14.dp)
        ) {}
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/** Bar chart of category totals (positive values). */
@Composable
private fun CategoryBarChart(ui: ReportsUiState) {
    val labels = ui.categoryTotals.keys.toList()
    val values = ui.categoryTotals.values.map { it.toFloat() }

    if (values.isEmpty()) {
        EmptyChartPlaceholder("No category data for the selected filters.")
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction { columnSeries { series(values) } }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val i = value.toInt()
            // ✅ must return a non-empty string:
            labels.getOrNull(i)?.takeIf { it.isNotBlank() } ?: i.toString()
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomFormatter,
                itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 1 }) }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 8.dp)
    )
}

/** Daily totals over the selected window. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DailyBarChart(ui: ReportsUiState) {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
    val labels = ui.dailyTotals.map { it.first.format(fmt) }
    val values = ui.dailyTotals.map { it.second.toFloat() }

    if (values.isEmpty()) {
        EmptyChartPlaceholder("No daily activity in this period.")
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction { columnSeries { series(values) } }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val i = value.toInt()
            // ✅ never empty:
            labels.getOrNull(i)?.takeIf { it.isNotBlank() } ?: i.toString()
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomFormatter,
                itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 1 }) }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 8.dp)
    )
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}

private fun Double.formatCurrency(): String {
    val nf = java.text.NumberFormat.getCurrencyInstance()
    return nf.format(this)
}
