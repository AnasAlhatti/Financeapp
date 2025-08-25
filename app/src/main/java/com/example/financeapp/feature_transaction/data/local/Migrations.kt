// feature_transaction/data/local/Migrations.kt
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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isRecurring` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurringRuleId` INTEGER")
        }
    }

    // NEW 4→5 for userId and remoteId
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // userId is non-null in the entity, so provide a DEFAULT to satisfy existing rows
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `remoteId` TEXT")
            // optional indices for query performance later:
            // db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_userId_date` ON `transactions`(`userId`, `date`)")
        }
    }
}
