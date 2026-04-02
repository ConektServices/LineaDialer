package com.linea.dialer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.linea.dialer.data.db.LineaDatabase
import com.linea.dialer.work.ReminderWorker

class LineaApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        LineaDatabase.getInstance(this)
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Incoming calls — IMPORTANCE_HIGH so the full-screen intent fires
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_INCOMING,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Linea incoming call alerts"
                    enableVibration(true)
                    setShowBadge(false)
                    setBypassDnd(true)
                }
            )

            // Ongoing calls — low importance, just a persistent status bar icon
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ONGOING,
                    "Ongoing Calls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Active call indicator"
                    setShowBadge(false)
                }
            )

            // Reminders
            nm.createNotificationChannel(
                NotificationChannel(
                    ReminderWorker.CHANNEL_ID,
                    "Follow-up Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Linea follow-up call reminders"
                    enableVibration(true)
                }
            )
        }
    }

    companion object {
        const val CHANNEL_INCOMING = "linea_incoming"
        const val CHANNEL_ONGOING  = "linea_ongoing"
    }
}