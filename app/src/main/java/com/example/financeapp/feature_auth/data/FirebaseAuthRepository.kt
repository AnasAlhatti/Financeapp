package com.example.financeapp.feature_auth.data

import android.util.Log
import com.example.financeapp.feature_auth.domain.model.AuthUser
import com.example.financeapp.feature_auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val AUTH_TIMEOUT_MS = 15_000L

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInEmail(email: String, password: String): Result<Unit> =
        runCatching {
            withTimeout(AUTH_TIMEOUT_MS) {
                auth.signInWithEmailAndPassword(email, password).await()
            }
        }
            .map { Unit }
            .onFailure { e -> Log.e("AuthRepo", "signInEmail failed", e) }

    override suspend fun registerEmail(email: String, password: String): Result<Unit> =
        runCatching {
            withTimeout(AUTH_TIMEOUT_MS) {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
        }
            .map { Unit }
            .onFailure { e -> Log.e("AuthRepo", "registerEmail failed", e) }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            withTimeout(AUTH_TIMEOUT_MS) {
                auth.signInWithCredential(credential).await()
            }
        }
            .map { Unit }
            .onFailure { e -> Log.e("AuthRepo", "signInWithGoogle failed", e) }

    override suspend fun signOut() { auth.signOut() }
}

private fun FirebaseUser.toDomain() =
    AuthUser(uid = uid, email = email ?: "")
