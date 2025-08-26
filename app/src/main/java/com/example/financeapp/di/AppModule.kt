package com.example.financeapp.di

import android.app.Application
import androidx.room.Room
import com.example.financeapp.feature_auth.data.FirebaseAuthRepository
import com.example.financeapp.feature_auth.domain.repository.AuthRepository
import com.example.financeapp.feature_auth.domain.use_case.AuthUseCases
import com.example.financeapp.feature_auth.domain.use_case.ObserveAuthState
import com.example.financeapp.feature_auth.domain.use_case.RegisterEmail
import com.example.financeapp.feature_auth.domain.use_case.SignInEmail
import com.example.financeapp.feature_auth.domain.use_case.SignInGoogle
import com.example.financeapp.feature_auth.domain.use_case.SignOut
import com.example.financeapp.feature_transaction.data.local.BudgetDao
import com.example.financeapp.feature_transaction.data.local.Migrations
import com.example.financeapp.feature_transaction.data.local.RecurringDao
import com.example.financeapp.feature_transaction.data.local.TransactionDao
import com.example.financeapp.feature_transaction.data.local.TransactionDatabase
import com.example.financeapp.feature_transaction.data.remote.BudgetRemoteDataSource
import com.example.financeapp.feature_transaction.data.remote.TransactionRemoteDataSource
import com.example.financeapp.feature_transaction.data.repository.BudgetRepositoryImpl
import com.example.financeapp.feature_transaction.data.repository.RecurringRepositoryImpl
import com.example.financeapp.feature_transaction.domain.recurring.RecurringProcessor
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepositoryImpl
import com.example.financeapp.feature_transaction.domain.repository.BudgetRepository
import com.example.financeapp.feature_transaction.domain.repository.RecurringRepository
import com.example.financeapp.feature_transaction.domain.repository.TransactionRepository
import com.example.financeapp.feature_transaction.domain.use_case.recurring.DeleteRecurring
import com.example.financeapp.feature_transaction.domain.use_case.recurring.GetRecurring
import com.example.financeapp.feature_transaction.domain.use_case.recurring.RecurringUseCases
import com.example.financeapp.feature_transaction.domain.use_case.recurring.UpsertRecurring
import com.example.financeapp.feature_transaction.domain.use_case.transaction.AddTransaction
import com.example.financeapp.feature_transaction.domain.use_case.transaction.DeleteTransaction
import com.example.financeapp.feature_transaction.domain.use_case.transaction.GetTransactionById
import com.example.financeapp.feature_transaction.domain.use_case.transaction.GetTransactions
import com.example.financeapp.feature_transaction.domain.use_case.transaction.TransactionUseCases
import com.example.financeapp.feature_transaction.presentation.budgets.BudgetAlertStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        return Room.databaseBuilder(app, TransactionDatabase::class.java, "transaction_db")
            .addMigrations(
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6
            )
            // .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: TransactionDatabase) = db.transactionDao

    @Singleton
    @Provides
    fun provideTransactionRepository(
        dao: TransactionDao,
        remote: TransactionRemoteDataSource,
        auth: FirebaseAuth
    ): TransactionRepository = TransactionRepositoryImpl(dao, remote, auth)

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
    fun provideBudgetRepository(
        dao: BudgetDao,
        remote: BudgetRemoteDataSource,
        auth: FirebaseAuth
    ): BudgetRepository = BudgetRepositoryImpl(dao, remote, auth)

    @Provides @Singleton
    fun provideBudgetRemote(ds: FirebaseFirestore) = BudgetRemoteDataSource(ds)

    // DAOs
    @Provides @Singleton
    fun provideRecurringDao(db: TransactionDatabase) = db.recurringDao

    // Repository
    @Provides @Singleton
    fun provideRecurringRepository(dao: RecurringDao)
            : RecurringRepository =
        RecurringRepositoryImpl(dao)

    // Use cases
    @Provides @Singleton
    fun provideRecurringUseCases(
        repo: RecurringRepository
    ) = RecurringUseCases(
        getRecurring = GetRecurring(repo),
        upsertRecurring = UpsertRecurring(repo),
        deleteRecurring = DeleteRecurring(repo)
    )

    // Processor
    @Provides @Singleton
    fun provideRecurringProcessor(
        recurringRepo: RecurringRepository,
        addTransaction: AddTransaction,
        txRepo: TransactionRepository
    ): RecurringProcessor =
        RecurringProcessor(
            recurringRepo, addTransaction, txRepo
        )

    @Provides @Singleton
    fun provideBudgetAlertStore(
        @dagger.hilt.android.qualifiers.ApplicationContext ctx: android.content.Context
    ) = BudgetAlertStore(ctx)

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository = FirebaseAuthRepository(auth)

    // Auth UseCases
    @Provides @Singleton
    fun provideAuthUseCases(repo: AuthRepository) = AuthUseCases(
        observeAuthState = ObserveAuthState(repo),
        signInEmail = SignInEmail(repo),
        registerEmail = RegisterEmail(repo),
        signInGoogle = SignInGoogle(repo),
        signOut = SignOut(repo)
    )

    // di/AppModule.kt
    @Provides fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Singleton
    @Provides
    fun provideTransactionRemoteDataSource(db: FirebaseFirestore) =
        TransactionRemoteDataSource(db)

}
