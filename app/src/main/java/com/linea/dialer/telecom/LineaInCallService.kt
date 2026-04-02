package com.linea.dialer.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.linea.dialer.LineaApp
import com.linea.dialer.data.repository.ContactsRepository
import com.linea.dialer.ui.call.IncomingCallActivity
import kotlinx.coroutines.*

private const val TAG = "LineaInCallService"

class LineaInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        Log.d(TAG, "onCallAdded state=${call.state} number=$number")

        scope.launch {
            val contact = runCatching {
                ContactsRepository.getInstance(applicationContext).findByNumber(number)
            }.getOrNull()

            withContext(Dispatchers.Main) {
                CallManager.onCallAdded(call, contact)

                when (call.state) {
                    Call.STATE_RINGING -> {
                        Log.d(TAG, "Incoming call — showing notification")
                        showIncomingCallNotification(number, contact?.name)
                    }
                    Call.STATE_DIALING,
                    Call.STATE_ACTIVE -> {
                        Log.d(TAG, "Outgoing/active call")
                        showOngoingCallNotification(number, contact?.name)
                    }
                }
            }
        }

        // Register callback so we track state changes on this call
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                Log.d(TAG, "onStateChanged state=$state")
                when (state) {
                    Call.STATE_ACTIVE -> showOngoingCallNotification(
                        c.details?.handle?.schemeSpecificPart ?: "", null
                    )
                    Call.STATE_DISCONNECTED,
                    Call.STATE_DISCONNECTING -> dismissAllNotifications()
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved")
        CallManager.onCallRemoved(call)
        dismissAllNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ── Incoming call — full-screen intent + heads-up notification ────────────

    private fun showIncomingCallNotification(number: String, callerName: String?) {
        ensureChannels()

        // Full-screen intent — launches IncomingCallActivity over lock screen
        val fullScreenIntent = PendingIntent.getActivity(
            this, NOTIF_INCOMING,
            Intent(this, IncomingCallActivity::class.java).apply {
                putExtra(IncomingCallActivity.EXTRA_NUMBER, number)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline button on notification
        val declineIntent = PendingIntent.getBroadcast(
            this, NOTIF_INCOMING + 1,
            Intent(ACTION_DECLINE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer button on notification
        val answerIntent = PendingIntent.getActivity(
            this, NOTIF_INCOMING + 2,
            Intent(this, IncomingCallActivity::class.java).apply {
                putExtra(IncomingCallActivity.EXTRA_NUMBER, number)
                putExtra(IncomingCallActivity.EXTRA_AUTO_ANSWER, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, LineaApp.CHANNEL_INCOMING)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(callerName ?: number)
            .setContentText("Incoming call")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // This is the critical line — makes Android show the full-screen UI
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declineIntent)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerIntent)
            .build()

        // startForeground keeps the service alive during the call
        try {
            startForeground(NOTIF_INCOMING, notification)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}")
            getSystemService(NotificationManager::class.java)?.notify(NOTIF_INCOMING, notification)
        }
    }

    // ── Ongoing call notification ─────────────────────────────────────────────

    private fun showOngoingCallNotification(number: String, contactName: String?) {
        ensureChannels()

        val openIntent = PendingIntent.getActivity(
            this, NOTIF_ONGOING,
            Intent(this, com.linea.dialer.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_call", number)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, LineaApp.CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Call in progress")
            .setContentText(contactName ?: number)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .build()

        try {
            startForeground(NOTIF_ONGOING, notification)
        } catch (e: Exception) {
            getSystemService(NotificationManager::class.java)?.notify(NOTIF_ONGOING, notification)
        }
    }

    private fun dismissAllNotifications() {
        runCatching {
            stopForeground(STOP_FOREGROUND_REMOVE)
            val nm = getSystemService(NotificationManager::class.java)
            nm.cancel(NOTIF_INCOMING)
            nm.cancel(NOTIF_ONGOING)
        }
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    LineaApp.CHANNEL_INCOMING,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setShowBadge(false)
                    enableVibration(true)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    LineaApp.CHANNEL_ONGOING,
                    "Ongoing Calls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    companion object {
        const val NOTIF_INCOMING   = 1001
        const val NOTIF_ONGOING    = 1002
        const val ACTION_DECLINE   = "com.linea.dialer.ACTION_DECLINE_CALL"
    }
}