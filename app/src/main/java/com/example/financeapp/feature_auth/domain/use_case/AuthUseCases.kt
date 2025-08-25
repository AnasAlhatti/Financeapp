package com.example.financeapp.feature_auth.domain.use_case

data class AuthUseCases(
    val observeAuthState: ObserveAuthState,
    val signInEmail: SignInEmail,
    val registerEmail: RegisterEmail,
    val signInGoogle: SignInGoogle,
    val signOut: SignOut
)
