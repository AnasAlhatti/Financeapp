package com.example.financeapp.feature_settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.feature_auth.presentation.auth.AuthViewModel
import com.example.financeapp.ui.common.DrawerRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenTransactions: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenBudgets: () -> Unit = {},
    onOpenScanReceipt: () -> Unit,
    viewModel: CurrencyViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val code by viewModel.currencyCode.collectAsState()
    val authVm: AuthViewModel = hiltViewModel()
    val authUser by authVm.user.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.example.financeapp.ui.common.AppDrawer(
                onNavigateTransactions = {
                    scope.launch { drawerState.close() }
                    onOpenTransactions()
                },
                onNavigateReports = {
                    scope.launch { drawerState.close() }
                    onOpenReports()
                },
                onNavigateBudgets = {
                    scope.launch { drawerState.close() }
                    onOpenBudgets()
                },
                onNavigateScanReceipt = {
                    scope.launch { drawerState.close() }
                    onOpenScanReceipt()
                },
                onNavigateSettings = { scope.launch { drawerState.close() } },
                currentUserEmail = authUser?.email,
                onLogout = {
                    scope.launch { drawerState.close() }
                    authVm.logout()
                },
                selectedRoute = DrawerRoute.Settings
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Currency", style = MaterialTheme.typography.titleMedium)

                CurrencyOptionRow(
                    label = "Turkish Lira (TRY)",
                    selected = code == "TRY",
                    onClick = { viewModel.setCurrency("TRY") }
                )
                CurrencyOptionRow(
                    label = "US Dollar (USD)",
                    selected = code == "USD",
                    onClick = { viewModel.setCurrency("USD") }
                )
                CurrencyOptionRow(
                    label = "Euro (EUR)",
                    selected = code == "EUR",
                    onClick = { viewModel.setCurrency("EUR") }
                )
                CurrencyOptionRow(
                    label = "UAE Dirham (AED)",
                    selected = code == "AED",
                    onClick = { viewModel.setCurrency("AED") }
                )
            }
        }
    }
}

@Composable
private fun CurrencyOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, modifier = Modifier.weight(1f))
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
