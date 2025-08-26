package com.example.financeapp.feature_transaction.presentation.transaction_list

import android.R.attr.maxWidth
import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.feature_auth.presentation.auth.AuthViewModel
import com.example.financeapp.feature_settings.CurrencyViewModel
import com.example.financeapp.feature_settings.rememberCompactMoneyFormatter
import com.example.financeapp.feature_settings.rememberCurrencyFormatter
import com.example.financeapp.feature_transaction.domain.model.Transaction
import com.example.financeapp.feature_transaction.presentation.budgets.BudgetWarningsBanner
import com.example.financeapp.ui.common.AppDrawer
import com.example.financeapp.ui.common.DrawerRoute
import com.example.financeapp.ui.common.rememberCategoryUi
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.YearMonth
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    var searchMode by remember { mutableStateOf(false) }
    val kb = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val compact by currencyVm.compactMoney.collectAsState()
    val compactMoney = rememberCompactMoneyFormatter(currencyCode)

    var showMonthPicker by remember { mutableStateOf(false) }
    val authVm: AuthViewModel = hiltViewModel()
    val authUser by authVm.user.collectAsState()
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
            val all = uiState.allTransactions
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
                onNavigateScanReceipt = {
                    scope.launch { drawerState.close() }
                    onOpenScanReceipt()
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
                currentUserEmail = authUser?.email,
                onLogout = {
                    scope.launch { drawerState.close() }
                    authVm.logout()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (searchMode) {
                            TextField(
                                value = searchQuery,
                                onValueChange = viewModel::onQueryChange,
                                singleLine = true,
                                placeholder = { Text("Search transactions") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { kb?.hide() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        } else {
                            Text("Transactions")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Open navigation")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (searchMode) {
                                viewModel.clearQuery()
                                searchMode = false
                                kb?.hide()
                            } else {
                                searchMode = true
                            }
                        }) {
                            Icon(
                                imageVector = if (searchMode) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = if (searchMode) "Close search" else "Search"
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

                TotalsHeaderAdaptive(
                    income  = if (compact) compactMoney(uiState.totalIncome)  else currencyFormat.format(uiState.totalIncome),
                    expense = if (compact) compactMoney(uiState.totalExpense) else currencyFormat.format(uiState.totalExpense),
                    balance = if (compact) compactMoney(uiState.balance)     else currencyFormat.format(uiState.balance),
                    compactEnabled = compact,
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        item { SummaryCard(title = "Income", value = income, modifier = Modifier.fillMaxWidth()) }
        item { SummaryCard(title = "Expense", value = expense, modifier = Modifier.fillMaxWidth()) }
        item(span = { GridItemSpan(2) }) {
            SummaryCard(title = "Balance", value = balance, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            AutoResizeText(
                text = value,
                style = MaterialTheme.typography.titleLarge, // reasonable starting size
                minSize = 14.sp,
                maxSize = 20.sp                              // never bigger than this
            )
        }
    }
}
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun TotalsHeaderAdaptive(
    income: String,
    expense: String,
    balance: String,
    compactEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val spacing = 12.dp
        val innerHPad = 32.dp // 16dp start + 16dp end inside card
        val threeCardWidth = (maxWidth - spacing * 2) / 3
        val textStyle = MaterialTheme.typography.titleLarge

        // How wide are our texts at normal size?
        val maxTextWidthPx = listOf(income, expense, balance).maxOf {
            measurer.measure(AnnotatedString(it), style = textStyle).size.width
        }
        val textAreaWidthPx = with(density) { (threeCardWidth - innerHPad).coerceAtLeast(0.dp).toPx() }

        val fitsThree = maxTextWidthPx <= textAreaWidthPx

        if (compactEnabled || fitsThree) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                SummaryCard(title = "Income", value = income, modifier = Modifier.weight(1f))
                SummaryCard(title = "Expense", value = expense, modifier = Modifier.weight(1f))
                SummaryCard(title = "Balance", value = balance, modifier = Modifier.weight(1f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                item { SummaryCard("Income", income, Modifier.fillMaxWidth()) }
                item { SummaryCard("Expense", expense, Modifier.fillMaxWidth()) }
                item(span = { GridItemSpan(2) }) {
                    SummaryCard("Balance", balance, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
@Composable
private fun TransactionItem(
    transaction: Transaction,
    formattedDate: String,
    nextOccurrenceMillis: Long? = null, // unused in compact; add back if you want a third line
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyVm: CurrencyViewModel = hiltViewModel()
    val currencyCode by currencyVm.currencyCode.collectAsState()
    val currencyFormat = rememberCurrencyFormatter(currencyCode)

    val catUi = rememberCategoryUi(transaction.category)
    val amountText = (if (transaction.amount >= 0) "+" else "-") +
            currencyFormat.format(kotlin.math.abs(transaction.amount))
    val amountColor =
        if (transaction.amount >= 0) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // tighter spacing
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp) // compact inner padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading round icon (smaller)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(catUi.container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catUi.icon,
                    contentDescription = null,
                    tint = catUi.onContainer,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Title + tiny row of category chip and date
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Use SuggestionChip (smaller than AssistChip)
                    androidx.compose.material3.SuggestionChip(
                        onClick = { },
                        icon = { Icon(catUi.icon, null, modifier = Modifier.size(14.dp)) },
                        label = { Text(transaction.category, style = MaterialTheme.typography.labelSmall) },
                        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                            containerColor = catUi.chipContainer,
                            labelColor = catUi.chipLabel,
                            iconContentColor = catUi.chipLabel
                        )
                    )

                    Text(
                        formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Trailing: amount + delete (small)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    amountText,
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                    maxLines = 1
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                }
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
): List<Transaction> {
    val result = mutableListOf<Transaction>()
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

                result += Transaction(
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

@Composable
private fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minSize: TextUnit = 14.sp,
    maxSize: TextUnit = 20.sp,   // ⬅️ hard cap so it never gets huge
    stepSp: Float = 1f
) {
    val maxSp = maxSize.value
    val minSp = minSize.value
    var sizeSp by remember {
        mutableFloatStateOf(
            // start at the smaller of style size or max
            (if (style.fontSize != TextUnit.Unspecified) style.fontSize.value else maxSp)
                .coerceAtMost(maxSp)
        )
    }
    var ready by remember { mutableStateOf(false) }

    Text(
        text = text,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip,
        style = style.copy(
            fontSize = sizeSp.sp,
            fontFeatureSettings = "tnum" // tabular digits
        ),
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { res ->
            if ((res.didOverflowWidth || res.didOverflowHeight) && sizeSp > minSp) {
                sizeSp = (sizeSp - stepSp).coerceAtLeast(minSp)
            } else {
                ready = true
            }
        }
    )
}