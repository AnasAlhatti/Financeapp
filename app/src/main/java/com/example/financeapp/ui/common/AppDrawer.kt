package com.example.financeapp.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(
    onNavigateTransactions: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateBudgets: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateScanReceipt: () -> Unit,
    onLogout: (() -> Unit)? = null,
    currentUserEmail: String? = null,
    onExportCsv: (() -> Unit)? = null,
    onImportCsv: (() -> Unit)? = null,
    selectedRoute: DrawerRoute,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp) // narrow drawer
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Menu",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        currentUserEmail?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
        }

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

        NavigationDrawerItem(
            label = { Text("Settings") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            selected = selectedRoute == DrawerRoute.Settings,
            onClick = onNavigateSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Scan receipt") },
            icon  = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
            selected = false,
            onClick = onNavigateScanReceipt,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Optional “Data” section for CSV when available
        if (onExportCsv != null || onImportCsv != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Data",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (onExportCsv != null) {
                NavigationDrawerItem(
                    label = { Text("Export CSV") },
                    icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                    selected = false,
                    onClick = onExportCsv,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

            if (onImportCsv != null) {
                NavigationDrawerItem(
                    label = { Text("Import CSV") },
                    icon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                    selected = false,
                    onClick = onImportCsv,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
        if (onLogout != null) {
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            NavigationDrawerItem(
                label = { Text("Logout") },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

enum class DrawerRoute { Transactions, Reports, Budgets, Settings}
