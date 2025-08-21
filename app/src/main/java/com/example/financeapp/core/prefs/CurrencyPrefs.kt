package com.example.financeapp.core.prefs

import android.content.Context
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
    // Default to TRY because your timezone is Istanbul; change if you prefer USD
    val currencyCodeFlow: Flow<String> =
        context.dataStore.data.map { it[KEY_CURRENCY] ?: "TRY" }

    suspend fun setCurrency(code: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = code }
    }
}
