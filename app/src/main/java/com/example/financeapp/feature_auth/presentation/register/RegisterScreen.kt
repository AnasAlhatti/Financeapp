package com.example.financeapp.feature_auth.presentation.register

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
    vm: RegisterViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = s.email, onValueChange = vm::onEmail,
            label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, null) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.password, onValueChange = vm::onPassword,
            label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.confirm, onValueChange = vm::onConfirm,
            label = { Text("Confirm Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation()
        )

        s.error?.let {
            Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.register(onRegisterSuccess) },
            enabled = !s.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (s.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Register")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateLogin) { Text("Already have an account? Login") }
    }
}
