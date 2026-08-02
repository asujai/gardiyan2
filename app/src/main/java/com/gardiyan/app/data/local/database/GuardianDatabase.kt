package com.gardiyan.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gardiyan.app.data.local.dao.GuardianDao
import com.gardiyan.app.data.local.entity.ActiveUsageSessionEntity
import com.gardiyan.app.data.local.entity.FriendEntity
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.local.entity.StatusLogEntity
import com.gardiyan.app.data.local.entity.UserSessionEntity

@Database(
    entities = [
        UserSessionEntity::class,
        FriendEntity::class,
        StatusLogEntity::class,
        RestrictedAppEntity::class,
        ActiveUsageSessionEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun guardianDao(): GuardianDao

    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        fun getDatabase(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_db"
                )
                .addMigrations(
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restricted_apps ADD COLUMN lastResetDate TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `active_usage_session` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`appId` INTEGER NOT NULL, " +
                    "`packageName` TEXT NOT NULL, " +
                    "`appName` TEXT NOT NULL, " +
                    "`entryAtMillis` INTEGER NOT NULL, " +
                    "`lastSeenAtMillis` INTEGER NOT NULL, " +
                    "`isActive` INTEGER NOT NULL, " +
                    "`createdAtMillis` INTEGER NOT NULL, " +
                    "`updatedAtMillis` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restricted_apps ADD COLUMN nextDayLimitMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restricted_apps ADD COLUMN nextDayActiveDays TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restricted_apps ADD COLUMN lastLimitUpdateDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE restricted_apps ADD COLUMN todayMinLimitMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "usageStatsBaselineMillisToday",
                    "ALTER TABLE restricted_apps ADD COLUMN usageStatsBaselineMillisToday INTEGER NOT NULL DEFAULT -1"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "lastUsageStatsObservedMillisToday",
                    "ALTER TABLE restricted_apps ADD COLUMN lastUsageStatsObservedMillisToday INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "lastUsageStatsReconciledAtMillis",
                    "ALTER TABLE restricted_apps ADD COLUMN lastUsageStatsReconciledAtMillis INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "usageStatsBaselineMillisToday",
                    "ALTER TABLE restricted_apps ADD COLUMN usageStatsBaselineMillisToday INTEGER NOT NULL DEFAULT -1"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "lastUsageStatsObservedMillisToday",
                    "ALTER TABLE restricted_apps ADD COLUMN lastUsageStatsObservedMillisToday INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "lastUsageStatsReconciledAtMillis",
                    "ALTER TABLE restricted_apps ADD COLUMN lastUsageStatsReconciledAtMillis INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "active_usage_session",
                    "entryElapsedRealtime",
                    "ALTER TABLE active_usage_session ADD COLUMN entryElapsedRealtime INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "active_usage_session",
                    "lastSeenElapsedRealtime",
                    "ALTER TABLE active_usage_session ADD COLUMN lastSeenElapsedRealtime INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "restrictionGroupId",
                    "ALTER TABLE restricted_apps ADD COLUMN restrictionGroupId TEXT NOT NULL DEFAULT ''"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "restrictionName",
                    "ALTER TABLE restricted_apps ADD COLUMN restrictionName TEXT NOT NULL DEFAULT ''"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "activeWindowEnabled",
                    "ALTER TABLE restricted_apps ADD COLUMN activeWindowEnabled INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "activeStartMinutes",
                    "ALTER TABLE restricted_apps ADD COLUMN activeStartMinutes INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "restricted_apps",
                    "activeEndMinutes",
                    "ALTER TABLE restricted_apps ADD COLUMN activeEndMinutes INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE restricted_apps SET restrictionGroupId = packageName WHERE restrictionGroupId = ''")
                db.execSQL("UPDATE restricted_apps SET restrictionName = appName WHERE restrictionName = ''")
            }
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            alterSql: String
        ) {
            var exists = false
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        exists = true
                        break
                    }
                }
            }
            if (!exists) {
                db.execSQL(alterSql)
            }
        }
    }
}
