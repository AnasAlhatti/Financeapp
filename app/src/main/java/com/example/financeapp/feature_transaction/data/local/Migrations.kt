package com.example.financeapp.feature_transaction.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recurring_rules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `title` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `category` TEXT NOT NULL,
                    `startAt` INTEGER NOT NULL,
                    `frequency` TEXT NOT NULL,
                    `dayOfMonth` INTEGER,
                    `dayOfWeek` INTEGER,
                    `endAt` INTEGER,
                    `nextAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // NEW 3→4
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isRecurring` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurringRuleId` INTEGER")
        }
    }
}
