package com.example.financeapp.feature_auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_auth.domain.use_case.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    fun onEmail(e: String)    = _state.update { it.copy(email = e,    error = null) }
    fun onPassword(p: String) = _state.update { it.copy(password = p, error = null) }
    fun onConfirm(c: String)  = _state.update { it.copy(confirm = c,  error = null) }

    fun register(onSuccess: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        if (s.password != s.confirm) {
            _state.update { it.copy(error = "Passwords do not match") }
            return@launch
        }
        performAuth(
            block = { useCases.registerEmail(s.email.trim(), s.password) },
            onSuccess = onSuccess
        )
    }

    private suspend fun performAuth(
        block: suspend () -> Result<Unit>,
        onSuccess: () -> Unit
    ) {
        _state.update { it.copy(loading = true, error = null) }
        val result = try {
            block()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            _state.update { it.copy(loading = false) }  // always re-enable
        }

        result.onSuccess { onSuccess() }
            .onFailure { e -> _state.update { it.copy(error = friendlyMessage(e)) } }
    }

    private fun friendlyMessage(e: Throwable): String {
        val msg = e.message ?: "Registration failed"
        return when {
            msg.contains("EMAIL_EXISTS", true) -> "Email already in use"
            msg.contains("WEAK_PASSWORD", true) -> "Password is too weak"
            msg.contains("NETWORK", true) -> "No internet connection"
            else -> msg
        }
    }
}
