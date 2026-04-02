package com.linea.dialer.data.repository

import android.content.Context
import com.linea.dialer.data.db.*
import com.linea.dialer.data.model.ContactTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single repository for all Linea-specific data stored in Room:
 *  - Contact metadata (tags)
 *  - Text notes on contacts
 *  - Notes attached to specific calls
 *  - Follow-up reminders
 */
class NotesRepository(context: Context) {

    private val db          = LineaDatabase.getInstance(context)
    private val metaDao     = db.contactMetaDao()
    private val notesDao    = db.notesDao()
    private val remindersDao = db.remindersDao()
    private val callNotesDao = db.callNotesDao()

    // ── Tags ─────────────────────────────────────────────────────────────────

    suspend fun getTag(lookupKey: String): ContactTag = withContext(Dispatchers.IO) {
        val raw = metaDao.getByKey(lookupKey)?.tag ?: return@withContext ContactTag.UNKNOWN
        runCatching { ContactTag.valueOf(raw) }.getOrDefault(ContactTag.UNKNOWN)
    }

    suspend fun saveTag(lookupKey: String, tag: ContactTag) = withContext(Dispatchers.IO) {
        // Ensure meta row exists then update tag
        val existing = metaDao.getByKey(lookupKey)
        if (existing == null) {
            metaDao.upsert(ContactMetaEntity(lookupKey = lookupKey, tag = tag.name))
        } else {
            metaDao.updateTag(lookupKey, tag.name)
        }
    }

    /** Bulk fetch all tags — used when enriching the full contact list. */
    suspend fun getAllTags(): Map<String, ContactTag> = withContext(Dispatchers.IO) {
        metaDao.getAll().associate { entity ->
            entity.lookupKey to runCatching { ContactTag.valueOf(entity.tag) }.getOrDefault(ContactTag.UNKNOWN)
        }
    }

    // ── Contact Notes ─────────────────────────────────────────────────────────

    fun observeNotesForContact(lookupKey: String): Flow<List<NoteEntity>> =
        notesDao.observeForContact(lookupKey)

    suspend fun getNotesForContact(lookupKey: String): List<NoteEntity> =
        withContext(Dispatchers.IO) { notesDao.getForContact(lookupKey) }

    suspend fun getLatestNoteBody(lookupKey: String): String? =
        withContext(Dispatchers.IO) { notesDao.latestNoteBody(lookupKey) }

    suspend fun addNote(lookupKey: String, body: String, callLogId: Long? = null): Long =
        withContext(Dispatchers.IO) {
            // Ensure meta row exists
            if (metaDao.getByKey(lookupKey) == null) {
                metaDao.upsert(ContactMetaEntity(lookupKey = lookupKey))
            }
            notesDao.insert(NoteEntity(contactLookupKey = lookupKey, body = body, callLogId = callLogId))
        }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) { notesDao.update(note) }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) { notesDao.delete(id) }

    suspend fun searchNotes(query: String): List<NoteEntity> =
        withContext(Dispatchers.IO) { notesDao.search(query) }

    // ── Call Notes ────────────────────────────────────────────────────────────

    fun observeCallNotesForContact(lookupKey: String): Flow<List<CallNoteEntity>> =
        callNotesDao.observeForContact(lookupKey)

    suspend fun getCallNotesForContact(lookupKey: String): List<CallNoteEntity> =
        withContext(Dispatchers.IO) { callNotesDao.getForContact(lookupKey) }

    suspend fun saveCallNote(note: CallNoteEntity): Long =
        withContext(Dispatchers.IO) { callNotesDao.insert(note) }

    suspend fun deleteCallNote(id: Long) = withContext(Dispatchers.IO) { callNotesDao.delete(id) }

    // ── Reminders ─────────────────────────────────────────────────────────────

    fun observePendingReminders(): Flow<List<ReminderEntity>> =
        remindersDao.observePending()

    fun observeRemindersForContact(lookupKey: String): Flow<List<ReminderEntity>> =
        remindersDao.observeForContact(lookupKey)

    suspend fun getRemindersForContact(lookupKey: String): List<ReminderEntity> =
        withContext(Dispatchers.IO) { remindersDao.getForContact(lookupKey) }

    suspend fun insertReminder(reminder: ReminderEntity): Long =
        withContext(Dispatchers.IO) { remindersDao.insert(reminder) }

    suspend fun setReminderWorkId(id: Long, uuid: String) =
        withContext(Dispatchers.IO) { remindersDao.setWorkId(id, uuid) }

    suspend fun markReminderDone(id: Long) =
        withContext(Dispatchers.IO) { remindersDao.markDone(id) }

    suspend fun deleteReminder(id: Long) =
        withContext(Dispatchers.IO) { remindersDao.delete(id) }

    companion object {
        @Volatile private var instance: NotesRepository? = null
        fun getInstance(context: Context): NotesRepository =
            instance ?: synchronized(this) {
                instance ?: NotesRepository(context.applicationContext).also { instance = it }
            }
    }
}
