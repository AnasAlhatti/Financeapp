package com.example.financeapp.feature_auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.feature_auth.domain.use_case.AuthUseCases
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    fun onEmail(e: String) = _state.update { it.copy(email = e, error = null) }
    fun onPassword(p: String) = _state.update { it.copy(password = p, error = null) }

    fun login(onSuccess: () -> Unit) = viewModelScope.launch {
        val email = _state.value.email.trim()
        val password = _state.value.password
        performAuth(
            block = { useCases.signInEmail(email, password) },
            onSuccess = onSuccess
        )
    }

    fun loginGoogle(idToken: String, onSuccess: () -> Unit) = viewModelScope.launch {
        performAuth(
            block = { useCases.signInGoogle(idToken) },
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
            // always re-enable the buttons
            _state.update { it.copy(loading = false) }
        }

        result.onSuccess { onSuccess() }
            .onFailure { e -> _state.update { it.copy(error = friendlyMessage(e)) } }
    }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is kotlinx.coroutines.TimeoutCancellationException -> "Login timed out. Check your internet or Play services."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email"
            "ERROR_WRONG_PASSWORD" -> "Wrong password"
            "ERROR_USER_NOT_FOUND" -> "No account found"
            "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection"
            else -> e.message ?: "Authentication failed"
        }
        else -> e.message ?: "Authentication failed"
    }

}
