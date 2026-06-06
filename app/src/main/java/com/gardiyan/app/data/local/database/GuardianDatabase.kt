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
    version = 7,
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
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
    }
}
