package com.example.financeapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.feature_transaction.presentation.add_edit.AddEditTransactionScreen
import com.example.financeapp.feature_transaction.presentation.budgets.BudgetsScreen
import com.example.financeapp.feature_transaction.presentation.recurring.ManageRecurringScreen
import com.example.financeapp.feature_transaction.presentation.reports.ReportsScreen
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
                                onEditClick = { id ->
                                    navController.navigate("add_edit_transaction?transactionId=$id")
                                },
                                onOpenReports = { navController.navigate("reports") },
                                onOpenBudgets = { navController.navigate("budgets") }
                            )
                        }
                        composable("budgets") {
                            BudgetsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("reports") {
                            ReportsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("recurring") {
                            ManageRecurringScreen(onBack = { navController.popBackStack() })
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
                    }
                }
            }
        }
    }
}