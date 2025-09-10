package com.example.financeapp.feature_transaction.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity): Long

    @Delete
    suspend fun delete(entity: BudgetEntity)

    // SCOPE BY USER
    @Query("SELECT * FROM budgets WHERE userId = :uid ORDER BY category ASC")
    fun getBudgets(uid: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): BudgetEntity?

    // Clear only current user's rows and legacy nulls
    @Query("DELETE FROM budgets WHERE userId = :uid OR userId IS NULL")
    suspend fun clearForUser(uid: String)

    // Optional nuke
    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
