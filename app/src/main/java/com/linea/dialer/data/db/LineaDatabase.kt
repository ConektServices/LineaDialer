package com.linea.dialer.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [
        ContactMetaEntity::class,
        NoteEntity::class,
        ReminderEntity::class,
        CallNoteEntity::class,
    ],
    version   = 1,
    exportSchema = true,
)
abstract class LineaDatabase : RoomDatabase() {

    abstract fun contactMetaDao(): ContactMetaDao
    abstract fun notesDao(): NotesDao
    abstract fun remindersDao(): RemindersDao
    abstract fun callNotesDao(): CallNotesDao

    companion object {
        @Volatile private var INSTANCE: LineaDatabase? = null

        fun getInstance(context: Context): LineaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LineaDatabase::class.java,
                    "linea.db"
                )
                // Wipe + rebuild on destructive migration (safe for v1 launch)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
