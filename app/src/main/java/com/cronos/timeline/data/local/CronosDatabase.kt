package com.cronos.timeline.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TimelineBlockEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CronosDatabase : RoomDatabase() {

    abstract fun timelineBlockDao(): TimelineBlockDao

    companion object {
        @Volatile
        private var INSTANCE: CronosDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timeline_blocks ADD COLUMN category TEXT NOT NULL DEFAULT 'WORK'"
                )
                db.execSQL(
                    "ALTER TABLE timeline_blocks ADD COLUMN categoryColorHex INTEGER NOT NULL DEFAULT 4281909240"
                )
                db.execSQL(
                    """
                    UPDATE timeline_blocks SET
                        category = CASE categoryIndex
                            WHEN 1 THEN 'MEETING'
                            WHEN 2 THEN 'PERSONAL'
                            WHEN 3 THEN 'URGENT'
                            WHEN 4 THEN 'VOICE_NOTE'
                            ELSE 'WORK'
                        END,
                        categoryColorHex = CASE categoryIndex
                            WHEN 1 THEN 4289170426
                            WHEN 2 THEN 4281652121
                            WHEN 3 THEN 4294669189
                            WHEN 4 THEN 4294286859
                            ELSE 4281909240
                        END
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timeline_blocks ADD COLUMN customColorHex INTEGER DEFAULT NULL"
                )
            }
        }

        fun getInstance(context: Context): CronosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CronosDatabase::class.java,
                    "cronos_timeline.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
