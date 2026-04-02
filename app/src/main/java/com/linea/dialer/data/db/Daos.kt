package com.linea.dialer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Contact Metadata ─────────────────────────────────────────────────────────
@Dao
interface ContactMetaDao {

    @Query("SELECT * FROM contact_meta WHERE lookupKey = :key LIMIT 1")
    suspend fun getByKey(key: String): ContactMetaEntity?

    @Query("SELECT * FROM contact_meta")
    suspend fun getAll(): List<ContactMetaEntity>

    @Query("SELECT * FROM contact_meta")
    fun observeAll(): Flow<List<ContactMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContactMetaEntity)

    @Query("UPDATE contact_meta SET tag = :tag, updatedAt = :now WHERE lookupKey = :key")
    suspend fun updateTag(key: String, tag: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM contact_meta WHERE lookupKey = :key")
    suspend fun delete(key: String)
}

// ── Notes ────────────────────────────────────────────────────────────────────
@Dao
interface NotesDao {

    @Query("SELECT * FROM notes WHERE contactLookupKey = :key ORDER BY createdAt DESC")
    fun observeForContact(key: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE contactLookupKey = :key ORDER BY createdAt DESC")
    suspend fun getForContact(key: String): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE body LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun search(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes WHERE contactLookupKey = :key")
    suspend fun deleteAllForContact(key: String)

    // Latest note body for a contact (used on contact list to show preview)
    @Query("SELECT body FROM notes WHERE contactLookupKey = :key ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestNoteBody(key: String): String?
}

// ── Reminders ────────────────────────────────────────────────────────────────
@Dao
interface RemindersDao {

    @Query("SELECT * FROM reminders WHERE isDone = 0 ORDER BY scheduledAt ASC")
    fun observePending(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY scheduledAt DESC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE contactLookupKey = :key ORDER BY scheduledAt DESC")
    suspend fun getForContact(key: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE contactLookupKey = :key ORDER BY scheduledAt DESC")
    fun observeForContact(key: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Long)

    @Query("UPDATE reminders SET workRequestId = :uuid WHERE id = :id")
    suspend fun setWorkId(id: Long, uuid: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reminders WHERE isDone = 1")
    suspend fun clearCompleted()
}

// ── Call Notes ───────────────────────────────────────────────────────────────
@Dao
interface CallNotesDao {

    @Query("SELECT * FROM call_notes ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<CallNoteEntity>

    @Query("SELECT * FROM call_notes WHERE contactLookupKey = :key ORDER BY createdAt DESC")
    fun observeForContact(key: String): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes WHERE contactLookupKey = :key ORDER BY createdAt DESC")
    suspend fun getForContact(key: String): List<CallNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: CallNoteEntity): Long

    @Query("DELETE FROM call_notes WHERE id = :id")
    suspend fun delete(id: Long)
}
