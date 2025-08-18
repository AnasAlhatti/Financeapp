package com.example.financeapp.feature_transaction.presentation.recurring

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.feature_transaction.domain.model.RecurringRule
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ManageRecurringScreen(
    onBack: () -> Unit,
    viewModel: ManageRecurringViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    var showDialog by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf(RecurringFormState()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Recurring") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                form = RecurringFormState()
                showDialog = true
            }) { Text("+") }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No recurring rules", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first recurring income or expense.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rules, key = { it.id ?: (it.title + it.category).hashCode() }) { rule ->
                    RecurringItem(
                        rule = rule,
                        onEdit = {
                            form = RecurringFormState(
                                id = rule.id,
                                title = rule.title,
                                amountInput = kotlin.math.abs(rule.amount).toString(),
                                isExpense = rule.amount < 0,
                                category = rule.category,
                                frequency = rule.frequency,
                                dayOfWeek = rule.dayOfWeek,
                                dayOfMonth = rule.dayOfMonth,
                                startAt = rule.startAt,
                                hasEnd = rule.endAt != null,
                                endAt = rule.endAt
                            )
                            showDialog = true
                        },
                        onDelete = { viewModel.delete(rule) },
                        nf = nf
                    )
                }
            }
        }
    }

    if (showDialog) {
        RecurringDialog(
            state = form,
            onState = { form = it },
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.upsert(form)
                showDialog = false
            },
            previews = viewModel.previewNext(form),
        )
    }
}

@Composable
private fun RecurringItem(
    rule: RecurringRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    nf: NumberFormat
) {
    val amountColor =
        if (rule.amount >= 0) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error

    // ✅ get formatter from a real Context
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mediumDf = remember(ctx) { android.text.format.DateFormat.getMediumDateFormat(ctx) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rule.title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
            Spacer(Modifier.height(4.dp))
            Text("${rule.category} • ${rule.frequency.name.lowercase().replaceFirstChar { it.titlecase() }}")
            Spacer(Modifier.height(2.dp))
            Text(nf.format(rule.amount), color = amountColor)
            Spacer(Modifier.height(6.dp))
            // ✅ use mediumDf instead of null
            Text(
                "Next: ${mediumDf.format(java.util.Date(rule.nextAt))}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun RecurringDialog(
    state: RecurringFormState,
    onState: (RecurringFormState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    previews: List<Long>
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // ✅ get a non-null Context-based date formatter
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mediumDf = remember(ctx) { android.text.format.DateFormat.getMediumDateFormat(ctx) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "Add Recurring" else "Edit Recurring") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ... (unchanged fields above)

                // Start date (opens picker)
                androidx.compose.material3.OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartPicker = true }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Start date", style = MaterialTheme.typography.labelMedium)
                        Text(mediumDf.format(java.util.Date(state.startAt)))
                    }
                }

                // End date toggle & field
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.hasEnd, onCheckedChange = { onState(state.copy(hasEnd = it)) })
                    Spacer(Modifier.width(8.dp))
                    Text("Has end date")
                }
                if (state.hasEnd) {
                    androidx.compose.material3.OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndPicker = true }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("End date", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = state.endAt?.let { mediumDf.format(java.util.Date(it)) } ?: "Pick date"
                            )
                        }
                    }
                }

                // Preview next occurrences
                if (previews.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Upcoming:", style = MaterialTheme.typography.labelMedium)
                    previews.forEach {
                        Text("• " + mediumDf.format(java.util.Date(it)))
                    }
                }
            }
        },
        confirmButton = { /* unchanged */ },
        dismissButton = { /* unchanged */ }
    )

    // Date pickers (unchanged logic)
    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = { TextButton(onClick = { showStartPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) {
            val dp = rememberDatePickerState(initialSelectedDateMillis = state.startAt)
            LaunchedEffect(dp.selectedDateMillis) {
                dp.selectedDateMillis?.let { onState(state.copy(startAt = it)) }
            }
            DatePicker(state = dp)
        }
    }
    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = { TextButton(onClick = { showEndPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) {
            val dp = rememberDatePickerState(initialSelectedDateMillis = state.endAt ?: state.startAt)
            LaunchedEffect(dp.selectedDateMillis) {
                dp.selectedDateMillis?.let { onState(state.copy(endAt = it)) }
            }
            DatePicker(state = dp)
        }
    }
}

