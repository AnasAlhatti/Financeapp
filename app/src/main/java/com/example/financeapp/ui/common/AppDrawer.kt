package com.example.financeapp.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(
    onNavigateTransactions: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateBudgets: () -> Unit,
    selectedRoute: DrawerRoute
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp)     // <- narrower drawer (Material suggests ~280dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Menu",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        NavigationDrawerItem(
            label = { Text("Transactions") },
            icon = { Icon(Icons.Outlined.ListAlt, contentDescription = null) },
            selected = selectedRoute == DrawerRoute.Transactions,
            onClick = onNavigateTransactions,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Reports") },
            icon = { Icon(Icons.Outlined.Insights, contentDescription = null) },
            selected = selectedRoute == DrawerRoute.Reports,
            onClick = onNavigateReports,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Budgets") },
            icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null) },
            selected = selectedRoute == DrawerRoute.Budgets,
            onClick = onNavigateBudgets,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(Modifier.height(8.dp))
    }
}

enum class DrawerRoute { Transactions, Reports, Budgets}
