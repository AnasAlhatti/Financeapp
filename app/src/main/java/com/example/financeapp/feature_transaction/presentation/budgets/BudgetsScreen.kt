package com.example.financeapp.feature_transaction.presentation.budgets

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<Int?>(null) }
    var category by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editId = null; category = ""; amountInput = ""; showDialog = true
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.items, key = { it.budget.id ?: it.budget.category.hashCode() }) { ui ->
                BudgetRow(
                    ui = ui,
                    onEdit = {
                        editId = ui.budget.id
                        category = ui.budget.category
                        amountInput = ui.budget.limitAmount.toString()
                        showDialog = true
                    },
                    onDelete = { viewModel.delete(ui.budget) }
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editId == null) "Add Budget" else "Edit Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") }
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Monthly limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (category.isNotBlank() && amt > 0.0) {
                        viewModel.upsert(category, amt, editId)
                        showDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BudgetRow(
    ui: BudgetUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val nf = remember { NumberFormat.getCurrencyInstance() }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ui.budget.category, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { ui.progress },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${nf.format(ui.spentThisMonth)} / ${nf.format(ui.budget.limitAmount)}" +
                        if (ui.over) " (over)" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = if (ui.over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}
