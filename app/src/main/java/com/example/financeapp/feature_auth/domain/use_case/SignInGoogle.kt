package com.example.financeapp.feature_auth.domain.use_case

import com.example.financeapp.feature_auth.domain.repository.AuthRepository

class SignInGoogle(private val repo: AuthRepository) {
    suspend operator fun invoke(idToken: String) = repo.signInWithGoogle(idToken)
}