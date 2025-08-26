package com.example.financeapp.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_CURRENCY = stringPreferencesKey("currency_code")

@Singleton
class CurrencyPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore  // your existing extension

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_code")
        val COMPACT = booleanPreferencesKey("compact_money")
    }

    val currencyCodeFlow: Flow<String> =
        dataStore.data.map { it[Keys.CURRENCY] ?: "TRY" }

    val compactMoneyFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.COMPACT] ?: false }

    suspend fun setCurrency(code: String) {
        dataStore.edit { it[Keys.CURRENCY] = code }
    }

    suspend fun setCompactMoney(enabled: Boolean) {
        dataStore.edit { it[Keys.COMPACT] = enabled }
    }
}
