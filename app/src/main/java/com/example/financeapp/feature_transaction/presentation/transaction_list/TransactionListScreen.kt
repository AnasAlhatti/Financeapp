package com.example.financeapp.feature_transaction.presentation.transaction_list

import android.content.Context
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.feature_settings.CurrencyViewModel
import com.example.financeapp.feature_settings.rememberCurrencyFormatter
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.presentation.budgets.BudgetWarningsBanner
import com.example.financeapp.ui.common.AppDrawer
import com.example.financeapp.ui.common.DrawerRoute
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
    onOpenSettings: () -> Unit,
    onOpenScanReceipt: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember(LocalContext.current) { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val currencyVm: CurrencyViewModel = hiltViewModel()
    val currencyCode by currencyVm.currencyCode.collectAsState()
    val currencyFormat = rememberCurrencyFormatter(currencyCode)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showMonthPicker by remember { mutableStateOf(false) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Budget warnings
    val budgetsVm: com.example.financeapp.feature_transaction.presentation.budgets.BudgetsSummaryViewModel = hiltViewModel()
    val warnings by budgetsVm.warnings.collectAsState()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val all = uiState.allTransactions // or uiState.filteredTransactions if you want only current selection
            val ok = writeTransactionsCsv(context, uri, all)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) "Exported ${all.size} transactions." else "Export failed."
                )
            }
        }
    }

// OpenDocument for import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val imported = readTransactionsCsv(context, uri)
            if (imported.isNotEmpty()) {
                viewModel.addTransactions(imported)
                scope.launch {
                    snackbarHostState.showSnackbar("Imported ${imported.size} transactions.")
                }
            } else {
                scope.launch { snackbarHostState.showSnackbar("No valid rows to import.") }
            }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                onNavigateTransactions = { scope.launch { drawerState.close() } },   // already here
                onNavigateReports = {
                    scope.launch { drawerState.close() }
                    onOpenReports()
                },
                onNavigateBudgets = {
                    scope.launch { drawerState.close() }
                    onOpenBudgets()
                },
                onNavigateSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                selectedRoute = DrawerRoute.Transactions,
                onExportCsv = {
                    scope.launch { drawerState.close() }
                    exportLauncher.launch("transactions_${System.currentTimeMillis()}.csv")
                },
                onImportCsv = {
                    scope.launch { drawerState.close() }
                    importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                },
                onNavigateScanReceipt = {
                    scope.launch { drawerState.close() }
                onOpenScanReceipt()
                }
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
                    formatter = currencyFormat,
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

@Composable
private fun TransactionItem(
    transaction: Transaction,
    formattedDate: String,
    nextOccurrenceMillis: Long? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyVm: CurrencyViewModel = hiltViewModel()
    val currencyCode by currencyVm.currencyCode.collectAsState()
    val currencyFormat = rememberCurrencyFormatter(currencyCode)
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
                Text(
                    "Category: ${transaction.category}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Date: $formattedDate", style = MaterialTheme.typography.bodySmall)

                // ⬇️ Show next occurrence (when available)
                if (transaction.isRecurring && nextOccurrenceMillis != null) {
                    val ctx = LocalContext.current
                    val mediumDf =
                        remember(ctx) { android.text.format.DateFormat.getMediumDateFormat(ctx) }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Next on ${mediumDf.format(Date(nextOccurrenceMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = (if (transaction.amount >= 0) "+" else "-") +
                            currencyFormat.format(kotlin.math.abs(transaction.amount)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun writeTransactionsCsv(
    context: Context,
    uri: Uri,
    items: List<Transaction>
): Boolean {
    return try {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        context.contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                // header
                writer.appendLine("Title,Category,Date,Amount,Recurring")
                items.forEach { tx ->
                    val dateStr = dateFormat.format(Date(tx.date))
                    writer.appendLine(
                        listOf(
                            tx.title.csvEsc(),
                            tx.category.csvEsc(),
                            dateStr,  // ✅ formatted date
                            tx.amount.toString(),
                            tx.isRecurring.toString()
                        ).joinToString(",")
                    )
                }
            }
        }
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }
}

/** Read CSV with the same columns we write above. */
private fun readTransactionsCsv(
    context: Context,
    uri: Uri
): List<com.example.financeapp.feature_transaction.domain.model.Transaction> {
    val result = mutableListOf<com.example.financeapp.feature_transaction.domain.model.Transaction>()
    context.contentResolver.openInputStream(uri)?.use { input ->
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
            var first = true
            br.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach
                // skip header if present
                if (first && line.lowercase(Locale.getDefault()).startsWith("title,category,date,amount,recurring")) {
                    first = false
                    return@forEach
                }
                first = false

                val cols = parseCsvLine(line)
                if (cols.size < 5) return@forEach

                val title = cols[0].trim()
                val category = cols[1].trim()
                val dateMillis = parseDateFlexible(cols[2]) ?: return@forEach
                val amount = cols[3].toDoubleOrNull() ?: return@forEach
                val recurring = cols[4].toBooleanStrictOrNull() ?: cols[4].equals("true", ignoreCase = true)

                result += com.example.financeapp.feature_transaction.domain.model.Transaction(
                    id = null,
                    title = title,
                    amount = amount,
                    category = category,
                    date = dateMillis,
                    isRecurring = recurring
                )
            }
        }
    }
    return result
}

/** Robust CSV line parser supporting quotes and commas. */
private fun parseCsvLine(line: String): List<String> {
    val out = mutableListOf<String>()
    val cur = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    // Escaped quote
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            }
            c == ',' && !inQuotes -> {
                out += cur.toString()
                cur.setLength(0)
            }
            else -> cur.append(c)
        }
        i++
    }
    out += cur.toString()
    return out
}

/** Accept dd/MM/yyyy, yyyy-MM-dd, MM/dd/yyyy, or millis. */
private fun parseDateFlexible(input: String): Long? {
    val s = input.trim()
    // 1) millis?
    s.toLongOrNull()?.let { return it }

    // 2) try supported patterns
    val patterns = listOf("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy")
    for (p in patterns) {
        try {
            val df = SimpleDateFormat(p, Locale.getDefault())
            df.isLenient = false
            val d = df.parse(s)
            if (d != null) return d.time
        } catch (_: Exception) { /* try next */ }
    }
    return null
}
fun String.csvEsc(): String {
    val mustQuote = contains(',') || contains('"') || contains('\n') || contains('\r')
    val cleaned = replace("\"", "\"\"")
    return if (mustQuote) "\"$cleaned\"" else cleaned
}