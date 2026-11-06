package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        OsintReportEntity::class,
        VpnProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun osintReportDao(): OsintReportDao
    abstract fun vpnProfileDao(): VpnProfileDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `osint_reports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `target` TEXT NOT NULL, `rawData` TEXT NOT NULL, `summary` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `vpn_profiles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `serverHost` TEXT NOT NULL, `serverPort` INTEGER NOT NULL, `protocol` TEXT NOT NULL, `countryCode` TEXT NOT NULL, `username` TEXT NOT NULL, `dnsServer` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openv_browser.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
