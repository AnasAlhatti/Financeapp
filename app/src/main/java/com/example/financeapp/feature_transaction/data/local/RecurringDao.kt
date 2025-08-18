package com.example.financeapp.feature_transaction.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY nextAt ASC")
    fun getAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_rules WHERE nextAt <= :now ORDER BY nextAt ASC")
    suspend fun getDue(now: Long): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringEntity): Long

    @Delete
    suspend fun delete(rule: RecurringEntity)

    @Query("UPDATE recurring_rules SET nextAt = :nextAt WHERE id = :id")
    suspend fun updateNext(id: Int, nextAt: Long)
}
