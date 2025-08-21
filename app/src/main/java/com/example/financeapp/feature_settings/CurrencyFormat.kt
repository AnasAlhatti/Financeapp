// app/src/main/java/com/example/financeapp/feature_settings/CurrencyFormat.kt
package com.example.financeapp.feature_settings

import androidx.compose.runtime.*
import java.text.NumberFormat
import java.util.Currency

/**
 * Returns a NumberFormat configured with the current currency selection.
 * Re-composes when currencyCode changes.
 */
@Composable
fun rememberCurrencyFormatter(currencyCode: String?): NumberFormat {
    val code = currencyCode ?: "TRY"
    return remember(code) {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(code)
        }
    }
}
