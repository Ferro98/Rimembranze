package com.example.rimembranze.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ItemEntity::class,
        DeadlineEntity::class,
        RecordEntity::class,
        AppointmentEntity::class
        ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun recordDao(): RecordDao
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Aggiunge notes a deadlines (se non esiste già — idempotente)
                try {
                    db.execSQL("ALTER TABLE deadlines ADD COLUMN notes TEXT")
                } catch (_: Exception) {
                    // Colonna già presente (es. su dispositivi che avevano il DB ricreato)
                }

                // Crea la tabella appointments
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS appointments (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId           INTEGER NOT NULL,
                        title            TEXT NOT NULL,
                        dateEpochMs      INTEGER NOT NULL,
                        notes            TEXT,
                        amountCents      INTEGER,
                        isDone           INTEGER NOT NULL DEFAULT 0,
                        isPaid           INTEGER NOT NULL DEFAULT 0,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY (itemId) REFERENCES items(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_appointments_itemId ON appointments(itemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_appointments_dateEpochMs ON appointments(dateEpochMs)")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rimembranze.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}