package com.example.financeapp.feature_auth.domain.repository

import com.example.financeapp.feature_auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>        // emits current user or null
    suspend fun signInEmail(email: String, password: String): Result<Unit>
    suspend fun registerEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
}
