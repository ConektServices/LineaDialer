package com.linea.dialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles the "Decline" action button on the incoming call notification.
 * Registered in AndroidManifest.xml for ACTION_DECLINE_CALL.
 */
class DeclineCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LineaInCallService.ACTION_DECLINE) {
            Log.d("DeclineCallReceiver", "Declining call via notification button")
            CallManager.endCall()
        }
    }
}