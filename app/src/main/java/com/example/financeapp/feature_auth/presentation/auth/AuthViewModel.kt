package com.example.financeapp.feature_auth.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_auth.domain.model.AuthUser
import com.example.financeapp.feature_auth.domain.use_case.AuthUseCases
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val useCases: AuthUseCases,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val user: StateFlow<AuthUser?> =
        useCases.observeAuthState()
            .onEach { u ->
                if (u != null) {
                    transactionRepository.startSync(u.uid)   // begin mirroring
                    budgetRepository.startSync(u.uid)
                } else {
                    transactionRepository.stopSync()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout() = viewModelScope.launch {
        transactionRepository.clearLocal()    // prevent leakage to next account
        useCases.signOut()
    }
}
