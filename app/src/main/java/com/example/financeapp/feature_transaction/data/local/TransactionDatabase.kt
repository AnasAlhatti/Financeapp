package com.example.financeapp.feature_transaction.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringEntity::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2) // keep your existing auto-migration
    ]
)
abstract class TransactionDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val budgetDao: BudgetDao
    abstract val recurringDao: RecurringDao   // ✅ expose DAO
}
