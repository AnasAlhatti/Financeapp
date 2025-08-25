package com.example.financeapp.feature_auth.domain.use_case

import com.example.financeapp.feature_auth.domain.repository.AuthRepository

class SignInEmail(private val repo: AuthRepository) {
    suspend operator fun invoke(e: String, p: String) = repo.signInEmail(e, p)
}