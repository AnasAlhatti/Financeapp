package com.example.financeapp.feature_transaction.domain.use_case

data class TransactionUseCases(
    val getTransactions: GetTransactions,
    val getTransactionById: GetTransactionById,
    val addTransaction: AddTransaction,
    val deleteTransaction: DeleteTransaction
)
