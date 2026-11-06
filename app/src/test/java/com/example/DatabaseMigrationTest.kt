package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {
    @Test fun updatePreservesBookmarksAndHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, url TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, url TEXT NOT NULL, visitedAt INTEGER NOT NULL, isPrivate INTEGER NOT NULL)")
            old.execSQL("INSERT INTO bookmarks VALUES (1, 'Favorito salvo', 'https://example.com', 10)")
            old.execSQL("INSERT INTO history VALUES (1, 'Visita salva', 'https://example.com', 20, 0)")
            old.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val db = database.openHelper.writableDatabase
            db.query("SELECT title FROM bookmarks WHERE id=1").use { cursor ->
                cursor.moveToFirst(); assertEquals("Favorito salvo", cursor.getString(0))
            }
            db.query("SELECT title FROM history WHERE id=1").use { cursor ->
                cursor.moveToFirst(); assertEquals("Visita salva", cursor.getString(0))
            }
            db.execSQL("INSERT INTO osint_reports VALUES (1, 'Consulta', 'DNS', 'example.com', '{}', 'OK', 30)")
            db.execSQL("INSERT INTO vpn_profiles VALUES ('test', 'Perfil', 'example.com', 51820, 'WIREGUARD', 'BR', '', '1.1.1.1', 0)")
            assertEquals(2, db.version)
        } finally {
            database.close(); context.deleteDatabase(name)
        }
    }
}
