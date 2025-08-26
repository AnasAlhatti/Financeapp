package com.example.financeapp.feature_settings

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/**
 * Returns a lambda that formats numbers like TRY1.2K / TRY3.4M / TRY2.1B.
 * Sign is preserved: -TRY1.2K
 */
@SuppressLint("LocalContextConfigurationRead")
@Composable
fun rememberCompactMoneyFormatter(currencyCode: String?): (Double) -> String {
    val ctx = LocalContext.current
    val locale: Locale = ctx.resources.configuration.locales.let {
        if (it.isEmpty) Locale.getDefault() else it[0]
    }
    val code = currencyCode ?: "TRY"
    val currency = Currency.getInstance(code)
    val symbol = currency.getSymbol(locale)

    val num = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
            isGroupingUsed = true
        }
    }

    return { value ->
        val sign = if (value < 0) "-" else ""
        val v = abs(value)
        val scaled: Double
        val suffix: String
        when {
            v >= 1_000_000_000 -> { scaled = v / 1_000_000_000.0; suffix = "B" }
            v >= 1_000_000     -> { scaled = v / 1_000_000.0;     suffix = "M" }
            v >= 1_000         -> { scaled = v / 1_000.0;         suffix = "K" }
            else               -> { scaled = v;                   suffix = ""  }
        }
        if (suffix.isEmpty()) {
            // fall back to full currency when < 1k
            NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }.format(value)
        } else {
            "$sign$symbol${num.format(scaled)}$suffix"
        }
    }
}
