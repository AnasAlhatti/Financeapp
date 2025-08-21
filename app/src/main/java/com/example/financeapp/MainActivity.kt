package com.example.financeapp

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.feature_settings.SettingsScreen
import com.example.financeapp.feature_transaction.presentation.add_edit.AddEditTransactionScreen
import com.example.financeapp.feature_transaction.presentation.budgets.BudgetsScreen
import com.example.financeapp.feature_transaction.presentation.reports.ReportsScreen
import com.example.financeapp.feature_transaction.presentation.scan.ScanReceiptScreen
import com.example.financeapp.feature_transaction.presentation.transaction_list.TransactionListScreen
import com.example.financeapp.ui.theme.FinanceappTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var recurringProcessor: com.example.financeapp.feature_transaction.domain.recurring.RecurringProcessor
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceappTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "transaction_list"
                    ) {
                        composable("transaction_list") {
                            TransactionListScreen(
                                onAddClick = { navController.navigate("add_edit_transaction") },
                                onEditClick = { id -> navController.navigate("add_edit_transaction?transactionId=$id") },
                                onOpenReports = { navController.navigate("reports") },
                                onOpenBudgets = { navController.navigate("budgets") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenScanReceipt = { navController.navigate("scan_receipt") }
                            )
                        }

                        composable("reports") {
                            ReportsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenTransactions = {
                                    // Go back to list if it's already in the back stack, otherwise navigate
                                    val popped = navController.popBackStack(route = "transaction_list", inclusive = false)
                                    if (!popped) navController.navigate("transaction_list") {
                                        popUpTo("transaction_list") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenBudgets = {
                                    navController.navigate("budgets") {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenSettings = {
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenScanReceipt = {
                                    navController.navigate("scan_receipt") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable("budgets") {
                            BudgetsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenTransactions = {
                                    val popped = navController.popBackStack(route = "transaction_list", inclusive = false)
                                    if (!popped) navController.navigate("transaction_list") {
                                        popUpTo("transaction_list") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenReports = {
                                    navController.navigate("reports") {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenSettings = {
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenScanReceipt = {
                                    navController.navigate("scan_receipt") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(
                            route = "add_edit_transaction?transactionId={transactionId}",
                            arguments = listOf(
                                androidx.navigation.navArgument("transactionId") {
                                    type = androidx.navigation.NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("transactionId")?.takeIf { it != -1 }
                            AddEditTransactionScreen(
                                transactionIdArg = id,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenTransactions = { navController.navigate("transaction_list") },
                                onOpenReports = { navController.navigate("reports") },
                                onOpenBudgets = { navController.navigate("budgets") },
                                onOpenScanReceipt = { navController.navigate("scan_receipt") }
                            )
                        }
                        composable("scan_receipt") {
                            ScanReceiptScreen(
                                onBack = { navController.popBackStack() },
                                onParsed = { parsed ->
                                    // Navigate to Add/Edit with prefilled query args
                                    val title = Uri.encode(parsed.merchant ?: "Receipt")
                                    val amount = parsed.amount ?: 0.0
                                    val date = parsed.dateMillis ?: System.currentTimeMillis()
                                    val category = Uri.encode(parsed.categorySuggestion ?: "Other")

                                    navController.navigate(
                                        "add_edit_transaction?transactionId=-1&" +
                                                "prefillTitle=$title&" +
                                                "prefillAmount=$amount&" +
                                                "prefillDate=$date&" +
                                                "prefillCategory=$category"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}