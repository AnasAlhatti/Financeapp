package com.example.financeapp.feature_auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_auth.domain.use_case.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val useCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state = _state.asStateFlow()

    fun onEmail(e: String) { _state.value = _state.value.copy(email = e) }
    fun onPassword(p: String) { _state.value = _state.value.copy(password = p) }
    fun onConfirm(c: String) { _state.value = _state.value.copy(confirm = c) }

    fun register(onSuccess: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        if (s.password != s.confirm) {
            _state.value = s.copy(error = "Passwords do not match")
            return@launch
        }
        _state.value = s.copy(loading = true, error = null)
        val res = useCases.registerEmail(s.email.trim(), s.password)
        _state.value = _state.value.copy(loading = false)
        res.onSuccess { onSuccess() }.onFailure { _state.value = _state.value.copy(error = it.message) }
    }
}
