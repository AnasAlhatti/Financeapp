package com.example.financeapp.feature_transaction.presentation.budgets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun BudgetWarningsBanner(
    warnings: List<BudgetWarning>,
    modifier: Modifier = Modifier
) {
    if (warnings.isEmpty()) return
    val nf = remember { java.text.NumberFormat.getCurrencyInstance() }

    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text("Budgets", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                warnings.take(5).forEach { w ->
                    val level = levelForRatio(w.ratio)
                    val fg = colorForLevel(level)
                    val bg = containerFrom(fg)

                    val pctText = "${(w.ratio * 100).toInt()}%"
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${w.category}: $pctText (${nf.format(w.spent)}/${nf.format(w.limit)})",
                                fontWeight = if (level == BudgetLevel.OVER) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = bg,
                            labelColor = fg
                        )
                    )
                }
            }
        }
    }
}