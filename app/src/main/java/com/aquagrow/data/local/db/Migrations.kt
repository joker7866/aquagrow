package com.aquagrow.data.local.db

import androidx.room.migration.Migration

/**
 * Every schema change after the first public release gets a [Migration] here, and a matching
 * case in `MigrationTest` (androidTest). Never use destructive fallback: users would lose their
 * garden and tank history.
 *
 * Pattern for the first change:
 * ```
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE plants ADD COLUMN sunlightHours REAL")
 *     }
 * }
 * ```
 * then bump `AquaGrowDatabase` to version 2, add it to [ALL_MIGRATIONS] and commit the new
 * `app/schemas/.../2.json`.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf()
