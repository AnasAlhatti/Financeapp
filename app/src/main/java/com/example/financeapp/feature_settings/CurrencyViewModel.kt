package com.example.financeapp.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.core.prefs.CurrencyPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val prefs: CurrencyPrefs
) : ViewModel() {
    val currencyCode: StateFlow<String> =
        prefs.currencyCodeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "TRY")

    val compactMoney: StateFlow<Boolean> =
        prefs.compactMoneyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setCurrency(code: String) = viewModelScope.launch { prefs.setCurrency(code) }
    fun setCompactMoney(enabled: Boolean) = viewModelScope.launch { prefs.setCompactMoney(enabled) }
}
