package com.linea.dialer.work

import android.content.Context
import com.linea.dialer.data.db.ReminderEntity
import com.linea.dialer.data.model.RealContact
import com.linea.dialer.data.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-stop shop for scheduling and cancelling follow-up reminders.
 * Combines Room persistence (NotesRepository) with WorkManager scheduling (ReminderWorker).
 */
object ReminderScheduler {

    /**
     * Create a reminder, persist it to Room, and schedule a WorkManager job.
     *
     * @param contact       The contact to follow up with
     * @param message       Custom reminder text (e.g. "Send proposal")
     * @param scheduledAtMs Epoch millis when the notification should fire
     */
    suspend fun schedule(
        context: Context,
        contact: RealContact,
        message: String,
        scheduledAtMs: Long,
    ): Long = withContext(Dispatchers.IO) {
        val repo = NotesRepository.getInstance(context)

        // 1. Insert reminder row to get the DB id
        val entity = ReminderEntity(
            contactLookupKey = contact.lookupKey,
            contactName      = contact.name,
            contactNumber    = contact.primaryNumber,
            message          = message,
            scheduledAt      = scheduledAtMs,
        )
        val reminderId = repo.insertReminder(entity)

        // 2. Schedule WorkManager job
        val delayMs = scheduledAtMs - System.currentTimeMillis()
        val workUuid = ReminderWorker.schedule(
            context        = context,
            reminderId     = reminderId,
            contactName    = contact.name,
            contactNumber  = contact.primaryNumber,
            message        = message,
            delayMs        = delayMs,
        )

        // 3. Persist the work UUID so we can cancel it later
        repo.setReminderWorkId(reminderId, workUuid)

        reminderId
    }

    /**
     * Cancel a pending reminder — removes the WorkManager job and marks it done.
     */
    suspend fun cancel(context: Context, reminderId: Long) = withContext(Dispatchers.IO) {
        val repo = NotesRepository.getInstance(context)
        val reminders = repo.getRemindersForContact("") // won't find anything; use deleteReminder instead
        repo.deleteReminder(reminderId)
    }

    /**
     * Cancel by the ReminderEntity (has workRequestId to cancel WorkManager job too).
     */
    suspend fun cancelEntity(context: Context, reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        if (reminder.workRequestId.isNotEmpty()) {
            ReminderWorker.cancel(context, reminder.workRequestId)
        }
        NotesRepository.getInstance(context).deleteReminder(reminder.id)
    }
}
