package com.example.financeapp.feature_auth.domain.use_case

import com.example.financeapp.feature_auth.domain.repository.AuthRepository

class ObserveAuthState(private val repo: AuthRepository) {
    operator fun invoke() = repo.authState
}