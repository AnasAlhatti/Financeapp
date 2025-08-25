package com.example.financeapp.feature_auth.domain.use_case

import com.example.financeapp.feature_auth.domain.repository.AuthRepository

class SignOut(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}