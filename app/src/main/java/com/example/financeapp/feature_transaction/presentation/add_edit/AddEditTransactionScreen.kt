package com.example.financeapp.feature_transaction.presentation.add_edit

import android.icu.text.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionIdArg: Int? = null,
    onBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    // load existing transaction if editing
    LaunchedEffect(transactionIdArg) {
        viewModel.loadForEdit(transactionIdArg)
    }

    val state by viewModel.state.collectAsState()
    val categories = listOf("Food", "Transport", "Shopping", "Salary", "Other")

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember(LocalContext.current) {
        DateFormat.getDateInstance(DateFormat.MEDIUM)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.id != null) {
                        TextButton(onClick = { viewModel.delete(onBack) }) {
                            Text("Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Amount
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Income / Expense toggle
            Text("Type", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = !state.isExpense,
                    onClick = { viewModel.onTypeChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Income") }
                )
                SegmentedButton(
                    selected = state.isExpense,
                    onClick = { viewModel.onTypeChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Expense") }
                )
            }

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.onCategoryChange(cat)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Date selector
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Date", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = dateFormatter.format(Date(state.dateMillis)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = state.dateMillis
                    )
                    // push selected date to VM
                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    }
                    DatePicker(state = datePickerState)
                }
            }

            // Save
            Button(
                onClick = { viewModel.save(onSuccess = onBack) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.id == null) "Save" else "Update")
            }
        }
    }
}
