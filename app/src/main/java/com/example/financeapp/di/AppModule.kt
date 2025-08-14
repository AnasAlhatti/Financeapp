package com.example.financeapp.di

import android.app.Application
import androidx.room.Room
import com.example.financeapp.feature_transaction.data.local.BudgetDao
import com.example.financeapp.feature_transaction.data.local.TransactionDao
import com.example.financeapp.feature_transaction.data.local.TransactionDatabase
import com.example.financeapp.feature_transaction.data.repository.BudgetRepositoryImpl
import com.example.financeapp.feature_transaction.data.repository.TransactionRepositoryImpl
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository
import com.example.financeapp.feature_transaction.domain.use_case.AddTransaction
import com.example.financeapp.feature_transaction.domain.use_case.DeleteTransaction
import com.example.financeapp.feature_transaction.domain.use_case.GetTransactionById
import com.example.financeapp.feature_transaction.domain.use_case.GetTransactions
import com.example.financeapp.feature_transaction.domain.use_case.TransactionUseCases
import com.example.financeapp.feature_transaction.domain.use_case.budget.BudgetUseCases
import com.example.financeapp.feature_transaction.domain.use_case.budget.DeleteBudget
import com.example.financeapp.feature_transaction.domain.use_case.budget.GetBudgets
import com.example.financeapp.feature_transaction.domain.use_case.budget.UpsertBudget
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTransactionDatabase(app: Application): TransactionDatabase {
        return Room.databaseBuilder(
            app,
            TransactionDatabase::class.java,
            "transaction_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: TransactionDatabase) = db.transactionDao

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository {
        return TransactionRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideTransactionUseCases(repository: TransactionRepository): TransactionUseCases {
        return TransactionUseCases(
            getTransactions = GetTransactions(repository),
            getTransactionById = GetTransactionById(repository),
            addTransaction = AddTransaction(repository),
            deleteTransaction = DeleteTransaction(repository)
        )
    }

    @Provides @Singleton
    fun provideBudgetDao(db: TransactionDatabase) = db.budgetDao

    @Provides @Singleton
    fun provideBudgetRepository(dao: BudgetDao): BudgetRepository =
        BudgetRepositoryImpl(dao)

}
