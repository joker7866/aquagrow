package com.aquagrow.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquagrow.data.local.db.ALL_MIGRATIONS
import com.aquagrow.data.local.db.AquaGrowDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test pattern. Room checks each migrated schema against the exported JSON in
 * app/schemas. When MIGRATION_1_2 is added:
 *  1. create v1 with [helper] and insert a few rows with raw SQL,
 *  2. `helper.runMigrationsAndValidate(db, 2, true, MIGRATION_1_2)`,
 *  3. read the rows back and assert nothing was lost.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val db = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AquaGrowDatabase::class.java)

    @Test
    fun version1OpensWithAllMigrationsAndKeepsRows() {
        helper.createDatabase(db, 1).use { v1 ->
            v1.execSQL(
                "INSERT INTO plants (id, name, category, plantingDate, growthStages, currentStageIndex, archived, createdAt, updatedAt) " +
                    "VALUES (1, 'Tomato', 'VEGETABLE', 20700, '[]', 0, 0, 0, 0)",
            )
        }
        // Opening through Room runs every migration up to the current version and validates the schema.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(context, AquaGrowDatabase::class.java, db).addMigrations(*ALL_MIGRATIONS).build()
        room.openHelper.writableDatabase.query("SELECT name FROM plants WHERE id = 1").use { c ->
            check(c.moveToFirst() && c.getString(0) == "Tomato") { "row lost during migration" }
        }
        room.close()
    }
}
