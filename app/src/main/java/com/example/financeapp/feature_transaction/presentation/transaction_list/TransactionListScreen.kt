package com.example.financeapp.feature_transaction.presentation.transaction_list

import android.icu.text.DateFormat
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.presentation.budgets.BudgetWarningsBanner
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TransactionListScreen(
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onOpenReports: () -> Unit,
    onOpenBudgets: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember(LocalContext.current) { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showMonthPicker by remember { mutableStateOf(false) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Budget warnings
    val budgetsVm: com.example.financeapp.feature_transaction.presentation.budgets.BudgetsSummaryViewModel = hiltViewModel()
    val warnings by budgetsVm.warnings.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // ✅ Use the SAME AppDrawer as Reports/Budgets with the same narrow width
            com.example.financeapp.ui.common.AppDrawer(
                onNavigateTransactions = { scope.launch { drawerState.close() } },   // already here
                onNavigateReports = {
                    scope.launch { drawerState.close() }
                    onOpenReports()
                },
                onNavigateBudgets = {
                    scope.launch { drawerState.close() }
                    onOpenBudgets()
                },
                selectedRoute = com.example.financeapp.ui.common.DrawerRoute.Transactions
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Transactions") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open navigation"
                            )
                        }
                    }
                )
            },
            floatingActionButton = { FloatingActionButton(onClick = onAddClick) { Text("+") } },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Spacer(Modifier.height(4.dp))

                TotalsHeader(
                    income = currencyFormat.format(uiState.totalIncome),
                    expense = currencyFormat.format(uiState.totalExpense),
                    balance = currencyFormat.format(uiState.balance),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BudgetWarningsBanner(
                    warnings = warnings,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                )

                FilterBar(
                    dateRange = uiState.dateRange,
                    selectedMonth = uiState.selectedMonth,
                    amountFilter = uiState.amountFilter,
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onDateRangeSelected = viewModel::onDateRangeSelected,
                    onPickMonth = { showMonthPicker = true },
                    onTypeSelected = viewModel::onTypeSelected,
                    onCategorySelected = viewModel::onCategorySelected,
                    onClearAll = {
                        viewModel.onDateRangeSelected(DateRange.ALL)
                        viewModel.onTypeSelected(AmountFilter.ALL)
                        viewModel.onCategorySelected(null)
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.filteredTransactions, key = { it.id ?: it.hashCode() }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            formattedDate = dateFormatter.format(Date(tx.date)),
                            onClick = { tx.id?.let(onEditClick) },
                            onDelete = {
                                viewModel.deleteTransaction(tx)
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = "Transaction deleted",
                                        actionLabel = "Undo",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreLastDeleted()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initial = uiState.selectedMonth ?: YearMonth.now(),
            onDismiss = { showMonthPicker = false },
            onConfirm = { ym ->
                viewModel.onMonthPicked(ym)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun TotalsHeader(
    income: String,
    expense: String,
    balance: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(title = "Income", value = income, modifier = Modifier.weight(1f))
        SummaryCard(title = "Expense", value = expense, modifier = Modifier.weight(1f))
        SummaryCard(title = "Balance", value = balance, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeChips(
    selected: DateRange,
    selectedMonth: YearMonth?,
    onSelect: (DateRange) -> Unit,
    onPickMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ymFormatter = remember { DateTimeFormatter.ofPattern("MMM uuuu") }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selected == DateRange.ALL, onClick = { onSelect(DateRange.ALL) }, label = { Text("All") })
        FilterChip(selected = selected == DateRange.THIS_WEEK, onClick = { onSelect(DateRange.THIS_WEEK) }, label = { Text("This Week") })
        FilterChip(selected = selected == DateRange.THIS_MONTH, onClick = { onSelect(DateRange.THIS_MONTH) }, label = { Text("This Month") })
        FilterChip(selected = selected == DateRange.LAST_30D, onClick = { onSelect(DateRange.LAST_30D) }, label = { Text("Last 30d") })
        FilterChip(
            selected = selected == DateRange.MONTH,
            onClick = onPickMonth,
            label = {
                Text(if (selected == DateRange.MONTH && selectedMonth != null) selectedMonth.format(ymFormatter) else "Pick Month")
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )

        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeChips(
    selected: AmountFilter,
    onSelect: (AmountFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selected == AmountFilter.ALL, onClick = { onSelect(AmountFilter.ALL) }, label = { Text("All Types") })
        FilterChip(selected = selected == AmountFilter.INCOME, onClick = { onSelect(AmountFilter.INCOME) }, label = { Text("Income") })
        FilterChip(selected = selected == AmountFilter.EXPENSE, onClick = { onSelect(AmountFilter.EXPENSE) }, label = { Text("Expense") })
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    formattedDate: String,
    nextOccurrenceMillis: Long? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                    if (transaction.isRecurring) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(
                            onClick = { /* no-op */ },
                            label = { Text("Recurring") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Autorenew,
                                    contentDescription = null
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Category: ${transaction.category}", style = MaterialTheme.typography.bodySmall)
                Text("Date: $formattedDate", style = MaterialTheme.typography.bodySmall)

                // ⬇️ Show next occurrence (when available)
                if (transaction.isRecurring && nextOccurrenceMillis != null) {
                    val ctx = LocalContext.current
                    val mediumDf = remember(ctx) { android.text.format.DateFormat.getMediumDateFormat(ctx) }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Next on ${mediumDf.format(Date(nextOccurrenceMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = (if (transaction.amount >= 0) "+" else "-") + run {
                        val fmt = NumberFormat.getCurrencyInstance()
                        fmt.format(kotlin.math.abs(transaction.amount))
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}