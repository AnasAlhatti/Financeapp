package com.example.financeapp.feature_transaction.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity): Long

    @Delete
    suspend fun delete(entity: BudgetEntity)

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): BudgetEntity?

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
