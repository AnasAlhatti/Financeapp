package com.example.financeapp.feature_auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_auth.domain.use_case.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val useCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onEmail(e: String) { _state.value = _state.value.copy(email = e) }
    fun onPassword(p: String) { _state.value = _state.value.copy(password = p) }

    fun login(onSuccess: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        val res = useCases.signInEmail(s.email.trim(), s.password)
        _state.value = _state.value.copy(loading = false)
        res.onSuccess { onSuccess() }.onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun loginGoogle(idToken: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val res = useCases.signInGoogle(idToken)
        _state.value = _state.value.copy(loading = false)
        res.onSuccess { onSuccess() }.onFailure { _state.value = _state.value.copy(error = it.message) }
    }
}
