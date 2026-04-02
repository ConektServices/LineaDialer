package com.linea.dialer.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.linea.dialer.MainActivity
import com.linea.dialer.R
import com.linea.dialer.data.repository.NotesRepository
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that fires a follow-up reminder notification.
 *
 * Input data keys:
 *   REMINDER_ID      — Long — Row ID in reminders table
 *   CONTACT_NAME     — String
 *   CONTACT_NUMBER   — String
 *   MESSAGE          — String
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId    = inputData.getLong(KEY_REMINDER_ID, -1L)
        val contactName   = inputData.getString(KEY_CONTACT_NAME)  ?: "Contact"
        val contactNumber = inputData.getString(KEY_CONTACT_NUMBER) ?: ""
        val message       = inputData.getString(KEY_MESSAGE)        ?: "Time to follow up"

        // Show notification
        createChannel()
        showNotification(reminderId, contactName, contactNumber, message)

        // Mark reminder done in DB
        if (reminderId != -1L) {
            runCatching {
                NotesRepository.getInstance(applicationContext).markReminderDone(reminderId)
            }
        }

        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow-up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Linea follow-up call reminders"
                enableVibration(true)
            }
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        reminderId: Long,
        name: String,
        number: String,
        message: String,
    ) {
        val openIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId.toInt(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_number", number)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callIntent = PendingIntent.getActivity(
            applicationContext,
            (reminderId + 1000).toInt(),
            Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Follow up with $name")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_call, "Call Now", callIntent)
            .build()

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true

        if (hasPermission) {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIF_BASE_ID + reminderId.toInt(), notification)
        }
    }

    companion object {
        const val KEY_REMINDER_ID      = "reminder_id"
        const val KEY_CONTACT_NAME     = "contact_name"
        const val KEY_CONTACT_NUMBER   = "contact_number"
        const val KEY_MESSAGE          = "message"
        const val CHANNEL_ID           = "linea_reminders"
        const val NOTIF_BASE_ID        = 2000

        /**
         * Schedule a one-time reminder.
         * Returns the WorkRequest UUID string.
         */
        fun schedule(
            context: Context,
            reminderId: Long,
            contactName: String,
            contactNumber: String,
            message: String,
            delayMs: Long,
        ): String {
            val data = workDataOf(
                KEY_REMINDER_ID      to reminderId,
                KEY_CONTACT_NAME     to contactName,
                KEY_CONTACT_NUMBER   to contactNumber,
                KEY_MESSAGE          to message,
            )
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(data)
                .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .addTag("reminder_$reminderId")
                .build()

            WorkManager.getInstance(context).enqueue(request)
            return request.id.toString()
        }

        /** Cancel a previously scheduled reminder by its WorkManager UUID. */
        fun cancel(context: Context, workRequestId: String) {
            WorkManager.getInstance(context)
                .cancelWorkById(java.util.UUID.fromString(workRequestId))
        }
    }
}
