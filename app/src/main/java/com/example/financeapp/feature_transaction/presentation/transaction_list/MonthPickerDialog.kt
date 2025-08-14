package com.example.financeapp.feature_transaction.presentation.transaction_list

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Month
import java.time.YearMonth
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerDialog(
    initial: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    val currentInitial by rememberUpdatedState(initial)
    var selectedYear by remember { mutableIntStateOf(currentInitial.year) }
    var selectedMonth by remember { mutableStateOf(currentInitial.month) }

    val years = remember {
        val nowYear = YearMonth.now().year
        // last 6 years (customize if you want more)
        (nowYear downTo (nowYear - 5)).toList()
    }
    val months = remember {
        Month.values().toList() // JAN..DEC
    }

    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Month") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedYear.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            years.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y.toString()) },
                                    onClick = {
                                        selectedYear = y
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedMonth.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())) },
                                    onClick = {
                                        selectedMonth = m
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(YearMonth.of(selectedYear, selectedMonth)) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
