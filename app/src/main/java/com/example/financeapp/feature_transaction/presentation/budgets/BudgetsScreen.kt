package com.example.financeapp.feature_transaction.presentation.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    onOpenTransactions: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val nf = remember { NumberFormat.getCurrencyInstance() }
    val snackbar = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<Int?>(null) }
    var category by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.example.financeapp.ui.common.AppDrawer(
                onNavigateTransactions = {
                    scope.launch { drawerState.close() }
                    onOpenTransactions()
                },
                onNavigateReports = {
                    scope.launch { drawerState.close() }
                    onOpenReports()
                },
                onNavigateBudgets = { scope.launch { drawerState.close() } },
                selectedRoute = com.example.financeapp.ui.common.DrawerRoute.Budgets
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Budgets") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editId = null; category = ""; amountInput = ""; showDialog = true
                }) { Text("+") }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            if (state.items.isEmpty()) {
                EmptyBudgets(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    onAdd = {
                        editId = null; category = ""; amountInput = ""; showDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        state.items,
                        key = { it.budget.id ?: it.budget.category.hashCode() }) { ui ->
                        val ratio =
                            if (ui.budget.limitAmount > 0.0) ui.spentThisMonth / ui.budget.limitAmount else 0.0
                        val level = levelForRatio(ratio)
                        val barColor = colorForLevel(level)

                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        ui.budget.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = barColor
                                    )
                                    IconButton(onClick = { viewModel.delete(ui.budget) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { ui.progress },
                                    color = barColor,
                                    trackColor = barColor.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${nf.format(ui.spentThisMonth)} / ${nf.format(ui.budget.limitAmount)}" +
                                            if (ui.over) " (over)" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = barColor
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        editId = ui.budget.id
                                        category = ui.budget.category
                                        amountInput = ui.budget.limitAmount.toString()
                                        showDialog = true
                                    }) { Text("Edit") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            title = if (editId == null) "Add Budget" else "Edit Budget",
            category = category,
            onCategoryChange = { category = it },
            amountInput = amountInput,
            onAmountChange = { amountInput = it },
            categorySuggestions = state.categories,
            onDismiss = { showDialog = false },
            onConfirm = {
                val amt = amountInput.toDoubleOrNull() ?: return@BudgetDialog
                if (category.isBlank() || amt <= 0.0) return@BudgetDialog
                viewModel.upsert(category, amt, editId)
                showDialog = false
            }
        )
    }
    if (showDialog) {
        BudgetDialog(
            title = if (editId == null) "Add Budget" else "Edit Budget",
            category = category,
            onCategoryChange = { category = it },
            amountInput = amountInput,
            onAmountChange = { amountInput = it },
            categorySuggestions = state.categories,
            onDismiss = { showDialog = false },
            onConfirm = {
                val amt = amountInput.toDoubleOrNull()
                if (category.isBlank() || amt == null || amt <= 0.0) {
                    // simple validation
                    return@BudgetDialog
                }
                viewModel.upsert(category, amt, editId)
                showDialog = false
            }
        )
    }
}

@Composable
private fun EmptyBudgets(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit
) {
    Box(modifier) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No budgets yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Create monthly limits per category to track your spending.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) { Text("Add a budget") }
        }
    }
}

/** Dialog with category dropdown (suggestions) + amount input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    title: String,
    category: String,
    onCategoryChange: (String) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    categorySuggestions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var catMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category with suggestions; still editable as free text
                ExposedDropdownMenuBox(
                    expanded = catMenu,
                    onExpandedChange = { catMenu = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = onCategoryChange,
                        label = { Text("Category") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = catMenu,
                        onDismissRequest = { catMenu = false }
                    ) {
                        if (categorySuggestions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No suggestions") },
                                onClick = { catMenu = false },
                                enabled = false
                            )
                        } else {
                            categorySuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        onCategoryChange(suggestion)
                                        catMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    label = { Text("Monthly limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
